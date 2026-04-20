package ic2_120.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.entity.player.PlayerEntity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    @Final
    public DefaultedList<Slot> slots;

    @Shadow
    @Final
    private DefaultedList<ItemStack> trackedStacks;

    @Shadow
    public int syncId;

    /**
     * 保护 updateTrackedSlot 方法，防止越界访问 slots/trackedStacks 列表。
     * JEI 在创造模式下拿取物品时会通过 sendContentUpdates → updateTrackedSlot 路径
     * 访问到不存在的槽位索引（如 index 54，而列表只有 54 个元素，有效索引为 0-53）。
     */
    @Inject(method = "updateTrackedSlot", at = @At("HEAD"), cancellable = true)
    private void ic2_120$guardUpdateTrackedSlot(int index, ItemStack stack, Supplier<ItemStack> stackSupplier, CallbackInfo ci) {
        if (index < 0 || index >= this.slots.size() || index >= this.trackedStacks.size()) {
            ScreenHandler self = (ScreenHandler) (Object) this;
            LOGGER.warn(
                "[IC2/ScreenGuard] blocked out-of-bounds updateTrackedSlot: handlerClass={}, handlerType={}, syncId={}, index={}, slotsSize={}, trackedSize={}",
                self.getClass().getName(),
                self.getType(),
                this.syncId,
                index,
                this.slots.size(),
                this.trackedStacks.size()
            );
            ci.cancel();
        }
    }

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void ic2_120$guardOutOfBoundsSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        // -999 是"点击容器外部"的合法槽位，不应拦截。
        if (slotIndex >= 0 && slotIndex >= this.slots.size()) {
            ScreenHandler self = (ScreenHandler) (Object) this;
            LOGGER.warn(
                "[IC2/ScreenGuard] blocked out-of-bounds slot click: handlerClass={}, handlerType={}, syncId={}, slotIndex={}, slotsSize={}, actionType={}, button={}, player={}",
                self.getClass().getName(),
                self.getType(),
                this.syncId,
                slotIndex,
                this.slots.size(),
                actionType,
                button,
                player.getEntityName()
            );
            ci.cancel();
        }
    }
}
