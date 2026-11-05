package logisticspipes.ticks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.google.common.collect.MapMaker;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.ServerTickExecutor;

public class LPTickHandler {

	public static int adjChecksDone = 0;

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

	@SubscribeEvent
	public void clientTick(ClientTickEvent.Post event) {
		FluidIdentifier.initFromNeoForge(true);
		ClientTaskQueue.runQueued();
		clientTick++;
	}

	@SubscribeEvent
	public void serverTick(ServerTickEvent.Post event) {
		HudUpdateTick.tick();
		serverTick++;
		LPTickHandler.adjChecksDone = 0;
		ServerTickExecutor.INSTANCE.tick();
	}

	private static Map<Level, LPWorldInfo> worldInfo = new MapMaker().weakKeys().makeMap();

	@SubscribeEvent
	public void worldTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		LPWorldInfo info = LPTickHandler.getWorldInfo(serverLevel);
		info.worldTick++;
	}

	public static LPWorldInfo getWorldInfo(Level level) {
		LPWorldInfo info = LPTickHandler.worldInfo.get(level);
		if (info == null) {
			info = new LPWorldInfo();
			LPTickHandler.worldInfo.put(level, info);
		}
		return info;
	}

	@Data
	public static class LPWorldInfo {

		@Getter
		@Setter(value = AccessLevel.PRIVATE)
		private long worldTick = 0;
		@Getter
		private Set<BlockPos> updateQueued = new HashSet<>();

		@Getter
		@Setter
		private boolean skipBlockUpdateForWorld = false;
	}
}
