package logisticspipes.integrations.refinedstorage;

import java.util.stream.Stream;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.fluids.FluidStack;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;

import logisticspipes.interfaces.ISpecialTankUtilProvider;
import logisticspipes.interfaces.ITankUtil;
import logisticspipes.utils.NetworkTankUtil;

/**
 * Exposes the fluids of a Refined Storage network to the fluid pipes, the counterpart of
 * {@code RefinedStorageInventoryHandler} on the item side.
 *
 * <p>As there, {@link FluidResource} sits in RS's {@code common.support.resource} rather than under
 * {@code common.api}, so it is the one implementation-package type this depends on and the most
 * likely thing to break on an RS update.</p>
 */
public class RSNetworkTankHandler implements ISpecialTankUtilProvider {

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean isType(BlockEntity blockEntity, @Nullable Direction dir) {
        return RSNetworks.findStorage(blockEntity, dir) != null;
    }

    @Nullable
    @Override
    public ITankUtil getTankUtilFor(BlockEntity blockEntity, @Nullable Direction dir) {
        StorageNetworkComponent storage = RSNetworks.findStorage(blockEntity, dir);
        if (storage == null) {
            return null;
        }
        return new RefinedStorageTankUtil(storage);
    }

    private static final class RefinedStorageTankUtil extends NetworkTankUtil {

        private final StorageNetworkComponent storage;

        private RefinedStorageTankUtil(StorageNetworkComponent storage) {
            this.storage = storage;
        }

        private static FluidResource toResource(FluidStack stack) {
            return new FluidResource(stack.getFluid(), stack.getComponentsPatch());
        }

        private static FluidStack toStack(FluidResource resource, int amount) {
            return new FluidStack(resource.fluid().builtInRegistryHolder(), amount, resource.components());
        }

        @Override
        protected Stream<FluidStack> storedFluids() {
            Stream.Builder<FluidStack> out = Stream.builder();
            for (ResourceAmount entry : storage.getAll()) {
                if (!(entry.resource() instanceof FluidResource resource) || entry.amount() <= 0) {
                    continue;
                }
                out.add(RefinedStorageTankUtil.toStack(resource,
                    (int) Math.min(entry.amount(), Integer.MAX_VALUE)));
            }
            return out.build();
        }

        @Override
        protected int insert(FluidStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return 0;
            }
            long moved = storage.insert(RefinedStorageTankUtil.toResource(stack), stack.getAmount(),
                simulate ? Action.SIMULATE : Action.EXECUTE, RSNetworks.ACTOR);
            return (int) Math.min(moved, Integer.MAX_VALUE);
        }

        @Override
        protected int extract(FluidStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return 0;
            }
            long moved = storage.extract(RefinedStorageTankUtil.toResource(stack), stack.getAmount(),
                simulate ? Action.SIMULATE : Action.EXECUTE, RSNetworks.ACTOR);
            return (int) Math.min(moved, Integer.MAX_VALUE);
        }

        @Override
        protected long storedAmount(FluidStack probe) {
            return probe.isEmpty() ? 0L : storage.get(RefinedStorageTankUtil.toResource(probe));
        }
    }
}
