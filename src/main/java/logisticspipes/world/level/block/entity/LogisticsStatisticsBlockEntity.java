package logisticspipes.world.level.block.entity;

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

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IBlockEntityMenuProvider;
import logisticspipes.interfaces.IScreenOpenController;
import logisticspipes.network.to_client.block.TrackingTasksMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.util.TrackingTask;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.inventory.StatisticsMenu;
import logisticspipes.api.connection.NeighborBlockEntity;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

public class LogisticsStatisticsBlockEntity extends LogisticsSolidBlockEntity
    implements IBlockEntityMenuProvider, IScreenOpenController {

    public List<TrackingTask> tasks = new ArrayList<>();
    private final PlayerCollectionList guiListener = new PlayerCollectionList();
    private int tickCount;
    private @Nullable CoreRoutedPipe cachedConnectedPipe;

    public LogisticsStatisticsBlockEntity(BlockPos pos, BlockState state) {
        super(LPBlockEntityTypes.STATISTICS_TABLE.get(), pos, state);
    }

    /**
     * Starts or stops tracking an item.
     *
     * <p>Adding an item already tracked does nothing rather than tracking it twice, which is what
     * the two packets that used to do this each checked for in their own way.
     */
    public void setTracked(ItemIdentifier item, boolean tracked) {
        if (tracked) {
            if (tasks.stream().noneMatch(task -> task.item.equals(item))) {
                final TrackingTask task = new TrackingTask(item);
                final CoreRoutedPipe pipe = getConnectedPipe();
                if (pipe != null) {
                    // Otherwise the graph reads zero until the next sample, a minute away.
                    task.record(pipe);
                }
                tasks.add(task);
            }
        } else {
            tasks.removeIf(task -> task.item.equals(item));
        }
        setChanged();
        updateClients();
    }

    private void updateClients() {
        guiListener.send(new TrackingTasksMessage(getBlockPos(), tasks));
    }

    @Override
    public void screenOpenedByPlayer(Player player) {
        guiListener.add(player);
    }

    @Override
    public void screenClosedByPlayer(Player player) {
        guiListener.remove(player);
    }

    @Override
    public void notifyOfBlockChange() {
        super.notifyOfBlockChange();
        cachedConnectedPipe = null;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        tickCount++;
        if (tickCount % TrackingTask.TICKS_PER_SAMPLE != 0 || tasks.isEmpty()) {
            return;
        }
        final CoreRoutedPipe pipe = getConnectedPipe();
        if (pipe == null) {
            return;
        }
        for (TrackingTask task : tasks) {
            task.record(pipe);
        }
        setChanged();
        updateClients();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tasks.addAll(input.read("Tasks", TrackingTask.CODEC.listOf()).orElse(List.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Tasks", TrackingTask.CODEC.listOf(), tasks);
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

    public @Nullable CoreRoutedPipe getConnectedPipe() {
        if (cachedConnectedPipe == null) {
            new WorldCoordinatesWrapper(this).allNeighborBlockEntities().stream()
                .filter(NeighborBlockEntity::isLogisticsPipe)
                .filter(
                    adjacent -> ((LogisticsTileGenericPipe) adjacent.getBlockEntity()).pipe instanceof CoreRoutedPipe)
                .map(adjacent -> (CoreRoutedPipe) (((LogisticsTileGenericPipe) adjacent.getBlockEntity()).pipe))
                .findFirst()
                .ifPresent(coreRoutedPipe -> cachedConnectedPipe = coreRoutedPipe);
        }
        return cachedConnectedPipe;
    }
}
