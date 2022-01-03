package carlos.webcrawler;

import java.io.Serial;

public enum StandardContentType {
    LINK(new ContentType("link") {
        @Serial
        private static final long serialVersionUID = -6727177813399172363L;

        @Override
        public String pattern() {
            return "(?<=href=\")https?://[A-Za-z0-9./:_()\\[\\]{}-]+?(?=\")";
        }

    }),
    TEXT(new ContentType("text") {

        @Serial
        private static final long serialVersionUID = -3131684393596396308L;

        String clearTags(String html) {
            return html.replaceAll("<.*?>", "");
        }

        @Override
        public String pattern() {
            return "[A-Z0-9][A-Za-z0-9,. -]*?[!.?](?!\\w)";
        }

        @Override
        public String transform(String html) {
            return clearTags(html);
        }

        @Override
        public boolean onAddFilter(String s) {
            var tmp = s.split("\\s+");
            return tmp.length >= 3 && tmp.length <= 10;
        }
    });

    private final ContentType type;

    StandardContentType(ContentType type) {
        this.type = type;
    }

    public ContentType get() {
        return type;
    }
}
