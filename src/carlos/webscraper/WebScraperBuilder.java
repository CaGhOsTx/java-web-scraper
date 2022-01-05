package carlos.webscraper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static carlos.webscraper.StandardHTMLParser.cast;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Builder class for {@link WebScraper}.
 * @author Carlos Milkovic
 * @version a0.9
 * @see WebScraper
 */
public final class WebScraperBuilder {
    private OptionHandler optionHandler = OptionHandler.EMPTY;
    private final ContentHandler contentHandler;
    private LinkParser linkParser;
    private ThreadHandler threadHandler;
    private String initialURL;
    private LanguagePattern languagePattern;
    private int limit = 0;

    private WebScraperBuilder(String initialURL, HTMLParser... customParsers) {
        this.initialURL = requireNonNull(initialURL);
        this.contentHandler = new ContentHandler(customParsers);
    }

    private WebScraperBuilder(HTMLParser... customParsers) {
        this.contentHandler = new ContentHandler(customParsers);
    }

    /**
     * <b> ONLY USE TO GIVE A TEMPLATE TO {@link ScraperService}!!! </b> <br/>
     * Static factory for a {@link WebScraperBuilder} with the bare minimum requirements for a {@link WebScraper} instance.
     * @param parsers  predefined implementations of {@link HTMLParser}. <br/>
     * @throws NullPointerException if the initial URL is null.
     * @see WebScraperBuilder
     * @see StandardHTMLParser
     * @see ScraperService
     */
    public static WebScraperBuilder of(StandardHTMLParser... parsers) throws NullPointerException {
        return new WebScraperBuilder(cast(parsers));
    }

    /**
     * <b> ONLY USE TO GIVE A TEMPLATE TO {@link ScraperService}!!! </b> <br/>
     * Static factory for a {@link WebScraperBuilder} with the bare minimum requirements for a {@link WebScraper} instance.
     * @param parsers  custom implementations of {@link HTMLParser}. <br/>
     * @throws NullPointerException if the initial URL is null.
     * @see WebScraperBuilder
     * @see HTMLParser
     * @see ScraperService
     */
    public static WebScraperBuilder of(HTMLParser... parsers) throws NullPointerException {
        return new WebScraperBuilder(parsers);
    }

    /**
     * Static factory for a {@link WebScraperBuilder} with the bare minimum requirements for a {@link WebScraper} instance.
     * @param initialURL url the scraper starts with.
     * @param parsers  custom implementations of {@link HTMLParser}.
     * @throws NullPointerException if the initial URL is null.
     * @see WebScraperBuilder
     * @see HTMLParser
     */
    public static WebScraperBuilder of(String initialURL, HTMLParser... parsers) throws NullPointerException {
        return new WebScraperBuilder(initialURL, parsers);
    }

    /**
     * Static factory for a {@link WebScraperBuilder} with the bare minimum requirements for a {@link WebScraper} instance.
     * @param initialURL url the scraper starts with.
     * @param parsers  predefined implementations of {@link HTMLParser}. <br/>
     *                 <b>visit {@link StandardHTMLParser} to see if any match your needs. </b>
     * @throws NullPointerException if the initial URL is null.
     * @see WebScraperBuilder
     * @see StandardHTMLParser
     */
    public static WebScraperBuilder of(String initialURL, StandardHTMLParser... parsers) {
        return new WebScraperBuilder(requireNonNull(initialURL), cast(requireNonNull(parsers)));
    }

    /**
     * Loads a serialized {@link WebScraper} from the given {@link Path}. Needs to be started ad hoc.
     * @param path path to object binary
     * @return stateful {@link WebScraper}
     * @throws NullPointerException if path is null.
     * @throws IOException when the file doesn't exist or can't be accessed for any reason
     * @throws ClassNotFoundException when the serialized binary is not a {@link WebScraper}
     * or it's version is different
     * @deprecated serialization does not work at the moment.
     * @see WebScraper
     * @see Path
     */
    @Deprecated
    public static WebScraper deserialize(Path path) throws IOException, ClassNotFoundException {
        requireNonNull(path);
        return WebScraper.deserialize(path);
    }

    /**
     * Sets the data token collection limit effective for all HTML parsers specified previously.
     * @param limit limit of collected data tokens.
     * @return the same object.
     * @throws IllegalArgumentException if limit is not positive.
     * @see WebScraperBuilder
     * @see HTMLParser
     */
    public WebScraperBuilder withDataLimit(int limit) {
        if(limit <= 0) throw new IllegalArgumentException("limit must be positive");
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
    public WebScraperBuilder withCustomLinkParser(LinkParser linkParser) throws NullPointerException {
        this.linkParser = requireNonNull(linkParser);
        return this;
    }

    /**
     * Restricts the {@link LinkParser} of this object to only accept links containing regex: \.xx[./]|[./]xx\.|/xx/
     * in the link, xx being the country code set in the {@link LanguagePattern} enum.
     * @param languagePattern constant from which the country code is provided.
     * @return this {@link WebScraperBuilder} instance.
     * @throws NullPointerException if provided {@link LanguagePattern} is null.
     * @see WebScraperBuilder
     * @see LanguagePattern
     */
    public WebScraperBuilder restrictLanguage(LanguagePattern languagePattern) throws NullPointerException {
        requireNonNull(this.languagePattern = languagePattern);
        return this;
    }

    /**
     * Sets options to trigger inferred actions in the {@link WebScraper} at runtime.
     * @param options options to be set.
     * @return this {@link WebScraperBuilder} instance.
     * @throws NullPointerException if options is null.
     * @see WebScraperBuilder
     * @see Option
     */
    public WebScraperBuilder withOptions(Option... options) throws NullPointerException {
        requireNonNull(options);
        optionHandler = new OptionHandler(Arrays.stream(options).collect(toList()));
        return this;
    }

    /**
     * Sets the amount of threads to scrape in parallel initially.
     * @param nThreads number of threads.
     * @throws IllegalArgumentException if the number of threads is non-positive.
     * @return this {@link WebScraperBuilder} instance.
     */
    public WebScraperBuilder withThreadPoolSize(int nThreads) throws IllegalArgumentException {
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
        if(limit > 0) contentHandler.setLimit(limit);
        if(linkParser != null)
            contentHandler.setCustomLinkParser(linkParser);
        if(optionHandler.isPresent(Option.SAVE_LINKS))
            linkParser.setShouldSave(true);
        if(optionHandler.isPresent(Option.SAVE_PARSED_ELEMENTS))
            contentHandler.setContentSaving(true);
        if(optionHandler.isPresent(Option.STAY_ON_WEBSITE))
            linkParser.restrictToSite();
        return new WebScraper(initialURL, optionHandler, contentHandler, threadHandler);
    }

    /**
     * Sets the inital url the {@link WebScraper} would start from.
     * @param url initial url to be visited.
     * @return this {@link WebScraperBuilder} instance.
     * @throws NullPointerException if the url specified is null.
     * @throws IllegalArgumentException if the current {@link LinkParser} does not match the url.
     * @see WebScraperBuilder
     * @see LinkParser
     * @see WebScraper
     */
    public WebScraperBuilder setInitialURL(String url) throws IllegalArgumentException, NullPointerException {
        contentHandler.getLinkParser().verify(url);
        requireNonNull(this.initialURL = url);
        return this;
    }
}
