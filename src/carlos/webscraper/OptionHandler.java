package carlos.webscraper;

import carlos.utilities.BitSet;

import java.io.*;
import java.util.List;

final class OptionHandler implements Serializable {
    @Serial
    private static final long serialVersionUID = -7870065255227151797L;
    static final OptionHandler EMPTY = new OptionHandler();
    private final BitSet bits;

    private OptionHandler() {
        bits = BitSet.EMPTY;
    }

    OptionHandler(List<Option> options) {
        bits = new BitSet(options.stream().map(Enum::ordinal).toList());
    }

    void addOption(Option o) {
        bits.setBit(o.ordinal());
    }

    boolean isTrue(Option o) {
        return bits.isSet(o.ordinal());
    }
}
