package logisticspipes.ticks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.MapMaker;
import logisticspipes.commands.commands.debug.DebugGuiController;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifier;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import network.rs485.grow.ServerTickDispatcher;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LPTickHandler {

	public static int adjChecksDone = 0;

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void clientTick(ClientTickEvent.Post event) {
		FluidIdentifier.initFromNeoForge(true);
		SimpleServiceLocator.clientBufferHandler.clientTick();
		MainProxy.getProxy(true).tickClient();
		DebugGuiController.instance().execClient();
	}

	@SubscribeEvent
	public void serverTick(ServerTickEvent.Post event) {
		HudUpdateTick.tick();
		SimpleServiceLocator.serverBufferHandler.serverTick();
		MainProxy.getProxy(false).tickServer();
		LPTickHandler.adjChecksDone = 0;
		DebugGuiController.instance().execServer();
		ServerTickDispatcher.INSTANCE.tick();
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
		private Set<DoubleCoordinates> updateQueued = new HashSet<>();

		@Getter
		@Setter
		private boolean skipBlockUpdateForWorld = false;
	}
}
