package ic2_120.integration.ftbchunks

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI
import dev.ftb.mods.ftbchunks.api.Protection
import dev.ftb.mods.ftblibrary.math.ChunkDimPos
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID

/**
 * FTB Chunks 领地保护集成。
 * 在 IC2 破坏方块前检查目标位置是否在他人领地内。
 *
 * FTB Chunks 为软依赖：未安装时所有检查直接放行。
 */
object ClaimProtection {

    private val ftbChunksLoaded: Boolean by lazy {
        FabricLoader.getInstance().isModLoaded("ftbchunks")
    }

    /**
     * 检查在 [pos] 处的方块操作是否应被领地保护阻止。
     *
     * @param world 服务端世界
     * @param pos    目标方块位置
     * @param ownerUuid  操作发起者的 UUID（机器放置者或玩家），用于判断是否为领地成员
     * @param protection FTB Chunks 保护类型，默认 [Protection.EDIT_BLOCK]
     * @return true 表示应阻止操作
     */
    fun isProtected(world: World, pos: BlockPos, ownerUuid: UUID? = null, protection: Protection = Protection.EDIT_BLOCK): Boolean {
        if (world.isClient) return false
        if (!ftbChunksLoaded) return false
        return checkProtection(world, pos, ownerUuid, protection)
    }

    /**
     * 检查在 [pos] 处的方块操作是否应被领地保护阻止（以实体为发起者）。
     *
     * @param world 服务端世界
     * @param pos    目标方块位置
     * @param actor  操作发起实体（如玩家、镭射弹的 owner）
     * @param protection FTB Chunks 保护类型，默认 [Protection.EDIT_BLOCK]
     * @return true 表示应阻止操作
     */
    fun isProtected(world: World, pos: BlockPos, actor: net.minecraft.entity.Entity?, protection: Protection = Protection.EDIT_BLOCK): Boolean {
        if (world.isClient) return false
        if (!ftbChunksLoaded) return false
        return checkProtectionWithActor(world, pos, actor, protection)
    }

    private fun checkProtection(world: World, pos: BlockPos, ownerUuid: UUID?, protection: Protection): Boolean {
        val api = FTBChunksAPI.api()
        if (!api.isManagerLoaded) return false
        val manager = api.manager

        // 尝试用在线玩家身份检查
        val server = world.server ?: return false
        val ownerPlayer: ServerPlayerEntity? = ownerUuid?.let { server.playerManager.getPlayer(it) }

        if (ownerPlayer != null) {
            return manager.shouldPreventInteraction(
                ownerPlayer, Hand.MAIN_HAND, pos,
                protection, null
            )
        }

        // 玩家离线：手动检查领地归属
        return checkOfflineProtection(manager, world, pos, ownerUuid, protection)
    }

    private fun checkProtectionWithActor(world: World, pos: BlockPos, actor: net.minecraft.entity.Entity?, protection: Protection): Boolean {
        val api = FTBChunksAPI.api()
        if (!api.isManagerLoaded) return false
        val manager = api.manager

        if (actor is ServerPlayerEntity) {
            return manager.shouldPreventInteraction(
                actor, Hand.MAIN_HAND, pos,
                protection, null
            )
        }

        // 非玩家实体（如镭射弹），取 owner UUID 做离线检查
        val ownerUuid = (actor as? net.minecraft.entity.projectile.ProjectileEntity)?.owner
            ?.uuid
        return checkOfflineProtection(manager, world, pos, ownerUuid, protection)
    }

    /**
     * 玩家离线时，根据领地具体权限设置（PUBLIC/ALLIES/PRIVATE）判断。
     */
    private fun checkOfflineProtection(
        manager: dev.ftb.mods.ftbchunks.api.ClaimedChunkManager,
        world: World,
        pos: BlockPos,
        ownerUuid: UUID?,
        protection: Protection
    ): Boolean {
        val chunkDimPos = ChunkDimPos(world, pos)
        val claimedChunk = manager.getChunk(chunkDimPos) ?: return false // 未被领取，允许

        if (ownerUuid == null) return true // 被领取但无 owner 信息，阻止

        val teamData = claimedChunk.teamData
        val property = when (protection) {
            Protection.INTERACT_BLOCK, Protection.RIGHT_CLICK_ITEM ->
                dev.ftb.mods.ftbchunks.api.FTBChunksProperties.BLOCK_INTERACT_MODE
            Protection.INTERACT_ENTITY ->
                dev.ftb.mods.ftbchunks.api.FTBChunksProperties.ENTITY_INTERACT_MODE
            Protection.ATTACK_NONLIVING_ENTITY ->
                dev.ftb.mods.ftbchunks.api.FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE
            else ->
                dev.ftb.mods.ftbchunks.api.FTBChunksProperties.BLOCK_EDIT_MODE
        }
        val mode = teamData.team.getProperty(property)

        if (mode == dev.ftb.mods.ftbteams.api.property.PrivacyMode.PUBLIC) return false
        if (mode == dev.ftb.mods.ftbteams.api.property.PrivacyMode.ALLIES) return !teamData.isAlly(ownerUuid)
        // PRIVATE: 仅 owner
        return !teamData.team.getRankForPlayer(ownerUuid).isOwner
    }
}
