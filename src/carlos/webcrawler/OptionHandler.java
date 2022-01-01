package carlos.webcrawler;

import java.io.*;
import java.util.List;

public final class OptionHandler implements Serializable {
    @Serial
    private static final long serialVersionUID = -7870065255227151797L;
    static OptionHandler EMPTY = new OptionHandler();
    int[] res;

    private OptionHandler() {
        res = new int[1];
    }

    public OptionHandler(List<Options> options) {
        res = getOptionsAsInt(options);
    }

    int[] getOptionsAsInt(List<Options> setOptions) {
        int[] options = new int[Options.values().length / 32 + 1];
        for (var o : setOptions) {
            options[getIndex(o.position)] |= (1 << o.position);
        }
        return options;
    }

    private int getIndex(int position) {
        int index = 0;
        for(int i = 1; i <= position; i++) {
            if(i % 32 == 0) index++;
        }
        return index;
    }

    public boolean isTrue(Options o) {
        return (res[getIndex(o.position)] & (1 << o.position)) != 0;
    }

}
