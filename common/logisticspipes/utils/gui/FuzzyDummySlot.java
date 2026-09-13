package logisticspipes.utils.gui;

import java.util.EnumSet;

import net.minecraft.world.Container;

import logisticspipes.api.property.IBitSet;
import logisticspipes.interfaces.IFuzzySlot;
import logisticspipes.utils.FuzzyFlag;

public class FuzzyDummySlot extends DummySlot implements IFuzzySlot {

	private final IBitSet fuzzyFlags;
	private final EnumSet<FuzzyFlag> usedFlags;

	public FuzzyDummySlot(Container container, int i, int j, int k, IBitSet fuzzyFlags) {
		this(container, i, j, k, fuzzyFlags, EnumSet.allOf(FuzzyFlag.class));
	}

	/** For a module that honours only some of the flags; the others are not offered. */
	public FuzzyDummySlot(Container container, int i, int j, int k, IBitSet fuzzyFlags, EnumSet<FuzzyFlag> usedFlags) {
		super(container, i, j, k);
		this.fuzzyFlags = fuzzyFlags;
		this.usedFlags = usedFlags;
	}

	@Override
	public EnumSet<FuzzyFlag> getUsedFlags() {
		return usedFlags;
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
