package logisticspipes.network.guis.block;

import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;
import logisticspipes.gui.GuiPowerJunction;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class PowerJunctionGui extends CoordinatesGuiProvider {

	public PowerJunctionGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		return new GuiPowerJunction(player, getTileAs(player.level(), LogisticsPowerJunctionBlockEntity.class));
	}

	@Override
	public DummyContainer getContainer(Player player) {
		DummyContainer dummy = new DummyContainer(player, null, getTileAs(player.level(), LogisticsPowerJunctionBlockEntity.class));
		dummy.addNormalSlotsForPlayerInventory(8, 80);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new PowerJunctionGui(getId());
	}
}
