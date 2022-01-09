package carlos.webscraper;

/**
 * <h1>List of available options and their effects:</h1>
 * <ul>
 *     <li>DEBUG_MODE           - enables console logging</li>
 *     <li>SAVE_LINKS           - enables link saving</li>
 *     <li>SAVE_PARSED_ELEMENTS - enables saving for all parsers</li>
 *     <li>STAY_ON_WEBSITE      - makes the implementing link parser filter out
 *     any links not from the same domain as {@link WebScraperBuilder#initialURL}</li>
 *     <li>SERIALIZE_ON_CLOSE   - serializes the {@link WebScraper} on close. <b>DEPRECATED</b></li>
 * </ul>
 * for use with {@link WebScraperBuilder#withOptions(Option...)}
 * @author Carlos Milkovic
 * @version 1.0
 * @see WebScraperBuilder
 */
public enum Option {
    DEBUG_MODE,
    SAVE_LINKS,
    SAVE_PARSED_ELEMENTS,
    UNLIMITED,
    STAY_ON_WEBSITE,
    @Deprecated
    SERIALIZE_ON_CLOSE
}
