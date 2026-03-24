package ic2_120.config

import ic2_120.Ic2_120
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

@Serializable
data class Ic2MainConfig(
    val general: GeneralConfig = GeneralConfig(),
    val creative: CreativeConfig = CreativeConfig(),
    val recycler: RecyclerConfig = RecyclerConfig()
)

@Serializable
data class GeneralConfig(
    val logConfigOnLoad: Boolean = true
)

@Serializable
data class CreativeConfig(
    val addFullChargeStorageItems: Boolean = true,
    val addFullFuelJetpack: Boolean = true
)

@Serializable
data class RecyclerConfig(
    // Item id list, e.g. ["minecraft:stick", "ic2_120:scrap"]
    val blacklist: List<String> = listOf("minecraft:stick")
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
    var current: Ic2MainConfig = Ic2MainConfig()
        private set

    fun loadOrThrow() {
        current = readOrCreateDefault()
        logLoaded("loaded")
    }

    fun reloadOrThrow() {
        current = readOrCreateDefault()
        logLoaded("reloaded")
    }

    private fun readOrCreateDefault(): Ic2MainConfig {
        if (!Files.exists(configPath)) {
            writeDefaultConfig(configPath)
            return Ic2MainConfig()
        }

        return try {
            val raw = Files.readString(configPath, StandardCharsets.UTF_8)
            json.decodeFromString(Ic2MainConfig.serializer(), raw)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse config: $configPath", e)
        }
    }

    private fun writeDefaultConfig(path: Path) {
        Files.createDirectories(path.parent)
        Files.writeString(path, defaultConfigText(), StandardCharsets.UTF_8)
    }

    private fun defaultConfigText(): String {
        return json.encodeToString(Ic2MainConfig.serializer(), Ic2MainConfig())
    }

    private fun logLoaded(action: String) {
        if (!current.general.logConfigOnLoad) return
        logger.info(
            "Config {}: creative.addFullChargeStorageItems={}, creative.addFullFuelJetpack={}, recycler.blacklist.size={}",
            action,
            current.creative.addFullChargeStorageItems,
            current.creative.addFullFuelJetpack,
            current.recycler.blacklist.size
        )
    }
}
