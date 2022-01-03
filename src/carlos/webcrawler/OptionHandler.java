package carlos.webcrawler;

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

    OptionHandler(List<Options> options) {
        bits = new BitSet(options.stream().map(o -> o.position).toList());
    }

    void addOption(Options o) {
        bits.setBit(o.position);
    }

    boolean isTrue(Options o) {
        return bits.isSet(o.position);
    }
}
