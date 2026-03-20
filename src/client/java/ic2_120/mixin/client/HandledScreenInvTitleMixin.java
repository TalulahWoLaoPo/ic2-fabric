package ic2_120.mixin.client;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 将模组 Screen 自带的 "Inv" 文本移到屏幕外，避免遮挡自定义 UI。
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenInvTitleMixin {

    @Shadow
    private int playerInventoryTitleY;

    @Inject(method = "init", at = @At("TAIL"))
    private void moveInvTitleOffScreen(CallbackInfo ci) {
        Object self = this;
        if (self.getClass().getName().startsWith("ic2_120.client.screen.")) {
            playerInventoryTitleY = -1000;
        }
    }
}
