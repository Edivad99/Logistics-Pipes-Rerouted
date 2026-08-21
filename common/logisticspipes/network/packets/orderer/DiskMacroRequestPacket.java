package logisticspipes.network.packets.orderer;

import logisticspipes.world.item.LPItems;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.StaticResolve;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

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
		if (pipe.pipe instanceof PipeItemsRequestLogisticsMk2 pipeItemsRequestLogisticsMk2) {
			if (pipeItemsRequestLogisticsMk2.getDisk().isEmpty()) {
				return;
			}
			if (!pipeItemsRequestLogisticsMk2.getDisk().getItem().equals(LPItems.DISK.get())) {
				return;
			}
			if (!pipeItemsRequestLogisticsMk2.getDisk().has(DataComponents.CUSTOM_DATA)) {
				return;
			}
			CompoundTag nbt = Objects.requireNonNull(pipeItemsRequestLogisticsMk2.getDisk().get(DataComponents.CUSTOM_DATA)).copyTag();
			if (!nbt.contains("macroList")) {
				ListTag list = new ListTag();
				nbt.put("macroList", list);
			}
			ListTag list = nbt.getListOrEmpty("macroList");
			for (int i = 0; i < list.size(); i++) {
				if (i == getInteger()) {
					CompoundTag itemlist = list.getCompoundOrEmpty(i);
					RequestHandler.requestMacrolist(itemlist, pipeItemsRequestLogisticsMk2, player);
					break;
				}
			}
		}
		if (pipe.pipe instanceof PipeBlockRequestTable pipeBlockRequestTable) {
			if (pipeBlockRequestTable.getDisk() == null) {
				return;
			}
			if (!pipeBlockRequestTable.getDisk().getItem().equals(LPItems.DISK.get())) {
				return;
			}
			if (!pipeBlockRequestTable.getDisk().has(DataComponents.CUSTOM_DATA)) {
				return;
			}
			CompoundTag nbt = Objects.requireNonNull(pipeBlockRequestTable.getDisk().get(DataComponents.CUSTOM_DATA)).copyTag();
			if (!nbt.contains("macroList")) {
				ListTag list = new ListTag();
				nbt.put("macroList", list);
			}
			ListTag list = nbt.getListOrEmpty("macroList");
			for (int i = 0; i < list.size(); i++) {
				if (i == getInteger()) {
					CompoundTag itemlist = list.getCompoundOrEmpty(i);
					RequestHandler.requestMacrolist(itemlist, pipeBlockRequestTable, player);
					break;
				}
			}
		}
	}
}
