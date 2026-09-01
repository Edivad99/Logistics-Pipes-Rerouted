package logisticspipes.network.packets.orderer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.to_client.DiskContentMessage;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.world.item.LPItems;

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
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, new DiskContentMessage(pipe.getBlockPos(), disk));
			}
		}
	}
}
