package ic2_120.content.item

import ic2_120.content.reactor.AbstractDamageableReactorComponent
import ic2_120.content.reactor.AbstractReactorComponent
import ic2_120.content.reactor.IReactor
import ic2_120.content.reactor.IReactorComponent
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.ItemStack

/**
 * 散热片基类：热容量 + 自身蒸发 + 吸堆温。
 * selfVent: 每周期自身蒸发热量；reactorVent: 每周期从堆吸收热量。
 */
abstract class ReactorHeatVentBase(
    settings: FabricItemSettings,
    heatStorage: Int,
    private val selfVent: Int,
    private val reactorVent: Int
) : AbstractDamageableReactorComponent(settings, heatStorage) {

    override fun canStoreHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Boolean = true

    override fun getMaxHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = maxUse

    override fun getCurrentHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = getUse(stack)

    override fun alterHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heat: Int): Int {
        var myHeat = getCurrentHeat(stack, reactor, x, y)
        myHeat += heat
        val max = getMaxHeat(stack, reactor, x, y)
        return if (myHeat > max) {
            reactor.setItemAt(x, y, null)
            max - myHeat + heat  // 返回未能吸收的热量（溢出）
        } else {
            if (myHeat < 0) {
                val overflow = myHeat
                myHeat = 0
                setUse(stack, 0)
                overflow
            } else {
                setUse(stack, myHeat)
                0
            }
        }
    }

    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        if (!heatRun) return

        if (reactorVent > 0) {
            var rheat = reactor.getHeat()
            val reactorDrain = minOf(rheat, reactorVent)
            rheat -= reactorDrain
            if (alterHeat(stack, reactor, x, y, reactorDrain) > 0) return
            reactor.setHeat(rheat)
        }

        alterHeat(stack, reactor, x, y, -selfVent)
    }
}

@ModItem(name = "heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class HeatVentItem : ReactorHeatVentBase(FabricItemSettings(), 1000, 6, 0)

@ModItem(name = "reactor_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorHeatVentItem : ReactorHeatVentBase(FabricItemSettings(), 1000, 5, 5)

@ModItem(name = "advanced_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class AdvancedHeatVentItem : ReactorHeatVentBase(FabricItemSettings(), 1000, 12, 12)

@ModItem(name = "overclocked_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class OverclockedHeatVentItem : ReactorHeatVentBase(FabricItemSettings(), 1000, 20, 36)

/** 元件散热片：无热容量，向四方向邻接可储热组件蒸发 4 点热量 */
@ModItem(name = "component_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ComponentHeatVentItem(settings: FabricItemSettings = FabricItemSettings()) : AbstractReactorComponent(settings) {

    private val sideVent = 4

    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        if (!heatRun) return
        cool(reactor, x - 1, y)
        cool(reactor, x + 1, y)
        cool(reactor, x, y - 1)
        cool(reactor, x, y + 1)
    }

    private fun cool(reactor: IReactor, x: Int, y: Int) {
        val other = reactor.getItemAt(x, y) ?: return
        if (other.item !is IReactorComponent) return
        val comp = other.item as IReactorComponent
        if (!comp.canStoreHeat(other, reactor, x, y)) return
        comp.alterHeat(other, reactor, x, y, -sideVent)
    }
}
