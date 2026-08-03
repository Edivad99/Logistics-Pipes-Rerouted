package logisticspipes.network.packets.orderer;

import logisticspipes.world.item.LPItems;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;

@StaticResolve
public class DiskRequestConectPacket extends CoordinatesPacket {

	public DiskRequestConectPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new DiskRequestConectPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof PipeItemsRequestLogisticsMk2 pipeItemsRequestLogisticsMk2) {
			if (!pipeItemsRequestLogisticsMk2.getDisk().isEmpty()) {
				if (pipeItemsRequestLogisticsMk2.getDisk().getItem().equals(LPItems.DISK.get())) {
					if (!pipeItemsRequestLogisticsMk2.getDisk().has(DataComponents.CUSTOM_DATA)) {
						pipeItemsRequestLogisticsMk2.getDisk().set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
					}
				}
			}
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(DiscContent.class).setStack(pipeItemsRequestLogisticsMk2.getDisk()).setBlockPos(pipe.getBlockPos()), player);
		}
		if (pipe.pipe instanceof PipeBlockRequestTable pipeBlockRequestTable) {
			if (!pipeBlockRequestTable.diskInv.getItem(0).isEmpty()) {
				if (pipeBlockRequestTable.diskInv.getItem(0).getItem().equals(LPItems.DISK.get())) {
					if (!pipeBlockRequestTable.diskInv.getItem(0).has(DataComponents.CUSTOM_DATA)) {
						pipeBlockRequestTable.diskInv.getItem(0).set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
					}
				}
			}
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(DiscContent.class).setStack(pipeBlockRequestTable.diskInv.getItem(0)).setBlockPos(pipe.getBlockPos()), player);
		}
	}
}
