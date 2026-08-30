package logisticspipes.network.guis;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.gui.popup.GuiAddChannelPopup;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.PopupGuiProvider;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class AddChannelGuiProvider extends PopupGuiProvider {

	@Getter
	@Setter
	private UUID securityStationID;

	public AddChannelGuiProvider(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeUUID(securityStationID);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		securityStationID = input.readUUID();
	}

	@Override
	public Object getClientGui(Player player) {
		return new GuiAddChannelPopup(securityStationID);
	}

	@Override
	public GuiProvider template() {
		return new AddChannelGuiProvider(getId());
	}
}
