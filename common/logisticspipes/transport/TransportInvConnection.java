package logisticspipes.transport;

import java.util.Objects;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.utils.OrientationsUtil;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

public class TransportInvConnection extends PipeTransportLogistics {

	public TransportInvConnection() {
		super(true);
	}

	@Override
	protected boolean isItemUnwanted(ItemIdentifierStack stack) {
		return false;
	}

	@Override
	protected void inventorySystemConnectorHook(ItemRoutingInformation info, BlockEntity tile) {
		if (tile == null) {
			return;
		}

		final Direction orientationOfTilewithTile = OrientationsUtil.getOrientationOfTilewithTile(getPipe().container, tile);
		Objects.requireNonNull(orientationOfTilewithTile, "Could not get direction from pipe and tile entity");

		var level = tile.getLevel();
		if (level != null)
		{
			var itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, tile.getBlockPos(), orientationOfTilewithTile.getOpposite());
			if (itemHandler != null)
			{
				((PipeItemsInvSysConnector) container.pipe).handleItemEnterInv(info, tile);
			}
		}
	}
}
