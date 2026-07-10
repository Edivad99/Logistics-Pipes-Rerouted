package logisticspipes.network.guis.upgrade;

import logisticspipes.gui.popup.DisconnectionConfigurationPopup;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.UpgradeCoordinatesGuiProvider;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.UpgradeSlot;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class DisconnectionUpgradeConfigGuiProvider extends UpgradeCoordinatesGuiProvider {

	public DisconnectionUpgradeConfigGuiProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe bPipe = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(bPipe.pipe instanceof CoreRoutedPipe)) {
			return null;
		}

		return new DisconnectionConfigurationPopup((CoreRoutedPipe) bPipe.pipe, getSlot(player, UpgradeSlot.class));
	}

	@Override
	public GuiProvider template() {
		return new DisconnectionUpgradeConfigGuiProvider(getId());
	}
}
