package carlos.webscraper;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import static java.nio.file.Files.newBufferedWriter;

/**
 * Extension of the {@link HTMLParser} abstract class.
 * Used for parsing links and has an option to parse links
 * that are from the same site only or ones which stick to
 * a specific language.<br/>
 * Implement if you want a different {@link Parsable#pattern()} for link parsing,
 *  otherwise use {@link LinkParser#newStandardLinkParser()} method.
 *  <br/>
 * <b> In situations when multiple {@link WebScraper}s are being used simultaneously,
 * and use the same {@link LinkParser} implementation,
 * give each {@link WebScraperBuilder} a clone of your implementation
 * (make a method similar to</b> {@link LinkParser#newStandardLinkParser()}
 * @see Parsable
 * @author Carlos Milkovic
 * @version a0.9
 */
public abstract class LinkParser extends HTMLParser {
    /**
     * Standard implementation of the {@link LinkParser}.<br/>
     * Write a custom implementation if you are dissatisfied with the {@link LinkParser#pattern()}. <br/>
     * REGEX: <code>(?<=href=")https?://[A-Za-z0-9./:_()\[\]{}-]+?(?=")</code>
     * @return a new instance of the standard implementation of {@link LinkParser}
     * @see LinkParser
     */
    static LinkParser newStandardLinkParser() {
        return new LinkParser() {
            @Serial
            private static final long serialVersionUID = -7245878207925294233L;

            @Override
            public String pattern() {
                return "(?<=href=\")https?://[A-Za-z0-9./:_()\\[\\]{}-]+?(?=\")";
            }
        };
    }

    @Serial
    private static final long serialVersionUID = 1303388778823614737L;
    private LanguagePattern languagePattern;
    public static final Pattern HTTP_PATTERN =
    Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&=]*)");
    private final Pattern siteIdentifier = Pattern.compile("(?<=\\.)([A-Za-z_]+?)(?=[.])");
    private boolean stayOnSite;

    /**
     * Creates a new instance of {@link LinkParser} with the {@link HTMLParser#NAME} field pre-set to "link"
     * @see LinkParser
     */
    public LinkParser() {
        super("link");
    }

    @Override
    public abstract String pattern();

    @Override
    final public boolean onAddFilter(String element) {
        return  !stayOnSite || matches(element, siteIdentifier)
                && languagePattern == null || matches(element, languagePattern.PATTERN);
    }

    private boolean matches(String element, Pattern pattern) {
        return pattern.matcher(element).find();
    }

    @Override
    final public Path pathToContent() {
        return Paths.get("visited$" + super.pathToContent());
    }

    /**
     * Saves the links to a file. Segregates visited and unvisited links.
     * @param links {@link WebScraper#unvisitedLinks}
     * @throws IOException if links could not be saved for any reason.
     * @see WebScraper
     * @see LinkParser
     */
    final synchronized void saveLinks(Queue<String> links, WebScraper webScraper) throws IOException {
        flush();
        if(!links.isEmpty()) {
            try (var w = newBufferedWriter(Paths.get(webScraper + "$unvisited" + super.pathToContent()), openOption())) {
                for (var link : links) {
                    w.write(link);
                    w.newLine();
                }
            }
        }
    }

    /**
     * Adds the specified link to the visited links {@link HTMLParser#cache} <br/>
     * and flushes the {@link HTMLParser#cache} if necessary.
     * @param link link to be added to the {@link HTMLParser#cache}
     * @param limit target limit {@link ContentHandler#DATA_LIMIT}
     * @see ContentHandler
     * @see LinkParser
     */
    final synchronized void addVisitedLink(String link, int limit) {
        int previousSize = cache.size();
        if(dataWithinLimit(limit))
            cache.add(link);
        if(cacheOverflowing()) flushCache();
        collected += cache.size() - previousSize;
    }

    /**
     * <b>THIS METHOD IS DEPRECATED!</b>
     * Due to the way gathering links differs to gathering other types of data, adding links in bulk is not
     * supported, only one link can be visited at a time.
     * @param parsedElements elements to be added.
     * @param limit {@link ContentHandler#DATA_LIMIT}
     * @return {@link UnsupportedOperationException}
     * @throws UnsupportedOperationException if used.
     * @see LinkParser
     */
    @Override
    @Deprecated
    final synchronized int addData(Set<String> parsedElements, int limit) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("only one link can be visited at a time!");
    }

    /**
     * Enables site restriction filter for this {@link LinkParser}.
     * @see WebScraperBuilder#build()
     * @see LinkParser
     */
    final void restrictToSite() {
        this.stayOnSite = true;
    }

    /**
     * Tests if this parser has already visited provided link.
     * @param link link to be tested.
     * @return true if this {@link LinkParser} has already visited this link.
     * @see LinkParser
     */
    final boolean alreadyVisited(String link) {
        return cache.contains(link);
    }

    /**
     * Enables language restriction filter for this {@link LinkParser}.
     * @param languagePattern pattern which to apply.
     * @see LanguagePattern
     * @see WebScraperBuilder#restrictLanguage(LanguagePattern)
     * @see WebScraperBuilder#build()
     * @see LinkParser
     */
    final void addLanguageFilter(LanguagePattern languagePattern) {
        this.languagePattern = languagePattern;
    }

    /**
     * Tests if the given URL is matched by this {@link LinkParser}.
     * @param url URL to be tested.
     * @throws IllegalArgumentException if the URL is not matched.
     * @see LinkParser
     */
    final void verify(String url) throws IllegalArgumentException {
        if(!HTTP_PATTERN.matcher(url).matches())
        throw new IllegalArgumentException(url + " is not recognized by " + this + " (" + pattern() + ")");
    }
}
