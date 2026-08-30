package logisticspipes.network.guis;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.gui.popup.GuiEditChannelPopup;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.PopupGuiProvider;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class EditChannelGuiProvider extends PopupGuiProvider {

	@Getter
	@Setter
	private ChannelInformation channel;

	@Getter
	@Setter
	private UUID responsibleSecurityID;

	public EditChannelGuiProvider(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeChannelInformation(channel);
		output.writeUUID(responsibleSecurityID);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		channel = input.readChannelInformation();
		responsibleSecurityID = input.readUUID();
	}

	@Override
	public Object getClientGui(Player player) {
		return new GuiEditChannelPopup(responsibleSecurityID, channel);
	}

	@Override
	public GuiProvider template() {
		return new EditChannelGuiProvider(getId());
	}
}
