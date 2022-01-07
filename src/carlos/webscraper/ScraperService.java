package carlos.webscraper;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static carlos.webscraper.Action.*;

final public class ScraperService implements Serializable {

    @Serial
    private static final long serialVersionUID = -4106870085490275339L;
    private final ExecutorService service = Executors.newCachedThreadPool();
    private final List<WebScraper> scrapers;
    private static int globalID;
    private final int id;
    private final LocalTime startTime = LocalTime.now();

    public ScraperService(List<WebScraper> scrapers) {
        this.scrapers = scrapers;
        id = ++globalID;
    }

    public ScraperService(Supplier<WebScraperBuilder> builderTemplate, String... links) {
        scrapers = new ArrayList<>(links.length);
        Arrays.stream(links).forEach(l -> scrapers.add(builderTemplate.get().setInitialURL(l).build()));
        id = ++globalID;
    }

    void actionOnScraper(List<String> options, Consumer<WebScraper> cons) {
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

    public void start() {
        control();
    }

    public ScraperService(WebScraper... scrapers) {
        this.scrapers = Arrays.stream(scrapers).toList();
        id = ++globalID;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        control();
    }

    public static ScraperService deserialize(Path path) {
        try {
            return  (ScraperService) new ObjectInputStream(new BufferedInputStream(new FileInputStream(path.toFile()))).readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException, InterruptedException {
        out.defaultWriteObject();
    }
    @Deprecated
    public Path serialize() {
        scrapers.forEach(ws -> ws.addOption(Option.SERIALIZE_ON_CLOSE));
        stop();
        try {
            writeObject(new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("ScrapingService " + id))));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return Path.of("ScrapingService " + id).toAbsolutePath();
    }

    private void control() {
        System.out.println("Welcome to scraping service 1.0, type help to get a list of possible commands!");
        try (var sc = new Scanner(System.in)) {
            String in;
            do {
                in = sc.nextLine();
                if(in.equals("kill")) {
                    scrapers.forEach(WebScraper::kill);
                    break;
                }
                try {
                    service.execute(command(in));
                } catch (IllegalStateException e) {
                    System.out.println(e.getMessage());
                }
            }while(!in.equals("stop") && !in.equals("serialize"));
        }
    }

    private Runnable command(String in) {
        return switch(fromString(in)) {
            case NAME -> () -> System.out.println(this);
            case HELP -> this::help;
            case INFO -> this::info;
            case START -> this::startScrapers;
            case STOP -> this::stop;
            case TIME -> this::time;
            case LIST -> this::printScraperNames;
            case SERIALIZE -> () -> System.out.println(serialize());
            case START_SCRAPER -> () -> actionOnScraper(parseOptions(in), WebScraper::start);
            case STOP_SCRAPER -> () -> actionOnScraper(parseOptions(in), WebScraper::stop);
            default -> throw new IllegalStateException("Unexpected value: " + fromString(in));
        };
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

    void stop() {
        System.out.println("Stopping all scrapers...");
        try {
            stopAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Scraper service finished!");
    }

    private void startAll() {
        scrapers.forEach(WebScraper::start);
    }

    public void stopAll() throws InterruptedException {
        scrapers.forEach(WebScraper::stop);
    }

    public boolean isRunning() {
        return scrapers.stream().anyMatch(WebScraper::isRunning);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScraperService that = (ScraperService) o;
        return scrapers.equals(that.scrapers) && startTime.equals(that.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scrapers, startTime);
    }

    @Override
    public String toString() {
        return "ScraperService" + id + "{" +
                "scrapers=" + scrapers +
                '}';
    }

    public void add(WebScraper scraper) {
        scrapers.add(scraper);
    }
}
