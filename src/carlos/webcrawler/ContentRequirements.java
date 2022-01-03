package carlos.webcrawler;

public interface ContentRequirements {

    String pattern();
    default String transform(String html) {
        return html;
    }
    default boolean onAddFilter(String s) {
        return true;
    }
}
