package carlos.webcrawler;

import java.io.Serial;

public class LinkType extends ContentType{
    static final LinkType STANDARD = new LinkType();
    @Serial
    private static final long serialVersionUID = -5048241926547386740L;
    String regex;

    private LinkType () {
        super("link");
        this.regex = "(?<=href=\")https?://[A-Za-z0-9./:_()\\[\\]{}-]+?(?=\")";
    }

    LinkType (String regex) {
        super("link");
        this.regex = regex;
    }
    @Override
    public String pattern() {
        return regex;
    }

    @Override
    public String transform(String html) {
        return html;
    }

    @Override
    public boolean onAddFilter(String s) {
        return true;
    }
}
