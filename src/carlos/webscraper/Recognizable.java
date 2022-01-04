package carlos.webscraper;

import java.io.Serializable;

interface Recognizable extends Serializable {

    String pattern();

    default String transform(String html) {
        return html;
    }

    default boolean onAddFilter(String s) {
        return true;
    }
}
