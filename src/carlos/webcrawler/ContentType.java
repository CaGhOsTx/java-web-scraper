package carlos.webcrawler;

import java.util.regex.Pattern;

public abstract class ContentType {
    final String NAME;
    private final Pattern PATTERN;

    public ContentType(String NAME) {
        this.NAME = NAME;
        this.PATTERN = Pattern.compile(pattern());
    }
    public abstract String pattern();
    public abstract String transform(String html);
    public abstract boolean onAddFilter(String s);
    public abstract int limit();

    public boolean isCollected(int current) {
        return current >= limit();
    }

    public Pattern getPATTERN() {
        return PATTERN;
    }

    public boolean is(String name) {
        return this.NAME.equals(name);
    }

    public String toString() {
        return "ContentType-" + NAME;
    }
}
