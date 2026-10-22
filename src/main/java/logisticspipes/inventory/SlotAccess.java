package logisticspipes.inventory;

public interface SlotAccess {

    default void compactFirst(int size) {
        for (int firstSlot = 0; firstSlot < size; firstSlot++) {
            for (int secondSlot = firstSlot + 1; secondSlot < size; secondSlot++) {
                if (!isSlotEmpty(secondSlot) && (isSlotEmpty(firstSlot) || canMerge(firstSlot, secondSlot))) {
                    mergeSlots(firstSlot, secondSlot);
                }
            }
        }
    }

    void mergeSlots(int intoSlot, int fromSlot);

    boolean canMerge(int intoSlot, int fromSlot);

    boolean isSlotEmpty(int idx);
}
