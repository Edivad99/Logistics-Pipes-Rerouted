package logisticspipes.blocks.stats;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import logisticspipes.interfaces.IBlockEntityMenuProvider;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.inventory.StatisticsMenu;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

public class LogisticsStatisticsTileEntity extends LogisticsSolidBlockEntity implements IBlockEntityMenuProvider {

	public LogisticsStatisticsTileEntity(BlockPos pos, BlockState state) {
		super(LPBlockEntityTypes.STATISTICS_TABLE.get(), pos, state);
	}

	public List<TrackingTask> tasks = new ArrayList<>();

	/**
	 * Starts or stops tracking an item.
	 *
	 * <p>Adding an item already tracked does nothing rather than tracking it twice, which is what
	 * the two packets that used to do this each checked for in their own way.
	 */
	public void setTracked(ItemIdentifier item, boolean tracked) {
		if (tracked) {
			if (tasks.stream().noneMatch(task -> task.item.equals(item))) {
				final TrackingTask task = new TrackingTask();
				task.item = item;
				tasks.add(task);
			}
		} else {
			tasks.removeIf(task -> task.item.equals(item));
		}
	}
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
		for (ValueInput entry : input.childrenListOrEmpty("Tasks")) {
			TrackingTask task = new TrackingTask();
			task.deserialize(entry);
			tasks.add(task);
		}
	}

	@Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
		ValueOutput.ValueOutputList list = output.childrenList("Tasks");
		for (TrackingTask task : tasks) {
			task.serialize(list.addChild());
		}
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(getBlockState().getBlock().getDescriptionId());
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new StatisticsMenu(containerId, inventory, this);
	}

	@Override
	public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
		IBlockEntityMenuProvider.super.writeClientSideData(menu, buffer);
		TrackingTask.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, tasks);
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
