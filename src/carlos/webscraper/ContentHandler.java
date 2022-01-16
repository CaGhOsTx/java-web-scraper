package carlos.webscraper;

import carlos.webscraper.exceptions.ReachedEndException;
import carlos.webscraper.parser.HTMLParser;
import carlos.webscraper.parser.link.LanguagePattern;
import carlos.webscraper.parser.link.LinkParser;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toConcurrentMap;

/**
 * Wrapper class for handling {@link HTMLParser} instances linked to
 * the {@link WebScraper} containing this {@link ContentHandler}.
 * @author Carlos Milkovic
 * @version a0.9
 */
final class ContentHandler implements Serializable {
    @Serial
    private static final long serialVersionUID = 395515185246116492L;
    private LinkParser linkParser;
    private final Map<HTMLParser, Integer> contributionsToParser;

    /**
     * Creates a new {@link ContentHandler} instance and links provided {@link HTMLParser}s to it.
     * Sets the default {@link LinkParser} to {@link LinkParser#newStandardLinkParser()}.
     * @param parsers {@link HTMLParser}s to be linked.
     * @see LinkParser
     * @see ContentHandler
     */
    ContentHandler(HTMLParser... parsers) {
        contributionsToParser = Arrays.stream(parsers).collect(toConcurrentMap(collectable -> collectable, i -> 0));
        linkParser = LinkParser.newStandardLinkParser();
    }

    Queue<String> loadUnvisitedLinks(WebScraper scraper) throws ReachedEndException {
        Queue<String> q = null;
        try {
            q = Files.lines(getLinkParser().pathToUnvisited(scraper))
                    .limit(1_000_000)
                    .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
            if(q.isEmpty()) throw new ReachedEndException(scraper);
            overwriteUnvisitedFile(scraper, q);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return q != null ? q : new ConcurrentLinkedQueue<>();
    }

    private void overwriteUnvisitedFile(WebScraper scraper, Queue<String> q) throws IOException {
        getLinkParser().flushUnvisited(loadUntouchedLinks(scraper, q), scraper, StandardOpenOption.WRITE);
    }

    private List<String> loadUntouchedLinks(WebScraper scraper, Queue<String> q) throws IOException {
        return Files.lines(getLinkParser().pathToUnvisited(scraper)).skip(q.size()).toList();
    }

    /**
     * Retrieves the number of tokens contributed by the parent {@link WebScraper}.
     * @param parser parser for which the contributions are to be retrieved.
     * @return number of elements contributed.
     * @see WebScraper
     * @see ContentHandler
     * @see HTMLParser
     */
    int getContributed(HTMLParser parser) {
        return contributionsToParser.get(parser);
    }

    /**
     * Enables url filtering by language for the contained {@link LinkParser}
     * @param languagePattern pattern constant which the contained {@link LinkParser} will use.
     * @throws NullPointerException if provided {@link LanguagePattern} is null.
     * @see LinkParser
     * @see LanguagePattern
     * @see ContentHandler
     */
    void restrictLanguage(LanguagePattern languagePattern) throws NullPointerException {
        Objects.requireNonNull(languagePattern);
        linkParser.addLanguageFilter(languagePattern);
    }

    void setCustomLinkParser(LinkParser linkParser) {
        this.linkParser = linkParser;
    }

    /**
     * Parses and stores relative parsed data for each {@link HTMLParser} controlled by this class,
     * from the given raw HTML.
     * Updates the {@link ContentHandler#contributionsToParser} with the new data added.
     * @param html HTML to be parsed.
     * @see HTMLParser
     * @see ContentHandler
     */
    void addAllNewContent(String html) {
        contributionsToParser.replaceAll(updateContributions(html));
    }

    private synchronized BiFunction<HTMLParser, Integer, Integer> updateContributions(String html) {
        return (parser, contribution) -> contributionsToParser.get(parser) + parser.addContentFrom(html);
    }

    void enableSavingForAllParsers() {
        for(var parser : contributionsToParser.keySet())
            parser.enableSaving();
    }

    Set<String> getLinks(String html) {
        return linkParser.getContent(html);
    }

    /**
     * Saves data stored in each {@link HTMLParser} cache controlled by this {@link ContentHandler} instance.
     */
    synchronized void saveAllContent() {
        for(var parser : contributionsToParser.keySet())
            parser.flush(parser.pathToContent());
    }

    void saveUnvisitedLinks(WebScraper scraper) {
        getLinkParser().flush(getLinkParser().pathToUnvisited(scraper));
    }

    /**
     * Tests whether the given link has already been visited i.e.
     * @param link link to be tested
     * @return true if the link is not present in the set.
     */
    boolean linkNotVisited(String link) {
        return !this.linkParser.alreadyVisited(link);
    }

    LinkParser getLinkParser() {
        return linkParser;
    }

    /**
     * @return Returns the set of all {@link HTMLParser} instances linked to this {@link ContentHandler} instance.
     */
    Set<HTMLParser> getParsers() {
        return contributionsToParser.keySet();
    }

    /**
     * @return true if not all parsers reached their limit.
     */
    boolean allAreCollected() {
        return contributionsToParser.keySet().stream().allMatch(HTMLParser::reachedLimit);
    }

    void addLink(String link) {
        linkParser.addVisitedLink(link);
    }


    boolean hasParser(HTMLParser parser) {
        return contributionsToParser.containsKey(parser);
    }
}
