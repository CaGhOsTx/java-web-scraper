package carlos.webscraper.parser;
import carlos.webscraper.WebScraper;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Thread.currentThread;
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
public abstract class HTMLParser implements LimitedParser {

    @Serial
    private static final long serialVersionUID = -2044228837718462802L;
    public static final int CACHE_LIMIT = 1_000_000;

    public final LocalDateTime timeOfCreation = LocalDateTime.now();
    public final String NAME;
    public final Pattern PATTERN;

    protected AtomicInteger collected = new AtomicInteger(0);
    protected final Set<String> cache = newSetFromMap(new ConcurrentHashMap<>(CACHE_LIMIT));
    protected boolean shouldSave;

    /**
     * Creates a new {@link HTMLParser} with this name.
     * @param name name of the {@link HTMLParser}.
     * @throws NullPointerException if name is null
     */
    public HTMLParser(String name) {
        Objects.requireNonNull(this.NAME = name);
        PATTERN = Pattern.compile(pattern());
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
        return Paths.get(getDateTime() + "@" + NAME + ".txt");
    }

    /**
     * Saves the content stored in the cache.
     * Reason for the method being static is to stop multiple threads
     * writing the same data when multiple {@link WebScraper}s are collectively using this {@link HTMLParser}.<br/>
     * The path to saved content is {@link HTMLParser#pathToContent()}
     * @see WebScraper
     */
    public final synchronized void flush(Path p) {
        if(shouldSave && !cache.isEmpty()) {
            try (var w = newBufferedWriter(pathToContent(), openOption(p))) {
                for (var token : cache) {
                    w.write(token);
                    w.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cache.clear();
    }

    /**
     * Tests if the current limit has been reached. collected keeps track of saved and cached data.
     * @return true if limit has been reached.
     */
    public final boolean reachedLimit() {
        return collected.get() >= limit();
    }

    /**
     * Adds parsed elements from the given raw HTML {@link String} to this {@link HTMLParser}'s {@link HTMLParser#cache}.
     * @param html raw html source {@link String}.
     * @return change in size of the {@link HTMLParser#cache}.
     */
    public final synchronized int addContentFrom(String html) {
        if(!reachedLimit())
            return addData(getContent(html));
        return 0;
    }

    /**
     * Adds data provided to the parsers internal cache.
     * @param parsedElements elements to be added.
     * @return the change in size of the cache used to monitor {@link WebScraper} individual contributions.
     * @see WebScraper
     */
    private synchronized int addData(Set<String> parsedElements) {
        int previousSize = cache.size();
        addNewData(parsedElements);
        int difference = cache.size() - previousSize;
        collected.addAndGet(difference);
        if(cacheOverflowing()) flush(pathToContent());
        return difference;
    }

    /**
     * Retrieves the date and time in the following format:
     * "d-MMM-uuu&HH-mm-ss".
     * @return date and time.
     * @see DateTimeFormatter#ofPattern(String)
     */
    protected final String getDateTime() {
        return timeOfCreation.format(DateTimeFormatter.ofPattern("d-MMM-uuu&HH-mm-ss"));
    }

    /**
     * Returns {@link Pattern} associated with the pattern provided in {@link HTMLParser#pattern()}
     * @return {@link Pattern}
     * @see Pattern
     */
    private Pattern getPATTERN() {
        return PATTERN;
    }

    /**
     *
     * @return cached data size.
     */
    private int cacheSize() {
        return cache.size();
    }

    private void addNewData(Set<String> parsedElements) {
        for(var e : parsedElements){
            if(!reachedLimit())
                cache.add(e);
            else break;
        }
    }

    /**
     *
     * @return the total amount of parsed elements collected.
     */
    public final int getTotal() {
        return collected.get();
    }

    /**
     * tests if the current cache size is greater than the {@link HTMLParser#CACHE_LIMIT}
     * @return true if it is overflowing.
     */
    final protected  boolean cacheOverflowing() {
        return cacheSize() >= CACHE_LIMIT;
    }

    /**
     * Sets the File opening option, in order not to overwrite the file on re-entering.
     * @return {@link StandardOpenOption#APPEND} if file exists, {@link StandardOpenOption#CREATE} if it doesn't.
     */
    protected final StandardOpenOption openOption(Path p) {
        if (exists(p))
            return StandardOpenOption.APPEND;
        return StandardOpenOption.CREATE;
    }

    /**
     * Retrieves Parsed elements from the given raw HTML {@link String}
     * @param html raw HTML {@link String}
     * @return a {@link Set} of parsed elements.
     */
    public final Set<String> getContent(String html) {
        return getPATTERN().matcher(transform(html)).results()
                .map(MatchResult::group)
                .filter(this::onAddFilter)
                .collect(Collectors.toSet());
    }

    public void enableSaving() {
        shouldSave = true;
    }
}
