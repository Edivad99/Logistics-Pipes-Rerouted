package logisticspipes.pipes.basic.debug;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.network.to_client.debug.SendLogLineMessage;
import logisticspipes.network.to_client.debug.SendLogWindowMessage;
import logisticspipes.network.to_client.debug.UpdateStatusEntriesMessage;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.utils.PlayerCollectionList;

public class DebugLogController {

	private static int nextID = 0;
	private final int ID = DebugLogController.nextID++;
	public final CoreUnroutedPipe pipe;
	public boolean debugThisPipe = false;
	private List<StatusEntry> oldList = new ArrayList<>();
	private PlayerCollectionList players = new PlayerCollectionList();

	public DebugLogController(CoreUnroutedPipe pipe) {
		this.pipe = pipe;
	}

	public void log(String info) {
		if (players.isEmptyWithoutCheck()) {
			return;
		}
		players.send(new SendLogLineMessage(ID, info));
	}

	public void tick() {
		if (players.isEmpty()) {
			return;
		}
		generateStatus();
	}

	public void generateStatus() {
		List<StatusEntry> status = new ArrayList<>();
		pipe.addStatusInformation(status);
		if (!status.equals(oldList)) {
			players.send(new UpdateStatusEntriesMessage(ID, status));
			oldList = status;
		}
	}

	public void openForPlayer(Player player) {
		players.add(player);
		List<StatusEntry> status = new ArrayList<>();
		pipe.addStatusInformation(status);
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new SendLogWindowMessage(ID, pipe.toString()));
		}
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new UpdateStatusEntriesMessage(ID, status));
		}
	}
}
