package logisticspipes.proxy.progressprovider;

import java.util.List;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.provider.IGenericProgressProvider;
import logisticspipes.api.provider.IProgressProvider;

public class MachineProgressProvider {

	private final List<IGenericProgressProvider> providers;

	/**
	 * @param providers the readers to consult, in order, taken once and not added to afterwards:
	 *        they are collected from {@link logisticspipes.api.event.RegisterProgressProvidersEvent}
	 *        during startup and read from the server tick, with no lock between the two.
	 */
	public MachineProgressProvider(List<IGenericProgressProvider> providers) {
		this.providers = List.copyOf(providers);
	}

	public byte getProgressForBlockEntity(BlockEntity blockEntity) {
		if (blockEntity instanceof IProgressProvider provider) {
			return provider.getMachineProgressForLP();
		}
		for (IGenericProgressProvider provider : providers) {
			if (provider.isType(blockEntity)) {
				return provider.getProgress(blockEntity);
			}
		}
		return 0;
	}
}
