package logisticspipes.network.packets.gui;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;



import logisticspipes.asm.ClientSideOnlyMethodContent;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class OpenChatGui extends ModernPacket {

	public OpenChatGui(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	@ClientSideOnlyMethodContent
	public void processPacket(Player player) {
		net.minecraft.client.Minecraft.getInstance().setScreen(new ChatScreen(""));
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new OpenChatGui(getId());
	}
}
