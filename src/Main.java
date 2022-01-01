import carlos.webcrawler.ContentType;
import carlos.webcrawler.WebScraper;
import carlos.webcrawler.WebScraperBuilder;

import static carlos.webcrawler.Options.*;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        ContentType text = new ContentType("text") {

            String clearTags(String html) {
                return html.replaceAll("<.*?>", "");
            }

            @Override public String pattern() {
                return "[A-Z0-9][A-Za-z0-9,. -]*?[!.?](?!\\w)";
            }
            @Override public String transform(String html) {
                return clearTags(html);
            }

            @Override public boolean onAddFilter(String s) {
                var tmp = s.split("\\s+");
                return tmp.length >= 4 && tmp.length <= 10;
            }

            @Override public int limit() {return 1_000_000_000;}
        };
//        WebScraper scraper1 = new WebScraperBuilder(text)
//                .withOptions(SAVE_CONTENT, DEBUG_MODE)
//                .withThreadPoolSize(5).build().startFrom("https://www.fandom.com/");
        WebScraper scraper2 = new WebScraperBuilder(text)
                .withOptions(SAVE_CONTENT, DEBUG_MODE ,RESTRICT_LANGUAGE)
                .withThreadPoolSize(6).build().startFrom("https://en.wikipedia.org/wiki/Main_Page");
        while(scraper2.isRunning()) {
            Thread.sleep(10000);
        }
        //Path path = Paths.get("C:\\Users\\carlo\\IdeaProjects\\CS210 Project\\Main$21204256593&text.txt");
        //new HashComparator(path).start();
    }
}