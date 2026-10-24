package logisticspipes.logistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IFluidSink;
import logisticspipes.interfaces.routing.IProvideFluids;
import logisticspipes.particle.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.FluidSinkReply;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.LogisticsFluidContainer;
import logisticspipes.world.item.component.LPDataComponents;

public final class LogisticsFluidManager {

	private LogisticsFluidManager() {
	}

	/** The fluid sink a stack should go to, by the id of its router. */
	public record BestReply(int routerId, FluidSinkReply reply) {}

	public static @Nullable BestReply getBestReply(FluidIdentifierStack stack, IRouter sourceRouter, List<Integer> jamList) {
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
			if (serverRouter == null) return;
			CoreRoutedPipe pipe = serverRouter.getPipe();
			if (pipe != null) pipe.spawnParticle(Particles.BLUE_SPARKLE, 10);
		});
		return bestReply.orElse(null);
	}

	public static ItemIdentifierStack getFluidContainer(FluidIdentifierStack stack) {
		ItemStack item = new ItemStack(LPItems.FLUID_CONTAINER.get(), 1);
        FluidStack fluidStack = stack.makeFluidStack();
        if (!fluidStack.isEmpty()) {
            item.set(
                LPDataComponents.FLUID,
                SimpleFluidContent.copyOf(fluidStack)
            );
        }
		return ItemIdentifierStack.getFromStack(item);
	}

    @Nullable
	public static FluidIdentifierStack getFluidFromContainer(ItemIdentifierStack stack) {
		ItemStack itemStack = stack.makeNormalStack();
		if (itemStack.getItem() instanceof LogisticsFluidContainer) {
            if (itemStack.has(LPDataComponents.FLUID)) {
                return FluidIdentifierStack.getFromStack(
                    itemStack.getOrDefault(LPDataComponents.FLUID, SimpleFluidContent.EMPTY).copy());
            }
		}
		return null;
	}

	public static TreeSet<FluidIdentifierStack> getAvailableFluid(List<ExitRoute> validDestinations) {
		Map<FluidIdentifier, Integer> allAvailableItems = new HashMap<>();
		for (ExitRoute r : validDestinations) {
			if (r == null) {
				continue;
			}
			if (!r.containsFlag(PipeRoutingConnectionType.CAN_REQUEST_FROM)) {
				continue;
			}
			if (!(r.destination.getPipe() instanceof IProvideFluids provider)) {
				continue;
			}

            Map<FluidIdentifier, Integer> allItems = provider.getAvailableFluids();

			for (Entry<FluidIdentifier, Integer> liquid : allItems.entrySet()) {
				Integer amount = allAvailableItems.get(liquid.getKey());
				if (amount == null) {
					allAvailableItems.put(liquid.getKey(), liquid.getValue());
				} else {
					long addition = ((long) amount) + liquid.getValue();
					if (addition > Integer.MAX_VALUE) {
						addition = Integer.MAX_VALUE;
					}
					allAvailableItems.put(liquid.getKey(), (int) addition);
				}
			}
		}
		return allAvailableItems.entrySet().stream()
				.map(item -> new FluidIdentifierStack(item.getKey(), item.getValue()))
				.collect(Collectors.toCollection(TreeSet::new));
	}
}
