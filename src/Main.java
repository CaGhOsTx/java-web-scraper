import carlos.webcrawler.WebScraper;
import carlos.webcrawler.WebScraperBuilder;

import java.util.Scanner;

import static carlos.webcrawler.Options.DEBUG_MODE;
import static carlos.webcrawler.Options.SAVE_CONTENT;
import static carlos.webcrawler.StandardContentType.TEXT;

public class Main {

    public static void main(String[] args) {
        int dataLimit = 10_000_000;
        WebScraper fandom = new WebScraperBuilder("https://www.fandom.com/", TEXT)
                .withOptions(SAVE_CONTENT, DEBUG_MODE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(4).build().start();
        WebScraper irishTimes = new WebScraperBuilder("https://www.irishtimes.com/", TEXT)
                .withOptions(SAVE_CONTENT, DEBUG_MODE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build().start();
        WebScraper wikipedia = new WebScraperBuilder("https://en.wikipedia.org/wiki/Main_Page", TEXT)
                .withOptions(SAVE_CONTENT, DEBUG_MODE)
                .withDataLimit(dataLimit)
                .withCustomLinkRegex("(?<=href=\")https?://en\\.wiki[A-Za-z0-9./:_()\\[\\]{}-]+?(?=\")")
                .withThreadPoolSize(6).build().start();
        WebScraper reddit = new WebScraperBuilder("https://www.reddit.com/", TEXT)
                .withOptions(SAVE_CONTENT, DEBUG_MODE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build().start();
        WebScraper boards = new WebScraperBuilder("https://www.boards.ie/", TEXT)
                .withOptions(SAVE_CONTENT, DEBUG_MODE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(4).build().start();
        WebScraper youtube = new WebScraperBuilder("https://www.youtube.com/", TEXT)
                .withOptions(SAVE_CONTENT, DEBUG_MODE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build().start();
        Scanner sc = new Scanner(System.in);
        var stop = "";
        do {
            stop = sc.nextLine();
        } while (!stop.equals("1"));
        if (TEXT.get().getData().size() > 0) {
            fandom.stop();
            irishTimes.stop();
            wikipedia.stop();
            reddit.stop();
            boards.stop();
            youtube.stop();
        }
        //Path path = Paths.get("C:\\Users\\carlo\\IdeaProjects\\CS210 Project\\Main$21204256593&text.txt");
        //new HashComparator(path).start();
    }
}