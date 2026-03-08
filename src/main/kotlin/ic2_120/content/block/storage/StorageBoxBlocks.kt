package ic2_120.content.block.storage

import ic2_120.content.ModBlockEntities
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.block.BlockRenderType
import net.minecraft.block.Blocks
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

/**
 * 储物箱基类
 *
 * 类似潜影盒，破坏时保留物品内容。
 */
abstract class StorageBoxBlock(settings: AbstractBlock.Settings) : BlockWithEntity(settings) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return StorageBoxBlockEntity(pos, state)
    }

    /**
     * 确保方块使用模型渲染（而不是实体渲染）
     */
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun onStacksDropped(state: BlockState, world: ServerWorld, pos: BlockPos, tool: ItemStack, dropExperience: Boolean) {
        // 不调用 super，防止掉落物品内容，只掉落方块本身
    }

    override fun getComparatorOutput(state: BlockState, world: net.minecraft.world.World, pos: BlockPos): Int {
        // 类似箱子，根据物品栏填充度输出红石信号
        val blockEntity = world.getBlockEntity(pos)
        if (blockEntity is StorageBoxBlockEntity) {
            if (blockEntity.isEmpty()) return 0

            val inventory = blockEntity.getInventory()
            var filledSlots = 0

            for (stack in inventory) {
                if (!stack.isEmpty) {
                    filledSlots++
                }
            }

            // 计算填充度：0-15
            val filledRatio = filledSlots.toFloat() / inventory.size
            val strength = (filledRatio * 14).toInt() + if (filledSlots > 0) 1 else 0
            return strength.coerceIn(0, 15)
        }
        return 0
    }

    override fun hasComparatorOutput(state: BlockState): Boolean = true
}

// ========== 储物箱方块 ==========

/**
 * 木质储物箱 - 27 格容量
 */
@ModBlock(name = "wooden_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage")
class WoodenStorageBoxBlock : StorageBoxBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.5f))

/**
 * 青铜储物箱 - 45 格容量
 */
@ModBlock(name = "bronze_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage")
class BronzeStorageBoxBlock : StorageBoxBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))


/**
 * 铁质储物箱 - 45 格容量
 */
@ModBlock(name = "iron_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage")
class IronStorageBoxBlock : StorageBoxBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))


/**
 * 钢制储物箱 - 63 格容量
 */
@ModBlock(name = "steel_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage")
class SteelStorageBoxBlock : StorageBoxBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))

/**
 * 铱储物箱 - 126 格容量
 */
@ModBlock(name = "iridium_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage")
class IridiumStorageBoxBlock : StorageBoxBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(6.0f, 8.0f))
