package carlos.webscraper.parser.link;

import carlos.webscraper.*;
import carlos.webscraper.parser.HTMLParser;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

import static java.nio.file.Files.newBufferedWriter;

/**
 * Extension of the {@link HTMLParser} abstract class.
 * Used for parsing links and has an option to parse links
 * that are from the same site only or ones which stick to
 * a specific language.<br/>
 * Implement if you want a different {@link carlos.webscraper.parser.Parser#pattern()} for link parsing,
 *  otherwise use {@link LinkParser#newStandardLinkParser()} method.
 *  <br/>
 * <b> In situations when multiple {@link WebScraper}s are being used simultaneously,
 * and use the same {@link LinkParser} implementation,
 * give each {@link WebScraperBuilder} a clone of your implementation
 * (make a method similar to</b> {@link LinkParser#newStandardLinkParser()}
 * @see carlos.webscraper.parser.Parser
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
    public static LinkParser newStandardLinkParser() {
        return new LinkParser() {
            @Serial
            private static final long serialVersionUID = -7245878207925294233L;

            @Override
            public String pattern() {
                return "(?<=href=\")https?://[A-Za-z0-9./_()\\[\\]{}-]+?(?=\")";
            }

            @Override
            public long limit() {
                return 10_000_000;
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
    public final boolean onAddFilter(String element) {
        return  (!stayOnSite || matches(element, siteIdentifier))
                && (languagePattern == null || matches(element, languagePattern.PATTERN));
    }

    private boolean matches(String element, Pattern pattern) {
        return pattern.matcher(element).find();
    }

    final public Path pathToVisited() {
        return Paths.get("$visited$" + pathToContent());
    }

    final public Path pathToUnvisited(WebScraper scraper) {
        return Paths.get(scraper + "$unvisited$" + super.pathToContent());
    }

    public final synchronized void flushUnvisited(Collection<String> links, WebScraper webScraper) {
        flushUnvisited(links, webScraper, openOption(pathToUnvisited(webScraper)));
    }

    public final synchronized void flushUnvisited(Collection<String> links, WebScraper webScraper, StandardOpenOption option) {
        if(!links.isEmpty()) {
            try (var w = newBufferedWriter(pathToUnvisited(webScraper), option)) {
                for (var link : links) {
                    w.write(link);
                    w.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        links.clear();
    }

    /**
     * Adds the specified link to the visited links {@link HTMLParser#cache} <br/>
     * and flushes the {@link HTMLParser#cache} if necessary.
     * @param link link to be added to the {@link HTMLParser#cache}
     * @see LinkParser
     */
    public final void addVisitedLink(String link) {
        if(!reachedLimit())
            collected.addAndGet(cache.add(link) ? 1 : 0);
        if(cacheOverflowing()) flush(pathToVisited());
    }

    /**
     * Enables site restriction filter for this {@link LinkParser}.
     * @see WebScraperBuilder#build()
     * @see LinkParser
     */
    public final void restrictToSite() {
        this.stayOnSite = true;
    }

    /**
     * Tests if this parser has already visited provided link.
     * @param link link to be tested.
     * @return true if this {@link LinkParser} has already visited this link.
     * @see LinkParser
     */
    public final boolean alreadyVisited(String link) {
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
    public final void addLanguageFilter(LanguagePattern languagePattern) {
        this.languagePattern = languagePattern;
    }

    /**
     * Tests if the given URL is matched by this {@link LinkParser}.
     * @param url URL to be tested.
     * @throws IllegalArgumentException if the URL is not matched.
     * @see LinkParser
     */
    public final void verify(String url) throws IllegalArgumentException {
        if(!HTTP_PATTERN.matcher(url).matches())
        throw new IllegalArgumentException(url + " is not a valid url.");
    }
}
