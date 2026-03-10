package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

// ========== 反应堆核心部件 ==========

@ModItem(name = "reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorCoolantCellItem : Item(FabricItemSettings())

@ModItem(name = "triple_reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class TripleReactorCoolantCellItem : Item(FabricItemSettings())

@ModItem(name = "sextuple_reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class SextupleReactorCoolantCellItem : Item(FabricItemSettings())

@ModItem(name = "reactor_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorPlatingItem : Item(FabricItemSettings())

@ModItem(name = "reactor_heat_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorHeatPlatingItem : Item(FabricItemSettings())

@ModItem(name = "containment_reactor_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ContainmentReactorPlatingItem : Item(FabricItemSettings())

@ModItem(name = "heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class HeatExchangerItem : Item(FabricItemSettings())

@ModItem(name = "reactor_heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorHeatExchangerItem : Item(FabricItemSettings())

@ModItem(name = "component_heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ComponentHeatExchangerItem : Item(FabricItemSettings())

@ModItem(name = "advanced_heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class AdvancedHeatExchangerItem : Item(FabricItemSettings())

@ModItem(name = "heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class HeatVentItem : Item(FabricItemSettings())

@ModItem(name = "reactor_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorHeatVentItem : Item(FabricItemSettings())

@ModItem(name = "overclocked_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class OverclockedHeatVentItem : Item(FabricItemSettings())

@ModItem(name = "component_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ComponentHeatVentItem : Item(FabricItemSettings())

@ModItem(name = "advanced_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class AdvancedHeatVentItem : Item(FabricItemSettings())

@ModItem(name = "neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class NeutronReflectorItem : Item(FabricItemSettings())

@ModItem(name = "thick_neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ThickNeutronReflectorItem : Item(FabricItemSettings())

@ModItem(name = "iridium_neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class IridiumNeutronReflectorItem : Item(FabricItemSettings())

@ModItem(name = "rsh_condensator", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class RshCondensatorItem : Item(FabricItemSettings())

@ModItem(name = "lzh_condensator", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class LzhCondensatorItem : Item(FabricItemSettings())

@ModItem(name = "uranium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class UraniumFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "dual_uranium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DualUraniumFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "quad_uranium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class QuadUraniumFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "mox_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class MoxFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "dual_mox_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DualMoxFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "quad_mox_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class QuadMoxFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "lithium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class LithiumFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "depleted_isotope_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DepletedIsotopeFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "heatpack", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class HeatpackItem : Item(FabricItemSettings())
