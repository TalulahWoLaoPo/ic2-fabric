package ic2_120

import ic2_120.content.recipes.ModBlockLootTableProvider
import ic2_120.content.recipes.ModBlockTagProvider
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
		// 注册方块掉落表生成器（机器方块需扳手拆才掉完整机器）
		pack.addProvider { output: net.fabricmc.fabric.api.datagen.v1.FabricDataOutput ->
			ModBlockLootTableProvider(output)
		}
		// 注册方块标签生成器（机器方块加入 mineable/pickaxe、needs_iron_tool，使铁镐能正常挖掘）
		pack.addProvider { output, registriesFuture ->
			ModBlockTagProvider(output, registriesFuture)
		}
	}
}
