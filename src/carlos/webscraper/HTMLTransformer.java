package carlos.webscraper;

/**
 * Utility class containing some useful transformation functions on HTML strings.
 * Useful for {@link HTMLParser} implementations.
 * @author Carlos Milkovic
 * @version a0.9
 */
public class HTMLTransformer {
    private HTMLTransformer() {}

    /**
     * Removes all the tags in the given html file leaving only text.
     * @param html raw HTML {@link String}.
     * @return transformed HTML.
     */
    public static String clearTags(String html) {
        return html.replaceAll("<.*?>", "");
    }
}
