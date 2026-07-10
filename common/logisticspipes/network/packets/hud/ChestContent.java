package logisticspipes.network.packets.hud;

import logisticspipes.interfaces.IChestContentReceiver;
import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class ChestContent extends InventoryModuleCoordinatesPacket {

	public ChestContent(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ChestContent(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe != null && pipe.pipe instanceof IChestContentReceiver) {
			((IChestContentReceiver) pipe.pipe).setReceivedChestContent(getIdentList());
		}

	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
