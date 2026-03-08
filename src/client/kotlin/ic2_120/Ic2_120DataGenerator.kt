package ic2_120

import ic2_120.content.recipes.ModRecipeProvider
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

object Ic2_120DataGenerator : DataGeneratorEntrypoint {
	override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
		val pack = fabricDataGenerator.createPack()

		// 注册配方数据生成器
		pack.addProvider { output: net.fabricmc.fabric.api.datagen.v1.FabricDataOutput ->
			ModRecipeProvider(output)
		}
	}
}
