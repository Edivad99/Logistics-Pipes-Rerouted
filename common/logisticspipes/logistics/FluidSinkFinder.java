package logisticspipes.logistics;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IFluidSink;
import logisticspipes.particle.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.FluidSinkReply;

/**
 * Finds the fluid sink a fluid should go to: the fluid counterpart of {@link ItemSinkFinder}.
 */
public final class FluidSinkFinder {

    private FluidSinkFinder() {
    }

    public static @Nullable BestReply getBestReply(FluidIdentifierStack stack, IRouter sourceRouter,
        List<Integer> jamList) {
        Optional<BestReply> bestReply = sourceRouter.getIRoutersByCost().stream()
            .filter(it -> it.containsFlag(PipeRoutingConnectionType.CAN_ROUTE_TO) &&
                it.destination.getId() != sourceRouter.getId() &&
                !jamList.contains(it.destination.getSimpleID()) &&
                it.destination.getPipe() instanceof IFluidSink &&
                it.destination.getPipe().isEnabled() &&
                !it.destination.getPipe().isOnSameContainer(sourceRouter.getPipe()))
            .sorted()
            .map(it -> new BestReply(it.destination.getSimpleID(),
                ((IFluidSink) it.destination.getPipe()).sinkAmount(stack)))
            .filter(best -> best.reply() != null && best.reply().sinkAmount != 0L)
            .reduce((left, right) ->
                left.reply().fixedPriority.compareTo(right.reply().fixedPriority) < 0 ? right : left);
        bestReply.ifPresent(best -> {
            ServerRouter serverRouter = RouterManager.get().getServerRouter(best.routerId());
            if (serverRouter == null) {
                return;
            }
            CoreRoutedPipe pipe = serverRouter.getPipe();
            if (pipe != null) {
                pipe.spawnParticle(Particles.BLUE_SPARKLE, 10);
            }
        });
        return bestReply.orElse(null);
    }

    /**
     * The fluid sink a stack should go to, by the id of its router.
     */
    public record BestReply(int routerId, FluidSinkReply reply) {}
}
