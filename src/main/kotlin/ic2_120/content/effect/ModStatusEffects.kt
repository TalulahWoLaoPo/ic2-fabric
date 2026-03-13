package ic2_120.content.effect

import ic2_120.Ic2_120
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

object ModStatusEffects {
    val SOLAR_GENERATING: StatusEffect = Registry.register(
        Registries.STATUS_EFFECT,
        Ic2_120.id("solar_generating"),
        object : StatusEffect(StatusEffectCategory.BENEFICIAL, 0xF6D743) {}
    )

    fun register() {
        // 触发对象初始化，保证效果已注册。
    }
}

