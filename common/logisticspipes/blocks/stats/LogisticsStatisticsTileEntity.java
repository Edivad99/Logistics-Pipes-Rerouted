package logisticspipes.blocks.stats;

import java.util.ArrayList;
import java.util.List;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.StatisticsGui;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

public class LogisticsStatisticsTileEntity extends LogisticsSolidBlockEntity implements IGuiTileEntity {

	public LogisticsStatisticsTileEntity(BlockPos pos, BlockState state) {
		super(LPBlockEntityTypes.STATISTICS_TABLE.get(), pos, state);
	}

	public List<TrackingTask> tasks = new ArrayList<>();
	private int tickCount;
	private CoreRoutedPipe cachedConnectedPipe;

	@Override
	public void notifyOfBlockChange() {
		super.notifyOfBlockChange();
		cachedConnectedPipe = null;
	}

	@Override
	public void update() {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		tickCount++;
		if (getConnectedPipe() == null) {
			return;
		}
		for (TrackingTask task : tasks) {
			task.tick(tickCount, getConnectedPipe());
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		int size = tag.getIntOr("taskSize", 0);
		for (int i = 0; i < size; i++) {
			CompoundTag subTag = (CompoundTag) tag.get("Task_" + i);
			TrackingTask task = new TrackingTask();
			task.readFromNBT(subTag, registries);
			tasks.add(task);
		}
	}

	@Override
	public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("taskSize", tasks.size());
		int count = 0;
		for (TrackingTask task : tasks) {
			CompoundTag subtag = new CompoundTag();
			task.writeToNBT(subtag, registries);
			tag.put("Task_" + count, subtag);
			count++;
		}
	}

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(StatisticsGui.class).setTrackingList(tasks);
	}

	public CoreRoutedPipe getConnectedPipe() {
		if (cachedConnectedPipe == null) {
			new WorldCoordinatesWrapper(this).allNeighborTileEntities().stream()
					.filter(NeighborTileEntity::isLogisticsPipe)
					.filter(adjacent -> ((LogisticsTileGenericPipe) adjacent.getTileEntity()).pipe instanceof CoreRoutedPipe)
					.map(adjacent -> (CoreRoutedPipe) (((LogisticsTileGenericPipe) adjacent.getTileEntity()).pipe))
					.findFirst()
					.ifPresent(coreRoutedPipe -> cachedConnectedPipe = coreRoutedPipe);
		}
		return cachedConnectedPipe;
	}
}
