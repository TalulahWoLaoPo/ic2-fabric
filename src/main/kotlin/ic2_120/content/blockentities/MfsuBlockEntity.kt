package ic2_120.content.blockentities

import ic2_120.content.MfsuSync
import ic2_120.content.ModBlockEntities
import ic2_120.content.block.MfsuBlock
import ic2_120.content.screen.MfsuScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * MFSU 方块实体。仅能量存储，通过 Energy API 与电网交互。
 */
@ModBlockEntity(block = MfsuBlock::class)
class MfsuBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ExtendedScreenHandlerFactory {

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = MfsuSync(
        syncedData,
        { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        { world?.time }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(MfsuBlockEntity::class),
        pos,
        state
    )

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text =
        Text.translatable("container.ic2_120.mfsu")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MfsuScreenHandler(syncId, playerInventory, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(MfsuSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        syncedData.writeNbt(nbt)
        nbt.putLong(MfsuSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
