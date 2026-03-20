package ic2_120.client

import ic2_120.content.item.DiamondDrill
import ic2_120.content.item.Drill
import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.energy.IElectricTool
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * 钻头 Tooltip 客户端处理器
 *
 * 参考量子套装，动态添加：
 * - 还能挖 X 次（根据当前电量与单次耗能计算）
 * - 铱钻头：精准采集快捷键（动态显示玩家绑定的按键）
 */
@Environment(EnvType.CLIENT)
object DrillTooltipHandler {

    private const val DRILL_EU_PER_BLOCK = 50L
    private const val DIAMOND_DRILL_EU_PER_BLOCK = 80L
    private const val IRIDIUM_DRILL_EU_PER_BLOCK = 800L

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, lines ->
            val item = stack.item
            if (item !is IElectricTool) return@register

            val euPerBlock = when (item) {
                is Drill -> DRILL_EU_PER_BLOCK
                is DiamondDrill -> DIAMOND_DRILL_EU_PER_BLOCK
                is IridiumDrill -> IRIDIUM_DRILL_EU_PER_BLOCK
                else -> null
            }

            if (euPerBlock != null) {
                val energy = item.getEnergy(stack)
                val remainingBlocks = if (euPerBlock > 0) energy / euPerBlock else 0L
                lines.add(
                    Text.literal("还能挖 ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal("$remainingBlocks").formatted(Formatting.YELLOW))
                        .append(Text.literal(" 次").formatted(Formatting.GRAY))
                )
            }

            // 铱钻头：精准采集快捷键（与夜视/飞行共用统一模式键 Alt+M）
            if (item is IridiumDrill) {
                val modeKey = ModeKeybinds.getModeKey()
                val boundKeyName = modeKey.boundKeyLocalizedText.string
                lines.add(
                    Text.literal("精准采集按键: ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal("Alt + ").formatted(Formatting.YELLOW))
                        .append(Text.literal(boundKeyName).formatted(Formatting.YELLOW))
                )
            }
        }
    }
}
