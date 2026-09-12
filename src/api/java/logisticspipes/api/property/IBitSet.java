package logisticspipes.api.property;

import java.util.BitSet;
import java.util.stream.IntStream;

public interface IBitSet {

    boolean get(int bit);

    void set(int bit, boolean value);

    void flip(int bit);

    int nextSetBit(int idx);

    void replaceWith(BitSet other);

    void replaceWith(IBitSet other);

    BitSet copyValue();

    void clear();

    /** The set bit indices, ascending. */
    default IntStream stream() {
        return IntStream.iterate(nextSetBit(0), bit -> bit != -1, bit -> nextSetBit(bit + 1));
    }
}
