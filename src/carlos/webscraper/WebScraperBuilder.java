package carlos.webscraper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public final class WebScraperBuilder {
    private OptionHandler optionHandler = OptionHandler.EMPTY;
    private final ContentHandler contentHandler;
    private LinkParser linkParser;
    private ThreadHandler threadHandler;
    private final String initialURL;
    private LanguagePattern languagePattern;
    private int limit = 0;

    /**
     * Creates an {@link WebScraperBuilder} with the bare minimum requirements for a {@link WebScraper} instance.
     * @param initialURL url the scraper starts with.
     * @param customContent custom implementations of {@link HTMLParser} abstract class.
     * @throws NullPointerException if the initial URL is null.
     * @see WebScraperBuilder
     * @see HTMLParser
     */
    public WebScraperBuilder(String initialURL, HTMLParser... customContent) {
        this.initialURL = Objects.requireNonNull(initialURL);
        this.contentHandler = new ContentHandler(customContent);
    }

    /**
     * Loads a serialized {@link WebScraper} from the given {@link Path}. Needs to be started ad hoc.
     * @param path path to object binary
     * @return stateful {@link WebScraper}
     * @throws NullPointerException if path is null.
     * @throws IOException when the file doesn't exist or can't be accessed for any reason
     * @throws ClassNotFoundException when the serialized binary is not a {@link WebScraper}
     * or it's version is different
     * @see WebScraper
     * @see Path
     */
    public static WebScraper deserialize(Path path) throws IOException, ClassNotFoundException {
        Objects.requireNonNull(path);
        return WebScraper.deserialize(path);
    }

    /**
     * Creates an {@link WebScraperBuilder} with the bare minimum requirements for a {@link WebScraper} instance.
     * @param initialURL url the scraper starts with.
     * @param content  predefined implementations of {@link HTMLParser}. <br/>
     *                 <b>visit {@link StandardHTMLParser} to see if any match your needs. </b>
     * @throws NullPointerException if the initial URL is null.
     * @see WebScraperBuilder
     * @see StandardHTMLParser
     */
    public WebScraperBuilder(String initialURL, StandardHTMLParser... content) {
        this.initialURL = Objects.requireNonNull(initialURL);
        this.contentHandler = new ContentHandler(Arrays.stream(content)
                .map(StandardHTMLParser::get)
                .toArray(HTMLParser[]::new));
    }

    /**
     * Sets the data token collection limit effective for all HTML parsers specified previously.
     * @param limit limit of collected data tokens.
     * @return the same object.
     * @see WebScraperBuilder
     * @see HTMLParser
     */
    public WebScraperBuilder withDataLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets a custom {@link LinkParser} implementation for this instance.
     * @param linkParser custom link implementation.
     * @return the same object.
     * @throws NullPointerException if the provided {@link LinkParser} is null.
     * @see WebScraperBuilder
     */
    public WebScraperBuilder withCustomLinkParser(LinkParser linkParser) {
        this.linkParser = Objects.requireNonNull(linkParser);
        return this;
    }

    /**
     * Restricts the {@link LinkParser} of this object to only accept links containing regex: \.xx[./]|[./]xx\.|/xx/
     * in the link, xx being the country code set in the {@link LanguagePattern} enum.
     * @param languagePattern constant from which the country code is provided.
     * @return this {@link WebScraperBuilder} instance.
     * @see WebScraperBuilder
     * @see LanguagePattern
     */
    public WebScraperBuilder restrictLanguage(LanguagePattern languagePattern) {
        this.languagePattern = languagePattern;
        return this;
    }

    /**
     * Sets options to trigger inferred actions in the {@link WebScraper} at runtime.
     * @param options options to be set.
     * @return this {@link WebScraperBuilder} instance.
     * @see WebScraperBuilder
     * @see Option
     */
    public WebScraperBuilder withOptions(Option... options) {
        optionHandler = new OptionHandler(Arrays.stream(options).collect(toList()));
        return this;
    }

    /**
     * Sets the amount of threads to scrape in parallel initially.
     * @param nThreads number of threads.
     * @throws IllegalArgumentException if the number of threads is non-positive.
     * @return this {@link WebScraperBuilder} instance.
     */
    public WebScraperBuilder withThreadPoolSize(int nThreads) {
        if(nThreads < 1) throw new IllegalArgumentException("nThreads must be greater than 0!");
        threadHandler = new ThreadHandler(nThreads);
        return this;
    }

    /**
     * Finalizes the build of the {@link WebScraper}.
     * @return new {@link WebScraper} instance.
     * @see WebScraperBuilder
     * @see WebScraper
     */
    public WebScraper build() {
        if(threadHandler == null) threadHandler = new ThreadHandler(1);
        if(languagePattern != null)
            contentHandler.restrictLanguage(languagePattern);
        if(linkParser != null)
            contentHandler.setCustomLinkType(linkParser);
        contentHandler.setLimit(limit);
        return new WebScraper(initialURL, optionHandler, contentHandler, threadHandler);
    }
}
