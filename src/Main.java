import carlos.webcrawler.HashComparator;
import carlos.webcrawler.MultiScraper;
import carlos.webcrawler.WebScraper;
import carlos.webcrawler.WebScraperBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import static carlos.webcrawler.Options.*;
import static carlos.webcrawler.StandardContentType.TEXT;

public class Main {

    public static void main(String[] args) throws IOException {
        int dataLimit = 1_000_000_000;
        WebScraper fandom = new WebScraperBuilder("https://www.fandom.com/", TEXT)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(4).build();
        WebScraper irishTimes = new WebScraperBuilder("https://www.irishtimes.com/", TEXT)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build();
        WebScraper wikipedia = new WebScraperBuilder("https://en.wikipedia.org/wiki/Main_Page", TEXT)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withCustomLinkRegex("(?<=href=\")https?://en\\.wiki[A-Za-z0-9./:_()\\[\\]{}-]+?(?=\")")
                .withThreadPoolSize(6).build();
        WebScraper reddit = new WebScraperBuilder("https://www.reddit.com/", TEXT)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build();
        WebScraper boards = new WebScraperBuilder("https://www.boards.ie/", TEXT)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(4).build().start();
        WebScraper youtube = new WebScraperBuilder("https://www.youtube.com/", TEXT)
                .withOptions(SAVE_CONTENT, SERIALIZE_ON_CLOSE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build();
        new MultiScraper(fandom, irishTimes, wikipedia, reddit, boards, youtube).startAll();
         Path path = Paths.get("/Users/carlos/IdeaProjects/cs210-hash/carlos.webcrawler.StandardContentType$11318911453&text.txt");
        new HashComparator(path).start();
    }
}