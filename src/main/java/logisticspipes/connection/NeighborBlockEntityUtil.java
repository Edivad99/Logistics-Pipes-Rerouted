package logisticspipes.connection;

import java.util.Optional;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.NeighborBlockEntity;
import logisticspipes.api.util.ITankUtil;
import logisticspipes.inventory.IInventoryUtil;
import logisticspipes.pipes.PipeFluidUtil;
import logisticspipes.proxy.SimpleServiceLocator;

/** The main-side helpers for {@link NeighborBlockEntity}, which cannot live on the API type itself. */
public final class NeighborBlockEntityUtil {

    private NeighborBlockEntityUtil() {
    }

    public static @Nullable IInventoryUtil getInventoryUtil(NeighborBlockEntity<?> neighbor) {
        return SimpleServiceLocator.inventoryUtilFactory
            .getInventoryUtil(neighbor.getBlockEntity(), neighbor.getOurDirection());
    }

    public static @Nullable ITankUtil getTankUtil(NeighborBlockEntity<?> neighbor) {
        return PipeFluidUtil.getTankUtilForBE(neighbor.getBlockEntity(), neighbor.getOurDirection());
    }

    public static <T extends BlockEntity> LPNeighborBlockEntitySneakyInsertion<T> sneakyInsertion(
        NeighborBlockEntity<T> neighbor) {
        return new LPNeighborBlockEntitySneakyInsertion<>(neighbor.getBlockEntity(), neighbor.getDirection());
    }

    public static <T extends BlockEntity, C extends T> Optional<LPNeighborBlockEntity<C>> optionalIs(
        NeighborBlockEntity<T> neighbor, Class<C> clazz) {
        if (clazz.isInstance(neighbor.getBlockEntity())) {
            return Optional.of(new LPNeighborBlockEntity<>(clazz.cast(neighbor.getBlockEntity()), neighbor.getDirection()));
        }
        return Optional.empty();
    }
}
