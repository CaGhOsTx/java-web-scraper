package carlos.webscraper;

import java.nio.file.Path;
import java.util.Arrays;

import static java.util.stream.Collectors.toList;

public final class WebScraperBuilder {
    private OptionHandler optionHandler = OptionHandler.EMPTY;
    private final ContentHandler contentHandler;
    private ContentType link;
    private threadHandler threadHandler;
    private final String startURL;
    private Language language;
    int limit = 0;

    public WebScraperBuilder(String startURL, ContentType... customContent) {
        this.startURL = startURL;
        this.contentHandler = new ContentHandler(customContent);
    }

    public static WebScraper deserialize(Path path) {
        return WebScraper.deserialize(path);
    }

    public WebScraperBuilder(String startURL, StandardContentType... content) {
        this.startURL = startURL;
        this.contentHandler = new ContentHandler(Arrays.stream(content)
                .map(StandardContentType::get)
                .toArray(ContentType[]::new));
    }

    public WebScraperBuilder withDataLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public WebScraperBuilder withCustomLinkParser(ContentType link) {
        this.link = link;
        return this;
    }

    public WebScraperBuilder restrictLanguage(Language language) {
        this.language = language;
        return this;
    }

    @SafeVarargs
    final public <T extends Option> WebScraperBuilder withOptions(T... op) {
        optionHandler = new OptionHandler(Arrays.stream(op).collect(toList()));
        return this;
    }

    public WebScraper build() {
        if(threadHandler == null) threadHandler = new threadHandler(1);
        if(language != null)
            contentHandler.restrictLanguage(language);
        if(link != null)
            contentHandler.setCustomLink(link);
        return new WebScraper(startURL, optionHandler, contentHandler, threadHandler, limit);
    }

    public WebScraperBuilder withThreadPoolSize(int nThreads) {
        threadHandler = new threadHandler(nThreads);
        return this;
    }
}
