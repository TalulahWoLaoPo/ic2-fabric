package ic2_120.content.worldgen

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.loot.v2.LootTableEvents
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.loot.LootPool
import net.minecraft.loot.entry.EmptyEntry
import net.minecraft.loot.entry.ItemEntry
import net.minecraft.loot.function.SetCountLootFunction
import net.minecraft.loot.provider.number.UniformLootNumberProvider
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 以代码方式向原版奖励箱追加 IC2 战利品，避免整表覆盖并降低与其他模组的冲突概率。
 */
object ChestLootInjector {
    private val industrialChests = setOf(
        chest("abandoned_mineshaft"),
        chest("nether_bridge")
    )

    private val adventureChests = setOf(
        chest("desert_pyramid"),
        chest("end_city_treasure"),
        chest("igloo_chest"),
        chest("jungle_temple")
    )

    private val strongholdStandardChests = setOf(
        chest("stronghold_crossing"),
        chest("stronghold_library")
    )

    // 旧版 village_blacksmith 在现代版本已拆分到多个村民职业建筑箱中，这里统一追加。
    private val villageBlacksmithEquivalents = setOf(
        chest("village_blacksmith"),
        chest("village/village_armorer"),
        chest("village/village_toolsmith"),
        chest("village/village_weaponsmith")
    )

    fun register() {
        LootTableEvents.MODIFY.register { _, _, lootTableId, tableBuilder, source ->
            if (!source.isBuiltin) return@register

            when (lootTableId) {
                in industrialChests -> tableBuilder.pool(createIndustrialPool())
                in adventureChests -> tableBuilder.pool(createAdventurePool())
                chest("simple_dungeon") -> tableBuilder.pool(createDungeonPool())
                chest("spawn_bonus_chest") -> tableBuilder.pool(createSpawnBonusPool())
                chest("stronghold_corridor") -> tableBuilder.pool(createStrongholdCorridorPool())
                in strongholdStandardChests -> tableBuilder.pool(createStrongholdStandardPool())
                in villageBlacksmithEquivalents -> tableBuilder.pool(createVillageBlacksmithPool())
            }
        }
    }

    private fun createIndustrialPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 5f)
            .withWeightedItem(modItem("iridium_shard"), 8, 2f, 5f)
            .withWeightedItem(modItem("bronze_pickaxe"), 1)
            .withWeightedItem(modItem("filled_tin_can"), 8, 4f, 16f)
            .withEmpty(34)

    private fun createAdventurePool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 5f)
            .withWeightedItem(modItem("iridium_shard"), 8, 1f, 2f)
            .withWeightedItem(modItem("bronze_pickaxe"), 3)
            .withWeightedItem(modItem("bronze_sword"), 3)
            .withWeightedItem(modItem("bronze_helmet"), 3)
            .withWeightedItem(modItem("bronze_chestplate"), 3)
            .withWeightedItem(modItem("bronze_leggings"), 3)
            .withWeightedItem(modItem("bronze_boots"), 3)
            .withEmpty(43)

    private fun createDungeonPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 10, 2f, 5f)
            .withWeightedItem(modItem("tin_ingot"), 10, 2f, 5f)
            .withWeightedItem(modItem("iridium_ore_item"), 2, 1f, 2f)
            .withWeightedItem(modItem("iridium_shard"), 10, 6f, 14f)
            .withEmpty(32)

    private fun createSpawnBonusPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 1f))
            .withWeightedItem(modItem("treetap"), 8)
            .withEmpty(2)

    private fun createStrongholdCorridorPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 5f)
            .withWeightedItem(modItem("iridium_ore_item"), 1, 1f, 4f)
            .withWeightedItem(modItem("iridium_shard"), 8, 4f, 14f)
            .withWeightedItem(modItem("bronze_pickaxe"), 3)
            .withWeightedItem(modItem("bronze_sword"), 3)
            .withWeightedItem(modItem("bronze_helmet"), 3)
            .withWeightedItem(modItem("bronze_chestplate"), 3)
            .withWeightedItem(modItem("bronze_leggings"), 3)
            .withWeightedItem(modItem("bronze_boots"), 3)
            .withEmpty(44)

    private fun createStrongholdStandardPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 5f)
            .withWeightedItem(modItem("iridium_ore_item"), 1)
            .withWeightedItem(modItem("iridium_shard"), 8, 2f, 5f)
            .withWeightedItem(modItem("bronze_pickaxe"), 3)
            .withWeightedItem(modItem("bronze_sword"), 3)
            .withWeightedItem(modItem("bronze_helmet"), 3)
            .withWeightedItem(modItem("bronze_chestplate"), 3)
            .withWeightedItem(modItem("bronze_leggings"), 3)
            .withWeightedItem(modItem("bronze_boots"), 3)
            .withEmpty(44)

    private fun createVillageBlacksmithPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 5f)
            .withWeightedItem(modItem("iridium_shard"), 8, 3f, 7f)
            .withWeightedItem(modItem("bronze_pickaxe"), 3)
            .withWeightedItem(modItem("bronze_sword"), 3)
            .withWeightedItem(modItem("bronze_helmet"), 3)
            .withWeightedItem(modItem("bronze_chestplate"), 3)
            .withWeightedItem(modItem("bronze_leggings"), 3)
            .withWeightedItem(modItem("bronze_boots"), 3)
            .withWeightedItem(modItem("bronze_ingot"), 5, 2f, 4f)
            .withWeightedItem(modItem("rubber_sapling"), 4, 1f, 4f)
            .withEmpty(52)

    private fun LootPool.Builder.withWeightedItem(item: Item, weight: Int, min: Float? = null, max: Float? = null): LootPool.Builder {
        val entry = ItemEntry.builder(item).weight(weight)
        if (min != null && max != null) {
            entry.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max)))
        }
        return with(entry)
    }

    private fun LootPool.Builder.withEmpty(weight: Int): LootPool.Builder =
        with(EmptyEntry.builder().weight(weight))

    private fun modItem(path: String): Item = Registries.ITEM.get(Ic2_120.id(path))

    private fun chest(path: String): Identifier = Identifier("minecraft", "chests/$path")
}
