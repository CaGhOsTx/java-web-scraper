package carlos.webcrawler;

public class WebCrawlerBuilder {
    String startURL;
    int numberOfSentences, minLength, maxLength;
    boolean debug;

    public WebCrawlerBuilder(String startURL) {
        this.startURL = startURL;
        maxLength = Integer.MAX_VALUE;
        minLength = 0;
    }

    public WebCrawlerBuilder sentenceLimit(int numberOfSentences) {
        this.numberOfSentences = numberOfSentences;
        return this;
    }

    public WebCrawlerBuilder withMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public WebCrawlerBuilder withMinLength(int minLength) {
        this.minLength = minLength;
        return this;
    }

    public WebCrawlerBuilder debugMode(boolean debug) {
        this.debug = debug;
        return this;
    }

    public WebCrawler build() {
        return new WebCrawler(startURL, numberOfSentences, minLength, maxLength, debug);
    }
}
