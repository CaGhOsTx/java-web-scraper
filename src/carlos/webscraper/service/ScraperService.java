package carlos.webscraper.service;

import carlos.webscraper.Option;
import carlos.webscraper.WebScraper;
import carlos.webscraper.WebScraperBuilder;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static carlos.webscraper.service.Action.*;

final public class ScraperService implements Serializable {

    @Serial
    private static final long serialVersionUID = -4106870085490275339L;
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

    public void start() {
        try {
            control();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void stop() throws InterruptedException {
        scrapers.forEach(WebScraper::stop);
    }

    public static ScraperService deserialize(Path path) {
        try {
            return  (ScraperService) new ObjectInputStream(new BufferedInputStream(new FileInputStream(path.toFile()))).readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }

    @Deprecated
    public Path serialize() {
        scrapers.forEach(ws -> ws.addOption(Option.SERIALIZE_ON_CLOSE));
        stopAll();
        try {
            writeObject(new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("ScraperService"))));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return Path.of("ScraperService").toAbsolutePath();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            control();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException, InterruptedException {
        out.defaultWriteObject();
    }

    private void control() throws InterruptedException {
        System.out.println("Welcome to scraping service 1.0, type help to get a list of possible commands!");
        try (var sc = new Scanner(System.in)) {
            String in;
            do {
                in = sc.nextLine();
                try {
                    service.execute(command(in));
                } catch (IllegalStateException e) {
                    System.out.println(e.getMessage());
                }
            }while(!in.equals("stop") && !in.equals("serialize"));
            Thread.sleep(10_000);
            service.shutdownNow();
        }
    }

    private Runnable command(String in) {
        return switch(fromString(in)) {
            case NAME -> () -> System.out.println(this);
            case HELP -> this::help;
            case INFO -> this::info;
            case START -> this::startScrapers;
            case STOP -> this::stopAll;
            case TIME -> this::time;
            case LIST -> this::printScraperNames;
            case SERIALIZE -> () -> System.out.println(serialize());
            case START_SCRAPER -> () -> actionOnScraper(parseOptions(in), WebScraper::start);
            case STOP_SCRAPER -> () -> actionOnScraper(parseOptions(in), WebScraper::stop);
            default -> throw new IllegalStateException("Unexpected value: " + fromString(in));
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
        try {
            stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Scraper service finished!");
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
