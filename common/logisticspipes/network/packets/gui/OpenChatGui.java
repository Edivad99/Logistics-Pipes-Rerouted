package logisticspipes.network.packets.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class OpenChatGui extends ModernPacket {

	public OpenChatGui(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(Player player) {
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			openChatScreen();
		}
	}

    private void openChatScreen() {
		Minecraft.getInstance().setScreen(new ChatScreen("", false));
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new OpenChatGui(getId());
	}
}
