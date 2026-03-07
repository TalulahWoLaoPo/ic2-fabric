package ic2_120.content.screen

import ic2_120.content.MfsuSync
import ic2_120.content.SyncedDataView
import ic2_120.content.block.MfsuBlock
import ic2_120.registry.annotation.ModScreenHandler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Direction
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext

/**
 * MFSU GUI 的 ScreenHandler。无机器槽位，仅玩家背包 + 能量同步。
 */
@ModScreenHandler(block = MfsuBlock::class)
class MfsuScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ModScreenHandlers.getType(MfsuScreenHandler::class), syncId) {

    /** 客户端仅用于 GUI 显示，getFacing 用占位即可（SIDED 仅在服务端 BlockEntity 使用）。 */
    val sync = MfsuSync(
        schema = SyncedDataView(propertyDelegate),
        getFacing = { Direction.NORTH }
    )

    init {
        addProperties(propertyDelegate)
        // 玩家背包
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(net.minecraft.screen.slot.Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(net.minecraft.screen.slot.Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): net.minecraft.item.ItemStack {
        return net.minecraft.item.ItemStack.EMPTY
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is MfsuBlock && player.squaredDistanceTo(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MfsuScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val ctx = ScreenHandlerContext.create(playerInventory.player.world, pos)
            return MfsuScreenHandler(syncId, playerInventory, ctx, ArrayPropertyDelegate(propertyCount))
        }
    }
}
