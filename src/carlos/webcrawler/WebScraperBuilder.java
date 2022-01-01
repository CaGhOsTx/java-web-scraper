package carlos.webcrawler;

import java.util.Arrays;

import static java.util.stream.Collectors.toList;

public final class WebScraperBuilder {
    String customLinkRegex;
    private OptionHandler optionHandler = OptionHandler.EMPTY;
    private final ContentHandler contentHandler;
    private ThreadPoolHandler threadPoolHandler;
    int limit = 0;

    public WebScraperBuilder(ContentType... customContent) {
        this.contentHandler = new ContentHandler(customContent);
    }

    public WebScraperBuilder(StandardContentType... content) {
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
        if(threadPoolHandler == null)
            threadPoolHandler = new ThreadPoolHandler(1);
        if(customLinkRegex != null) {
            contentHandler.setCustomLinkRegex(customLinkRegex);
        }
        return new WebScraper(optionHandler, contentHandler, threadPoolHandler, limit);
    }

    public WebScraperBuilder withThreadPoolSize(int nThreads) {
        threadPoolHandler = new ThreadPoolHandler(nThreads);
        return this;
    }
}
