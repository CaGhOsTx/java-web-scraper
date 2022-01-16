package carlos.webscraper.service;

import carlos.webscraper.Option;
import carlos.webscraper.WebScraper;
import carlos.webscraper.WebScraperBuilder;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static carlos.webscraper.service.Action.*;

final public class ScraperService {

    private static final ScraperService SINGLETON = new ScraperService();

    private final ExecutorService service = Executors.newCachedThreadPool();
    private final List<WebScraper> scrapers = new ArrayList<>();
    private final LocalTime startTime = LocalTime.now();

    public ScraperService add(Supplier<WebScraperBuilder> builderTemplate, String... links) {
        Arrays.stream(links).forEach(l -> SINGLETON.scrapers.add(builderTemplate.get().setInitialURL(l).build()));
        return SINGLETON;
    }

    public ScraperService add(List<WebScraper> scrapers) {
        SINGLETON.scrapers.addAll(scrapers);
        return SINGLETON;
    }


    public void remove(WebScraper scraper) {
        scraper.stop();
        scrapers.remove(scraper);
    }

    public static ScraperService singletonService() {
        return SINGLETON;
    }

    public void add(WebScraper scraper) {
        scrapers.add(scraper);
    }

    public CompletableFuture<Void> startAsync() {
        return CompletableFuture.runAsync(this::control, service);
    }

    private void control() {
        System.out.println("Welcome to scraping service 1.0, type help to get a list of possible commands!");
        try (var sc = new Scanner(System.in)) {
            String in;
            do {
                in = sc.nextLine();
                try {
                    service.submit(command(in));
                } catch (IllegalStateException e) {
                    System.out.println(e.getMessage());
                }
            }while(!in.equals("stop_all"));
            try {
                Thread.sleep(1000);
                service.shutdown();
                service.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Scraper service finished!");
        }
    }
    //TODO decouple this into Action
    private Runnable command(String in) {
        return switch(fromString(in)) {
            case NAME -> () -> System.out.println(this);
            case HELP -> this::help;
            case INFO -> this::info;
            case START_ALL -> this::startScrapers;
            case STOP_ALL -> this::stopAll;
            case TIME -> this::time;
            case SLS -> this::printScraperNames;
            case START -> () -> actionOnScraper(parseOptions(in), WebScraper::start);
            case STOP -> () -> actionOnScraper(parseOptions(in), WebScraper::stop);
        };
    }

    private void actionOnScraper(List<String> options, Consumer<WebScraper> cons) {
        if(options.isEmpty()) return;
        for(var scraper : scrapers)
            if(scraper.toString().equals(options.get(0))) {
                cons.accept(scraper);
                return;
            }
        System.out.println("Scraper \"" + options.get(0) + "\" doesn't exist");
        printScraperNames();
    }

    private void printScraperNames() {
        System.out.println("\tscrapers:");
        scrapers.stream().map(WebScraper::toString).forEach(name -> System.out.println("\t\t" + name));
    }


    private void help() {
        for(var action : Action.values())
            System.out.println(action + "\t- " + action.info);
    }

    void time() {
        long s = Duration.between(startTime, LocalTime.now()).toSeconds();
        System.out.println("Time elapsed: " + formattedTime(s));
    }

    private String formattedTime(long s) {
        return String.format("%d hours %d minutes %d seconds", s / 3600, (s % 3600) / 60, (s % 60));
    }

    private void info() {
        System.out.println("COLLECTED DATA:");
        scrapers.stream().map(WebScraper::getInfo)
                .forEach(System.out::println);
        printAmountRunning();
    }

    private void startScrapers() {
        System.out.println("Starting all scrapers...");
        startAll();
        printAmountRunning();
    }

    private void printAmountRunning() {
        long started = scrapers.stream().filter(WebScraper::isRunning).count();
        System.out.printf("%d/%d running%n", started, scrapers.size());
    }

    void stopAll() {
        System.out.println("Stopping all scrapers...");
        CompletableFuture.runAsync(() -> scrapers.forEach(WebScraper::stop), service).join();
        time();
    }

    private void startAll() {
        scrapers.forEach(WebScraper::start);
    }


    public boolean isRunning() {
        return scrapers.stream().anyMatch(WebScraper::isRunning);
    }

    @Override
    public String toString() {
        return "ScraperService{" +
                "scrapers=" + scrapers + '}';
    }
}
