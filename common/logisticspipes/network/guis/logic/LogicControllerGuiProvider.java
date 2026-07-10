package logisticspipes.network.guis.logic;

import logisticspipes.logic.gui.LogicLayoutGui;
import logisticspipes.logic.interfaces.ILogicControllerTile;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

@StaticResolve
public class LogicControllerGuiProvider extends CoordinatesGuiProvider {

	public LogicControllerGuiProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		BlockEntity pipe = getTileAs(player.level(), BlockEntity.class);
		if (pipe instanceof ILogicControllerTile) {
			return new LogicLayoutGui(((ILogicControllerTile) pipe).getLogicController(), player);
		}
		return null;
	}

	@Override
	public AbstractContainerMenu getContainer(Player player) {
		BlockEntity pipe = getTileAs(player.level(), BlockEntity.class);
		if (pipe instanceof ILogicControllerTile) {
			DummyContainer dummy = new DummyContainer(player.getInventory(), null);
			dummy.addNormalSlotsForPlayerInventory(50, 190);
			return dummy;
		}
		return null;
	}

	@Override
	public GuiProvider template() {
		return new LogicControllerGuiProvider(getId());
	}
}
