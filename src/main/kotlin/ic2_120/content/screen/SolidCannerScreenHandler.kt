package ic2_120.content.screen

import ic2_120.content.block.SolidCannerBlock
import ic2_120.content.block.machines.SolidCannerBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.recipes.SolidCannerRecipes
import ic2_120.content.sync.SolidCannerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier

@ModScreenHandler(block = SolidCannerBlock::class)
class SolidCannerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(SolidCannerScreenHandler::class.type(), syncId) {

    val sync = SolidCannerSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    private val tinCanSlotSpec = SlotSpec(
        canInsert = { stack -> !stack.isEmpty && stack.item == Registries.ITEM.get(Identifier("ic2_120", "tin_can")) }
    )
    private val foodSlotSpec = SlotSpec(
        canInsert = { stack ->
            stack.item !is IBatteryItem && stack.item !is IUpgradeItem &&
                SolidCannerRecipes.isCanningFood(stack.item)
        }
    )
    private val outputSlotSpec = SlotSpec(canInsert = { false }, canTake = { true })
    private val dischargingSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IBatteryItem },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, SolidCannerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)
        addSlot(PredicateSlot(blockInventory, SolidCannerBlockEntity.SLOT_TIN_CAN, TIN_CAN_SLOT_X, BLOCK_SLOTS_Y, tinCanSlotSpec))
        addSlot(PredicateSlot(blockInventory, SolidCannerBlockEntity.SLOT_FOOD, FOOD_SLOT_X, BLOCK_SLOTS_Y, foodSlotSpec))
        addSlot(PredicateSlot(blockInventory, SolidCannerBlockEntity.SLOT_OUTPUT, OUTPUT_SLOT_X, BLOCK_SLOTS_Y, outputSlotSpec))
        addSlot(PredicateSlot(blockInventory, SolidCannerBlockEntity.SLOT_DISCHARGING, TIN_CAN_SLOT_X, BLOCK_SLOTS_Y + SLOT_SIZE, dischargingSlotSpec))
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    SolidCannerBlockEntity.SLOT_UPGRADE_INDICES[i],
                    UpgradeSlotLayout.SLOT_X,
                    UpgradeSlotLayout.slotY(i),
                    upgradeSlotSpec
                )
            )
        }
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
                index == SLOT_OUTPUT_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index == SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                        SlotTarget(slots[it], upgradeSlotSpec)
                    }
                    val dischargingTarget = SlotTarget(slots[SLOT_DISCHARGING_INDEX], dischargingSlotSpec)
                    val tinCanTarget = SlotTarget(slots[SLOT_TIN_CAN_INDEX], tinCanSlotSpec)
                    val foodTarget = SlotTarget(slots[SLOT_FOOD_INDEX], foodSlotSpec)
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(tinCanTarget, foodTarget, dischargingTarget) + upgradeTargets
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is SolidCannerBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val TIN_CAN_SLOT_X = 38
        const val FOOD_SLOT_X = 56
        const val OUTPUT_SLOT_X = 116
        const val BLOCK_SLOTS_Y = 54
        const val PLAYER_INV_Y = 108
        const val HOTBAR_Y = 166
        const val SLOT_SIZE = 18

        const val SLOT_TIN_CAN_INDEX = 0
        const val SLOT_FOOD_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_DISCHARGING_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 44

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): SolidCannerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(SolidCannerBlockEntity.INVENTORY_SIZE)
            return SolidCannerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
