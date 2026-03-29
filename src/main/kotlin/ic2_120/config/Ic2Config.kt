package ic2_120.config

import ic2_120.Ic2_120
import ic2_120.content.uu.UuTemplateEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

@Serializable
data class Ic2MainConfig(
    val general: GeneralConfig = GeneralConfig(),
    val recycler: RecyclerConfig = RecyclerConfig(),
    val nuclear: NuclearConfig = NuclearConfig(),
    val uuReplication: UuReplicationConfig = UuReplicationConfig()
)

@Serializable
data class GeneralConfig(
    val logConfigOnLoad: Boolean = true
)

@Serializable
data class RecyclerConfig(
    // Item id list, e.g. ["minecraft:stick", "ic2_120:scrap"]
    val blacklist: List<String> = listOf("minecraft:stick")
)

@Serializable
data class NuclearConfig(
    /** 是否允许核反应堆在过热时爆炸 */
    val enableReactorExplosion: Boolean = true
)

@Serializable
data class UuReplicationConfig(
    /**
     * 可复制物品白名单：key 为物品 id，value 为所需 UU 物质，单位 uB。
     * 默认值来自旧版 IC2 `uu_scan_values.ini`，按内部 UU 值 * 10 转换为当前项目使用的 uB。
     */
    val replicationWhitelist: Map<String, Int> = defaultReplicationWhitelist()
)

private fun defaultReplicationWhitelist(): Map<String, Int> = linkedMapOf(
    "minecraft:cobblestone" to 10,
    "minecraft:dirt" to 149,
    "minecraft:coarse_dirt" to 168449,
    "minecraft:sand" to 1069,
    "minecraft:red_sand" to 130585,
    "minecraft:gravel" to 445,
    "minecraft:andesite" to 164,
    "minecraft:granite" to 171,
    "minecraft:diorite" to 178,
    "minecraft:sandstone" to 1336,
    "minecraft:chiseled_sandstone" to 144361168,
    "minecraft:smooth_sandstone" to 6901729,
    "minecraft:cut_sandstone" to 6901729,
    "minecraft:sandstone_stairs" to 10071709,
    "minecraft:sandstone_slab" to 72180584,
    "minecraft:red_sandstone" to 48120389,
    "minecraft:terracotta" to 6156,
    "minecraft:white_terracotta" to 52578,
    "minecraft:orange_terracotta" to 16098,
    "minecraft:yellow_terracotta" to 57941,
    "minecraft:light_gray_terracotta" to 92421,
    "minecraft:brown_terracotta" to 21024,
    "minecraft:red_terracotta" to 24934,
    "minecraft:blue_terracotta" to 54323740,
    "minecraft:coal" to 606,
    "minecraft:redstone" to 797,
    "minecraft:lapis_lazuli" to 4166,
    "minecraft:iron_ore" to 1073,
    "ic2_120:tin_ore" to 1360,
    "ic2_120:lead_ore" to 9182,
    "minecraft:gold_ore" to 10150,
    "ic2_120:uranium_ore" to 16070,
    "minecraft:clay_ball" to 17345,
    "minecraft:flint" to 3998,
    "minecraft:snowball" to 2763,
    "minecraft:prismarine" to 293815,
    "minecraft:prismarine_bricks" to 234638,
    "minecraft:dark_prismarine" to 9313624,
    "minecraft:prismarine_crystals" to 10071709,
    "minecraft:oak_log" to 35055,
    "minecraft:spruce_log" to 29388,
    "minecraft:birch_log" to 56050,
    "minecraft:jungle_log" to 71128,
    "minecraft:acacia_log" to 550122,
    "minecraft:dark_oak_log" to 26548,
    "minecraft:oak_planks" to 20540,
    "minecraft:oak_fence" to 27737,
    "minecraft:oak_sapling" to 53492,
    "minecraft:spruce_sapling" to 100233,
    "minecraft:birch_sapling" to 132929,
    "minecraft:jungle_sapling" to 780331,
    "minecraft:acacia_sapling" to 1275651,
    "minecraft:dark_oak_sapling" to 206796,
    "ic2_120:rubber_sapling" to 3571823,
    "ic2_120:rubber_wood" to 930362,
    "ic2_120:rubber_log" to 930362,
    "ic2_120:resin" to 33314116,
    "minecraft:stick" to 1154889,
    "minecraft:string" to 97213,
    "minecraft:rail" to 81174,
    "minecraft:mossy_cobblestone" to 197981,
    "minecraft:torch" to 677222,
    "minecraft:apple" to 428054,
    "minecraft:dandelion" to 301485,
    "minecraft:lily_pad" to 454920,
    "minecraft:poppy" to 465681,
    "minecraft:blue_orchid" to 12199535,
    "minecraft:allium" to 4256349,
    "minecraft:azure_bluet" to 1611473,
    "minecraft:red_tulip" to 4487912,
    "minecraft:orange_tulip" to 3866817,
    "minecraft:white_tulip" to 3733478,
    "minecraft:pink_tulip" to 8288679,
    "minecraft:oxeye_daisy" to 2767307,
    "minecraft:sunflower" to 4746121,
    "minecraft:lilac" to 3807327,
    "minecraft:rose_bush" to 4330835,
    "minecraft:peony" to 5021258,
    "minecraft:cocoa_beans" to 5346710,
    "minecraft:wheat_seeds" to 47381,
    "minecraft:melon_seeds" to 13025068,
    "minecraft:pumpkin_seeds" to 12832104,
    "minecraft:beetroot_seeds" to 12114224,
    "minecraft:wheat" to 6740599,
    "minecraft:potato" to 18627247,
    "minecraft:carrot" to 52494970,
    "minecraft:beetroot" to 144361168,
    "minecraft:cactus" to 2640753,
    "minecraft:sugar_cane" to 1944258,
    "minecraft:pumpkin" to 24399071,
    "minecraft:melon" to 8749162,
    "minecraft:brown_mushroom" to 629711,
    "minecraft:red_mushroom" to 1651415,
    "minecraft:bone" to 2616819,
    "minecraft:rotten_flesh" to 2264489,
    "minecraft:gunpowder" to 2471233,
    "minecraft:spider_eye" to 54323740,
    "minecraft:oak_stairs" to 5774447,
    "minecraft:oak_door" to 144361168,
    "minecraft:ladder" to 216541751,
    "minecraft:book" to 27497365,
    "minecraft:chest" to 8210114,
    "minecraft:bucket" to 43308350,
    "minecraft:stone_slab" to 32685547,
    "minecraft:stone_pressure_plate" to 54323740,
    "minecraft:stone_bricks" to 64160519,
    "minecraft:cracked_stone_bricks" to 866167005,
    "minecraft:crafting_table" to 866167005,
    "minecraft:obsidian" to 433083503,
    "minecraft:tnt" to 6035971,
    "minecraft:bread" to 21654175,
    "minecraft:diamond" to 28793,
    "minecraft:emerald" to 267542,
    "minecraft:iron_ingot" to 13428946,
    "minecraft:gold_ingot" to 57744467,
    "minecraft:gold_block" to 108270876,
    "minecraft:golden_apple" to 33967334,
    "minecraft:enchanted_golden_apple" to 288722335,
    "minecraft:name_tag" to 25106290,
    "minecraft:saddle" to 27067719,
    "minecraft:enchanted_book" to 1732334010,
    "minecraft:iron_horse_armor" to 31496982,
    "minecraft:golden_horse_armor" to 43308350,
    "minecraft:diamond_horse_armor" to 96240778,
    "minecraft:music_disc_13" to 35353755,
    "minecraft:music_disc_cat" to 39371228,
    "minecraft:wet_sponge" to 66628231,
    "minecraft:black_wool" to 54323740
)

private val DEFAULT_CONFIG_TEMPLATE = Ic2MainConfig(
    general = GeneralConfig(
        logConfigOnLoad = true
    ),
    recycler = RecyclerConfig(
        blacklist = listOf("minecraft:stick")
    ),
    nuclear = NuclearConfig(
        enableReactorExplosion = true
    ),
    uuReplication = UuReplicationConfig(
        replicationWhitelist = defaultReplicationWhitelist()
    )
)

object Ic2Config {
    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/config")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val configPath: Path by lazy {
        FabricLoader.getInstance().configDir.resolve("${Ic2_120.MOD_ID}.json")
    }

    @Volatile
    var current: Ic2MainConfig = DEFAULT_CONFIG_TEMPLATE
        private set

    fun loadOrThrow() {
        current = readOrCreateDefault()
        logLoaded("loaded")
    }

    fun reloadOrThrow() {
        current = readOrCreateDefault()
        logLoaded("reloaded")
    }

    fun prettyCurrentConfig(): String {
        return json.encodeToString(current)
    }

    fun getReplicationCostUb(itemId: String): Int? {
        val normalized = itemId.trim()
        if (normalized.isEmpty()) return null
        return current.uuReplication.replicationWhitelist[normalized]
            ?.takeIf { it > 0 }
    }

    fun getReplicationTemplate(itemId: String): UuTemplateEntry? {
        val cost = getReplicationCostUb(itemId) ?: return null
        return UuTemplateEntry(itemId, cost)
    }

    private fun readOrCreateDefault(): Ic2MainConfig {
        if (!Files.exists(configPath)) {
            writeDefaultConfig(configPath)
            return DEFAULT_CONFIG_TEMPLATE
        }

        return try {
            val raw = Files.readString(configPath, StandardCharsets.UTF_8)
            val config = json.decodeFromString<Ic2MainConfig>(raw)
            val hasLegacyCreativeSection = json.parseToJsonElement(raw).jsonObject.containsKey("creative")
            if (hasLegacyCreativeSection) {
                Files.writeString(configPath, json.encodeToString(config), StandardCharsets.UTF_8)
            }
            config
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse config: $configPath", e)
        }
    }

    private fun writeDefaultConfig(path: Path) {
        Files.createDirectories(path.parent)
        Files.writeString(path, json.encodeToString(DEFAULT_CONFIG_TEMPLATE), StandardCharsets.UTF_8)
    }

    private fun defaultConfigText(): String {
        return json.encodeToString(DEFAULT_CONFIG_TEMPLATE)
    }

    private fun logLoaded(action: String) {
        if (!current.general.logConfigOnLoad) return
        logger.info(
            "Config {}:\n{}",
            action,
            prettyCurrentConfig()
        )
    }
}
