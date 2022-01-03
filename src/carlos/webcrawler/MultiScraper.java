package carlos.webcrawler;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MultiScraper {

    Thread manager = new Thread(this::control);
    List<WebScraper> scrapers;
    LocalTime startTime = LocalTime.now();

    public MultiScraper(List<WebScraper> scrapers) {
        this.scrapers = scrapers;
    }

    public MultiScraper(WebScraper... scrapers) {
        this.scrapers = Arrays.stream(scrapers).toList();
    }

    private void control() {
        try (var sc = new Scanner(System.in)) {
            String in = "";
            while (!in.equals("stop")) action(in = sc.nextLine());
        }
    }

    private void action(String s) {
        switch(s) {
            case "info" -> info();
            case "start" -> start();
            case "stop" -> stop();
            case "time" -> time();
        }
    }

    private void time() {
        System.out.println(Duration.between(startTime, LocalTime.now()));
    }

    private void info() {
        System.out.println("COLLECTED DATA:");
        scrapers.stream().map(WebScraper::getCollectedInfo)
                .forEach(System.out::println);
    }

    private void start() {
        System.out.println("Starting all scrapers...");
        startAll();
        long started = scrapers.stream().filter(WebScraper::isRunning).count();
        System.out.printf("%d/%d started successfully%n", started, scrapers.size());
    }

    private void stop() {
        System.out.println("Stopping all scrapers...");
        stopAll();
        while(isRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("MultiScraper finished!");
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
