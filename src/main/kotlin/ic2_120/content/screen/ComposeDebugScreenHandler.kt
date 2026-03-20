package ic2_120.content.screen

import ic2_120.content.block.ComposeDebugBlock
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.util.math.BlockPos

/**
 * ComposeDebugScreen 的服务端 ScreenHandler。
 * 不需要任何槽位或同步数据，纯展示用。
 */
@ModScreenHandler(block = ComposeDebugBlock::class)
class ComposeDebugScreenHandler(
    syncId: Int,
    val playerInventory: PlayerInventory
) : ScreenHandler(ComposeDebugScreenHandler::class.type(), syncId) {

    override fun quickMove(player: PlayerEntity, index: Int): net.minecraft.item.ItemStack =
        net.minecraft.item.ItemStack.EMPTY

    override fun canUse(player: PlayerEntity): Boolean = true

    companion object {
        const val GUI_WIDTH = 240
        const val GUI_HEIGHT = 200

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf?): ComposeDebugScreenHandler {
            return ComposeDebugScreenHandler(syncId, playerInventory)
        }
    }
}
