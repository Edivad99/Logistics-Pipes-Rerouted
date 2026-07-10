package logisticspipes.network.guis;

import logisticspipes.gui.GuiLogisticsSettings;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

@StaticResolve
public class LogisticsPlayerSettingsGuiProvider extends GuiProvider {

	public LogisticsPlayerSettingsGuiProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		return new GuiLogisticsSettings(player);
	}

	@Override
	public AbstractContainerMenu getContainer(Player player) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), null);
		dummy.addNormalSlotsForPlayerInventory(0, 0); // server does not care where the slots are
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new LogisticsPlayerSettingsGuiProvider(getId());
	}
}
