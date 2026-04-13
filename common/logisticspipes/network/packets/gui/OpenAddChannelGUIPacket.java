package logisticspipes.network.packets.gui;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.AddChannelGuiProvider;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class OpenAddChannelGUIPacket extends CoordinatesPacket {

	public OpenAddChannelGUIPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		BlockEntity tile = player.level().getBlockEntity(new BlockPos(getPosX(), getPosY(), getPosZ()));
		UUID securityID = null;
		if (tile instanceof LogisticsSecurityTileEntity) {
			LogisticsSecurityTileEntity security = (LogisticsSecurityTileEntity) tile;
			securityID = security.getSecId();
		}
		UUID finalSecurityID = securityID;
		NewGuiHandler.getGui(AddChannelGuiProvider.class).setSecurityStationID(finalSecurityID).open(player);
	}

	@Override
	public ModernPacket template() {
		return new OpenAddChannelGUIPacket(getId());
	}
}
