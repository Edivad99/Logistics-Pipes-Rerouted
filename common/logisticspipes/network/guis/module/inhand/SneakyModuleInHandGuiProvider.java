package logisticspipes.network.guis.module.inhand;

import logisticspipes.gui.modules.GuiSneakyConfigurator;
import logisticspipes.items.ItemModule;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.DummyModuleContainer;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.module.SneakyDirection;

@StaticResolve
public class SneakyModuleInHandGuiProvider extends ModuleInHandGuiProvider {

	public SneakyModuleInHandGuiProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsModule module = ItemModule.getLogisticsModule(player, getInvSlot());
		if (!(module instanceof Gui && module instanceof SneakyDirection)) {
			return null;
		}
		return new GuiSneakyConfigurator(player.getInventory(), module);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		DummyModuleContainer dummy = new DummyModuleContainer(player, getInvSlot());
		if (!(dummy.getModule() instanceof SneakyDirection)) {
			return null;
		}
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new SneakyModuleInHandGuiProvider(getId());
	}
}
