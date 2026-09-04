package logisticspipes.proxy.side;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.interfaces.IProxy;

public class ServerProxy implements IProxy {

    @Override
	public void addLogisticsPipesOverride(Object par1IIconRegister, int index, String override1, String override2, boolean flag) {}

	@Override
	public void sendBroadCast(String message) {
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
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
	public boolean checkSinglePlayerOwner(String commandSenderName) {
		return false;
	}

	@Override
	public void openFluidSelectGui(int slotId) {}

}
