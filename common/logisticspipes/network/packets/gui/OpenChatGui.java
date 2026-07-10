package logisticspipes.network.packets.gui;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
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
	public void processPacket(Player player) {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			openChatScreen();
		}
	}

	// Client body kept out of processPacket's verified bytecode. The dedicated server links this
	// packet (so it can SEND it) only when no client class appears in a non-@OnlyIn method; the
	// RuntimeDistCleaner strips this @OnlyIn helper before verification on the server.
	@OnlyIn(Dist.CLIENT)
	private void openChatScreen() {
		Minecraft.getInstance().setScreen(new ChatScreen(""));
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new OpenChatGui(getId());
	}
}
