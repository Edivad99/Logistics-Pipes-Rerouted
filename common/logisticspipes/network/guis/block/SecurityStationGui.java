package logisticspipes.network.guis.block;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.gui.GuiSecurityStation;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

@StaticResolve
public class SecurityStationGui extends CoordinatesGuiProvider {

	public SecurityStationGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		return new GuiSecurityStation(getTileAs(player.level(), LogisticsSecurityTileEntity.class), player);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsSecurityTileEntity securityStation = getTileAs(player.level(), LogisticsSecurityTileEntity.class);
		DummyContainer dummy = new DummyContainer(player, null, securityStation);
		dummy.addRestrictedSlot(0, securityStation.inv, 50, 50, (Item) null);
		dummy.addNormalSlotsForPlayerInventory(10, 210);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new SecurityStationGui(getId());
	}
}
