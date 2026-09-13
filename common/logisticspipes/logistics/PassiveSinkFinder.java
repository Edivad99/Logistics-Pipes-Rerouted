package logisticspipes.logistics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.particle.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.AsyncRouting;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * Finds the routers that passively accept an item, best offer first, for the modules that push
 * stacks out of their own accord.
 *
 * <p>Distinct from {@link LogisticsManager}, which serves active requests and the GUIs: this one
 * starts from the interest index rather than a list of candidates, takes only modules that
 * {@link logisticspipes.modules.LogisticsModule#receivePassive() receive passively}, and charges
 * the destination for what it accepts.
 */
public final class PassiveSinkFinder {

    private PassiveSinkFinder() {
    }

    /** A router that accepts the item, and on what terms. */
    public record Destination(int routerId, SinkReply sinkReply) {}

    /**
     * Every destination the item can go to, one lookup at a time: each router handed out is left out
     * of the following lookups, and {@code filter} is asked before each one, so a caller that has run
     * out of items stops the search without paying for another.
     */
    public static Iterator<Destination> allDestinations(ItemStack stack, ItemIdentifier itemid, boolean canBeDefault,
        ServerRouter sourceRouter, BooleanSupplier filter) {
        List<Integer> jamList = new ArrayList<>();
        return new Iterator<>() {

            private @Nullable Destination next;
            private boolean exhausted;

            @Override
            public boolean hasNext() {
                if (next == null && !exhausted) {
                    next = filter.getAsBoolean()
                        ? getDestination(stack, itemid, canBeDefault, sourceRouter, jamList)
                        : null;
                    if (next == null) {
                        exhausted = true;
                    } else {
                        jamList.add(next.routerId());
                    }
                }
                return next != null;
            }

            @Override
            public Destination next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Destination destination = Objects.requireNonNull(next);
                next = null;
                return destination;
            }
        };
    }

    public static @Nullable Destination getDestination(ItemStack stack, ItemIdentifier itemid, boolean canBeDefault,
        ServerRouter sourceRouter, List<Integer> routersToExclude) {
        Stream<ExitRoute> destinationStream = ServerRouter.getRoutersInterestedIn(itemid).stream()
            .mapToObj(SimpleServiceLocator.routerManager::getServerRouter)
            .flatMap(router -> {
                if (router == null) {
                    return Stream.empty();
                }
                List<ExitRoute> routes = AsyncRouting.getDistance(sourceRouter, router);
                return routes == null
                    ? Stream.<ExitRoute>empty()
                    : routes.stream().filter(exitRoute -> exitRoute.containsFlag(PipeRoutingConnectionType.canRouteTo));
            });
        return getBestReply(stack, itemid, sourceRouter, destinationStream, routersToExclude, canBeDefault);
    }

    private static @Nullable Destination getBestReply(ItemStack stack, ItemIdentifier itemid, ServerRouter sourceRouter,
        Stream<ExitRoute> destinationStream, List<Integer> routersToExclude, boolean canBeDefault) {
        CoreRoutedPipe sourcePipe = sourceRouter.getPipe();
        Integer[] resultRouterId = { null };
        SinkReply[] result = { null };
        destinationStream
            .filter(exitRoute -> canAccept(exitRoute, itemid, sourceRouter, sourcePipe, routersToExclude))
            .sorted()
            .forEachOrdered(exitRoute -> {
                LogisticsModule module = exitRoute.destination.getLogisticsModule();
                if (module == null) {
                    return;
                }
                SinkReply best = result[0];
                SinkReply reply;
                if (best == null) {
                    reply = module.sinksItem(stack, itemid, -1, 0, canBeDefault, true, true);
                } else if (best.maxNumberOfItems < 0) {
                    reply = null;
                } else {
                    reply = module.sinksItem(stack, itemid, best.fixedPriority.ordinal(), best.customPriority,
                        canBeDefault, true, true);
                }
                if (reply != null && (best == null
                    || reply.fixedPriority.ordinal() > best.fixedPriority.ordinal()
                    || (reply.fixedPriority == best.fixedPriority && reply.customPriority > best.customPriority))) {
                    resultRouterId[0] = exitRoute.destination.getSimpleID();
                    result[0] = reply;
                }
            });

        SinkReply sinkReply = result[0];
        Integer destinationRouterId = resultRouterId[0];
        if (sinkReply == null || destinationRouterId == null) {
            return null;
        }
        ServerRouter destinationRouter =
            Objects.requireNonNull(SimpleServiceLocator.routerManager.getServerRouter(destinationRouterId));
        CoreRoutedPipe pipe = Objects.requireNonNull(destinationRouter.getPipe());
        pipe.useEnergy(sinkReply.energyUse);
        pipe.spawnParticle(Particles.BLUE_SPARKLE, 10);
        return new Destination(destinationRouterId, sinkReply);
    }

    private static boolean canAccept(ExitRoute exitRoute, ItemIdentifier itemid, ServerRouter sourceRouter,
        @Nullable CoreRoutedPipe sourcePipe, List<Integer> routersToExclude) {
        IRouter destination = exitRoute.destination;
        CoreRoutedPipe destinationPipe = destination.getPipe();
        LogisticsModule module = destination.getLogisticsModule();
        return !destination.getId().equals(sourceRouter.getId())
            && !routersToExclude.contains(destination.getSimpleID())
            && exitRoute.containsFlag(PipeRoutingConnectionType.canRouteTo)
            && exitRoute.filters.stream().noneMatch(filter ->
                filter.blockRouting() || filter.isBlocked() == filter.isFilteredItem(itemid))
            && module != null && module.receivePassive()
            && destinationPipe != null && destinationPipe.isEnabled()
            && (sourcePipe == null || !destinationPipe.isOnSameContainer(sourcePipe));
    }
}
