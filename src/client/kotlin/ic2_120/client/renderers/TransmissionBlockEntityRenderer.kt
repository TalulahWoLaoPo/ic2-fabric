package ic2_120.client.renderers

import ic2_120.content.block.transmission.BevelGearBlock
import ic2_120.content.block.transmission.ShaftMaterial
import ic2_120.content.block.transmission.TransmissionBlockEntity
import ic2_120.content.block.transmission.TransmissionShaftBlock
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix3f
import org.joml.Matrix4f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class TransmissionBlockEntityRenderer(
    context: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<TransmissionBlockEntity> {
    companion object {
        private val WOOD_TEXTURE = Identifier("ic2", ShaftMaterial.WOOD.texturePath)
        private val IRON_TEXTURE = Identifier("ic2", ShaftMaterial.IRON.texturePath)
        private val STEEL_TEXTURE = Identifier("ic2", ShaftMaterial.STEEL.texturePath)
        private val CARBON_TEXTURE = Identifier("ic2", ShaftMaterial.CARBON.texturePath)
        private val BEVEL_TEXTURE = STEEL_TEXTURE

        private const val SHAFT_HALF = 1.0f / 6.0f
        private const val SHAFT_LENGTH_HALF = 0.5f
        private const val GEAR_THICKNESS_HALF = 0.065f
    }

    init {
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    override fun render(
        entity: TransmissionBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val world = entity.world ?: return
        val state = entity.cachedState
        val angle = ((world.time + tickDelta) * 8.0f) % 360.0f
        val fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE

        when (val block = state.block) {
            is TransmissionShaftBlock -> {
                val axis = state.get(Properties.AXIS)
                val texture = textureForMaterial(block.material)
                val vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture))
                matrices.push()
                matrices.translate(0.5, 0.5, 0.5)
                rotateByAxis(matrices, axis, angle)
                drawShaftAlongAxis(matrices, vc, fullLight, overlay, axis)
                matrices.pop()
            }

            is BevelGearBlock -> {
                val plane = state.get(BevelGearBlock.PLANE)
                val gearOffset = BevelGearBlock.distanceFromStep(state.get(BevelGearBlock.DISTANCE_STEP))
                val gearOuterRadius = gearOuterRadiusFromOffset(gearOffset)
                val vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(BEVEL_TEXTURE))
                val (firstAxis, secondAxis) = plane.axes()

                matrices.push()
                matrices.translate(0.5, 0.5, 0.5)

                // 第一只伞齿轮
                matrices.push()
                translateAlongAxis(matrices, firstAxis, gearOffset)
                rotateByAxis(matrices, firstAxis, angle)
                drawGear8Teeth(matrices, vc, fullLight, overlay, firstAxis, gearOuterRadius)
                matrices.pop()

                // 第二只伞齿轮（反向转动，形成啮合感）
                matrices.push()
                translateAlongAxis(matrices, secondAxis, -gearOffset)
                rotateByAxis(matrices, secondAxis, -angle)
                drawGear8Teeth(matrices, vc, fullLight, overlay, secondAxis, gearOuterRadius)
                matrices.pop()

                matrices.pop()
            }
        }
    }

    private fun textureForMaterial(material: ShaftMaterial): Identifier = when (material) {
        ShaftMaterial.WOOD -> WOOD_TEXTURE
        ShaftMaterial.IRON -> IRON_TEXTURE
        ShaftMaterial.STEEL -> STEEL_TEXTURE
        ShaftMaterial.CARBON -> CARBON_TEXTURE
    }

    private fun translateAlongAxis(matrices: MatrixStack, axis: Direction.Axis, offset: Float) {
        when (axis) {
            Direction.Axis.X -> matrices.translate(offset.toDouble(), 0.0, 0.0)
            Direction.Axis.Y -> matrices.translate(0.0, offset.toDouble(), 0.0)
            Direction.Axis.Z -> matrices.translate(0.0, 0.0, offset.toDouble())
        }
    }

    private fun rotateByAxis(matrices: MatrixStack, axis: Direction.Axis, angle: Float) {
        when (axis) {
            Direction.Axis.X -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle))
            Direction.Axis.Y -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle))
            Direction.Axis.Z -> matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle))
        }
    }

    private fun drawShaftAlongAxis(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        axis: Direction.Axis
    ) {
        drawOctagonalShaft(matrices, vc, light, overlay, axis, SHAFT_LENGTH_HALF, SHAFT_HALF)
    }

    private fun drawOctagonalShaft(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        axis: Direction.Axis,
        halfLength: Float,
        radius: Float
    ) {
        val entry = matrices.peek()
        val pos = entry.positionMatrix
        val normal = entry.normalMatrix

        val basis = basisForAxis(axis)
        val ax = basis[0]
        val ay = basis[1]
        val az = basis[2]
        val ux = basis[3]
        val uy = basis[4]
        val uz = basis[5]
        val vx = basis[6]
        val vy = basis[7]
        val vz = basis[8]
        val segments = 8
        val angleStep = (2.0 * PI / segments).toFloat()

        for (i in 0 until segments) {
            val a0 = i * angleStep
            val a1 = (i + 1) * angleStep

            val r0x = (cos(a0.toDouble()).toFloat() * ux + sin(a0.toDouble()).toFloat() * vx) * radius
            val r0y = (cos(a0.toDouble()).toFloat() * uy + sin(a0.toDouble()).toFloat() * vy) * radius
            val r0z = (cos(a0.toDouble()).toFloat() * uz + sin(a0.toDouble()).toFloat() * vz) * radius

            val r1x = (cos(a1.toDouble()).toFloat() * ux + sin(a1.toDouble()).toFloat() * vx) * radius
            val r1y = (cos(a1.toDouble()).toFloat() * uy + sin(a1.toDouble()).toFloat() * vy) * radius
            val r1z = (cos(a1.toDouble()).toFloat() * uz + sin(a1.toDouble()).toFloat() * vz) * radius

            val sx = -ax * halfLength
            val sy = -ay * halfLength
            val sz = -az * halfLength
            val ex = ax * halfLength
            val ey = ay * halfLength
            val ez = az * halfLength

            val p1x = sx + r0x
            val p1y = sy + r0y
            val p1z = sz + r0z
            val p2x = sx + r1x
            val p2y = sy + r1y
            val p2z = sz + r1z
            val p3x = ex + r1x
            val p3y = ey + r1y
            val p3z = ez + r1z
            val p4x = ex + r0x
            val p4y = ey + r0y
            val p4z = ez + r0z

            val mid = (a0 + a1) * 0.5f
            val nx = cos(mid.toDouble()).toFloat() * ux + sin(mid.toDouble()).toFloat() * vx
            val ny = cos(mid.toDouble()).toFloat() * uy + sin(mid.toDouble()).toFloat() * vy
            val nz = cos(mid.toDouble()).toFloat() * uz + sin(mid.toDouble()).toFloat() * vz

            val u0 = i / segments.toFloat()
            val u1 = (i + 1) / segments.toFloat()
            quadUv(
                vc, pos, normal, light, overlay,
                p1x, p1y, p1z, u0, 0f,
                p2x, p2y, p2z, u1, 0f,
                p3x, p3y, p3z, u1, 1f,
                p4x, p4y, p4z, u0, 1f,
                nx, ny, nz
            )
        }
    }

    // 为不同轴定义纹理/几何基向量，修正旧实现的 90 度偏转。
    private fun basisForAxis(axis: Direction.Axis): FloatArray = when (axis) {
        Direction.Axis.X -> floatArrayOf(
            1f, 0f, 0f,   // a (轴向)
            0f, 0f, 1f,   // u
            0f, 1f, 0f    // v
        )
        Direction.Axis.Y -> floatArrayOf(
            0f, 1f, 0f,
            1f, 0f, 0f,
            0f, 0f, 1f
        )
        Direction.Axis.Z -> floatArrayOf(
            0f, 0f, 1f,
            1f, 0f, 0f,
            0f, 1f, 0f
        )
    }

    private fun drawGear8Teeth(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        axis: Direction.Axis,
        outerRadius: Float
    ) {
        val toothBarHalfWidth = min(0.085f, outerRadius * 0.24f)
        repeat(4) { i ->
            matrices.push()
            rotateByAxis(matrices, axis, i * 45.0f)
            when (axis) {
                Direction.Axis.X -> {
                    drawCuboid(
                        matrices, vc, light, overlay,
                        -GEAR_THICKNESS_HALF, GEAR_THICKNESS_HALF,
                        -outerRadius, outerRadius,
                        -toothBarHalfWidth, toothBarHalfWidth
                    )
                }
                Direction.Axis.Y -> {
                    drawCuboid(
                        matrices, vc, light, overlay,
                        -outerRadius, outerRadius,
                        -GEAR_THICKNESS_HALF, GEAR_THICKNESS_HALF,
                        -toothBarHalfWidth, toothBarHalfWidth
                    )
                }
                Direction.Axis.Z -> {
                    drawCuboid(
                        matrices, vc, light, overlay,
                        -outerRadius, outerRadius,
                        -toothBarHalfWidth, toothBarHalfWidth,
                        -GEAR_THICKNESS_HALF, GEAR_THICKNESS_HALF
                    )
                }
            }
            matrices.pop()
        }
    }

    /**
     * 1:1 伞齿轮（90°）近似：
     * - 两齿轮中心分别沿两根正交轴偏移 d
     * - 中心距 C = sqrt(2) * d
     * - 分度半径 r = C / 2 = d / sqrt(2)
     * - 外半径 = r + 齿高（按比例取值）
     */
    private fun gearOuterRadiusFromOffset(offset: Float): Float {
        val pitchRadius = offset / sqrt(2.0f)
        val addendum = (pitchRadius * 0.12f).coerceIn(0.018f, 0.05f)
        return (pitchRadius + addendum).coerceAtMost(0.46f)
    }

    private fun drawCuboid(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float,
        minZ: Float,
        maxZ: Float
    ) {
        val entry = matrices.peek()
        val pos = entry.positionMatrix
        val normal = entry.normalMatrix

        quad(vc, pos, normal, light, overlay, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0f, 0f, 1f)
        quad(vc, pos, normal, light, overlay, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0f, 0f, -1f)
        quad(vc, pos, normal, light, overlay, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1f, 0f, 0f)
        quad(vc, pos, normal, light, overlay, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1f, 0f, 0f)
        quad(vc, pos, normal, light, overlay, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0f, 1f, 0f)
        quad(vc, pos, normal, light, overlay, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0f, -1f, 0f)
    }

    private fun quad(
        vc: VertexConsumer,
        pos: Matrix4f,
        normal: Matrix3f,
        light: Int,
        overlay: Int,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        vertex(vc, pos, normal, x1, y1, z1, 0f, 0f, light, overlay, nx, ny, nz)
        vertex(vc, pos, normal, x2, y2, z2, 1f, 0f, light, overlay, nx, ny, nz)
        vertex(vc, pos, normal, x3, y3, z3, 1f, 1f, light, overlay, nx, ny, nz)
        vertex(vc, pos, normal, x4, y4, z4, 0f, 1f, light, overlay, nx, ny, nz)
    }

    private fun quadUv(
        vc: VertexConsumer,
        pos: Matrix4f,
        normal: Matrix3f,
        light: Int,
        overlay: Int,
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float,
        x4: Float, y4: Float, z4: Float, u4: Float, v4: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        vertex(vc, pos, normal, x1, y1, z1, u1, v1, light, overlay, nx, ny, nz)
        vertex(vc, pos, normal, x2, y2, z2, u2, v2, light, overlay, nx, ny, nz)
        vertex(vc, pos, normal, x3, y3, z3, u3, v3, light, overlay, nx, ny, nz)
        vertex(vc, pos, normal, x4, y4, z4, u4, v4, light, overlay, nx, ny, nz)
    }

    private fun vertex(
        vc: VertexConsumer,
        pos: Matrix4f,
        normal: Matrix3f,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        light: Int,
        overlay: Int,
        nx: Float,
        ny: Float,
        nz: Float
    ) {
        vc.vertex(pos, x, y, z)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(overlay.takeUnless { it == 0 } ?: OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(normal, nx, ny, nz)
            .next()
    }
}
