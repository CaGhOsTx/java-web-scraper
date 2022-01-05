package carlos.webscraper;

import java.io.Serial;
import java.util.Arrays;

/**
 * List of standard {@link HTMLParser}s to help with creating {@link WebScraper} instances. <br/>
 * for use with WebScraperBuilder#of(StandardHTMLParser...) <br/>
 * <h2>Current Implementations:</h2>
 * <ul>
 *     <li><b>TEXT_PARSER</b> - parses raw text, extend and overwrite it's {@link Parsable#onAddFilter(String)}
 *     for niche filtering purposes</li>
 * </ul>
 * @author Carlos Milkovic
 * @version a0.9
 * @see
 * @see StandardHTMLParser
 */
public enum StandardHTMLParser {
    TEXT_PARSER(new HTMLParser("text") {

        @Serial
        private static final long serialVersionUID = -3131684393596396308L;

        @Override
        public String pattern() {
            return "[A-Z0-9][A-Za-z0-9 ,.-]+?[!.?](?!\\w)";
        }

        @Override
        public String transform(String html) {
            return HTMLTransformer.clearTags(html);
        }

        @Override
        public boolean onAddFilter(String element) {
            var tmp = element.split("\\s+");
            return tmp.length >= 2 && tmp.length <= 10;
        }
    });

    /**
     * Casts {@link StandardHTMLParser} instances to {@link HTMLParser} instances. <br/>
     * Reduces the number of necessary {@link WebScraperBuilder} static factories.
     * @param standardParsers array of {@link StandardHTMLParser}s to be cast.
     * @return array of {@link HTMLParser} contained within the constants.
     * @see HTMLParser
     * @see WebScraperBuilder
     * @see StandardHTMLParser
     */
    static HTMLParser[] cast(StandardHTMLParser... standardParsers) {
        return Arrays.stream(standardParsers).map(StandardHTMLParser::get).toArray(HTMLParser[]::new);
    }

    private final HTMLParser PARSER;

    StandardHTMLParser(HTMLParser PARSER) {
        this.PARSER = PARSER;
    }

    /**
     * Deduces the correct {@link StandardHTMLParser} instance from the given {@link String}.
     * @param parserName name of parser.
     * @throws IllegalArgumentException if the string provided does
     * not match any available {@link StandardHTMLParser} instances
     * @return {@link HTMLParser} instance contained withing this {@link StandardHTMLParser} constant.
     */
    public static HTMLParser fromString(String parserName) throws IllegalArgumentException {
        for(var parser : values())
            if(parserName.equals(parser.toString()))
                return cast(parser)[0];
        throw new IllegalArgumentException("Standard HTML parser of name " + parserName + "doesn't exist");
    }

    /**
     * @return {@link HTMLParser} contained within this {@link StandardHTMLParser}.
     */
    public HTMLParser get() {
        return PARSER;
    }
}
