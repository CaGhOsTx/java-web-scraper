package carlos.webcrawler;

import java.util.Arrays;

import static java.util.stream.Collectors.toList;

public class WebScraperBuilder {
    private OptionHandler<?> optionHandler = OptionHandler.EMPTY;
    private final ContentHandler contentHandler;
    private ThreadPoolHandler threadPoolHandler;

    public WebScraperBuilder(ContentType... content) {
        this.contentHandler = new ContentHandler(content);
    }

    @SafeVarargs
    final public <T extends Options> WebScraperBuilder withOptions(T... op) {
        optionHandler = new OptionHandler<>(Arrays.stream(op).collect(toList()));
        return this;
    }

    public WebScraper build() {
        if(threadPoolHandler == null)
            threadPoolHandler = new ThreadPoolHandler(1);
        return new WebScraper(optionHandler, contentHandler, threadPoolHandler);
    }

    public WebScraperBuilder withThreadPoolSize(int nThreads) {
        threadPoolHandler = new ThreadPoolHandler(nThreads);
        return this;
    }
}
