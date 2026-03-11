package ic2_120.content.item

import ic2_120.content.reactor.AbstractDamageableReactorComponent
import ic2_120.content.reactor.IReactor
import ic2_120.content.reactor.IReactorComponent
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.ItemStack
import kotlin.math.roundToInt

/**
 * 热交换器基类：与邻接组件和/或反应堆交换热量。
 * switchSide: 每周期与每个邻接可储热组件的最大交换量
 * switchReactor: 每周期与反应堆的最大交换量
 */
abstract class ReactorHeatExchangerBase(
    settings: FabricItemSettings,
    heatStorage: Int,
    private val switchSide: Int,
    private val switchReactor: Int
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
            max - myHeat + heat
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

        var myHeatDelta = 0

        if (switchSide > 0) {
            val heatAcceptors = mutableListOf<Triple<ItemStack, Int, Int>>()
            checkHeatAcceptor(reactor, x - 1, y, heatAcceptors)
            checkHeatAcceptor(reactor, x + 1, y, heatAcceptors)
            checkHeatAcceptor(reactor, x, y - 1, heatAcceptors)
            checkHeatAcceptor(reactor, x, y + 1, heatAcceptors)

            for ((otherStack, ox, oy) in heatAcceptors) {
                val comp = otherStack.item as IReactorComponent
                val mymed = getCurrentHeat(stack, reactor, x, y) * 100.0 / getMaxHeat(stack, reactor, x, y)
                val othermed = comp.getCurrentHeat(otherStack, reactor, ox, oy) * 100.0 / comp.getMaxHeat(otherStack, reactor, ox, oy)
                var add = (comp.getMaxHeat(otherStack, reactor, ox, oy) / 100.0 * (othermed + mymed / 2)).roundToInt()
                add = add.coerceIn(-switchSide, switchSide)
                if (othermed + mymed / 2 < 0.25) add = add.coerceIn(-1, 1)
                else if (othermed + mymed / 2 < 0.5) add = (add / 8).coerceIn(-switchSide, switchSide)
                else if (othermed + mymed / 2 < 0.75) add = (add / 4).coerceIn(-switchSide, switchSide)
                else if (othermed + mymed / 2 < 1.0) add = (add / 2).coerceIn(-switchSide, switchSide)
                if (kotlin.math.round(othermed * 10) / 10.0 > kotlin.math.round(mymed * 10) / 10.0) add = -add
                else if (kotlin.math.round(othermed * 10) / 10.0 == kotlin.math.round(mymed * 10) / 10.0) add = 0
                myHeatDelta -= add
                comp.alterHeat(otherStack, reactor, ox, oy, add)
            }
        }

        if (switchReactor > 0) {
            val mymed = getCurrentHeat(stack, reactor, x, y) * 100.0 / getMaxHeat(stack, reactor, x, y)
            val reactorMed = reactor.getHeat() * 100.0 / reactor.getMaxHeat()
            var add = (reactor.getMaxHeat() / 100.0 * (reactorMed + mymed / 2)).roundToInt()
            add = add.coerceIn(-switchReactor, switchReactor)
            val avg = reactorMed + mymed / 2
            if (avg < 0.25) add = add.coerceIn(-1, 1)
            else if (avg < 0.5) add = (add / 8).coerceIn(-switchReactor, switchReactor)
            else if (avg < 0.75) add = (add / 4).coerceIn(-switchReactor, switchReactor)
            else if (avg < 1.0) add = (add / 2).coerceIn(-switchReactor, switchReactor)
            if (kotlin.math.round(reactorMed * 10) / 10.0 > kotlin.math.round(mymed * 10) / 10.0) add = -add
            else if (kotlin.math.round(reactorMed * 10) / 10.0 == kotlin.math.round(mymed * 10) / 10.0) add = 0
            myHeatDelta -= add
            reactor.setHeat((reactor.getHeat() + add).coerceIn(0, reactor.getMaxHeat()))
        }

        alterHeat(stack, reactor, x, y, myHeatDelta)
    }

    private fun checkHeatAcceptor(reactor: IReactor, x: Int, y: Int, out: MutableList<Triple<ItemStack, Int, Int>>) {
        val s = reactor.getItemAt(x, y) ?: return
        if (s.item is IReactorComponent && (s.item as IReactorComponent).canStoreHeat(s, reactor, x, y)) {
            out.add(Triple(s, x, y))
        }
    }
}

@ModItem(name = "heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class HeatExchangerItem : ReactorHeatExchangerBase(FabricItemSettings(), 2500, 12, 4)

@ModItem(name = "reactor_heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorHeatExchangerItem : ReactorHeatExchangerBase(FabricItemSettings(), 5000, 0, 72)

@ModItem(name = "component_heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ComponentHeatExchangerItem : ReactorHeatExchangerBase(FabricItemSettings(), 5000, 36, 0)

@ModItem(name = "advanced_heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class AdvancedHeatExchangerItem : ReactorHeatExchangerBase(FabricItemSettings(), 10000, 24, 8)
