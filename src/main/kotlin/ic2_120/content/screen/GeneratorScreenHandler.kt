package ic2_120.content.screen

import ic2_120.content.sync.GeneratorSync
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.block.machines.GeneratorBlockEntity
import ic2_120.content.block.machines.MachineBlockEntity
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.content.item.energy.IBatteryItem
import net.fabricmc.fabric.api.registry.FuelRegistry

/**
 * 燃料槽 - 只允许燃料
 */
private class FuelSlot(inventory: Inventory, slot: Int, x: Int, y: Int) : Slot(inventory, slot, x, y) {
    override fun canInsert(stack: net.minecraft.item.ItemStack): Boolean {
        // 检查是否是有效燃料
        return !stack.isEmpty && FuelRegistry.INSTANCE.get(stack.item) != null && FuelRegistry.INSTANCE.get(stack.item)!! > 0
    }
}

/**
 * 电池充电槽 - 只允许电池，且限制最大1个
 */
private class BatterySlot(inventory: Inventory, slot: Int, x: Int, y: Int) : Slot(inventory, slot, x, y) {
    override fun canInsert(stack: net.minecraft.item.ItemStack): Boolean {
        // 只允许电池物品
        return stack.item is IBatteryItem
    }

    override fun getMaxItemCount(stack: net.minecraft.item.ItemStack): Int {
        // 电池槽最多只能放1个
        return 1
    }
}

@ModScreenHandler(block = GeneratorBlock::class)
class GeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ModScreenHandlers.getType(GeneratorScreenHandler::class), syncId) {

    val sync = GeneratorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    init {
        checkSize(blockInventory, 2)
        addProperties(propertyDelegate)

        // 添加自定义槽位（带验证）
        addSlot(FuelSlot(blockInventory, MachineBlockEntity.FUEL_SLOT, FUEL_SLOT_X, BLOCK_SLOTS_Y))
        addSlot(BatterySlot(blockInventory, MachineBlockEntity.BATTERY_SLOT, BATTERY_SLOT_X, BLOCK_SLOTS_Y))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                // 燃料槽 -> 玩家物品栏
                index == MachineBlockEntity.FUEL_SLOT -> if (!insertItem(stackInSlot, 2, 38, true)) return ItemStack.EMPTY
                // 电池槽 -> 玩家物品栏
                index == MachineBlockEntity.BATTERY_SLOT -> if (!insertItem(stackInSlot, 2, 38, true)) return ItemStack.EMPTY
                // 玩家物品栏 -> 燃料槽或电池槽
                index in 2..37 -> {
                    // 先尝试燃料槽
                    if (!insertItem(stackInSlot, 0, 1, false)) {
                        // 燃料槽放不下，尝试电池槽（只插入单个）
                        val batterySlot = slots[MachineBlockEntity.BATTERY_SLOT]
                        if (batterySlot.canInsert(stackInSlot) && batterySlot.stack.isEmpty) {
                            // 只插入1个物品到电池槽
                            val singleItem = stackInSlot.copy()
                            singleItem.count = 1
                            batterySlot.stack = singleItem
                            // 减少原槽位的物品
                            stackInSlot.decrement(1)
                            slot.markDirty()
                        }
                    }
                }
                else -> if (!insertItem(stackInSlot, 2, 38, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is GeneratorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val FUEL_SLOT_X = 56
        const val BATTERY_SLOT_X = 116
        const val BLOCK_SLOTS_Y = 54
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142
        const val SLOT_SIZE = 18

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): GeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(2)
            return GeneratorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
