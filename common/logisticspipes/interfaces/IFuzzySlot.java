package logisticspipes.interfaces;

import java.util.EnumSet;

import network.rs485.logisticspipes.util.FuzzyFlag;

import logisticspipes.api.property.IBitSet;

public interface IFuzzySlot {

	/**
	 * Which of the four flags this slot actually honours: a screen offers only these, so a module
	 * does not advertise matching it will then ignore. All four by default.
	 */
	default EnumSet<FuzzyFlag> getUsedFlags() {
		return EnumSet.allOf(FuzzyFlag.class);
	}


	IBitSet getFuzzyFlags();

	int getX();

	int getY();

	int getSlotId();
}
