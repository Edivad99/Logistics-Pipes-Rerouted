package logisticspipes.network.packets.orderer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.to_client.DiskContentMessage;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.world.item.LPItems;

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
			sendDisk(pipeItemsRequestLogisticsMk2.getDisk(), pipe.getBlockPos(), player);
		}
		if (pipe.pipe instanceof PipeBlockRequestTable pipeBlockRequestTable) {
			if (!pipeBlockRequestTable.diskInv.getItem(0).isEmpty()) {
				if (pipeBlockRequestTable.diskInv.getItem(0).getItem().equals(LPItems.DISK.get())) {
					if (!pipeBlockRequestTable.diskInv.getItem(0).has(DataComponents.CUSTOM_DATA)) {
						pipeBlockRequestTable.diskInv.getItem(0).set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
					}
				}
			}
			sendDisk(pipeBlockRequestTable.diskInv.getItem(0), pipe.getBlockPos(), player);
		}
	}

	private static void sendDisk(ItemStack disk, BlockPos pos, Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new DiskContentMessage(pos, disk));
		}
	}
}
