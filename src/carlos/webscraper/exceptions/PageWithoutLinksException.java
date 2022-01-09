package carlos.webscraper.exceptions;

import carlos.webscraper.WebScraper;

import java.io.Serial;

/**
 * {@link RuntimeException} thrown when the HTML from a specified URL does not contain identifiable links.
 */
public final class PageWithoutLinksException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 7244117343903569290L;
    private final String link;
    private final WebScraper scraper;

    public PageWithoutLinksException(WebScraper scraper, String link) {
        this.scraper = scraper;
        this.link = link;
    }

    @Override
    public String getMessage() {
        return scraper + " page has no identifiable links! " + " LINK TO PAGE -> " + link;
    }
}
