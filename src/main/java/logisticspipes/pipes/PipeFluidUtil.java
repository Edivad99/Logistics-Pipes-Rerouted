package logisticspipes.pipes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.NeighborBlockEntity;
import logisticspipes.api.util.ITankUtil;
import logisticspipes.inventory.tank.ISpecialTankAccessHandler;
import logisticspipes.inventory.tank.SpecialTankHandler;
import logisticspipes.inventory.tank.SpecialTankUtil;
import logisticspipes.inventory.tank.TankUtil;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;

/**
 * Finding and reading the fluid inventories next to a fluid pipe.
 *
 * <p>Was Kotlin, where the three pipe-facing methods were extension functions on
 * {@link FluidRoutedPipe} -- which is why Java had to call them as
 * {@code PipeFluidUtil.getAdjacentTanks(pipe, flag)}. Every caller but two was already
 * Java writing it that way, so as plain statics the call sites read the same and one less object
 * exists at runtime.</p>
 */
public final class PipeFluidUtil {

    private PipeFluidUtil() {
    }

    /**
     * The fluid inventory a block entity offers on one side, or null if it has none.
     *
     * <p>Storage networks come first: they hold fluids without tanks to enumerate, so they cannot be
     * reached through the fluid capability at all. Same precedence as the item side, where
     * {@code InventoryUtilFactory} consults the special handlers before the item capability.</p>
     */
    @Nullable
    public static ITankUtil getTankUtilForBE(@Nullable BlockEntity blockEntity, @Nullable Direction dirOnEntity) {
        ITankUtil special = SpecialTankHandler.get().getSpecialTankUtilFor(blockEntity, dirOnEntity);
        if (special != null) {
            return special;
        }

        if (blockEntity == null || blockEntity.getLevel() == null) {
            return null;
        }
        ResourceHandler<FluidResource> fluidHandler =
            blockEntity.getLevel().getCapability(Capabilities.Fluid.BLOCK, blockEntity.getBlockPos(), dirOnEntity);
        if (fluidHandler == null) {
            return null;
        }

        if (SpecialTankHandler.get().hasHandlerFor(blockEntity)
            && SpecialTankHandler.get().getTankHandlerFor(
            blockEntity) instanceof ISpecialTankAccessHandler handler) {
            return new SpecialTankUtil(fluidHandler, blockEntity, handler);
        }
        return new TankUtil(fluidHandler);
    }

    /**
     * The neighbours this pipe may exchange fluid with, paired with their tank view.
     */
    public static List<Pair<NeighborBlockEntity<BlockEntity>, ITankUtil>> getAdjacentTanks(FluidRoutedPipe pipe,
        boolean listNearbyPipes) {
        List<Pair<NeighborBlockEntity<BlockEntity>, ITankUtil>> result = new ArrayList<>();
        for (NeighborBlockEntity<BlockEntity> adjacent : pipe.getAvailableAdjacent().fluidTanks()) {
            if (!pipe.isConnectableTank(adjacent.getBlockEntity(), adjacent.getDirection(), listNearbyPipes)) {
                continue;
            }
            ITankUtil util = getTankUtilForBE(adjacent.getBlockEntity(), adjacent.getOurDirection());
            if (util != null) {
                result.add(new Pair<>(adjacent, util));
            }
        }
        return result;
    }

    public static List<BlockEntity> getAllTankTiles(FluidRoutedPipe pipe) {
        List<BlockEntity> result = new ArrayList<>();
        for (Pair<NeighborBlockEntity<BlockEntity>, ITankUtil> pair : getAdjacentTanks(pipe, false)) {
            result.addAll(SpecialTankHandler.get().getBaseBlockEntitiesFor(pair.getValue1().getBlockEntity()));
        }
        return result;
    }

    /**
     * Everything the adjacent tanks hold, as the container items a satellite advertises, with the
     * amounts of one fluid summed into a single entry.
     */
    public static List<ItemIdentifierStack> fluidsToItemList(PipeFluidSatellite pipe) {
        Set<FluidIdentifier> seen = new HashSet<>();
        List<ItemIdentifierStack> outputList = new ArrayList<>();
        for (Pair<NeighborBlockEntity<BlockEntity>, ITankUtil> pair : getAdjacentTanks(pipe, false)) {
            for (FluidStack stack : pair.getValue2().tanks().toList()) {
                FluidIdentifierStack identStack = FluidIdentifierStack.getFromStack(stack);
                if (identStack == null) {
                    continue;
                }
                ItemIdentifier container = identStack.getFluid().getItemIdentifier();
                if (seen.add(identStack.getFluid())) {
                    outputList.add(container.makeStack(identStack.getAmount()));
                } else {
                    // Same fluid from a second tank: fold it into the entry already listed.
                    ItemIdentifierStack existing = outputList.stream()
                        .filter(entry -> entry.getItem() == container)
                        .findFirst()
                        .orElseThrow();
                    existing.setStackSize(existing.getStackSize() + identStack.getAmount());
                }
            }
        }
        return outputList;
    }
}
