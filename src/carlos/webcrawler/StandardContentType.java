package carlos.webcrawler;

import java.io.Serial;

public enum StandardContentType {
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

    final ContentType ct;

    StandardContentType(ContentType ct) {
        this.ct = ct;
    }

    public ContentType get() {
        return ct;
    }
}
