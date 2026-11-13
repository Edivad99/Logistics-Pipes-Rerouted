package logisticspipes.request.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.SharedConstants;
import net.minecraft.core.UUIDUtil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ILPPositionProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.modules.ModuleCrafter.CraftingChassisInformation;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.RouterManager;

/**
 * Orders read back from a saved world, waiting for the pipes they go to.
 *
 * <p>A pipe can load before the pipes its orders go to, or those may be gone for good, so each
 * order is turned back into a live one only once its destination is loaded. Those still waiting
 * after a minute are given up.</p>
 *
 * @param <S> the saved form of an order
 */
final class SavedOrders<S> {

    private static final int PATIENCE_TICKS = SharedConstants.TICKS_PER_MINUTE;

    private final List<S> pending = new ArrayList<>();
    private int ticksLeft = 0;

    void load(List<S> saved) {
        pending.clear();
        pending.addAll(saved);
        ticksLeft = PATIENCE_TICKS;
    }

    /**
     * The orders still waiting, which are saved again along with the live ones.
     */
    List<S> pending() {
        return pending;
    }

    /**
     * Tries every waiting order again; {@code restore} turns one into a live order and says whether
     * it could.
     */
    void restore(Predicate<S> restore, ILPPositionProvider pos) {
        if (pending.isEmpty()) {
            return;
        }
        pending.removeIf(restore);
        if (!pending.isEmpty() && --ticksLeft <= 0) {
            LogisticsPipes.LOG.info("Dropping {} orders of the pipe at {}: the pipes they go to did not "
                + "come back after loading", pending.size(), pos.getPos());
            pending.clear();
        }
    }

    /**
     * Where a saved order goes: the router of the pipe, and the chassis slots the additional target
     * information named.
     *
     * @param router       the destination's router; empty for an order that goes nowhere, such as
     *                     an extra
     * @param moduleSlot   the chassis slot of the module the order goes to
     * @param craftingSlot the crafting slot of that module the ingredient is for
     */
    record Target(Optional<UUID> router, Optional<Integer> moduleSlot, Optional<Integer> craftingSlot) {

        static final Codec<Target> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("router").forGetter(Target::router),
            Codec.INT.optionalFieldOf("moduleSlot").forGetter(Target::moduleSlot),
            Codec.INT.optionalFieldOf("craftingSlot").forGetter(Target::craftingSlot)
        ).apply(instance, Target::new));

        /**
         * The target as it can be saved, or null when the information is of a kind that cannot.
         */
        static @Nullable Target of(@Nullable IRouter router, @Nullable IAdditionalTargetInformation info) {
            Optional<UUID> routerId = router == null ? Optional.empty() : Optional.of(router.getId());
            if (info == null) {
                return new Target(routerId, Optional.empty(), Optional.empty());
            }
            if (info instanceof CraftingChassisInformation crafting) {
                return new Target(routerId, Optional.of(crafting.getModuleSlot()),
                    Optional.of(crafting.getCraftingSlot()));
            }
            if (info instanceof ChassiTargetInformation chassis) {
                return new Target(routerId, Optional.of(chassis.getModuleSlot()), Optional.empty());
            }
            return null;
        }

        /**
         * The pipe the order goes to, or null while it is not loaded.
         */
        @Nullable
        CoreRoutedPipe findPipe() {
            if (router.isEmpty()) {
                return null;
            }
            IRouter found = RouterManager.get().getRouter(RouterManager.get().getIDforUUID(router.get()));
            return found == null ? null : found.getPipe();
        }

        @Nullable
        IAdditionalTargetInformation information() {
            if (moduleSlot.isEmpty()) {
                return null;
            }
            return craftingSlot.isPresent()
                ? new CraftingChassisInformation(craftingSlot.get(), moduleSlot.get())
                : new ChassiTargetInformation(moduleSlot.get());
        }
    }
}
