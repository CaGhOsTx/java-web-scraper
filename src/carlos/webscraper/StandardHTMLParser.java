package carlos.webscraper;

import java.io.Serial;

public enum StandardHTMLParser {
    TEXT_PARSER(new HTMLParser("text") {

        @Serial
        private static final long serialVersionUID = -3131684393596396308L;

        @Override
        public String pattern() {
            return "[A-Z0-9][A-Za-z0-9,. -]*?[!.?](?!\\w)";
        }

        @Override
        public String transform(String html) {
            return HTMLHelper.clearTags(html);
        }

        @Override
        public boolean onAddFilter(String s) {
            var tmp = s.split("\\s+");
            return tmp.length >= 3 && tmp.length <= 10;
        }
    });

    private final HTMLParser data;

    StandardHTMLParser(HTMLParser data) {
        this.data = data;
    }

    public HTMLParser get() {
        return data;
    }
}
