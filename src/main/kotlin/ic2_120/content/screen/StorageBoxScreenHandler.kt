package ic2_120.content.screen

import ic2_120.content.block.storage.StorageBoxBlockEntity
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot

/**
 * 储物箱 GUI 的 ScreenHandler
 *
 * 所有容量的储物箱统一使用单列 9 格布局，容量通过 ScrollView 滚动显示。
 */
@ModScreenHandler(name = "storage_box")
class StorageBoxScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    val inventory: Inventory
) : ScreenHandler(StorageBoxScreenHandler::class.type(), syncId) {

    init {
        // 储物箱槽位：统一使用单列 9 格布局
        val inventorySize = inventory.size()
        val rows = (inventorySize + 8) / 9

        for (row in 0 until rows) {
            for (col in 0 until 9) {
                val slotIndex = row * 9 + col
                if (slotIndex < inventorySize) {
                    addSlot(Slot(inventory, slotIndex, 8 + col * 18, 18 + row * 18))
                }
            }
        }

        // 玩家背包槽位（3x9）
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18))
            }
        }

        // 玩家快捷栏槽位
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y))
        }
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
        var movedStack = ItemStack.EMPTY
        val slot = slots[slotIndex]
        if (slot.hasStack()) {
            val stack = slot.stack
            movedStack = stack.copy()

            // 从储物箱移到玩家背包
            if (slotIndex < inventory.size()) {
                if (!this.insertItem(stack, inventory.size(), slots.size, true)) {
                    return ItemStack.EMPTY
                }
            }
            // 从玩家背包移到储物箱
            else if (!this.insertItem(stack, 0, inventory.size(), false)) {
                return ItemStack.EMPTY
            }

            if (stack.isEmpty) {
                slot.markDirty()
            } else {
                slot.markDirty()
            }
        }

        return movedStack
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return inventory.canPlayerUse(player)
    }

    companion object {
        // 玩家背包 Y 起始（与 StorageBoxScreen 保持一致）
        const val PLAYER_INV_Y = 152
        const val HOTBAR_Y = 210

        /**
         * 从 PacketByteBuf 创建 ScreenHandler（客户端）
         */
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): StorageBoxScreenHandler {
            val pos = buf.readBlockPos()
            val world = playerInventory.player.world
            val blockEntity = world.getBlockEntity(pos)

            if (blockEntity is StorageBoxBlockEntity) {
                return StorageBoxScreenHandler(syncId, playerInventory, blockEntity)
            }

            throw IllegalStateException("储物箱 BlockEntity 不存在于位置 $pos")
        }

        /**
         * 直接从 BlockEntity 创建 ScreenHandler（服务端）
         */
        fun create(syncId: Int, playerInventory: PlayerInventory, blockEntity: StorageBoxBlockEntity): StorageBoxScreenHandler {
            return StorageBoxScreenHandler(syncId, playerInventory, blockEntity)
        }
    }
}
