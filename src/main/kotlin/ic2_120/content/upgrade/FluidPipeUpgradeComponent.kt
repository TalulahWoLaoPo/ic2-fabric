package ic2_120.content.upgrade

import ic2_120.content.item.FluidEjectorUpgrade
import ic2_120.content.item.FluidPullingUpgrade
import net.minecraft.fluid.Fluid
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction

object FluidPipeUpgradeComponent {
    private const val NBT_FILTER = "PipeFluidFilter"
    private const val NBT_DIRECTION = "PipeFluidDirection"

    /**
     * 统一入口：机器同时具备 Inventory 与 IFluidPipeUpgradeSupport 时，按升级槽应用流体管道升级。
     */
    fun <T> apply(machine: T, upgradeSlotIndices: IntArray) where T : Inventory, T : IFluidPipeUpgradeSupport {
        apply(machine as Inventory, upgradeSlotIndices, machine as Any)
    }

    fun apply(inventory: Inventory, upgradeSlotIndices: IntArray, machine: Any) {
        if (machine !is IFluidPipeUpgradeSupport) return

        var provider = false
        var receiver = false
        var providerFilter: Fluid? = null
        var receiverFilter: Fluid? = null
        var providerSide: Direction? = null
        var receiverSide: Direction? = null

        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (stack.isEmpty) continue
            when (stack.item) {
                is FluidEjectorUpgrade -> {
                    provider = true
                    if (providerFilter == null) providerFilter = readFilter(stack)
                    if (providerSide == null) providerSide = readDirection(stack)
                }
                is FluidPullingUpgrade -> {
                    receiver = true
                    if (receiverFilter == null) receiverFilter = readFilter(stack)
                    if (receiverSide == null) receiverSide = readDirection(stack)
                }
            }
        }

        machine.fluidPipeProviderEnabled = provider
        machine.fluidPipeReceiverEnabled = receiver
        machine.fluidPipeProviderFilter = providerFilter
        machine.fluidPipeReceiverFilter = receiverFilter
        machine.fluidPipeProviderSide = providerSide
        machine.fluidPipeReceiverSide = receiverSide
    }

    fun readFilter(stack: ItemStack): Fluid? {
        val nbt = stack.nbt ?: return null
        val raw = nbt.getString(NBT_FILTER)
        if (raw.isNullOrBlank()) return null
        val id = Identifier.tryParse(raw) ?: return null
        return if (Registries.FLUID.containsId(id)) Registries.FLUID.get(id) else null
    }

    fun writeFilter(stack: ItemStack, fluid: Fluid?) {
        val nbt = stack.orCreateNbt
        if (fluid == null) {
            nbt.remove(NBT_FILTER)
            return
        }
        val id = Registries.FLUID.getId(fluid)
        if (id.path != "empty") {
            nbt.putString(NBT_FILTER, id.toString())
        } else {
            nbt.remove(NBT_FILTER)
        }
    }

    fun readDirection(stack: ItemStack): Direction? {
        val nbt = stack.nbt ?: return null
        val raw = nbt.getString(NBT_DIRECTION)
        if (raw.isNullOrBlank()) return null
        return Direction.byName(raw.lowercase())
    }

    fun writeDirection(stack: ItemStack, side: Direction?) {
        val nbt = stack.orCreateNbt
        if (side == null) {
            nbt.remove(NBT_DIRECTION)
            return
        }
        nbt.putString(NBT_DIRECTION, side.name.lowercase())
    }

    fun nextDirection(current: Direction?): Direction? {
        return when (current) {
            null -> Direction.DOWN
            Direction.DOWN -> Direction.UP
            Direction.UP -> Direction.NORTH
            Direction.NORTH -> Direction.SOUTH
            Direction.SOUTH -> Direction.WEST
            Direction.WEST -> Direction.EAST
            Direction.EAST -> null
        }
    }
}
