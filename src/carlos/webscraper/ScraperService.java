package carlos.webscraper;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import static carlos.webscraper.Action.fromString;

public class ScraperService implements Serializable {

    @Serial
    private static final long serialVersionUID = -4106870085490275339L;
    private transient Thread manager = new Thread(this::control);
    private final List<WebScraper> scrapers;
    private static int globalID;
    private final int id;
    private final LocalTime startTime = LocalTime.now();

    public ScraperService(List<WebScraper> scrapers) {
        this.scrapers = scrapers;
        id = ++globalID;
        manager.start();
    }

    public ScraperService(WebScraper... scrapers) {
        this.scrapers = Arrays.stream(scrapers).toList();
        id = ++globalID;
        manager.start();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        manager = new Thread(this::control);
        manager.start();
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
                try {
                    action(fromString(in));
                } catch (IllegalStateException e) {
                    System.out.println(e.getMessage());
                }
            }while(!in.equals("stop") && !in.equals("serialize"));
        }
    }

    private void action(Action a) {
        switch(a) {
            case NAME -> System.out.println(this);
            case HELP -> help();
            case INFO -> info();
            case START -> start();
            case STOP -> stop();
            case TIME -> time();
            case SERIALIZE -> System.out.println(serialize());
        }
    }

    private void help() {
        for(var action : Action.values())
            System.out.println(action + "\t- " + action.info);
    }

    private void time() {
        long s = Duration.between(startTime, LocalTime.now()).toSeconds();
        System.out.println("Time elapsed: " + String.format("%d hours %d minutes %d seconds", s / 3600, (s % 3600) / 60, (s % 60)));
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
        System.out.println("Scraper service finished!");
    }

    private void startAll() {
        scrapers.forEach(WebScraper::start);
    }

    public void stopAll() {
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
        return manager.equals(that.manager) && scrapers.equals(that.scrapers) && startTime.equals(that.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(manager, scrapers, startTime);
    }

    @Override
    public String toString() {
        return "ScraperService{" +
                "scrapers=" + scrapers +
                '}';
    }
}
