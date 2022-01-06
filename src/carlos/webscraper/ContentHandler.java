package carlos.webscraper;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

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
    private int DATA_LIMIT = 1000_000;
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

    private BiFunction<HTMLParser, Integer, Integer> updateContributions(String html) {
        return (parser, contribution) -> contributionsToParser.get(parser) + parser.addContentFrom(html, DATA_LIMIT);
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
     * @throws IOException if the data could not be saved for any reason.
     */
    void saveAllContent() throws IOException {
        for(var parser : contributionsToParser.keySet())
            parser.saveContent();
    }

    /**
     * Tests whether the given link has already been visited i.e.
     * is present in the {@link HTMLParser#cache} which holds visited links.
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
     * @return true if not all parsers reached the {@link ContentHandler#DATA_LIMIT}.
     */
    boolean notAllAreCollected() {
        return !contributionsToParser.keySet().stream().allMatch(content -> content.reachedLimit(DATA_LIMIT));
    }

    void addLink(String link) {
        linkParser.addVisitedLink(link, DATA_LIMIT);
    }

    /**
     * Sets this {@link ContentHandler#DATA_LIMIT}.
     * @param limit limit to be set.
     * @throws IllegalArgumentException if limit is not positive.
     */
    void setLimit(int limit) throws IllegalArgumentException {
        if(limit <= 0) throw new IllegalArgumentException("limit must be positive!");
        DATA_LIMIT = limit;
    }

}
