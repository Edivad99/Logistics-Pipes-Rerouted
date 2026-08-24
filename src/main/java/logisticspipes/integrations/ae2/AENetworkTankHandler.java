package logisticspipes.integrations.ae2;

import java.util.stream.Stream;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.fluids.FluidStack;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.ISpecialTankUtilProvider;
import logisticspipes.interfaces.ITankUtil;
import logisticspipes.utils.NetworkTankUtil;

/**
 * Exposes the fluids of an Applied Energistics network to the fluid pipes, the counterpart of
 * {@code AEInterfaceInventoryHandler} on the item side.
 */
public class AENetworkTankHandler implements ISpecialTankUtilProvider {

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean isType(BlockEntity blockEntity, @Nullable Direction dir) {
        return AE2Networks.findStorage(blockEntity, dir) != null;
    }

    @Nullable
    @Override
    public ITankUtil getTankUtilFor(BlockEntity blockEntity, @Nullable Direction dir) {
        MEStorage storage = AE2Networks.findStorage(blockEntity, dir);
        if (storage == null) {
            return null;
        }
        return new AETankUtil(storage, AE2Networks.actionSource(blockEntity));
    }

    private static final class AETankUtil extends NetworkTankUtil {

        private final MEStorage storage;
        private final IActionSource source;

        private AETankUtil(MEStorage storage, IActionSource source) {
            this.storage = storage;
            this.source = source;
        }

        @Override
        protected Stream<FluidStack> storedFluids() {
            Stream.Builder<FluidStack> out = Stream.builder();
            for (Object2LongMap.Entry<AEKey> entry : storage.getAvailableStacks()) {
                if (!(entry.getKey() instanceof AEFluidKey key) || entry.getLongValue() <= 0) {
                    continue;
                }
                out.add(key.toStack((int) Math.min(entry.getLongValue(), Integer.MAX_VALUE)));
            }
            return out.build();
        }

        @Override
        protected int insert(FluidStack stack, boolean simulate) {
            AEFluidKey key = AEFluidKey.of(stack);
            if (key == null || stack.isEmpty()) {
                return 0;
            }
            long moved = storage.insert(key, stack.getAmount(),
                simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            return (int) Math.min(moved, Integer.MAX_VALUE);
        }

        @Override
        protected int extract(FluidStack stack, boolean simulate) {
            AEFluidKey key = AEFluidKey.of(stack);
            if (key == null || stack.isEmpty()) {
                return 0;
            }
            long moved = storage.extract(key, stack.getAmount(),
                simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            return (int) Math.min(moved, Integer.MAX_VALUE);
        }

        @Override
        protected long storedAmount(FluidStack probe) {
            AEFluidKey key = AEFluidKey.of(probe);
            return key == null ? 0L : storage.getAvailableStacks().get(key);
        }
    }
}
