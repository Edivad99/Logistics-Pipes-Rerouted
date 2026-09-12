package logisticspipes.api.property;

import java.util.BitSet;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BitSetProperty implements IBitSet, Property<BitSet> {

    private final BitSet bitset;
    private final String tagKey;

    private final CopyOnWriteArraySet<ObserverCallback<BitSet>> propertyObservers = new CopyOnWriteArraySet<>();

    public BitSetProperty(BitSet bitset, String tagKey) {
        this.bitset = bitset;
        this.tagKey = tagKey;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public CopyOnWriteArraySet<ObserverCallback<BitSet>> getPropertyObservers() {
        return propertyObservers;
    }

    @Override
    public BitSet copyValue() {
        return (BitSet) bitset.clone();
    }

    public BitSet copyValue(int startIdx, int endIdx) {
        return bitset.get(startIdx, endIdx + 1);
    }

    @Override
    public void clear() {
        bitset.clear();
        iChanged();
    }

    @Override
    public BitSetProperty copyProperty() {
        return new BitSetProperty(copyValue(), tagKey);
    }

    @Override
    public void replaceWith(BitSet other) {
        if (other.equals(bitset)) {
            return;
        }
        bitset.clear();
        bitset.or(other);
        iChanged();
    }

    @Override
    public void replaceWith(IBitSet other) {
        if (other.equals(bitset)) {
            return;
        }
        bitset.clear();
        bitset.or(other.copyValue());
        iChanged();
    }

    public void replaceWith(BitSetProperty other) {
        if (other == this) {
            return;
        }
        bitset.clear();
        bitset.or(other.bitset);
        iChanged();
    }

    @Override
    public void deserialize(ValueInput input) {
        input.read(tagKey, ExtraCodecs.BIT_SET).ifPresent(this::replaceWith);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store(tagKey, ExtraCodecs.BIT_SET, bitset);
    }

    @Override
    public boolean get(int bit) {
        return bitset.get(bit);
    }

    @Override
    public void set(int bit, boolean value) {
        bitset.set(bit, value);
        iChanged();
    }

    @Override
    public void flip(int bit) {
        bitset.flip(bit);
        iChanged();
    }

    @Override
    public int nextSetBit(int idx) {
        return bitset.nextSetBit(idx);
    }

    public IBitSet get(int startIdx, int endIdx) {
        if (startIdx < 0 || startIdx >= bitset.size()) {
            throw new IndexOutOfBoundsException("startIdx[" + startIdx + "] is out of bounds");
        }
        if (endIdx < 0 || endIdx >= bitset.size()) {
            throw new IndexOutOfBoundsException("endIdx[" + endIdx + "] is out of bounds");
        }
        return new PartialBitSet(startIdx, endIdx);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof BitSetProperty that
            && tagKey.equals(that.tagKey)
            && bitset.equals(that.bitset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagKey, bitset);
    }

    @Override
    public String toString() {
        return "BitSetProperty(tagKey=" + tagKey + ", bitset=" + bitset + ")";
    }

    public class PartialBitSet implements IBitSet {

        private final int first;
        private final int last;

        PartialBitSet(int first, int last) {
            if (last < first) {
                throw new IllegalArgumentException("start[" + first + "] must be <= end[" + last + "]");
            }
            this.first = first;
            this.last = last;
        }

        public int getSize() {
            return (last - first) + 1;
        }

        private int rangeCheck(int idx) {
            if (idx < 0 || idx >= getSize()) {
                throw new IndexOutOfBoundsException("idx[" + idx + "] out of bounds of " + this);
            }
            return idx;
        }

        @Override
        public boolean get(int bit) {
            return BitSetProperty.this.get(first + rangeCheck(bit));
        }

        @Override
        public void set(int bit, boolean value) {
            BitSetProperty.this.set(first + rangeCheck(bit), value);
        }

        @Override
        public void flip(int bit) {
            BitSetProperty.this.flip(first + rangeCheck(bit));
        }

        @Override
        public void clear() {
            bitset.clear(first, last + 1);
            iChanged();
        }

        @Override
        public BitSet copyValue() {
            return BitSetProperty.this.copyValue(first, last);
        }

        @Override
        public int nextSetBit(int idx) {
            if (idx >= getSize()) {
                return -1;
            }
            int found = BitSetProperty.this.nextSetBit(first + rangeCheck(idx));
            return found == -1 || found > last ? -1 : found - first;
        }

        @Override
        public void replaceWith(BitSet other) {
            bitset.clear(first, last + 1);
            other.stream().map(bit -> first + bit).filter(bit -> bit <= last).forEach(bitset::set);
            iChanged();
        }

        @Override
        public void replaceWith(IBitSet other) {
            bitset.clear(first, last + 1);
            other.stream().map(bit -> first + bit).takeWhile(bit -> bit <= last).forEach(bitset::set);
            iChanged();
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof IBitSet that && that.copyValue().equals(copyValue());
        }

        @Override
        public int hashCode() {
            return copyValue().hashCode();
        }

        @Override
        public String toString() {
            return "PartialBitSet(indices=" + first + ".." + last + ", bitset=" + copyValue() + ")";
        }
    }
}
