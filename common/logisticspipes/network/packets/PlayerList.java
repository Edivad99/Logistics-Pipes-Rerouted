package logisticspipes.network.packets;

import logisticspipes.interfaces.PlayerListReciver;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringListPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class PlayerList extends StringListPacket {

	public PlayerList(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new PlayerList(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (Minecraft.getInstance().screen instanceof PlayerListReciver) {
			((PlayerListReciver) Minecraft.getInstance().screen).receivePlayerList(getStringList());
		}
	}
}
