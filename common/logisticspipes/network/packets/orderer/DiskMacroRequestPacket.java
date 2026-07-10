package logisticspipes.network.packets.orderer;

import logisticspipes.LPItems;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.StaticResolve;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class DiskMacroRequestPacket extends IntegerCoordinatesPacket {

	public DiskMacroRequestPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new DiskMacroRequestPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof PipeItemsRequestLogisticsMk2) {
			if (((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk() == null) {
				return;
			}
			if (!((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().getItem().equals(LPItems.disk.get())) {
				return;
			}
			if (!((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().hasTag()) {
				return;
			}
			CompoundTag nbt = ((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().getTag();
			if (!nbt.contains("macroList")) {
				ListTag list = new ListTag();
				nbt.put("macroList", list);
			}
			ListTag list = nbt.getList("macroList", 10);
			for (int i = 0; i < list.size(); i++) {
				if (i == getInteger()) {
					CompoundTag itemlist = list.getCompound(i);
					RequestHandler.requestMacrolist(itemlist, (PipeItemsRequestLogisticsMk2) pipe.pipe, player);
					break;
				}
			}
		}
		if (pipe.pipe instanceof PipeBlockRequestTable) {
			if (((PipeBlockRequestTable) pipe.pipe).getDisk() == null) {
				return;
			}
			if (!((PipeBlockRequestTable) pipe.pipe).getDisk().getItem().equals(LPItems.disk.get())) {
				return;
			}
			if (!((PipeBlockRequestTable) pipe.pipe).getDisk().hasTag()) {
				return;
			}
			CompoundTag nbt = ((PipeBlockRequestTable) pipe.pipe).getDisk().getTag();
			if (!nbt.contains("macroList")) {
				ListTag list = new ListTag();
				nbt.put("macroList", list);
			}
			ListTag list = nbt.getList("macroList", 10);
			for (int i = 0; i < list.size(); i++) {
				if (i == getInteger()) {
					CompoundTag itemlist = list.getCompound(i);
					RequestHandler.requestMacrolist(itemlist, (PipeBlockRequestTable) pipe.pipe, player);
					break;
				}
			}
		}
	}
}
