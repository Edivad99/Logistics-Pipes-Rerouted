package logisticspipes.network.packets.orderer;

import logisticspipes.world.item.LPItems;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;

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
		if (pipe.pipe instanceof PipeItemsRequestLogisticsMk2 pipeItemsRequestLogisticsMk2) {
			if (pipeItemsRequestLogisticsMk2.getDisk() == null) {
				return;
			}
			if (!pipeItemsRequestLogisticsMk2.getDisk().getItem().equals(LPItems.DISK.get())) {
				return;
			}
			pipeItemsRequestLogisticsMk2.getDisk().update(
					DataComponents.CUSTOM_DATA,
					CustomData.EMPTY,
					customData -> {
						CompoundTag tag = customData.copyTag();
						tag.putString("name", getString());
						return CustomData.of(tag);
					}
			);
		}
	}
}
