package ic2_120.content.block.machines

import ic2_120.content.block.ManualKineticGeneratorBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.sync.ManualKineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

@ModBlockEntity(block = ManualKineticGeneratorBlock::class)
class ManualKineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), IKineticMachinePort {

    companion object {
        private const val KU_PER_CLICK = 400
        private const val COOLDOWN_TICKS = 40L
    }

    override val tier: Int = 1
    override val activeProperty: net.minecraft.state.property.BooleanProperty = ManualKineticGeneratorBlock.ACTIVE

    private val syncedData = SyncedData(this)
    val sync = ManualKineticGeneratorSync(syncedData)

    private var pendingOutputKu: Int = 0
    private var lastClickTick: Long = Long.MIN_VALUE

    constructor(pos: BlockPos, state: BlockState) : this(
        ManualKineticGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): net.minecraft.inventory.Inventory? = null

    fun onRightClick(player: PlayerEntity): net.minecraft.util.ActionResult {
        val world = world ?: return net.minecraft.util.ActionResult.PASS
        if (world.isClient) return net.minecraft.util.ActionResult.SUCCESS

        val currentTick = world.time
        if (lastClickTick != Long.MIN_VALUE && currentTick - lastClickTick < COOLDOWN_TICKS) {
            return net.minecraft.util.ActionResult.SUCCESS
        }

        lastClickTick = currentTick
        pendingOutputKu += KU_PER_CLICK
        sync.storedKu = pendingOutputKu
        markDirtyAndSync()
        return net.minecraft.util.ActionResult.SUCCESS
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.storedKu = pendingOutputKu.coerceAtLeast(0)
        val active = pendingOutputKu > 0
        setActiveState(world, pos, state, active)
        markDirty()
    }

    override fun canOutputKuTo(side: Direction): Boolean {
        val world = world ?: return false
        val state = world.getBlockState(pos)
        val facing = state.getOrEmpty(Properties.FACING).orElse(Direction.NORTH)
        return side == facing.opposite
    }

    override fun getStoredKu(side: Direction): Int =
        if (canOutputKuTo(side)) pendingOutputKu.coerceAtLeast(0) else 0

    override fun getKuCapacity(side: Direction): Int =
        if (canOutputKuTo(side)) Int.MAX_VALUE else 0

    override fun getMaxExtractableKu(side: Direction): Int =
        if (canOutputKuTo(side)) pendingOutputKu.coerceAtLeast(0) else 0

    override fun extractKu(side: Direction, amount: Int, simulate: Boolean): Int {
        if (!canOutputKuTo(side) || amount <= 0) return 0
        val extracted = minOf(amount, pendingOutputKu.coerceAtLeast(0))
        if (!simulate && extracted > 0) {
            pendingOutputKu = (pendingOutputKu - extracted).coerceAtLeast(0)
            sync.extractedKu = (sync.extractedKu + extracted).coerceAtLeast(0)
            markDirty()
        }
        return extracted
    }

    private fun markDirtyAndSync() {
        markDirty()
        val world = world ?: return
        if (!world.isClient) {
            val state = world.getBlockState(pos)
            world.updateListeners(pos, state, state, net.minecraft.block.Block.NOTIFY_LISTENERS)
            (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
        }
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        pendingOutputKu = nbt.getInt("PendingKu").coerceAtLeast(0)
        lastClickTick = nbt.getLong("LastClickTick")
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putInt("PendingKu", pendingOutputKu)
        nbt.putLong("LastClickTick", lastClickTick)
        syncedData.writeNbt(nbt)
    }
}