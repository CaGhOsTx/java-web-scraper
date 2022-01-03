package carlos.webcrawler;

import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.newSetFromMap;

public abstract class ContentType implements Serializable, ContentRequirements {
    @Serial
    private static final long serialVersionUID = -2044228837718462802L;
    final String NAME;
    private int collected = 0;
    private Set<String> data = newSetFromMap(new ConcurrentHashMap<>());
    private final Pattern PATTERN;
    
    public ContentType(String NAME) {
        this.NAME = NAME;
        PATTERN = Pattern.compile(pattern());
    }
    public abstract String pattern();

    public boolean reachedLimit(int limit) {
        return collected >= limit;
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

    synchronized int addData(Set<String> newData, int limit) {
        int previousSize = data.size();
        data.addAll(newData);
        if(newDataOverflowsLimit(limit))
            removeExcess(limit);
        int difference = data.size() - previousSize;
        collected += difference;
        return difference;
    }

    private boolean newDataOverflowsLimit(int limit) {
        return collected + data.size() > limit;
    }

    private void removeExcess(int limit) {
        data = data.stream().limit(limit - collected).collect(Collectors.toSet());
    }

    public synchronized int getTotal() {
        return collected;
    }

    public String toString() {
        return NAME;
    }
}
