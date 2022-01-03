package carlos.webcrawler;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MultiScraper {

    Thread manager = new Thread(this::control);
    List<WebScraper> scrapers;

    public MultiScraper(List<WebScraper> scrapers) {
        this.scrapers = scrapers;
    }

    private void control() {
        try (var sc = new Scanner(System.in)) {
            String in = "";
            while (!in.equals("stop")) {
                in = sc.next();
            }
        }
    }

    private void action(String s) {
        switch(s) {
            case "info" -> {
                System.out.println("COLLECTED DATA:");
                scrapers.stream().map(WebScraper::getCollectedInfo).forEach(System.out::println);
            }
        }
    }

    public MultiScraper(WebScraper... scrapers) {
        this.scrapers = Arrays.stream(scrapers).toList();
    }

    public void startAll() {
        scrapers.forEach(WebScraper::start);
    }

    public void stopAll() {
        scrapers.forEach(WebScraper::stop);
    }

    public boolean isRunning() {
        return scrapers.stream().anyMatch(WebScraper::isRunning);
    }
}
