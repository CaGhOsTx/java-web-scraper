import carlos.webcrawler.WebScraper;
import carlos.webcrawler.WebScraperBuilder;

import java.util.Scanner;

import static carlos.webcrawler.Options.DEBUG_MODE;
import static carlos.webcrawler.Options.SAVE_CONTENT;
import static carlos.webcrawler.StandardContentType.TEXT;

public class Main {

    public static void main(String[] args) {
        int dataLimit = 10_000_000;
        WebScraper fandom = new WebScraperBuilder(TEXT)
                .withOptions(SAVE_CONTENT)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build().startFrom("https://www.fandom.com/");
        WebScraper irishTimes = new WebScraperBuilder(TEXT)
                .withOptions(SAVE_CONTENT)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(6).build().startFrom("https://www.irishtimes.com/");
        WebScraper wikipedia = new WebScraperBuilder(TEXT)
                .withOptions(SAVE_CONTENT)
                .withDataLimit(dataLimit)
                .withCustomLinkRegex("\"(?<=href=\")https?:en.//[A-Za-z0-9.%/:-]+?(?=\")\"")
                .withThreadPoolSize(8).build().startFrom("https://en.wikipedia.org/wiki/Main_Page");
        WebScraper reddit = new WebScraperBuilder(TEXT)
                .withOptions(SAVE_CONTENT)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(24).build().startFrom("https://www.reddit.com/");
        WebScraper boards = new WebScraperBuilder(TEXT)
                .withOptions(SAVE_CONTENT)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(4).build().startFrom("https://www.boards.ie/");
        WebScraper youtube = new WebScraperBuilder(TEXT)
                .withOptions(SAVE_CONTENT)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(24).build().startFrom("https://www.youtube.com/");
        WebScraper quora = new WebScraperBuilder(TEXT)
                .withOptions(SAVE_CONTENT, DEBUG_MODE)
                .withDataLimit(dataLimit)
                .withThreadPoolSize(12).build().startFrom("https://www.quora.com/");
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
            quora.stop();
        }
        //Path path = Paths.get("C:\\Users\\carlo\\IdeaProjects\\CS210 Project\\Main$21204256593&text.txt");
        //new HashComparator(path).start();
    }
}