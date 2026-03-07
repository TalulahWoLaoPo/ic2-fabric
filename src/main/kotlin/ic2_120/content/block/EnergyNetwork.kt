package ic2_120.content.block

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import team.reborn.energy.api.EnergyStorage
import java.util.PriorityQueue

/**
 * 电网：一组相互连接的导线共享的能量池。
 *
 * 发电机通过任意成员导线的 [EnergyStorage] 向池中注入能量；
 * 每 game tick 电网自动按“消费者拉取”模型为边界处用电者供电。
 *
 * 新传输模型：
 * - 对每个消费者，统计到所有供电者（supportsExtraction）的路径；
 * - 按路径损耗（milliEU 总和）从小到大尝试拉取；
 * - 单条路径每 tick 最大可输送量 = 路径上剩余容量最小导线的剩余容量；
 * - 导线一旦被路径使用，会扣减该导线本 tick 临时容量，避免多路径叠加超载。
 */
class EnergyNetwork : SnapshotParticipant<Long>() {

    /** 所有成员导线位置（packed [BlockPos.asLong]）。 */
    val cables = mutableSetOf<Long>()
    var energy: Long = 0
    var capacity: Long = 0
    /** 每根导线的损耗 (milliEU)。 */
    private val cableLossMilliEu = mutableMapOf<Long, Long>()
    /** 拓扑缓存：导线邻接、边界连接、每根导线速率。 */
    private var topologyCache: TopologyCache? = null
    /** 按消费者入口导线集合缓存最短路结果（仅拓扑不变时可复用）。 */
    private val dijkstraCacheByEntries = mutableMapOf<String, DijkstraResult>()
    /** 按消费者入口导线集合缓存到全体导线的候选路径（按损耗升序）。 */
    private val bufferedCandidatesCacheByEntries = mutableMapOf<String, List<PathCandidate>>()
    var lastTickTime: Long = -1

    override fun createSnapshot(): Long = energy
    override fun readSnapshot(snapshot: Long) { energy = snapshot }

    fun addCable(pos: BlockPos, transferRate: Long, lossMilliEu: Long) {
        val key = pos.asLong()
        cables.add(key)
        capacity += transferRate
        cableLossMilliEu[key] = lossMilliEu
        invalidatePathCaches()
    }

    /** 导线邻接边界发生变化（如相邻机器放置/移除）时，刷新拓扑与路径缓存。 */
    fun invalidateConnectionCaches() {
        invalidatePathCaches()
    }

    fun insert(maxAmount: Long, transaction: TransactionContext): Long {
        val space = (capacity - energy).coerceAtLeast(0)
        val toInsert = minOf(maxAmount, space)
        if (toInsert > 0) {
            updateSnapshots(transaction)
            energy += toInsert
        }
        return toInsert
    }

    fun extract(maxAmount: Long, transaction: TransactionContext): Long {
        val toExtract = minOf(maxAmount, energy).coerceAtLeast(0)
        if (toExtract > 0) {
            updateSnapshots(transaction)
            energy -= toExtract
        }
        return toExtract
    }

    /** 由任一成员导线的 tick 触发；同一 game tick 仅执行一次。 */
    fun tickIfNeeded(world: World) {
        val time = world.time
        if (lastTickTime == time) return
        lastTickTime = time
        pushToConsumers(world)
    }

    private fun pushToConsumers(world: World) {
        if (cables.isEmpty()) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        val consumers = mutableMapOf<Long, Endpoint>()
        val providers = mutableMapOf<Long, Endpoint>()

        for (boundary in topology.boundaries) {
            val neighborPos = BlockPos.fromLong(boundary.neighborPosLong)
            val storage = EnergyStorage.SIDED.find(world, neighborPos, boundary.lookupFromNeighborSide) ?: continue
            if (storage.supportsInsertion()) {
                val endpoint = consumers.getOrPut(boundary.neighborPosLong) { Endpoint(storage) }
                endpoint.entryCables.add(boundary.cablePosLong)
            }
            if (storage.supportsExtraction()) {
                val endpoint = providers.getOrPut(boundary.neighborPosLong) { Endpoint(storage) }
                endpoint.entryCables.add(boundary.cablePosLong)
            }
        }

        if (consumers.isEmpty()) return

        val remainingCableCapacity = topology.cableRates.toMutableMap()

        // 先尝试让消费者从电网缓冲池取能（按路径损耗与路径容量计算）。
        for ((_, consumer) in consumers) {
            if (energy <= 0) break
            pullFromBufferedEnergyByPath(consumer, topology.neighbors, remainingCableCapacity)
        }

        if (providers.isEmpty()) return

        // 再按路径损耗从小到大，从所有供电者拉取。
        for ((_, consumer) in consumers) {
            pullFromProvidersByPath(consumer, providers, topology.neighbors, remainingCableCapacity)
        }
    }

    private fun pullFromBufferedEnergyByPath(
        consumer: Endpoint,
        neighbors: Map<Long, List<Long>>,
        remainingCableCapacity: MutableMap<Long, Long>
    ) {
        while (energy > 0) {
            val demand = simulateInsertion(consumer.storage, Long.MAX_VALUE)
            if (demand <= 0) break

            val candidates = buildBufferedCandidates(consumer, neighbors)
            if (candidates.isEmpty()) break

            var progressed = false
            for (candidate in candidates) {
                if (energy <= 0) break
                val pathCapacity = candidate.path.minOfOrNull { remainingCableCapacity[it] ?: 0L } ?: 0L
                if (pathCapacity <= 0) continue

                val pathLossEu = candidate.pathLossMilliEu / 1000
                val maxDeliverable = (pathCapacity - pathLossEu).coerceAtLeast(0L)
                if (maxDeliverable <= 0) continue

                val stepDemand = simulateInsertion(consumer.storage, maxDeliverable)
                if (stepDemand <= 0) break

                val takeFromPool = minOf(pathCapacity, stepDemand + pathLossEu, energy)
                if (takeFromPool <= pathLossEu) continue

                Transaction.openOuter().use { tx ->
                    val deliverable = (takeFromPool - pathLossEu).coerceAtLeast(0L)
                    val inserted = consumer.storage.insert(deliverable, tx)
                    if (inserted > 0) {
                        val moved = (inserted + pathLossEu).coerceAtMost(takeFromPool)
                        updateSnapshots(tx)
                        energy -= moved
                        for (cablePosLong in candidate.path) {
                            remainingCableCapacity[cablePosLong] =
                                (remainingCableCapacity[cablePosLong] ?: 0L) - moved
                        }
                        tx.commit()
                        progressed = true
                    }
                }
            }

            if (!progressed) break
        }
    }

    private fun pullFromProvidersByPath(
        consumer: Endpoint,
        providers: Map<Long, Endpoint>,
        neighbors: Map<Long, List<Long>>,
        remainingCableCapacity: MutableMap<Long, Long>
    ) {
        while (true) {
            val demand = simulateInsertion(consumer.storage, Long.MAX_VALUE)
            if (demand <= 0) break

            val candidates = buildProviderCandidates(consumer, providers, neighbors)
            if (candidates.isEmpty()) break

            var progressed = false
            for (candidate in candidates) {
                val pathCapacity = candidate.path.minOfOrNull { remainingCableCapacity[it] ?: 0L } ?: 0L
                if (pathCapacity <= 0) continue

                val pathLossEu = candidate.pathLossMilliEu / 1000
                val maxDeliverable = (pathCapacity - pathLossEu).coerceAtLeast(0L)
                if (maxDeliverable <= 0) continue

                val stepDemand = simulateInsertion(consumer.storage, maxDeliverable)
                if (stepDemand <= 0) break

                val needFromProvider = minOf(pathCapacity, stepDemand + pathLossEu)
                if (needFromProvider <= pathLossEu) continue

                Transaction.openOuter().use { tx ->
                    val extracted = candidate.provider.extract(needFromProvider, tx)
                    if (extracted <= pathLossEu) return@use

                    val deliverable = (extracted - pathLossEu).coerceAtLeast(0L)
                    if (deliverable <= 0) return@use

                    val inserted = consumer.storage.insert(deliverable, tx)
                    if (inserted > 0) {
                        val moved = (inserted + pathLossEu).coerceAtMost(extracted)
                        for (cablePosLong in candidate.path) {
                            remainingCableCapacity[cablePosLong] =
                                (remainingCableCapacity[cablePosLong] ?: 0L) - moved
                        }
                        tx.commit()
                        progressed = true
                    }
                }
            }

            if (!progressed) break
        }
    }

    private fun buildProviderCandidates(
        consumer: Endpoint,
        providers: Map<Long, Endpoint>,
        neighbors: Map<Long, List<Long>>
    ): List<ProviderPath> {
        val dijkstra = shortestLossFromSourcesCached(consumer.entryCables, neighbors)
        val candidates = mutableListOf<ProviderPath>()

        for ((_, provider) in providers) {
            var bestEnd: Long? = null
            var bestLoss = Long.MAX_VALUE
            for (entry in provider.entryCables) {
                val loss = dijkstra.dist[entry] ?: continue
                if (loss < bestLoss) {
                    bestLoss = loss
                    bestEnd = entry
                }
            }
            val end = bestEnd ?: continue
            val path = buildPath(end, dijkstra.prev)
            if (path.isNotEmpty()) {
                candidates.add(ProviderPath(provider.storage, path, bestLoss))
            }
        }
        candidates.sortBy { it.pathLossMilliEu }
        return candidates
    }

    private fun buildBufferedCandidates(
        consumer: Endpoint,
        neighbors: Map<Long, List<Long>>
    ): List<PathCandidate> {
        val cacheKey = entriesKey(consumer.entryCables)
        bufferedCandidatesCacheByEntries[cacheKey]?.let { return it }
        val dijkstra = shortestLossFromSourcesCached(consumer.entryCables, neighbors)
        val candidates = mutableListOf<PathCandidate>()
        for (cablePosLong in cables) {
            val loss = dijkstra.dist[cablePosLong] ?: continue
            val path = buildPath(cablePosLong, dijkstra.prev)
            if (path.isNotEmpty()) {
                candidates.add(PathCandidate(path, loss))
            }
        }
        candidates.sortBy { it.pathLossMilliEu }
        bufferedCandidatesCacheByEntries[cacheKey] = candidates
        trimPathCachesIfNeeded()
        return candidates
    }

    private fun shortestLossFromSourcesCached(
        sources: Set<Long>,
        neighbors: Map<Long, List<Long>>
    ): DijkstraResult {
        val cacheKey = entriesKey(sources)
        dijkstraCacheByEntries[cacheKey]?.let { return it }
        val result = shortestLossFromSources(sources, neighbors)
        dijkstraCacheByEntries[cacheKey] = result
        trimPathCachesIfNeeded()
        return result
    }

    private fun entriesKey(entries: Set<Long>): String =
        entries.sorted().joinToString(",")

    private fun trimPathCachesIfNeeded() {
        // 防止极端场景缓存无限增长（例如频繁移动机器导致入口集合持续变化）
        if (dijkstraCacheByEntries.size > 512) dijkstraCacheByEntries.clear()
        if (bufferedCandidatesCacheByEntries.size > 512) bufferedCandidatesCacheByEntries.clear()
    }

    private fun invalidatePathCaches() {
        topologyCache = null
        dijkstraCacheByEntries.clear()
        bufferedCandidatesCacheByEntries.clear()
    }

    private fun buildTopology(world: World): TopologyCache {
        val cableRates = mutableMapOf<Long, Long>()
        val neighbors = mutableMapOf<Long, MutableList<Long>>()
        val boundaries = mutableListOf<BoundaryEdge>()

        for (cablePosLong in cables) {
            val cablePos = BlockPos.fromLong(cablePosLong)
            val state = world.getBlockState(cablePos)
            val block = state.block as? BaseCableBlock ?: continue
            cableRates[cablePosLong] = block.getTransferRate()
            val adjacent = neighbors.getOrPut(cablePosLong) { mutableListOf() }
            for (dir in Direction.values()) {
                if (!state.get(BaseCableBlock.propertyFor(dir))) continue
                val neighborPos = cablePos.offset(dir)
                val neighborLong = neighborPos.asLong()
                if (neighborLong in cables) {
                    adjacent.add(neighborLong)
                } else {
                    boundaries.add(BoundaryEdge(cablePosLong, neighborLong, dir.opposite))
                }
            }
        }

        return TopologyCache(
            cableRates = cableRates,
            neighbors = neighbors.mapValues { it.value.toList() },
            boundaries = boundaries
        )
    }

    private fun simulateInsertion(storage: EnergyStorage, maxAmount: Long): Long {
        var accepted = 0L
        Transaction.openOuter().use { tx ->
            accepted = storage.insert(maxAmount, tx)
        }
        return accepted
    }

    private data class Endpoint(
        val storage: EnergyStorage,
        val entryCables: MutableSet<Long> = mutableSetOf()
    )

    private data class ProviderPath(
        val provider: EnergyStorage,
        val path: List<Long>,
        val pathLossMilliEu: Long
    )

    private data class PathCandidate(
        val path: List<Long>,
        val pathLossMilliEu: Long
    )

    private data class BoundaryEdge(
        val cablePosLong: Long,
        val neighborPosLong: Long,
        val lookupFromNeighborSide: Direction
    )

    private data class TopologyCache(
        val cableRates: Map<Long, Long>,
        val neighbors: Map<Long, List<Long>>,
        val boundaries: List<BoundaryEdge>
    )

    private data class DijkstraResult(
        val dist: Map<Long, Long>,
        val prev: Map<Long, Long?>
    )

    private fun shortestLossFromSources(
        sources: Set<Long>,
        neighbors: Map<Long, List<Long>>
    ): DijkstraResult {
        if (sources.isEmpty()) return DijkstraResult(emptyMap(), emptyMap())

        val dist = mutableMapOf<Long, Long>()
        val prev = mutableMapOf<Long, Long?>()
        val pq = PriorityQueue(compareBy<Pair<Long, Long>> { it.second })

        for (source in sources) {
            val startLoss = cableLossMilliEu[source] ?: 0L
            dist[source] = startLoss
            prev[source] = null
            pq.add(source to startLoss)
        }

        while (pq.isNotEmpty()) {
            val (node, currentDist) = pq.poll()
            if (currentDist != dist[node]) continue
            val nextNodes = neighbors[node] ?: emptyList()
            for (next in nextNodes) {
                val weight = cableLossMilliEu[next] ?: 0L
                val nd = currentDist + weight
                val od = dist[next]
                if (od == null || nd < od) {
                    dist[next] = nd
                    prev[next] = node
                    pq.add(next to nd)
                }
            }
        }

        return DijkstraResult(dist, prev)
    }

    private fun buildPath(end: Long, prev: Map<Long, Long?>): List<Long> {
        if (end !in prev) return emptyList()
        val reversed = mutableListOf<Long>()
        var current: Long? = end
        while (current != null) {
            reversed.add(current)
            current = prev[current]
        }
        reversed.reverse()
        return reversed
    }

    /** 获取每根导线应分摊的能量（用于 NBT 持久化）。 */
    fun getEnergySharePerCable(): Long {
        val count = cables.size
        return if (count > 0) energy / count else 0
    }
}
