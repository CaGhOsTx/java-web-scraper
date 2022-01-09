package carlos.webscraper.parser;

import carlos.webscraper.utilities.HTMLTransformer;
import carlos.webscraper.WebScraper;

import java.util.function.Predicate;

/**
 * List of standard {@link HTMLParser}s to help with creating {@link WebScraper} instances. <br/>
 * for use with WebScraperBuilder#of(StandardHTMLParser...) <br/>
 * <h2>Current Implementations:</h2>
 * <ul>
 *     <li><b>TEXT_PARSER</b> - parses raw text, extend and overwrite it's {@link LimitedParser#onAddFilter(String)}
 *     for niche filtering purposes</li>
 * </ul>
 * @author Carlos Milkovic
 * @version a0.9
 * @see StandardParser
 */
public enum StandardParser {
    TEXT(new Parser() {
        @Override
        public String pattern() {
            return "[A-Z0-9][A-Za-z0-9 ,.-]+?[!.?](?!\\w)";
        }

        @Override
        public String transform(String html) {
            return HTMLTransformer.clearTags(html);
        }
    });

    private final Parser PARSER;

    StandardParser(Parser PARSER) {
        this.PARSER = PARSER;
    }

    /**
     * Deduces the correct {@link StandardParser} instance from the given {@link String}.
     * @param parserName name of parser.
     * @throws IllegalArgumentException if the string provided does
     * not match any available {@link StandardParser} instances
     * @return {@link HTMLParser} instance contained withing this {@link StandardParser} constant.
     */
    public static Parser fromString(String parserName) throws IllegalArgumentException {
        for(var stdParser : values())
            if(parserName.equals(stdParser.toString()))
                return stdParser.PARSER;
        throw new IllegalArgumentException("Standard HTML parser of name " + parserName + "doesn't exist");
    }


    /**
     * Retrieves a {@link Parser} with the data limit set to 10_000_000 and filter ignored.
     * @return default {@link Parser} implementation.
     */
    public Parser get() {
        return getWithLimitAndFilter(10_000_000, s -> true);
    }

    /**
     * Retrieves a {@link Parser} with a custom data collection limit and <b>filter ignored. </b>
     * @param limit {@link LimitedParser#limit()}.
     * @return this {@link Parser} implementation.
     */
    public Parser getWithLimit(int limit) {
        return getWithLimitAndFilter(limit, s -> true);
    }

    /**
     * Retrieves a {@link Parser} with a custom data collection limit and filter.
     * @param limit {@link LimitedParser#limit()}.
     * @param filter {@link Parser#onAddFilter(String)}.
     * @return this {@link Parser} implementation.
     */
    public Parser getWithLimitAndFilter(int limit, Predicate<String> filter) {
        return new HTMLParser(this.name()) {
            @Override
            public String pattern() {
                return StandardParser.this.PARSER.pattern();
            }

            @Override
            public long limit() {
                return limit;
            }

            @Override
            public String transform(String html) {
                return StandardParser.this.PARSER.transform(html);
            }

            @Override
            public boolean onAddFilter(String element) {
                return filter.test(element);
            }
        };
    }
}
