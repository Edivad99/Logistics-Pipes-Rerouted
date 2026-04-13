package logisticspipes.network.guis.block;

import net.minecraft.world.entity.player.Player;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.gui.popup.GuiManageChannelPopup;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractpackets.ChannelInformationListCoordinatesPopupGuiProvider;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SecurityChannelManagerGui extends ChannelInformationListCoordinatesPopupGuiProvider {

	public SecurityChannelManagerGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		return new GuiManageChannelPopup(getChannelInformations(), getTileAs(player.level(), LogisticsSecurityTileEntity.class).getBlockPos());
	}

	@Override
	public GuiProvider template() {
		return new SecurityChannelManagerGui(getId());
	}
}
