package logisticspipes.network.packets;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifier;

@StaticResolve
public class UpdateName extends ModernPacket {

	@Getter
	@Setter
	private ItemIdentifier ident;

	@Getter
	@Setter
	private String name;

	public UpdateName(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new UpdateName(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (player instanceof LocalPlayer) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(UpdateName.class).setIdent(getIdent()).setName(getIdent().getFriendlyName()));
		} else {
			MainProxy.getProxy(false).updateNames(getIdent(), getName());
		}
	}

	@Override
	public void readData(LPDataInput input) {
		ident = input.readItemIdentifierStack().getItem();
		name = input.readUTF();
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeItemIdentifierStack(ident.makeStack(1));
		output.writeUTF(name);
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
