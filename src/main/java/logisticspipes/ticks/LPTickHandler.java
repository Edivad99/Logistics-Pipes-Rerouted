package logisticspipes.ticks;

import java.util.Map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.google.common.collect.MapMaker;
import lombok.Getter;

import logisticspipes.transport.LPTravelingItem;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.ServerTickExecutor;

/**
 * Counts the ticks LP times itself by, and runs the work that has to happen once per tick.
 */
public class LPTickHandler {

    /**
     * Ticks each server level has run since it was loaded, as opposed to its game time, which goes
     * on from the save. Weak keys, so an unloaded level is not kept around.
     */
    private static final Map<Level, Integer> LEVEL_TICKS = new MapMaker().weakKeys().makeMap();

    /**
     * Server ticks since the game started. Timers all over LP are expressed as an offset from it.
     *
     * <p>Each side has its own counter: in singleplayer both run in the same JVM, and a shared one
     * went up by two every tick, once from each thread, so every timer ran twice as fast and an
     * item could be moved twice in a tick.</p>
     */
    @Getter
    private static int serverTick;

    /**
     * Client ticks since the game started; see {@link #serverTick}.
     */
    @Getter
    private static int clientTick;

    /**
     * The tick counter of the side the level is on.
     */
    public static int getTick(Level level) {
        return level.isClientSide() ? clientTick : serverTick;
    }

    /**
     * Ticks the server level has run since it was loaded: 0 for a level that has not ticked yet,
     * and for a client level.
     */
    public static int getLevelTick(Level level) {
        return LEVEL_TICKS.getOrDefault(level, 0);
    }

    @SubscribeEvent
    public void clientTick(ClientTickEvent.Post event) {
        FluidIdentifier.initFromNeoForge(true);
        ClientTaskQueue.runQueued();
        // Items the client has just heard of are held for a few ticks, until a pipe takes them.
        synchronized (LPTravelingItem.forceKeep) {
            LPTravelingItem.forceKeep.replaceAll((item, ticksLeft) -> ticksLeft - 1);
            LPTravelingItem.forceKeep.values().removeIf(ticksLeft -> ticksLeft < 0);
        }
        clientTick++;
    }

    @SubscribeEvent
    public void serverTick(ServerTickEvent.Post event) {
        HudUpdateTick.tick();
        serverTick++;
        ServerTickExecutor.INSTANCE.tick();
    }

    @SubscribeEvent
    public void levelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVEL_TICKS.merge(level, 1, Integer::sum);
        }
    }
}
