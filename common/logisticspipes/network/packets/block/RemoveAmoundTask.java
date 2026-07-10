package logisticspipes.network.packets.block;

import java.util.Iterator;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.blocks.stats.TrackingTask;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifier;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class RemoveAmoundTask extends CoordinatesPacket {

	@Setter
	@Getter
	private ItemIdentifier item;

	public RemoveAmoundTask(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsStatisticsTileEntity tile = this.getTileAs(player.level(), LogisticsStatisticsTileEntity.class);
		Iterator<TrackingTask> iter = tile.tasks.iterator();
		while (iter.hasNext()) {
			TrackingTask task = iter.next();
			if (task.item == item) {
				iter.remove();
				break;
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeItemIdentifier(item);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		item = input.readItemIdentifier();
	}

	@Override
	public ModernPacket template() {
		return new RemoveAmoundTask(getId());
	}
}
