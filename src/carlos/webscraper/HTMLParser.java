package carlos.webscraper;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Runtime.getRuntime;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.Collections.newSetFromMap;

/**
 * HTML parsing class for use with {@link WebScraper}.
 * Custom implementations allow for niche data to be gathered.
 * @author Carlos Milkovic
 * @version a0.9
 * @see WebScraper
 */
public abstract class HTMLParser implements Parsable {

    static final List<HTMLParser> customParserPool = new ArrayList<>();
    static final int CACHE_LIMIT = 1_000_000;
    @Serial
    private static final long serialVersionUID = -2044228837718462802L;

    protected final String NAME;
    protected int collected = 0;
    protected Set<String> cache = newSetFromMap(new ConcurrentHashMap<>());
    protected boolean shouldSave;

    protected final Pattern PATTERN;

    /**
     * Saves the content stored in the cache.
     * Reason for the method being static is to stop multiple threads
     * writing the same data when multiple {@link WebScraper}s are collectively using this {@link HTMLParser}.
     * @param parser usually this parser.
     * @throws IOException if the data cannot be saved for any reason.
     * @see WebScraper
     */
    static synchronized void saveContent(HTMLParser parser) throws IOException {
        if(!parser.cache.isEmpty()) {
            try (var w = newBufferedWriter(parser.pathToContent(), parser.openOption())) {
                parser.removeDuplicates();
                for (var token : parser.cache) {
                    w.write(token);
                    w.newLine();
                }
            }
        }
    }

    private synchronized void removeDuplicates() {
        IntStream.iterate(0, i -> i < collected - cacheSize(), i -> i + CACHE_LIMIT).parallel()
                .forEach(this::removeDuplicateElements);
    }

    private synchronized void removeDuplicateElements(int i) {
        try {
            Files.lines(pathToContent()).skip(i).limit(CACHE_LIMIT).parallel()
                    .filter(cache::contains).forEach(cache::remove);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link HTMLParser} with this name. <br/>
     * Adds this implementation to the static field {@link HTMLParser#customParserPool}
     * which is used by {@link ScraperService} internally.
     * @param name name of the {@link HTMLParser}.
     * @throws NullPointerException if name is null
     */
    public HTMLParser(String name) {
        Objects.requireNonNull(this.NAME = name);
        PATTERN = Pattern.compile(pattern());
        customParserPool.add(this);
    }

    public abstract String pattern();

    public String toString() {
        return getClass().getName() + NAME;
    }

    /**
     * Returns the {@link Path} to which this {@link HTMLParser} data has been saved.
     * @return {@link Path} to file.
     */
    public Path pathToContent() {
        return Paths.get(this.getClass().getSimpleName() + "&" + NAME + ".txt");
    }

    /**
     * Tests if the current limit has been reached. collected keeps track of saved and cached data.
     * @param limit limit provided from the builder.
     * @return true if limit has been reached.
     */
    final boolean reachedLimit(int limit) {
        return collected >= limit;
    }

    /**
     * Returns {@link Pattern} associated with the pattern provided in {@link HTMLParser#pattern()}
     * @return {@link Pattern}
     * @see Pattern
     */
    final Pattern getPATTERN() {
        return PATTERN;
    }

    final void setShouldSave(boolean shouldSave) {
        this.shouldSave = shouldSave;
    }

    /**
     *
     * @return cached data size.
     */
    final synchronized int cacheSize() {
        return cache.size();
    }

    /**
     * Adds data provided to the parsers internal cache.
     * @param parsedElements elements to be added.
     * @param limit {@link ContentHandler#DATA_LIMIT}
     * @return the change in size of the cache. (used to monitor {@link WebScraper} individual contributions.
     * @see WebScraper
     */
    synchronized int addData(Set<String> parsedElements, int limit) {
        int previousSize = cache.size();
        cache.addAll(parsedElements);
        if(newDataOverflowsLimit(limit)) trimToSize(limit);
        int difference = cache.size() - previousSize;
        collected += difference;
        if(lessThan500MBRam() || cacheOverflowing()) flush();
        return difference;
    }

    /**
     * saves collected data from the cache if {@link HTMLParser#shouldSave} is true, and then clears the cache.
     */
    final synchronized void flush() {
        if(shouldSave) {
            try {
                saveContent(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cache.clear();
    }

    /**
     * Adds parsed elements from the given raw HTML {@link String} to this {@link HTMLParser}'s {@link HTMLParser#cache}.
     * @param html raw html source {@link String}.
     * @param limit {@link ContentHandler#DATA_LIMIT}
     * @return change in size of the {@link HTMLParser#cache}.
     */
    final synchronized int addContentFrom(String html, int limit) {
        if(!reachedLimit(limit))
            return addData(getContent(html), limit);
        return 0;
    }

    /**
     *
     * @return the total amount of parsed elements collected.
     */
    final synchronized int getTotal() {
        return collected;
    }

    /**
     * tests if the current cache size is greater than the {@link HTMLParser#CACHE_LIMIT}
     * @return true if it is overflowing.
     */
    final protected synchronized boolean cacheOverflowing() {
        return cacheSize() >= CACHE_LIMIT;
    }

    /**
     * Tests if already collected data and cache overflow the  {@link ContentHandler#DATA_LIMIT} set during the build.
     * @param limit {@link ContentHandler#DATA_LIMIT}.
     * @return true if overflowing.
     * @see ContentHandler
     */
    final protected synchronized boolean newDataOverflowsLimit(int limit) {
        return collected + cache.size() > limit;
    }

    /**
     * Removes excess elements from the cache to fit the {@link ContentHandler#DATA_LIMIT}.
     * @param limit {@link ContentHandler#DATA_LIMIT}.
     * @see ContentHandler
     */
    final protected void trimToSize(int limit) {
        cache = cache.stream().limit(limit - collected).collect(Collectors.toSet());
    }

    /**
     * Sets the File opening option, in order not to overwrite the file on re-entering.
     * @return {@link StandardOpenOption#APPEND} if file exists, {@link StandardOpenOption#CREATE} if it doesn't.
     */
    final protected StandardOpenOption openOption() {
        if (exists(pathToContent()))
            return StandardOpenOption.APPEND;
        return StandardOpenOption.CREATE;
    }

    /**
     * Retrieves Parsed elements from the given raw HTML {@link String}
     * @param html raw HTML {@link String}
     * @return a {@link Set} of parsed elements.
     */
    final protected Set<String> getContent(String html) {
        var matcher = getPATTERN().matcher(transform(html));
        Set<String> content = new HashSet<>();
        while(matcher.find()) {
            var group = matcher.group();
            if(onAddFilter(group))
                content.add(group);
        }
        return content;
    }

    private boolean lessThan500MBRam() {
        return getRuntime().freeMemory() < 100_000_000;
    }
}
