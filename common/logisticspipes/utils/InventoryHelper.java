package logisticspipes.utils;

import javax.annotation.Nullable;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.transactor.ITransactor;
import logisticspipes.utils.transactor.TransactorSimple;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import network.rs485.logisticspipes.inventory.ProviderMode;

public class InventoryHelper {

	//BC getTransactorFor using our getInventory
	public static ITransactor getTransactorFor(Object object, @Nullable Direction dir) {
		if (object instanceof BlockEntity tile) {
			ITransactor t = SimpleServiceLocator.inventoryUtilFactory.getSpecialHandlerFor(tile, dir, ProviderMode.DEFAULT);
			if (t != null) {
				return t;
			}
			// NeoForge 1.20.1: BlockCapability queried via static method
			var level = tile.getLevel();
			if (level != null) {
				var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, tile.getBlockPos(), dir);
				if (handler != null) {
					return new TransactorSimple(handler);
				}
			}
		}
		return null;
	}
}
