package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.block.storage.EnergyStorageBlockEntity
import ic2_120.content.block.storage.EnergyStorageBlock.EnergyStorageBlockItem
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

// ============== Block Definitions ==============

@ModBlock(name = "batbox", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class BatBoxBlock : EnergyStorageBlock(EnergyStorageConfig.BATBOX) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.BatBoxBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.BatBoxBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class BatBoxBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.BATBOX) {
        override val translationKeyFull: String = "block.ic2_120.batbox_full"
    }
}

@ModBlock(name = "cesu", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class CesuBlock : EnergyStorageBlock(EnergyStorageConfig.CESU) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.CesuBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.CesuBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class CesuBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.CESU) {
        override val translationKeyFull: String = "block.ic2_120.cesu_full"
    }
}

@ModBlock(name = "mfe", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfeBlock : EnergyStorageBlock(EnergyStorageConfig.MFE) {
    override fun getCasingDrop(): Item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "advanced_machine"))
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfeBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.MfeBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class MfeBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFE) {
        override val translationKeyFull: String = "block.ic2_120.mfe_full"
    }
}

@ModBlock(name = "mfsu", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfsuBlock : EnergyStorageBlock(EnergyStorageConfig.MFSU) {
    override fun getCasingDrop(): Item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "advanced_machine"))
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfsuBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.MfsuBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class MfsuBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFSU) {
        override val translationKeyFull: String = "block.ic2_120.mfsu_full"
    }
}

abstract class ChargepadBlock(config: EnergyStorageConfig) : EnergyStorageBlock(config) {
    init {
        defaultState = stateManager.defaultState
            .with(Properties.HORIZONTAL_FACING, net.minecraft.util.math.Direction.NORTH)
            .with(EnergyStorageBlock.ACTIVE, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(EnergyStorageBlock.ACTIVE)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(
        state: BlockState,
        world: net.minecraft.world.BlockView,
        pos: net.minecraft.util.math.BlockPos,
        context: net.minecraft.block.ShapeContext
    ): net.minecraft.util.shape.VoxelShape = FULL_MINUS_TOP

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: net.minecraft.world.BlockView,
        pos: net.minecraft.util.math.BlockPos,
        context: net.minecraft.block.ShapeContext
    ): net.minecraft.util.shape.VoxelShape = FULL_MINUS_TOP

    companion object {
        /** 去掉顶部的 1/16，与模型视觉对齐 */
        private val FULL_MINUS_TOP = net.minecraft.util.shape.VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 15.0 / 16.0, 1.0)
    }
}

@ModBlock(name = "batbox_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class BatBoxChargepadBlock : ChargepadBlock(EnergyStorageConfig.BATBOX_CHARGEPAD) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.BatBoxChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.BatBoxChargepadBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class BatBoxChargepadBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.BATBOX_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.batbox_chargepad_full"
    }
}

@ModBlock(name = "cesu_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class CesuChargepadBlock : ChargepadBlock(EnergyStorageConfig.CESU_CHARGEPAD) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.CesuChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.CesuChargepadBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class CesuChargepadBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.CESU_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.cesu_chargepad_full"
    }
}

@ModBlock(name = "mfe_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfeChargepadBlock : ChargepadBlock(EnergyStorageConfig.MFE_CHARGEPAD) {
    override fun getCasingDrop(): Item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "advanced_machine"))
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfeChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.MfeChargepadBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class MfeChargepadBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFE_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.mfe_chargepad_full"
    }
}

@ModBlock(name = "mfsu_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfsuChargepadBlock : ChargepadBlock(EnergyStorageConfig.MFSU_CHARGEPAD) {
    override fun getCasingDrop(): Item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "advanced_machine"))
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfsuChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.MfsuChargepadBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class MfsuChargepadBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFSU_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.mfsu_chargepad_full"
    }
}
