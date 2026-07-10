package logisticspipes.network.guis;

import java.util.UUID;
import logisticspipes.gui.popup.GuiAddChannelPopup;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.PopupGuiProvider;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

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
