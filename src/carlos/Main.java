package carlos;

import carlos.webscraper.WebScraperBuilder;
import carlos.webscraper.parser.HTMLParser;
import carlos.webscraper.parser.StandardParser;
import carlos.webscraper.service.ScraperService;

import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;

import static carlos.webscraper.Option.SAVE_LINKS;
import static carlos.webscraper.Option.SAVE_PARSED_ELEMENTS;
import static carlos.webscraper.parser.link.LanguagePattern.ENGLISH;


public class Main {

    private static boolean between3And10Words(String s) {
        int length = s.split("\\s+").length;
        return length >= 3 && length <= 10;
    }
    public static void main(String[] args) {
        System.out.println("PROJECT DEMO");
        System.out.println("Enter the number of sentences you would like to compare" +
                "(15 000 or less if you dont want to wait a long time):");
        try {
            var text = StandardParser.TEXT.getWithLimitAndFilter(new Scanner(System.in).nextInt(), Main::between3And10Words);
            var service = ScraperService.singletonService();
            service.add(() -> WebScraperBuilder.of(text)
                            .withThreadPoolSize(4)
                            .withOptions(SAVE_PARSED_ELEMENTS, SAVE_LINKS),
                    "https://www.boards.ie",
                    "https://www.reddit.com",
                    "https://www.irishtimes.com");
            service.add(() -> WebScraperBuilder.of(text)
                            .withThreadPoolSize(8)
                            .restrictLanguage(ENGLISH)
                            .withOptions(SAVE_PARSED_ELEMENTS, SAVE_LINKS),
                    "https://en.wikipedia.org");
            service.startAsync()
                    .whenComplete((s, e) -> runComparison(text)).join();
        }
        catch(Exception e) {
            System.err.println("your argument should be a single number!");
            main(null);
        }
    }
    private static void runComparison(HTMLParser text) {
        System.out.println("-------------------");
        System.out.println("Starting comparison");
        System.out.println("-------------------");
        Instant now = Instant.now();
        new HashComparator(text.pathToContent()).compare();
        System.out.println("Time taken to run comparison: " +
                Duration.between(now, Instant.now()).toSeconds() + " seconds");
    }
}
