package carlos.webcrawler;

import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ContentType implements Serializable {
    @Serial
    private static final long serialVersionUID = -2044228837718462802L;
    final String NAME;
    private final Pattern PATTERN;
    int collected = 0;
    private Set<String> data = new HashSet<>();
    
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

    public Path getPath() {
        return Paths.get(this.getClass().getName() + hashCode() + "&" + NAME + ".txt");
    }

    public synchronized Set<String> getData() {
        return data;
    }

    public synchronized void clearData() {
        data.clear();
    }

    public synchronized void addData(Set<String> newData) {
        int previousSize = data.size();
        data.addAll(newData);
        if(newDataOverflowsLimit())
            removeExcess();
        collected += data.size() - previousSize;
    }

    private boolean newDataOverflowsLimit() {
        return collected + data.size() > limit();
    }

    private void removeExcess() {
        data = data.stream().limit(limit() - collected).collect(Collectors.toSet());
    }

    public synchronized int getCollected() {
        return collected;
    }

    public String toString() {
        return "ContentType-" + NAME;
    }
}
