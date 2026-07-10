package logisticspipes.network.guis.upgrade;

import java.util.List;
import java.util.stream.Collectors;
import logisticspipes.gui.popup.SneakyConfigurationPopup;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.UpgradeCoordinatesGuiProvider;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.UpgradeSlot;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

@StaticResolve
public class SneakyUpgradeConfigGuiProvider extends UpgradeCoordinatesGuiProvider {

	public SneakyUpgradeConfigGuiProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe bPipe = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(bPipe.pipe instanceof CoreRoutedPipe)) {
			return null;
		}

		List<DoubleCoordinates> list = new WorldCoordinatesWrapper(bPipe).connectedTileEntities().stream()
				.filter(in -> SimpleServiceLocator.pipeInformationManager.isNotAPipe(in.getTileEntity()))
				.map(in -> new DoubleCoordinates(in.getTileEntity()))
				.collect(Collectors.toList());

		if (list.isEmpty()) {
			list = new WorldCoordinatesWrapper(bPipe).connectedTileEntities().stream()
					.map(in -> new DoubleCoordinates(in.getTileEntity()))
					.collect(Collectors.toList());
		}

		return new SneakyConfigurationPopup(list, getSlot(player, UpgradeSlot.class));
	}

	@Override
	public GuiProvider template() {
		return new SneakyUpgradeConfigGuiProvider(getId());
	}
}
