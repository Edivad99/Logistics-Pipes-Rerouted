package logisticspipes.network.packets.gui;

import java.util.Optional;
import java.util.UUID;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.EditChannelGuiProvider;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class OpenEditChannelGUIPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private String identifier;

	public OpenEditChannelGUIPacket(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		identifier = input.readUTF();
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeUTF(identifier);
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
		IChannelManager channelManager = SimpleServiceLocator.channelManagerProvider
				.getChannelManager(player.level());
		Optional<ChannelInformation> match = channelManager.getChannels().stream()
				.filter(channel -> channel.getChannelIdentifier().toString().equals(getIdentifier())).findFirst();
		match.ifPresent(channel -> NewGuiHandler.getGui(EditChannelGuiProvider.class).setResponsibleSecurityID(finalSecurityID).setChannel(channel).open(player));
	}

	@Override
	public ModernPacket template() {
		return new OpenEditChannelGUIPacket(getId());
	}
}
