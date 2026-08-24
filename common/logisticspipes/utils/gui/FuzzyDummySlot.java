package logisticspipes.utils.gui;

import net.minecraft.world.Container;

import logisticspipes.interfaces.IFuzzySlot;
import network.rs485.logisticspipes.property.IBitSet;

public class FuzzyDummySlot extends DummySlot implements IFuzzySlot {

	private final IBitSet fuzzyFlags;

	public FuzzyDummySlot(Container iinventory, int i, int j, int k, IBitSet fuzzyFlags) {
		super(iinventory, i, j, k);
		this.fuzzyFlags = fuzzyFlags;
	}

	@Override
	public IBitSet getFuzzyFlags() {
		return fuzzyFlags;
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public int getSlotId() {
		return this.index;
	}
}
