package logisticspipes.network.packets.block;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class RequestAmountTaskSubGui extends CoordinatesPacket {

	public RequestAmountTaskSubGui(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsStatisticsTileEntity tile = this.getTileAs(player.level(), LogisticsStatisticsTileEntity.class);
		CoreRoutedPipe pipe = tile.getConnectedPipe();
		if (pipe == null) {
			return;
		}

		Map<ItemIdentifier, Integer> availableItems = SimpleServiceLocator.logisticsManager.getAvailableItems(pipe.getRouter().getIRoutersByCost());
		LinkedList<ItemIdentifier> craftableItems = SimpleServiceLocator.logisticsManager.getCraftableItems(pipe.getRouter().getIRoutersByCost());

		TreeSet<ItemIdentifierStack> allItems = new TreeSet<>();

		for (Entry<ItemIdentifier, Integer> item : availableItems.entrySet()) {
			ItemIdentifierStack newStack = item.getKey().makeStack(item.getValue());
			allItems.add(newStack);
		}

		for (ItemIdentifier item : craftableItems) {
			if (availableItems.containsKey(item)) {
				continue;
			}
			allItems.add(item.makeStack(1));
		}

		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(AmountTaskSubGui.class).setIdentSet(allItems), player);
	}

	@Override
	public ModernPacket template() {
		return new RequestAmountTaskSubGui(getId());
	}
}
