package carlos.webcrawler;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class ContentType implements Serializable {
    @Serial
    private static final long serialVersionUID = -2044228837718462802L;
    final String NAME;
    private final Pattern PATTERN;
    int collected = 0;
    private final Set<String> data = new HashSet<>();

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

    public boolean reachedLimit() {
        return collected >= limit();
    }

    public Pattern getPATTERN() {
        return PATTERN;
    }

    public synchronized Set<String> getData() {
        return data;
    }

    public synchronized void clearData() {
        data.clear();
    }

    public synchronized void addData(Set<String> data) {
        int tmp = collected;
        this.data.addAll(data);
        collected += data.size() - tmp;
    }

    public synchronized int getCollected() {
        return collected;
    }

    public String toString() {
        return "ContentType-" + NAME;
    }
}
