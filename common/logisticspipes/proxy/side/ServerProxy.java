package logisticspipes.proxy.side;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.server.ServerLifecycleHooks;

import logisticspipes.LogisticsPipes;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.interfaces.IProxy;
import logisticspipes.utils.item.ItemIdentifier;

public class ServerProxy implements IProxy {

	// LangDatabase (server-side item name cache) is not implemented in 1.20.1.
	// getName() falls back to getFriendlyName(); updateNames/tick/sendNameUpdateRequest are no-ops.
	@SuppressWarnings("unused")
	private long saveThreadTime = 0;

	@Override
	public String getSide() {
		return "Server";
	}

	@Override
	public Level getWorld() {
		return null;
	}

	@Override
	public void registerTileEntities() {}

	@Override
	public Player getClientPlayer() {
		return null;
	}

	@Override
	public void registerParticles() {}

	@Override
	public String getName(ItemIdentifier item) {
		return item.getFriendlyName();
	}

	@Override
	public void updateNames(ItemIdentifier item, String name) {}

	@Override
	public void tick() {}

	@Override
	public void sendNameUpdateRequest(Player player) {}

	@Override
	public LogisticsTileGenericPipe getPipeInDimensionAt(int dimension, int x, int y, int z, Player player) {
		// Dimension is encoded as dim.location().hashCode() (see ModernPacket/RouterManager).
		// Scan all loaded server levels for a matching hash.
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return null;
		for (net.minecraft.server.level.ServerLevel lvl : server.getAllLevels()) {
			if (lvl.dimension().location().hashCode() == dimension) {
				return getPipe(lvl, x, y, z);
			}
		}
		return null;
	}

	protected static LogisticsTileGenericPipe getPipe(Level level, int x, int y, int z) {
		if (level == null) return null;
		BlockPos pos = new BlockPos(x, y, z);
		if (level.isEmptyBlock(pos)) return null;
		BlockEntity tile = level.getBlockEntity(pos);
		return tile instanceof LogisticsTileGenericPipe ? (LogisticsTileGenericPipe) tile : null;
	}

	@Override
	public void addLogisticsPipesOverride(Object par1IIconRegister, int index, String override1, String override2, boolean flag) {}

	@Override
	public void sendBroadCast(String message) {
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server != null && server.getPlayerList() != null) {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				p.sendSystemMessage(Component.literal("[LP] Server: " + message));
			}
		}
	}

	@Override
	public void tickServer() {
		MainProxy.addTick();
	}

	@Override
	public void tickClient() {}

	@Override
	public void getPlayerFromNetHandler(Object handler) {
		// TODO: Connection/ServerGamePacketListenerImpl — deferred to network rewrite
	}

	@Override
	public void setIconProviderFromPipe(ItemLogisticsPipe item, CoreUnroutedPipe dummyPipe) {}

	@Override
	public LogisticsModule getModuleFromGui() {
		return null;
	}

	@Override
	public boolean checkSinglePlayerOwner(String commandSenderName) {
		return false;
	}

	@Override
	public void openFluidSelectGui(int slotId) {}

	@Override
	public void registerTextures() {}

	@Override
	public void initModelLoader() {}

	@Override
	public int getRenderIndex() {
		return 0;
	}
}
