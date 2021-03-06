package carlos.webscraper;

import carlos.webscraper.parser.HTMLParser;
import carlos.webscraper.parser.Parser;
import carlos.webscraper.parser.StandardParser;
import carlos.webscraper.parser.link.LanguagePattern;
import carlos.webscraper.parser.link.LinkParser;
import carlos.webscraper.service.ScraperService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Builder class for {@link WebScraper}.
 * @author Carlos Milkovic
 * @version a0.9
 */
@SuppressWarnings("unused")
public final class WebScraperBuilder {
    private OptionHandler optionHandler = OptionHandler.EMPTY;
    private final ContentHandler contentHandler;
    private String initialURL;
    private LanguagePattern languagePattern;
    private int nThreads = 1;

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
     * @param parsers  custom implementations of {@link HTMLParser}. <br/>
     * @throws IllegalStateException if you try to build with this factory.
     */
    public static WebScraperBuilder of(Parser... parsers) throws IllegalStateException {
        return new WebScraperBuilder(Arrays.stream(parsers).map(p -> (HTMLParser) p).toArray(HTMLParser[]::new));
    }

    /**
     * Static factory for a {@link WebScraperBuilder} with the bare minimum requirements for a {@link WebScraper} instance.
     * @param initialURL url the scraper starts with.
     * @param parsers  implementations of {@link HTMLParser}. <br/>
     *                 <b>visit {@link StandardParser} to see if any match your needs. </b>
     * @throws NullPointerException if the initial URL is null.
     */
    public static WebScraperBuilder of(String initialURL, Parser... parsers) {
        return new WebScraperBuilder(requireNonNull(initialURL), Arrays.stream(parsers).map(p -> (HTMLParser) p).toArray(HTMLParser[]::new));
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
     */
    @Deprecated
    public static WebScraper deserialize(Path path) throws IOException, ClassNotFoundException {
        requireNonNull(path);
        return WebScraper.deserialize(path);
    }

    /**
     * Sets a custom {@link LinkParser} implementation for this instance.
     * @param linkParser custom link implementation.
     * @return the same object.
     * @throws NullPointerException if the provided {@link LinkParser} is null.
     * @see WebScraperBuilder
     */
    public WebScraperBuilder withCustomLinkParser(LinkParser linkParser) throws NullPointerException {
        this.contentHandler.setCustomLinkParser(requireNonNull(linkParser));
        return this;
    }

    /**
     * Restricts the {@link LinkParser} of this object to only accept links containing regex: \.xx[./]|[./]xx\.|/xx/
     * in the link, xx being the country code set in the {@link LanguagePattern} enum.
     * @param languagePattern constant from which the country code is provided.
     * @return this {@link WebScraperBuilder} instance.
     * @throws NullPointerException if provided {@link LanguagePattern} is null.
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
        this.nThreads = nThreads;
        return this;
    }

    /**
     * Finalizes the build of the {@link WebScraper}.
     * @return new {@link WebScraper} instance.
     * @throws IllegalStateException if {@link WebScraperBuilder#of(Parser...)} or
     * {@link WebScraperBuilder#of(Parser...)} are used as factories.
     * @see WebScraperBuilder
     */
    public WebScraper build() throws IllegalStateException {
        if(languagePattern != null)
            contentHandler.restrictLanguage(languagePattern);
        if(optionHandler.isPresent(Option.SAVE_LINKS))
            contentHandler.getLinkParser().enableSaving();
        if(optionHandler.isPresent(Option.SAVE_PARSED_ELEMENTS))
            contentHandler.enableSavingForAllParsers();
        if(optionHandler.isPresent(Option.STAY_ON_WEBSITE))
            contentHandler.getLinkParser().restrictToSite();
        return new WebScraper(initialURL, optionHandler, contentHandler, nThreads);
    }

    /**
     * Sets the initial url the {@link WebScraper} would start from.
     * @param url initial url to be visited.
     * @return this {@link WebScraperBuilder} instance.
     * @throws NullPointerException if the url specified is null.
     * @throws IllegalArgumentException if the current {@link LinkParser} does not match the url.
     */
    public WebScraperBuilder setInitialURL(String url) throws IllegalArgumentException, NullPointerException {
        contentHandler.getLinkParser().verify(url);
        requireNonNull(this.initialURL = url);
        return this;
    }
}
