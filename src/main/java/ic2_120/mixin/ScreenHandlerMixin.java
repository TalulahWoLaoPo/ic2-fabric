package ic2_120.mixin;

import com.mojang.logging.LogUtils;
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

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    @Final
    public DefaultedList<Slot> slots;

    @Shadow
    public int syncId;

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void ic2_120$guardOutOfBoundsSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        // -999 是“点击容器外部”的合法槽位，不应拦截。
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
