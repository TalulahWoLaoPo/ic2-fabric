package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item

// ========== 容器类 - 单元 ==========

@ModItem(name = "water_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class WaterCell : Item(FabricItemSettings())

@ModItem(name = "empty_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class EmptyCell : Item(FabricItemSettings())

@ModItem(name = "lava_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class LavaCell : Item(FabricItemSettings())

@ModItem(name = "air_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class AirCell : Item(FabricItemSettings())

@ModItem(name = "biofuel_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class BiofuelCell : Item(FabricItemSettings())

@ModItem(name = "bio_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class BioCell : Item(FabricItemSettings())

@ModItem(name = "weed_ex_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class WeedExCell : Item(FabricItemSettings())

// ========== 容器类 - 桶 ==========

@ModItem(name = "construction_foam_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
class ConstructionFoamBucket : Item(FabricItemSettings())

@ModItem(name = "biofuel_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
class BiofuelBucket : Item(FabricItemSettings())

@ModItem(name = "biomass_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
class BiomassBucket : Item(FabricItemSettings())

@ModItem(name = "construct_foam_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
class ConstructFoamBucket : Item(FabricItemSettings())

@ModItem(name = "coolant_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
class CoolantBucket : Item(FabricItemSettings())
