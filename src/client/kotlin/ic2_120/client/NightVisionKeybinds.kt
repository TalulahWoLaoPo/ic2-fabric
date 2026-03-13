package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.network.NetworkManager
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.network.PacketByteBuf
import org.lwjgl.glfw.GLFW

object NightVisionKeybinds {
    private val toggleKey: KeyBinding = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.ic2_120.night_vision_toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.ic2_120.ic2"
        )
    )

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.player == null) return@register
            while (toggleKey.wasPressed()) {
                if (!ScreenAltState.isAltDown(client)) continue
                ClientPlayNetworking.send(NetworkManager.TOGGLE_NIGHT_VISION_GOGGLES_PACKET, PacketByteBuf(Unpooled.buffer()))
            }
        }
    }
}

private object ScreenAltState {
    fun isAltDown(client: net.minecraft.client.MinecraftClient): Boolean {
        val window = client.window.handle
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT)
    }
}
