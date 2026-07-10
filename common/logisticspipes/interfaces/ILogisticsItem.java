package logisticspipes.interfaces;

import org.apache.commons.lang3.NotImplementedException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public interface ILogisticsItem {

	default String getModelPath() {
		return BuiltInRegistries.ITEM.getKey(getItem()).getPath();
	}

	default int getModelCount() {
		return 1;
	}

	default Item getItem() {
		if (this instanceof Item) {
			return (Item) this;
		} else {
			throw new NotImplementedException("not implemented");
		}
	}

}
