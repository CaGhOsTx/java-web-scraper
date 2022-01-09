package carlos.webscraper.parser;

import carlos.webscraper.parser.link.LinkParser;

import java.io.Serializable;

/**
 * The reason for this interface's existence is to emphasize which methods can be over-ridden
 * <ul>
 *     <li>{@link Parser#transform(String)}</li>
 *     <li>{@link Parser#onAddFilter(String)}</li>
 * </ul>
 * It also provides default implementations for optional methods so that users of
 * this API do not have to implement them if not required.<br/>
 * <br/>
 * Interface Implementations:
 * <ul>
 *     <li>{@link HTMLParser}</li>
 *     <li>{@link LinkParser}</li>
 * </ul>
 * @author Carlos Milkovic
 * @version a0.9
 */
public interface Parser extends Serializable {

    /**
     * String regex with which classes that implement this interface will parse raw HTML. <br/>
     * Serves as a {@link java.util.function.Supplier}. <br/>
     * @return the provided regex.
     */
    String pattern();

    /**
     * Method which pre-processes the raw HTML {@link String} prior to parsing. <br/>
     * Implement if pre-processing the HTML file results in a simpler final {@link Parser#pattern()}.
     * @param html raw HTML {@link String} to be pre-processed.
     * @return transformed HTML.
     */
    default String transform(String html) {
        return html;
    }

    /**
     * Method which filters parsed results. <br/>
     * Implement if you want to filter certain results.
     * @param element parsed element to be filtered.
     * @return false if the parsed element should be filtered out.
     */
    default boolean onAddFilter(String element) {
        return true;
    }
}
