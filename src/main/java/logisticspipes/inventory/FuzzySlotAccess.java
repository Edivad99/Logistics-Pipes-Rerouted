package logisticspipes.inventory;

import logisticspipes.api.property.BitSetProperty;
import logisticspipes.api.property.IBitSet;

/**
 * Moves a slot's fuzzy flags along with its contents.
 */
public class FuzzySlotAccess implements SlotAccess {

    private final SlotAccess slotAccess;
    private final BitSetProperty fuzzyFlags;

    public FuzzySlotAccess(SlotAccess slotAccess, BitSetProperty fuzzyFlags) {
        this.slotAccess = slotAccess;
        this.fuzzyFlags = fuzzyFlags;
    }

    private IBitSet bitsForSlot(int idx) {
        return fuzzyFlags.get(idx * 4, idx * 4 + 3);
    }

    @Override
    public void mergeSlots(int intoSlot, int fromSlot) {
        slotAccess.mergeSlots(intoSlot, fromSlot);
        bitsForSlot(intoSlot).replaceWith(bitsForSlot(fromSlot));
        bitsForSlot(fromSlot).clear();
    }

    @Override
    public boolean canMerge(int intoSlot, int fromSlot) {
        return slotAccess.canMerge(intoSlot, fromSlot)
            && (isSlotEmpty(intoSlot) || bitsForSlot(intoSlot).equals(bitsForSlot(fromSlot)));
    }

    @Override
    public boolean isSlotEmpty(int idx) {
        return slotAccess.isSlotEmpty(idx);
    }
}
