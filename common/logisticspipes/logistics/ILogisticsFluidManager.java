package logisticspipes.logistics;

import java.util.List;
import java.util.TreeSet;
import javax.annotation.Nullable;

import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.FluidSinkReply;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import net.minecraft.core.HolderLookup;

public interface ILogisticsFluidManager {

	@Nullable
	Pair<Integer, FluidSinkReply> getBestReply(FluidIdentifierStack stack, IRouter sourceRouter, List<Integer> jamList);

	ItemIdentifierStack getFluidContainer(FluidIdentifierStack stack, HolderLookup.Provider provider);

	@Nullable FluidIdentifierStack getFluidFromContainer(ItemIdentifierStack stack, HolderLookup.Provider provider);

	TreeSet<FluidIdentifierStack> getAvailableFluid(List<ExitRoute> list);
}
