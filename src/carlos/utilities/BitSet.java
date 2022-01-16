package carlos.utilities;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Class useful for compact storage of data, constant data mapping and bitwise operations.
 * Combining a BitSet with a constant enumeration whose constants hold a bit position results in a map like structure.
 * @author Carlos Milkovic
 * @version 1.0
 * @see Serializable
 * @see Iterable
 */
@SuppressWarnings("unused")
public final class BitSet implements Serializable, Iterable<Boolean> {
    public static final BitSet EMPTY = new BitSet(0);
    @Serial
    private static final long serialVersionUID = 9174287491114344015L;
    private byte[] bits;

    /**
     * Creates an empty {@link BitSet}
     * @see BitSet
     */
    public BitSet() {
        bits = new byte[1];
    }

    /**
     * Only required for the static {@link BitSet#EMPTY}. <br/>
     * API manages its size automatically, so it is not required to pre-set the size.
     * @param size initial size.
     */
    private BitSet(int size) {
        bits = new byte[size >>> 0b11];
    }

    /**
     * Creates a {@link BitSet} and pre-sets the bits defined in the list
     * @param bits bits to be set.
     * @throws NullPointerException if specified {@link List} is null.
     * @see BitSet
     * @see List
     */
    public BitSet(List<Integer> bits) {
        Objects.requireNonNull(bits);
        this.bits = new byte[(max(bits) >>> 0b11) + 0b1];
        setBits(bits);
    }

    /**
     * Copy constructor for this class.
     * @param copy {@link BitSet} to copy.
     * @throws NullPointerException if specified {@link BitSet} is null.
     * @see BitSet
     */
    public BitSet(BitSet copy) {
        Objects.requireNonNull(copy);
        bits = copy.bits;
    }

    /**
     * Returns the current bit-width of the set.
     * @return bit-width.
     * @see BitSet
     */
    public int size() {
        return (bits.length << 0b11);
    }

    /**
     * Inverts this {@link BitSet}.<br/>
     * Eg. 101 -> 010
     * @return the same object. <b>MUTABLE OPERATION</b>
     * @see BitSet
     */
    public BitSet invert() {
        for(int i = 0; i < bits.length; i++)
            bits[i] = (byte) ~bits[i];
        return this;
    }

    /**
     * Inverts this {@link BitSet}.<br/>
     * Eg. 101 -> 010
     * @return new inverted <code>BitSet</code>. <b>IMMUTABLE OPERATION</b>
     * @see BitSet
     */
    public BitSet not() {
        return new BitSet(this).invert();
    }

    /**
     * ANDs this {@link BitSet} with the specified {@link BitSet}. <br/>
     * Eg. 101 & 110 -> 100
     * @param other {@link BitSet} to be ANDed with.
     * @return new resultant {@link BitSet}. <b>IMMUTABLE OPERATION</b>
     * @throws NullPointerException if specified <code>BitSet</code> is null.
     * @see BitSet
     */
    public BitSet and(BitSet other) {
        Objects.requireNonNull(other);
        var result = new BitSet(Math.min(this.size(), other.size()));
        for(int i = 0; i < result.bits.length; i++)
            result.bits[i] = (byte) (bits[i] & other.bits[i]);
        return result;
    }

    /**
     * ORs this {@link BitSet} with the specified {@link BitSet}. <br/>
     * Eg. 101 & 110 -> 111
     * @param other {@link BitSet} to be ORed with.
     * @return new resultant {@link BitSet}. <b>IMMUTABLE OPERATION</b>
     * @throws NullPointerException if specified {@link BitSet} is null.
     * @see BitSet
     */
    public BitSet or(BitSet other) {
        Objects.requireNonNull(other);
        var result = new BitSet(Math.max(size(), other.size()));
        for(int i = 0; i < result.bits.length; i++)
            result.bits[i] = (byte) (bits[i] | other.bits[i]);
        return result;
    }

    /**
     * XORs this {@link BitSet} with the specified {@link BitSet}. <br/>
     * Eg. 101 & 110 -> 011
     * @param other {@link BitSet} to be ANDed with.
     * @return new resultant {@link BitSet}. <b>IMMUTABLE OPERATION</b>
     * @throws NullPointerException if specified {@link BitSet} is null.
     * @see BitSet
     */
    public BitSet xor(BitSet other) {
        Objects.requireNonNull(other);
        var result = new BitSet(Math.max(size(), other.size()));
        for(int i = 0; i < result.bits.length; i++)
            result.bits[i] = (byte) (bits[i] ^ other.bits[i]);
        return result;
    }

    /**
     * Sets the specified bit to 0.
     * @param b bit to be reset.
     * @throws IndexOutOfBoundsException if the bit is negative.
     * @see BitSet
     */
    public void resetBit(int b) {
        outOfBoundsCheck(b);
        bits[b >>> 0b11] &= ~((byte) 0b1 << (b & 0b111));
        shrinkIfPossible();
    }

    /**
     * Sets the specified bit to 1.
     * @param b bit to be set.
     * @throws IndexOutOfBoundsException if the bit is negative.
     * @see BitSet
     */
    public void setBit(int b) {
        expandBits(b);
        outOfBoundsCheck(b);
        // to clarify: a % x == a & (x - 1) ∀x ∈ { r = radix ∈ N | log(x) / log(r) ∈ N }
        bits[b >>> 0b11] |= ((byte) 0b1 << (b & 0b111));
    }

    /**
     * Toggles specified bit between 1 and 0.
     * @param b bit to be toggled.
     * @throws IndexOutOfBoundsException if the bit is negative.
     * @see BitSet
     */
    public void toggleBit(int b) {
        expandBits(b);
        outOfBoundsCheck(b);
        // to clarify: a % x == a & (x - 1) ∀x ∈ { r = radix ∈ N | log(x) / log(r) ∈ N }
        bits[b >>> 0b11] ^= ((byte) 0b1 << (b & 0b111));
        shrinkIfPossible();
    }

    /**
     * Sets all bits in the list to 1.
     * @param bits list of bits to be set.
     * @throws NullPointerException if the specified {@link List} is null.
     * @see BitSet
     * @see List
     */
    public void setBits(List<Integer> bits) {
        Objects.requireNonNull(bits);
        for (var b : bits) setBit(b);
    }

    /**
     * Tests whether the specified bit is on or off.
     * @param b bit to be tested.
     * @return true if 1, false if 0
     * @throws IndexOutOfBoundsException if the bit is negative.
     * @see BitSet
     */
    public boolean isSet(int b) {
        if(b < 0) throw new IndexOutOfBoundsException("Bit cannot be negative.");
        else if(b >= size()) return false;
        // to clarify: a % x == a & (x - 1) ∀x ∈ { log2(x) ∈ Z }
        return (bits[b >>> 0b11] & ((byte) 0b1 << (b & 0b111))) != 0;
    }
    @Override
    public String toString() {
        int binaryLength = (bits.length << 3) - 1, maxDecimalLength = (int) Math.log(binaryLength) + 1;
        var sb = new StringBuilder();
        appendIndexes(binaryLength, maxDecimalLength, sb);
        sb.append('\n');
        appendSetBits(binaryLength, maxDecimalLength, sb);
        return sb.append('\n').toString();
    }

    /**
     * Two bitsets are equal if their bits match. Identical to the bitwise XNOR operation.
     * @param o {@link BitSet} to be compared with.
     * @return true if they are equal.
     * @see BitSet
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitSet other = (BitSet) o;
        return Arrays.equals(bits, other.bits);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bits);
    }

    /**
     *
     * @return an iterator of bits in decreasing index order as boolean values.
     * @see BitSet
     * @see Iterator
     */
    @Override
    public Iterator<Boolean> iterator() {
        return new Iterator<>() {
            int i = 0;
            @Override
            public boolean hasNext() {
                return i < size();
            }

            @Override
            public Boolean next() {
                return isSet(i++);
            }
        };
    }

    private Integer max(List<Integer> bits) {
        return bits.stream().max(Integer::compareTo).orElseThrow();
    }

    private void shrinkIfPossible() {
        int index = bits.length;
        boolean shouldShrink = true;
        while(shouldShrink && index > 0)
            shouldShrink = bits[--index] == (byte) 0;
        bits = shrinkBits(index);
    }

    private byte[] shrinkBits(int index) {
        return Arrays.copyOfRange(bits, 0, index + 1);
    }

    private void outOfBoundsCheck(int bit) {
        if(bit < 0 || bit >= size()) throw new IndexOutOfBoundsException("Bit is out of bounds! -> " + bit + " for size 0 - " + size());
    }

    private void appendIndexes(int binaryLength, int maxDecimalLength, StringBuilder sb) {
        for(int i = binaryLength; i >= 0; i--)
            sb.append(String.format("%" + maxDecimalLength + "d", i));
    }

    private void appendSetBits(int binaryLength, int maxDecimalLength, StringBuilder sb) {
        for(int i = binaryLength; i >= 0; i--)
            sb.append(String.format("%" + maxDecimalLength + "s", isSet(i) ? 1 : 0));
    }

    private void expandBits(int b) {
        int index = b >>> 0b11;
        if(index >= bits.length)
            bits = Arrays.copyOf(bits, index + 1);
    }
}
