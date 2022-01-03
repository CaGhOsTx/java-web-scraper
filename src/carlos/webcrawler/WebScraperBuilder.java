package carlos.webcrawler;

import java.util.Arrays;

import static java.util.stream.Collectors.toList;

public final class WebScraperBuilder {
    private String customLinkRegex;
    private OptionHandler optionHandler = OptionHandler.EMPTY;
    private final ContentHandler contentHandler;
    private threadHandler threadHandler;
    private final String startURL;
    int limit = 0;

    public WebScraperBuilder(String startURL, ContentType... customContent) {
        this.startURL = startURL;
        this.contentHandler = new ContentHandler(customContent);
    }

    public static WebScraper deserialize(String startURL) {
        return new WebScraper(startURL);
    }

    public WebScraperBuilder(String startURL, StandardContentType... content) {
        this.startURL = startURL;
        this.contentHandler = new ContentHandler(Arrays.stream(content)
                .map(StandardContentType::get)
                .toArray(ContentType[]::new));
    }

    public WebScraperBuilder withCustomLinkRegex(String regex) {
        customLinkRegex = regex;
        return this;
    }

    public WebScraperBuilder withDataLimit(int limit) {
        this.limit = limit;
        return this;
    }

    @SafeVarargs
    final public <T extends Options> WebScraperBuilder withOptions(T... op) {
        optionHandler = new OptionHandler(Arrays.stream(op).collect(toList()));
        return this;
    }

    public WebScraper build() {
        if(threadHandler == null)
            threadHandler = new threadHandler(1);
        if(customLinkRegex != null) {
            contentHandler.setCustomLinkRegex(customLinkRegex);
        }
        return new WebScraper(startURL, optionHandler, contentHandler, threadHandler, limit);
    }

    public WebScraperBuilder withThreadPoolSize(int nThreads) {
        threadHandler = new threadHandler(nThreads);
        return this;
    }
}
