package logisticspipes.utils;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import net.minecraftforge.common.capabilities.ForgeCapabilities;

import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.transactor.ITransactor;
import logisticspipes.utils.transactor.TransactorSimple;
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
			if (tile.getLevel() != null) {
				var handler = tile.getCapability(ForgeCapabilities.ITEM_HANDLER, dir).orElse(null);
				if (handler != null) {
					return new TransactorSimple(handler);
				}
			}
		}
		return null;
	}
}
