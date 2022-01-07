package carlos.webscraper;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    protected volatile int collected = 0;
    protected final Set<String> cache = newSetFromMap(new ConcurrentHashMap<>(CACHE_LIMIT));
    protected boolean shouldSave;

    protected final Pattern PATTERN;

    /**
     * Saves the content stored in the cache.
     * Reason for the method being static is to stop multiple threads
     * writing the same data when multiple {@link WebScraper}s are collectively using this {@link HTMLParser}.<br/>
     * The path to saved content is {@link HTMLParser#pathToContent()}
     * @throws IOException if the data cannot be saved for any reason.
     * @see WebScraper
     */
     final synchronized void flush() throws IOException {
        if(shouldSave && !cache.isEmpty()) {
            System.out.println("saving " + cacheSize() + " elements...");
            try (var w = newBufferedWriter(pathToContent(), openOption())) {
                for (var token : cache) {
                    w.write(token);
                    w.newLine();
                }
            }
        }
         cache.clear();
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
     *
     * @return {@link Path} to file.
     */
    public Path pathToContent() {
        return Paths.get(this.getClass().getName() + "&" + NAME + ".txt");
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

    /**
     *
     * @return cached data size.
     */
    final int cacheSize() {
        return cache.size();
    }

    /**
     * Adds data provided to the parsers internal cache.
     * @param parsedElements elements to be added.
     * @param limit {@link ContentHandler#DATA_LIMIT}
     * @return the change in size of the cache used to monitor {@link WebScraper} individual contributions.
     * @see WebScraper
     */
    synchronized int addData(Set<String> parsedElements, int limit) {
        int previousSize = cache.size();
        addNewData(parsedElements, limit);
        int difference = cache.size() - previousSize;
        collected += difference;
        if(cacheOverflowing()) flushCache();
        return difference;
    }

    protected void flushCache() {
        try {
            flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addNewData(Set<String> parsedElements, int limit) {
        for(var e : parsedElements){
            if(dataWithinLimit(limit))
                cache.add(e);
            else break;
        }
    }

    /**
     * Adds parsed elements from the given raw HTML {@link String} to this {@link HTMLParser}'s {@link HTMLParser#cache}.
     * @param html raw html source {@link String}.
     * @param limit {@link ContentHandler#DATA_LIMIT}
     * @return change in size of the {@link HTMLParser#cache}.
     */
    final int addContentFrom(String html, int limit) {
        if(!reachedLimit(limit))
            return addData(getContent(html), limit);
        return 0;
    }

    /**
     *
     * @return the total amount of parsed elements collected.
     */
    final int getTotal() {
        return collected;
    }

    /**
     * tests if the current cache size is greater than the {@link HTMLParser#CACHE_LIMIT}
     * @return true if it is overflowing.
     */
    final protected  boolean cacheOverflowing() {
        return cacheSize() >= CACHE_LIMIT;
    }

    /**
     * Tests if already collected data and cache overflow the  {@link ContentHandler#DATA_LIMIT} set during the build.
     * @param limit {@link ContentHandler#DATA_LIMIT}.
     * @return true if overflowing.
     * @see ContentHandler
     */
    final protected  boolean dataWithinLimit(int limit) {
        return collected + cache.size() <= limit;
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
        return getPATTERN().matcher(transform(html)).results()
                .map(MatchResult::group)
                .filter(this::onAddFilter)
                .collect(Collectors.toSet());
    }

    void enableSaving() {
        shouldSave = true;
    }
}
