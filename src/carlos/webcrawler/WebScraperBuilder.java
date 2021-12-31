package carlos.webcrawler;

import java.util.Arrays;

import static carlos.webcrawler.Options.UNLIMITED;
import static java.util.stream.Collectors.toList;

public class WebScraperBuilder {
    private final String startURL, linkPrefix;
    private int numberOfSentences, minLength, maxLength;
    private OptionHandler<?> optionHandler = OptionHandler.EMPTY;
    private final ContentHandler contentHandler;
    private ThreadPoolHandler threadPoolHandler;

    public WebScraperBuilder(String startURL, String linkPrefix, ContentHandler contentHandler) {
        this.startURL = startURL;
        this.linkPrefix = linkPrefix;
        this.contentHandler = contentHandler;
    }

    @SafeVarargs
    final public <T extends Options> WebScraperBuilder withOptions(T... op) {
        optionHandler = new OptionHandler<>(Arrays.stream(op).collect(toList()));
        return this;
    }

    public WebScraper build() {
        if(threadPoolHandler == null)
            threadPoolHandler = new ThreadPoolHandler(1);
        if(optionHandler.isTrue(UNLIMITED))
            contentHandler.setUnlimited();
        return new WebScraper(startURL, linkPrefix, optionHandler, contentHandler, threadPoolHandler);
    }

    public WebScraperBuilder withThreadPoolSize(int nThreads) {
        threadPoolHandler = new ThreadPoolHandler(nThreads);
        return this;
    }
}
