package carlos.webscraper.parser;

/**
 * More specific implementation of {@link Parser} interface. <br/>
 * Extra layer abstraction was required to make {@link StandardParser} possible.
 */
interface LimitedParser extends Parser {

    /**
     * Limit of data tokens to be collected.
     * @return the provided limit.
     * Serves as a {@link java.util.function.Supplier}.
     */
    long limit();

}
