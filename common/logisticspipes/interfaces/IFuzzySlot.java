package logisticspipes.interfaces;

import logisticspipes.api.property.IBitSet;

public interface IFuzzySlot {

	IBitSet getFuzzyFlags();

	int getX();

	int getY();

	int getSlotId();
}
