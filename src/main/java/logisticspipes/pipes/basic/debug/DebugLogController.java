package logisticspipes.pipes.basic.debug;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.network.to_client.debug.OpenPipeLogMessage;
import logisticspipes.network.to_client.debug.SendLogLineMessage;
import logisticspipes.network.to_client.debug.UpdateStatusEntriesMessage;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.utils.PlayerCollectionList;

/**
 * The server side of a pipe's debug log window. Players who open it through the debug command
 * ({@link #openForPlayer}) get the lines the pipe passes to {@link #log} and, every tick, its
 * status entries, which are sent again only when they change.
 *
 * <p>{@link #debugThisPipe} is a separate on/off flag the debug command toggles; it does not open
 * any window.
 */
public class DebugLogController {

    private static int NEXT_ID = 0;
    private final CoreUnroutedPipe pipe;
    private final int ID = DebugLogController.NEXT_ID++;
    private final PlayerCollectionList players = new PlayerCollectionList();
    public boolean debugThisPipe = false;
    private List<StatusEntry> oldList = new ArrayList<>();

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
            PacketDistributor.sendToPlayer(serverPlayer, new OpenPipeLogMessage(ID, pipe.toString()));
            PacketDistributor.sendToPlayer(serverPlayer, new UpdateStatusEntriesMessage(ID, status));
        }
    }
}
