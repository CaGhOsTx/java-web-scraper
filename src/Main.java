import carlos.webscraper.ScraperService;
import carlos.webscraper.WebScraper;
import carlos.webscraper.WebScraperBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static carlos.webscraper.LanguagePattern.ENGLISH;
import static carlos.webscraper.Option.SAVE_CONTENT;
import static carlos.webscraper.Option.SERIALIZE_ON_CLOSE;
import static carlos.webscraper.StandardHTMLParser.TEXT_PARSER;

public class Main {

    public static void main(String[] args) {
        List<?> list = new ArrayList<>();
        int dataLimit = 1_000_000_000;
        WebScraper fandom = new WebScraperBuilder("https://www.fandom.com/", TEXT_PARSER)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(4).build();
        WebScraper irishTimes = new WebScraperBuilder("https://www.irishtimes.com/", TEXT_PARSER)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build();
        WebScraper wikipedia = new WebScraperBuilder("https://en.wikipedia.org/wiki/Main_Page", TEXT_PARSER)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .restrictLanguage(ENGLISH)
                .withThreadPoolSize(6).build();
        WebScraper reddit = new WebScraperBuilder("https://www.reddit.com/", TEXT_PARSER)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build();
        WebScraper boards = new WebScraperBuilder("https://www.boards.ie/", TEXT_PARSER)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(4).build();
        WebScraper youtube = new WebScraperBuilder("https://www.youtube.com/", TEXT_PARSER)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build();
        var scraper = new ScraperService(fandom, irishTimes, wikipedia, reddit, boards, youtube);
        scraper.start();
//         Path path = Paths.get("/Users/carlos/IdeaProjects/cs210-hash/carlos.webcrawler.StandardContentType$11318911453&text.txt");
//        new HashComparator(path).start();
    }
}