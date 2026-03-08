package ic2_120

import ic2_120.client.BatteryModelPredicates
import ic2_120.client.CableTooltip
import ic2_120.client.ClientBlockRenderLayers
import ic2_120.client.ClientEntityRenderers
import ic2_120.client.ClientScreenRegistrar
import ic2_120.client.StorageBoxColorProvider
import net.fabricmc.api.ClientModInitializer

object Ic2_120Client : ClientModInitializer {
	override fun onInitializeClient() {
		ClientScreenRegistrar.registerScreens(Ic2_120.MOD_ID, listOf("ic2_120.client"))
		CableTooltip.register()
		ClientEntityRenderers.register()
		ClientBlockRenderLayers.register()
		BatteryModelPredicates.register() // 注册电池模型 predicate

		// 注册储物箱着色器
		StorageBoxColorProvider.register()
	}
}
