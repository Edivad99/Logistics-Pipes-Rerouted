package logisticspipes.network.packets.orderer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

import logisticspipes.LPItems;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class DiskSetNamePacket extends StringCoordinatesPacket {

	public DiskSetNamePacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new DiskSetNamePacket(getId());
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
				((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().setTag(new CompoundTag());
			}
			CompoundTag nbt = ((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().getTag();
			nbt.putString("name", getString());
		}
	}
}
