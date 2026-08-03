package logisticspipes.network.packets.orderer;

import logisticspipes.world.item.LPItems;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@StaticResolve
public class DiskDropPacket extends CoordinatesPacket {

	public DiskDropPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new DiskDropPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof PipeItemsRequestLogisticsMk2 pipeItemsRequestLogisticsMk2) {
            ItemStack disk = pipeItemsRequestLogisticsMk2.getDisk();
			if (!disk.isEmpty()) {
				if (disk.getItem().equals(LPItems.DISK.get())) {
					if (!disk.has(DataComponents.CUSTOM_DATA)) {
                        disk.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
					}
				}
			}
			pipeItemsRequestLogisticsMk2.dropDisk();
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(DiscContent.class).setStack(disk).setBlockPos(pipe.getBlockPos()), player);
		}
	}
}
