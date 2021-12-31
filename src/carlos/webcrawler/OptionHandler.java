package carlos.webcrawler;

import java.util.List;

public class OptionHandler<T extends Options> {
    static OptionHandler<?> EMPTY = new OptionHandler<>();
    int[] res;

    private OptionHandler() {
        res = new int[1];
    }

    public OptionHandler(List<T> options) {
        res = getOptionsAsInt(options);
    }


    int[] getOptionsAsInt(List<T> setOptions) {
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
        var a = Integer.toString((res[getIndex(o.position)]), 2);
        var b = Integer.toString(1 << o.position - 1, 2);
        return (res[getIndex(o.position)] & (1 << o.position)) != 0;
    }

}
