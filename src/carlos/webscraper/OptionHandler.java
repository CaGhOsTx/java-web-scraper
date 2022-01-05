package carlos.webscraper;

import carlos.utilities.BitSet;

import java.io.*;
import java.util.List;

/**
 * A simple wrapper class which handles the options for {@link WebScraper} instances.
 * Uses a {@link BitSet} internally.
 * @see BitSet
 * @author Carlos Milkovic
 * @version 1.0
 */
@SuppressWarnings("unused")
final class OptionHandler implements Serializable {
    @Serial
    private static final long serialVersionUID = -7870065255227151797L;
    static final OptionHandler EMPTY = new OptionHandler();
    private final BitSet bits;

    private OptionHandler() {
        bits = BitSet.EMPTY;
    }

    /**
     * Creates an instance of {@link OptionHandler} and allocates the given {@link List} of options to the internal {@link BitSet}.
     * @param options {@link List} of {@link Option}s to be added.
     * @see Option
     * @see BitSet
     * @see OptionHandler
     */
    OptionHandler(List<Option> options) {
        bits = new BitSet(options.stream().map(Enum::ordinal).toList());
    }

    void removeOption(Option o) {
        bits.resetBit(o.ordinal());
    }

    void addOption(Option o) {
        bits.setBit(o.ordinal());
    }

    /**
     * Tests if given option is present in the internal {@link BitSet}.
     * @param o option to be tested.
     * @return true if option is present.
     * @see BitSet
     * @see OptionHandler
     */
    boolean isPresent(Option o) {
        return bits.isSet(o.ordinal());
    }
}
