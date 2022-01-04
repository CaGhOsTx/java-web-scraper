package carlos.webscraper;

public class HTMLHelper {
    private HTMLHelper() {}

    public static String clearTags(String html) {
        return html.replaceAll("<.*?>", "");
    }
}
