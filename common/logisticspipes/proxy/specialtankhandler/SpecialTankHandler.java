package logisticspipes.proxy.specialtankhandler;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.Lists;
import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ISpecialTankHandler;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SpecialTankHandler {

	private List<ISpecialTankHandler> handlers = new ArrayList<>();

	public void registerHandler(ISpecialTankHandler handler) {
		try {
			if (handler.init()) {
				handlers.add(handler);
				LogisticsPipes.log.info("Loaded SpecialTankHandler: " + handler.getClass().getName());
			} else {
				LogisticsPipes.log.warn("Didn't load SpecialTankHandler: " + handler.getClass().getName());
			}
		} catch (Exception e) {
			LogisticsPipes.log.error("Failed to register SpecialTankHandler", e);
		}
	}

	public List<BlockEntity> getBaseTileFor(BlockEntity tile) {
		for (ISpecialTankHandler handler : handlers) {
			if (handler.isType(tile)) {
				return handler.getBaseTilesFor(tile);
			}
		}
		return Lists.newArrayList(tile);
	}

	public boolean hasHandlerFor(BlockEntity tile) {
		if (tile == null) {
			return false;
		}
		for (ISpecialTankHandler handler : handlers) {
			if (handler.isType(tile)) {
				return true;
			}
		}
		return false;
	}

	public ISpecialTankHandler getTankHandlerFor(BlockEntity tile) {
		for (ISpecialTankHandler handler : handlers) {
			if (handler.isType(tile)) {
				return handler;
			}
		}
		String name = "null";
		if (tile != null) {
			name = tile.getClass().getName();
		}
		throw new RuntimeException("Unknwon TankBlockEntity Request, '" + name + "'");
	}
}
