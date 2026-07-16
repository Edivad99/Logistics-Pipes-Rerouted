package logisticspipes.network.packets.orderer;

import logisticspipes.world.item.LPItems;
import logisticspipes.network.abstractpackets.ItemPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;

import java.util.Objects;

@StaticResolve
public class DiscContent extends ItemPacket {

	public DiscContent(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new DiscContent(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe tile = this.getPipe(player.level());
		if (tile == null) {
			return;
		}
		if (tile.pipe instanceof PipeItemsRequestLogisticsMk2 itemsRequestLogisticsMk2) {
			if (MainProxy.isServer(tile.getLevel())) {
				if (!itemsRequestLogisticsMk2.getDisk().isEmpty() && itemsRequestLogisticsMk2.getDisk().getItem().equals(LPItems.DISK.get())) {
					if (!getStack().isEmpty() && getStack().getItem().equals(LPItems.DISK.get())) {
						if (getStack().has(DataComponents.CUSTOM_DATA)) {
							var copyTag = Objects.requireNonNull(getStack().get(DataComponents.CUSTOM_DATA)).copyTag();
							itemsRequestLogisticsMk2.getDisk().set(DataComponents.CUSTOM_DATA, CustomData.of(copyTag));
						}
					}
				}
			} else {
				itemsRequestLogisticsMk2.setDisk(getStack());
			}
		}
		if (tile.pipe instanceof PipeBlockRequestTable pipeBlockRequestTable) {
			if (MainProxy.isServer(tile.getLevel())) {
				if (!pipeBlockRequestTable.diskInv.getItem(0).isEmpty() && pipeBlockRequestTable.diskInv.getItem(0).getItem().equals(LPItems.DISK.get())) {
					if (!getStack().isEmpty() && getStack().getItem().equals(LPItems.DISK.get())) {
						if (getStack().has(DataComponents.CUSTOM_DATA)) {
							var copyTag = Objects.requireNonNull(getStack().get(DataComponents.CUSTOM_DATA)).copyTag();
							pipeBlockRequestTable.diskInv.getItem(0).set(DataComponents.CUSTOM_DATA, CustomData.of(copyTag));
						}
					}
				}
			} else {
				pipeBlockRequestTable.diskInv.setItem(0, getStack());
			}
		}
	}
}
