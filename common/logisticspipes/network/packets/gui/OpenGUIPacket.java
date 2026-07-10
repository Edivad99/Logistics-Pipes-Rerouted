package logisticspipes.network.packets.gui;

import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class OpenGUIPacket extends ModernPacket {

	/**
	 * GUI Type ID
	 */
	@Getter
	@Setter
	private int guiID;

	/**
	 * GUI Count ID
	 */
	@Getter
	@Setter
	private int windowID;

	/**
	 * GUI Additional Information
	 */
	@Getter
	@Setter
	private byte[] guiData;

	public OpenGUIPacket(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		guiID = input.readInt();
		windowID = input.readInt();
		guiData = input.readByteArray();
	}

	@Override
	public void processPacket(Player player) {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			handleClient(player);
		}
	}

	// See OpenChatGui: NewGuiHandler.openGui(OpenGUIPacket, ...) walks the client's open screens,
	// so the call lives in this @OnlyIn helper, stripped before verification on the dedicated server.
	@OnlyIn(Dist.CLIENT)
	private void handleClient(Player player) {
		NewGuiHandler.openGui(this, player);
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeInt(guiID);
		output.writeInt(windowID);
		output.writeByteArray(guiData);
	}

	@Override
	public ModernPacket template() {
		return new OpenGUIPacket(getId());
	}
}
