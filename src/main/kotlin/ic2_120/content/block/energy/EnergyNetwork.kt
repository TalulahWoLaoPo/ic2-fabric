package ic2_120.content.block.energy

import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.ITieredMachine.Companion.effectiveVoltageTier
import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.item.energy.ITiered
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.inventory.Inventory
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import team.reborn.energy.api.EnergyStorage
import kotlin.math.pow
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

    companion object {
        /** 漏电/低耐压烧毁的检查周期（tick）。x 秒 */
        const val damageIntervalTicks = 100
    }

    /** 低耐压导线烧毁比例（0.0–1.0）。每次检查时随机选择该比例的“耐压低于电网等级”的导线烧毁。 */
    private val underTierCableBurnRatio = 1

    /** 所有成员导线位置（packed [BlockPos.asLong]）。 */
    val cables = mutableSetOf<Long>()
    var energy: Long = 0
    var capacity: Long = 0
    /** 电网输出等级（1–5），由电网内输出等级最高的机器决定，决定所有导线的电压等级。 */
    var outputLevel: Int = 1
    /** 触电伤害触发 tick 偏移（0–199），用于错开不同电网的伤害时机。 */
    var damageTickOffset: Int = 0
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
        tryUninsulatedCableDamage(world)
        tryUnderTierCableBurn(world)
        tryMachineOvervoltageExplosion(world)
    }

    /** 漏电导线触电伤害：绝缘等级 < 电网输出等级的导线会漏电。每 10 秒触发，范围与伤害量由电网输出等级决定。 */
    private fun tryUninsulatedCableDamage(world: World) {
        if (world.isClient) return
        val serverWorld = world as? ServerWorld ?: return
        if ((world.time + damageTickOffset) % damageIntervalTicks != 0L) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        if (topology.cablesThatLeak.isEmpty() || outputLevel < 1) return

        val damageSource = createCableShockDamageSource(serverWorld)
        val rangeInt = outputLevel
        val range = rangeInt.toDouble()
        val damageAmount = outputLevel * 2f // n 心 = n * 2 伤害
        val leakingCables = topology.cablesThatLeak.toHashSet()

        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE
        for (cablePosLong in leakingCables) {
            val x = BlockPos.unpackLongX(cablePosLong)
            val y = BlockPos.unpackLongY(cablePosLong)
            val z = BlockPos.unpackLongZ(cablePosLong)
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        val scanBox = Box(
            minX - range, minY - range, minZ - range,
            maxX + range + 1.0, maxY + range + 1.0, maxZ + range + 1.0
        )

        for (entity in world.getEntitiesByClass(LivingEntity::class.java, scanBox) { e ->
            e.isAlive && !e.isSpectator
        }) {
            if (hasLeakingCableNearby(entity.blockPos, leakingCables, rangeInt)) {
                entity.damage(damageSource, damageAmount)
                // println("触电伤害：${entity.name}，电压等级：$outputLevel")
            }
        }
    }

    private fun hasLeakingCableNearby(entityPos: BlockPos, leakingCables: Set<Long>, range: Int): Boolean {
        val ex = entityPos.x
        val ey = entityPos.y
        val ez = entityPos.z
        for (dx in -range..range) {
            for (dy in -range..range) {
                for (dz in -range..range) {
                    if (BlockPos.asLong(ex + dx, ey + dy, ez + dz) in leakingCables) return true
                }
            }
        }
        return false
    }

    private fun createCableShockDamageSource(world: ServerWorld): DamageSource {
        val registry = world.registryManager.get(RegistryKeys.DAMAGE_TYPE)
        val key = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier("ic2_120", "cable_shock"))
        val entry = registry.getEntry(key).orElse(null)
            ?: registry.getEntry(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier("minecraft", "lightning"))).orElseThrow()
        return DamageSource(entry)
    }

    /**
     * 低耐压导线烧毁：电网输出等级高于导线电压等级时，按 [underTierCableBurnRatio] 比例随机烧毁部分导线。
     * 仅烧毁电压等级 &lt; outputLevel 的导线（如锡线接入含 MFSU 的玻璃纤维电网会被烧，玻璃纤维不烧）。
     * 烧毁处生成烟雾/火焰粒子，不掉落物品。
     */
    private fun tryUnderTierCableBurn(world: World) {
        if (world.isClient) return
        val serverWorld = world as? ServerWorld ?: return
        if ((world.time + damageTickOffset) % damageIntervalTicks != 0L) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        val toBurn = topology.cablesThatCanBurn
        if (toBurn.isEmpty() || outputLevel < 1) return

        val burnCount = (toBurn.size * underTierCableBurnRatio).toInt().coerceIn(0, toBurn.size)
        if (burnCount <= 0) return

        val shuffled = toBurn.toMutableList()
        for (i in shuffled.indices.reversed()) {
            if (i == 0) break
            val j = serverWorld.random.nextInt(i + 1)
            val t = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = t
        }
        for (i in 0 until burnCount) {
            val posLong = shuffled[i]
            val pos = BlockPos.fromLong(posLong)
            val state = world.getBlockState(pos)
            if (state.isAir) continue
            val block = state.block as? BaseCableBlock ?: continue
            if (block !is ITiered || block.tier >= outputLevel) continue

            val x = pos.x + 0.5
            val y = pos.y + 0.5
            val z = pos.z + 0.5
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.2, 0.2, 0.2, 0.02)
            serverWorld.spawnParticles(ParticleTypes.FLAME, x, y, z, 6, 0.15, 0.15, 0.15, 0.01)
            world.breakBlock(pos, false)
        }
    }

    /**
     * 机器耐压检测：与电网相连的机器若有效耐压等级 &lt; 电网 outputLevel，直接爆炸。
     * 有效耐压 = 机器 [ITieredMachine.tier] + 高压升级带来的 [ITransformerUpgradeSupport.voltageTierBonus]。
     * 不按比例随机，只要电压等级不对就炸。
     * [IGenerator] 发电机不参与过压爆炸。
     * 爆炸威力按电网电压等级：等级 4 对应 10 颗心伤害，每提高 1 级伤害翻倍。
     */
    private fun tryMachineOvervoltageExplosion(world: World) {
        if (world.isClient) return
        if ((world.time + damageTickOffset) % damageIntervalTicks != 0L) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        if (outputLevel < 1) return

        val checked = mutableSetOf<Long>()
        for (boundary in topology.boundaries) {
            val neighborLong = boundary.neighborPosLong
            if (neighborLong in checked) continue
            checked.add(neighborLong)
            val neighborPos = BlockPos.fromLong(neighborLong)
            val be = world.getBlockEntity(neighborPos) ?: continue
            if (be is IGenerator) continue
            if (be !is ITieredMachine) continue
            val effectiveTier = be.effectiveVoltageTier()
            if (outputLevel <= effectiveTier) continue
            if (be is Inventory) be.clear()
            world.breakBlock(neighborPos, false)
            val x = neighborPos.x + 0.5
            val y = neighborPos.y + 0.5
            val z = neighborPos.z + 0.5
            val power = explosionPowerForOutputLevel(outputLevel)
            world.createExplosion(null, x, y, z, power, false, World.ExplosionSourceType.BLOCK)
        }
    }

    /** 电网电压等级对应的爆炸威力：等级 4 = 10 颗心伤害（power=2），每提高 1 级伤害翻倍。 */
    private fun explosionPowerForOutputLevel(level: Int): Float {
        if (level <= 0) return 0.25f
        return (2f * 2.0.pow(level - 4)).toFloat()
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
        var maxLevel = 1

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
                    val neighborBe = world.getBlockEntity(neighborPos)
                    val storage = EnergyStorage.SIDED.find(world, neighborPos, dir.opposite)
                    //只考虑输出，输入不考虑，例如：mfsu作为被充电的一方是不会把电网电压等级拉高的
                    if (neighborBe is ITieredMachine && storage?.supportsExtraction() == true) {
                        maxLevel = maxOf(maxLevel, neighborBe.tier)
                    }
                }
            }
        }

        outputLevel = maxLevel

        val cablesThatLeak = cables.filter { cablePosLong ->
            val block = world.getBlockState(BlockPos.fromLong(cablePosLong)).block as? BaseCableBlock ?: return@filter true
            block.insulationLevel < outputLevel
        }

        val cablesThatCanBurn = cables.filter { cablePosLong ->
            val block = world.getBlockState(BlockPos.fromLong(cablePosLong)).block
            val tier = (block as? ITiered)?.tier ?: 1
            tier < outputLevel
        }

        return TopologyCache(
            cableRates = cableRates,
            neighbors = neighbors.mapValues { it.value.toList() },
            boundaries = boundaries,
            cablesThatLeak = cablesThatLeak,
            cablesThatCanBurn = cablesThatCanBurn
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
        val boundaries: List<BoundaryEdge>,
        val cablesThatLeak: List<Long>,
        val cablesThatCanBurn: List<Long>
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


