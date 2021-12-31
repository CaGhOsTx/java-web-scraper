package carlos.webcrawler;

import java.io.*;
import java.util.regex.Pattern;

public abstract class ContentType implements Serializable {
    @Serial
    private static final long serialVersionUID = -2044228837718462802L;
    final String NAME;
    private final Pattern PATTERN;

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
    
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
