package logisticspipes.network.packets.gui;

import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.client.gui.ClientGuiOpener;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
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
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			handleClient(player);
		}
	}

	private void handleClient(Player player) {
		ClientGuiOpener.openGui(this, player);
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
