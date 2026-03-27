package ic2_120.content.screen

import ic2_120.content.block.CannerBlock
import ic2_120.content.block.machines.CannerBlockEntity
import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.recipes.CannerMixingRecipes
import ic2_120.content.recipes.SolidCannerRecipes
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.CannerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import net.minecraft.text.Text
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = CannerBlock::class)
class CannerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(CannerScreenHandler::class.type(), syncId) {

    val sync = CannerSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    private val tinCanItem by lazy { Registries.ITEM.get(Identifier("ic2_120", "tin_can")) }

    private val containerSlotSpec = SlotSpec(
        canInsert = { stack ->
            stack.item !is IBatteryItem && stack.item !is FoamSprayerItem && (
                isFilledFluidContainer(stack) || isEmptyFluidContainer(stack) ||
                stack.item == tinCanItem
            )
        }
    )
    private val materialSlotSpec = SlotSpec(
        canInsert = { stack ->
            stack.item !is IBatteryItem && (
                SolidCannerRecipes.isCanningFood(stack.item) ||
                CannerMixingRecipes.isMixingMaterial(stack.item) ||
                (sync.getMode() == CannerSync.Mode.BOTTLE_LIQUID && stack.item is FoamSprayerItem &&
                    FoamSprayerItem.getFluidAmount(stack) < FoamSprayerItem.CAPACITY_DROPLETS)
            )
        }
    )
    private val outputSlotSpec = SlotSpec(
        canInsert = { stack ->
            if (sync.getMode() == CannerSync.Mode.BOTTLE_SOLID) return@SlotSpec false
            stack.item !is IBatteryItem && stack.item !is FoamSprayerItem && isEmptyFluidContainer(stack)
        },
        canTake = { true }
    )
    private val dischargingSlotSpec = SlotSpec(
        maxItemCount = 1,
        canInsert = { stack -> stack.item is IBatteryItem }
    )

    init {
        checkSize(blockInventory, CannerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_CONTAINER,
            CONTAINER_SLOT_X, CONTAINER_SLOT_Y, containerSlotSpec))
        addSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_MATERIAL,
            MATERIAL_SLOT_X, MATERIAL_SLOT_Y, materialSlotSpec))
        addSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_OUTPUT,
            OUTPUT_SLOT_X, OUTPUT_SLOT_Y, outputSlotSpec))
        addSlot(PredicateSlot(
            blockInventory,
            CannerBlockEntity.SLOT_DISCHARGING,
            POWER_SLOT_X,
            POWER_SLOT_Y,
            dischargingSlotSpec
        ))

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    CannerBlockEntity.SLOT_UPGRADE_INDICES[i],
                    UpgradeSlotLayout.SLOT_X,
                    UpgradeSlotLayout.slotY(i),
                    upgradeSlotSpec
                )
            )
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y))
        }
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos)
            if (be is CannerBlockEntity) {
                when (id) {
                    BUTTON_ID_MODE_CYCLE -> be.cycleMode()
                    BUTTON_ID_SWAP_TANKS -> {
                        val leftBefore = be.sync.leftFluidAmountMb
                        val rightBefore = be.sync.rightFluidAmountMb
                        val changed = be.swapTanks()
                        val leftAfter = be.sync.leftFluidAmountMb
                        val rightAfter = be.sync.rightFluidAmountMb
                        player.sendMessage(
                            Text.literal(
                                if (changed) {
                                    "液槽已交换: 左 ${leftBefore}mB -> ${leftAfter}mB, 右 ${rightBefore}mB -> ${rightAfter}mB"
                                } else {
                                    "液槽交换无可见变化: 左 ${leftAfter}mB, 右 ${rightAfter}mB"
                                }
                            ),
                            true
                        )
                    }
                    else -> return@get
                }
            }
        }, true)
        return id == BUTTON_ID_MODE_CYCLE || id == BUTTON_ID_SWAP_TANKS
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when (index) {
                SLOT_OUTPUT_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                else -> {
                    if (index in PLAYER_INV_START..HOTBAR_END) {
                        val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                            SlotTarget(slots[it], upgradeSlotSpec)
                        }
                        val moved = SlotMoveHelper.insertIntoTargets(
                            stackInSlot,
                            listOf(
                                SlotTarget(slots[SLOT_DISCHARGING_INDEX], dischargingSlotSpec),
                                SlotTarget(slots[SLOT_CONTAINER_INDEX], containerSlotSpec),
                                SlotTarget(slots[SLOT_MATERIAL_INDEX], materialSlotSpec)
                            ) + upgradeTargets
                        )
                        if (!moved) return ItemStack.EMPTY
                    } else if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY
                    }
                }
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
            world.getBlockState(pos).block is CannerBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        // 对称中轴布局（可用主区宽 170，组件总宽 104，左侧留白 33）
        const val POWER_SLOT_X = 33
        const val POWER_SLOT_Y = 56
        const val LEFT_TANK_X = 55
        const val LEFT_TANK_Y = 40
        const val CONTAINER_SLOT_X = 69
        const val CONTAINER_SLOT_Y = 56
        const val MATERIAL_SLOT_X = 87
        const val MATERIAL_SLOT_Y = 56
        const val OUTPUT_SLOT_X = 105
        const val OUTPUT_SLOT_Y = 56
        const val RIGHT_TANK_X = 131
        const val RIGHT_TANK_Y = 40
        const val PROGRESS_BAR_X = 69
        const val PROGRESS_BAR_Y = 84
        const val PROGRESS_BAR_W = 54
        const val PROGRESS_BAR_H = 8
        const val FLUID_BAR_W = 12
        const val FLUID_BAR_H = 52
        const val SLOT_SIZE = 18

        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 138    // 机器区与物品栏大幅拉开
        const val HOTBAR_Y = 196

        const val SLOT_CONTAINER_INDEX = 0
        const val SLOT_MATERIAL_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_DISCHARGING_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 44
        const val BUTTON_ID_MODE_CYCLE = 0
        const val BUTTON_ID_SWAP_TANKS = 1

        private fun isFilledFluidContainer(stack: ItemStack): Boolean {
            if (stack.isEmpty) return false
            if (stack.item == Items.WATER_BUCKET || stack.item == Items.LAVA_BUCKET) return true
            val ctx = ContainerItemContext.withConstant(stack)
            val storage = ctx.find(FluidStorage.ITEM) ?: return false
            for (view in storage) {
                if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) return true
            }
            return false
        }

        private fun isEmptyFluidContainer(stack: ItemStack): Boolean {
            if (stack.isEmpty) return false
            if (stack.item == Items.BUCKET) return true
            val ctx = ContainerItemContext.withConstant(stack)
            val storage = ctx.find(FluidStorage.ITEM) ?: return false
            return storage.supportsInsertion()
        }

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): CannerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(CannerBlockEntity.INVENTORY_SIZE)
            return CannerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
