package logisticspipes.network.packets;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

// DimensionManager removed — use ServerLevel directly

@StaticResolve
public class PlayerListRequest extends ModernPacket {

	public PlayerListRequest(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new PlayerListRequest(getId());
	}

	@Override
	public void processPacket(Player player) {
		// NeoForge 1.20.1: DimensionManager.getWorlds() removed — get players from server's player list
		Stream<?> allPlayers = ServerLifecycleHooks.getCurrentServer() != null
				? ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
				: Stream.empty();
		Stream<Player> allPlayerEntities = allPlayers.filter(o -> o instanceof Player).map(o -> (Player) o);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PlayerList.class)
				.setStringList(allPlayerEntities.map(entityPlayer -> entityPlayer.getGameProfile().name()).collect(Collectors.toList())), player);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void writeData(LPDataOutput output) {}
}
