package carlos.webscraper;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.Collections.newSetFromMap;

public abstract class HTMLParser implements Recognizable {
    @Serial
    private static final long serialVersionUID = -2044228837718462802L;
    private static final Runtime runtime = Runtime.getRuntime();
    final String NAME;
    protected int collected = 0;
    static final int CACHE_LIMIT = 10_000_000;
    protected Set<String> data = newSetFromMap(new ConcurrentHashMap<>());
    private final Pattern PATTERN;
    boolean saving;

    public HTMLParser(String NAME) {
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

    public Path pathToContent() {
        return Paths.get(this.getClass().getSimpleName() + "&" + NAME + ".txt");
    }

    synchronized void saveContent() throws IOException {
        if(!data.isEmpty()) {
            try (var w = newBufferedWriter(pathToContent(), openOption())) {
                for (var token : data) {
                    w.write(token);
                    w.newLine();
                }
            }
        }
    }

    void setSaving(boolean saving) {
        this.saving = saving;
    }

    synchronized int cache() {
        return data.size();
    }

    synchronized int addData(Set<String> newData, int limit) {
        int previousSize = data.size();
        data.addAll(newData);
        if(newDataOverflowsLimit(limit)) trimToSize(limit);
        int difference = data.size() - previousSize;
        collected += difference;
        if(saving && lessThan500MBRam() || cacheOverflowing()) resetCache();
        return difference;
    }

    private boolean lessThan500MBRam() {
        return runtime.freeMemory() < 500_000_000;
    }

    synchronized void resetCache() {
        try {
            saveContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        data.clear();
    }

    synchronized boolean cacheOverflowing() {
        return cache() >= CACHE_LIMIT;
    }

    protected boolean newDataOverflowsLimit(int limit) {
        return collected + data.size() > limit;
    }

    protected void trimToSize(int limit) {
        data = data.stream().limit(limit - collected).collect(Collectors.toSet());
    }

    public synchronized int getTotal() {
        return collected;
    }

    StandardOpenOption openOption() {
        if (exists(pathToContent()))
            return StandardOpenOption.APPEND;
        return StandardOpenOption.CREATE;
    }

    synchronized int addContentFrom(String html, int limit) {
        if(!reachedLimit(limit))
            return addData(getContent(html), limit);
        return 0;
    }

    Set<String> getContent(String html) {
        var matcher = getPATTERN().matcher(transform(html));
        Set<String> content = new HashSet<>();
        while(matcher.find()) {
            var group = matcher.group();
            if(onAddFilter(group))
                content.add(group);
        }
        return content;
    }

    public String toString() {
        return NAME;
    }
}
