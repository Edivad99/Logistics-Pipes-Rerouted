package logisticspipes.network.packets;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.to_client.security.PlayerListMessage;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

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
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new PlayerListMessage(
					allPlayerEntities.map(entityPlayer -> entityPlayer.getGameProfile().name()).collect(Collectors.toList())));
		}
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void writeData(LPDataOutput output) {}
}
