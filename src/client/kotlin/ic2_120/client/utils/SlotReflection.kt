package ic2_120.client.utils

import net.minecraft.screen.slot.Slot
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

/**
 * Slot 字段反射工具类
 *
 * 支持 fallback 机制，同时兼容：
 * - 生产环境（intermediary 混淆名）：field_7873 / field_7872
 * - 开发环境（Yarn 映射名）：x / y
 */
object SlotReflection {

    private val logger = LoggerFactory.getLogger("ic2_120/SlotReflection")

    /**
     * 获取 Slot 的 x 字段
     */
    val xField: Field? by lazy {
        // 先尝试生产环境的混淆名
        runCatching {
            Slot::class.java.getDeclaredField("field_7873").apply { isAccessible = true }
        }.getOrNull() ?: runCatching {
            // Fallback: 尝试开发环境的 Yarn 映射名
            Slot::class.java.getDeclaredField("x").apply { isAccessible = true }
        }.getOrNull()?.also {
            logger.debug("Using Yarn mapped name 'x' for Slot field (dev environment)")
        } ?: run {
            logger.error("Failed to access Slot x field!")
            null
        }
    }

    /**
     * 获取 Slot 的 y 字段
     */
    val yField: Field? by lazy {
        // 先尝试生产环境的混淆名
        runCatching {
            Slot::class.java.getDeclaredField("field_7872").apply { isAccessible = true }
        }.getOrNull() ?: runCatching {
            // Fallback: 尝试开发环境的 Yarn 映射名
            Slot::class.java.getDeclaredField("y").apply { isAccessible = true }
        }.getOrNull()?.also {
            logger.debug("Using Yarn mapped name 'y' for Slot field (dev environment)")
        } ?: run {
            logger.error("Failed to access Slot y field!")
            null
        }
    }

    /**
     * 设置 Slot 的 x 坐标
     */
    fun setX(slot: Slot, value: Int) {
        xField?.setInt(slot, value) ?: logger.warn("Cannot set Slot x field - field not accessible")
    }

    /**
     * 设置 Slot 的 y 坐标
     */
    fun setY(slot: Slot, value: Int) {
        yField?.setInt(slot, value) ?: logger.warn("Cannot set Slot y field - field not accessible")
    }

    /**
     * 获取 Slot 的 x 坐标
     */
    fun getX(slot: Slot): Int = xField?.getInt(slot) ?: 0

    /**
     * 获取 Slot 的 y 坐标
     */
    fun getY(slot: Slot): Int = yField?.getInt(slot) ?: 0
}
