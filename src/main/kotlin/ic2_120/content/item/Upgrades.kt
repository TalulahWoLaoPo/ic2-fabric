package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item

// ========== 工具升级类 ==========

@ModItem(name = "overclocker_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class OverclockerUpgrade : Item(FabricItemSettings())

@ModItem(name = "transformer_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class TransformerUpgrade : Item(FabricItemSettings())

@ModItem(name = "energy_storage_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class EnergyStorageUpgrade : Item(FabricItemSettings())

@ModItem(name = "redstone_inverter_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class RedstoneInverterUpgrade : Item(FabricItemSettings())

@ModItem(name = "ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class EjectorUpgrade : Item(FabricItemSettings())

@ModItem(name = "advanced_ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class AdvancedEjectorUpgrade : Item(FabricItemSettings())

@ModItem(name = "pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class PullingUpgrade : Item(FabricItemSettings())

@ModItem(name = "advanced_pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class AdvancedPullingUpgrade : Item(FabricItemSettings())

@ModItem(name = "fluid_ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class FluidEjectorUpgrade : Item(FabricItemSettings())

@ModItem(name = "fluid_pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class FluidPullingUpgrade : Item(FabricItemSettings())
