package carlos.webscraper.exceptions;

import carlos.webscraper.WebScraper;

public final class ReachedEndException extends RuntimeException {

    private final WebScraper scraper;

    public ReachedEndException(WebScraper scraper) {
        this.scraper = scraper;
    }

    @Override
    public String getMessage() {
        return scraper + ": " + Thread.currentThread().getName() + " reached end!" + "(" + this + ")";
    }
}
