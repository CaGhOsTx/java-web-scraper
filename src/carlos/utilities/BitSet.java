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
 */
@SuppressWarnings("unused")
public final class BitSet implements Serializable, Iterable<Boolean> {
    public static final BitSet EMPTY = new BitSet(0);
    @Serial
    private static final long serialVersionUID = 9174287491114344015L;
    private byte[] bits;

    /**
     * Creates an empty <code>BitSet</code>.
     * @see BitSet
     */
    public BitSet() {
        bits = new byte[1];
    }

    /**
     * Only required for the static <code>EMPTY BitSet</code>. <br/>
     * API manages its size automatically, so it is not required to pre-set the size.
     * @param size initial size.
     */
    private BitSet(int size) {
        bits = new byte[(size >>> 3) + 1];
    }

    /**
     * Creates a <code>BitSet</code> and pre-sets the bits defined in the list
     * @param bits bits to be set.
     * @throws NullPointerException if specified <code>List</code> is null.
     * @see BitSet
     * @see List
     */
    public BitSet(List<Integer> bits) {
        Objects.requireNonNull(bits);
        this.bits = new byte[(max(bits) >>> 3) + 1];
        setBits(bits);
    }

    /**
     * Copy constructor for this class.
     * @param copy <code>BitSet</code> to copy.
     * @throws NullPointerException if specified <code>BitSet</code> is null.
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
        return (bits.length << 3) + 1;
    }

    /**
     * Inverts this <code>BitSet</code>.<br/>
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
     * Inverts this <code>BitSet</code>.<br/>
     * Eg. 101 -> 010
     * @return new inverted <code>BitSet</code>. <b>IMMUTABLE OPERATION</b>
     * @see BitSet
     */
    public BitSet not() {
        return new BitSet(this).invert();
    }

    /**
     * ANDs this <code>BitSet</code> with the specified <code>BitSet</code>. <br/>
     * Eg. 101 & 110 -> 100
     * @param other <code>BitSet</code> to be ANDed with.
     * @return new resultant <code>BitSet</code>. <b>IMMUTABLE OPERATION</b>
     * @throws NullPointerException if specified <code>BitSet</code> is null.
     * @see BitSet
     */
    public BitSet and(BitSet other) {
        Objects.requireNonNull(other);
        var result = new BitSet(Math.min(bits.length, other.bits.length));
        for(int i = 0; i < bits.length && i < other.bits.length; i++)
            result.bits[i] = (byte) (bits[i] & other.bits[i]);
        return result;
    }

    /**
     * ORs this <code>BitSet</code> with the specified <code>BitSet</code>. <br/>
     * Eg. 101 & 110 -> 111
     * @param other <code>BitSet</code> to be ORed with.
     * @return new resultant <code>BitSet</code>. <b>IMMUTABLE OPERATION</b>
     * @throws NullPointerException if specified <code>BitSet</code> is null.
     * @see BitSet
     */
    public BitSet or(BitSet other) {
        Objects.requireNonNull(other);
        var result = new BitSet(Math.max(bits.length, other.bits.length));
        for(int i = 0; i < bits.length && i < other.bits.length; i++)
            result.bits[i] = (byte) (bits[i] | other.bits[i]);
        return result;
    }

    /**
     * XORs this <code>BitSet</code> with the specified <code>BitSet</code>. <br/>
     * Eg. 101 & 110 -> 011
     * @param other <code>BitSet</code> to be ANDed with.
     * @return new resultant <code>BitSet</code>. <b>IMMUTABLE OPERATION</b>
     * @throws NullPointerException if specified <code>BitSet</code> is null.
     * @see BitSet
     */
    public BitSet xor(BitSet other) {
        Objects.requireNonNull(other);
        var result = new BitSet(Math.max(bits.length, other.bits.length));
        for(int i = 0; i < bits.length && i < other.bits.length; i++)
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
        int index = getIndex(b);
        bits[index] &= ~((byte) 1 << (b % 8));
        shrinkIfPossible();
    }

    /**
     * Sets the specified bit to 1.
     * @param b bit to be set.
     * @throws IndexOutOfBoundsException if the bit is negative.
     * @see BitSet
     */
    public void setBit(int b) {
        bits[getIndex(b)] |= ((byte) 1 << (b & 0b111));
    }

    /**
     * Toggles specified bit between 1 and 0.
     * @param b bit to be toggled.
     * @throws IndexOutOfBoundsException if the bit is negative.
     * @see BitSet
     */
    public void toggleBit(int b) {
        bits[getIndex(b)] ^= ((byte) 1 << (b & 7));
    }

    /**
     * Sets all bits in the list to 1.
     * @param bits list of bits to be set.
     * @throws NullPointerException if the specified <code>List</code> is null.
     * @see BitSet
     * @see List
     */
    public void setBits(List<Integer> bits) {
        Objects.requireNonNull(bits);
        for (var b : bits) setBit(b);
    }

    /**
     * Tests whether the specified bit is on or off.
     * @param bit bit to be tested.
     * @return true if 1, false if 0
     * @throws IndexOutOfBoundsException if the bit is negative.
     * @see BitSet
     */
    public boolean isSet(int bit) {
        int index = getIndex(bit);
        if(isOutOfBounds(index) && isOutOfBounds(bit)) return false;
        return (bits[index] & ((byte) 0b1 << (bit & 0b111))) != 0;
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
     * @param o <code>BitSet</code> to be compared with.
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

    private boolean isOutOfBounds(int index) {
        if(index < 0) throw new IndexOutOfBoundsException("Bit positions are always positive! -> " + index);
        return index >= bits.length;
    }

    private void appendIndexes(int binaryLength, int maxDecimalLength, StringBuilder sb) {
        for(int i = binaryLength; i >= 0; i--)
            sb.append(String.format("%" + maxDecimalLength + "d", i));
    }

    private void appendSetBits(int binaryLength, int maxDecimalLength, StringBuilder sb) {
        for(int i = binaryLength; i >= 0; i--)
            sb.append(String.format("%" + maxDecimalLength + "s", isSet(i) ? 1 : 0));
    }

    private int getIndex(int bit) {
        int index = 0;
        for(int i = bit; i > 7; i -= 8) index++;
        if(isOutOfBounds(index)) expandBits(index);
        return index;
    }

    private void expandBits(int index) {
        bits = Arrays.copyOf(bits, index);
    }
}