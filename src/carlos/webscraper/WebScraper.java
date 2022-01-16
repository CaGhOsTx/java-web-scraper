package carlos.webscraper;

import carlos.utilities.SingleTaskService;
import carlos.webscraper.exceptions.PageWithoutLinksException;
import carlos.webscraper.exceptions.ReachedEndException;
import carlos.webscraper.parser.HTMLParser;
import carlos.webscraper.parser.link.LinkParser;
import carlos.webscraper.service.ScraperService;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import static carlos.webscraper.Option.*;
import static carlos.webscraper.parser.HTMLParser.CACHE_LIMIT;
import static java.lang.Thread.currentThread;

/**
 * Class used for scraping websites and data collection.
 * Designed for use alongside {@link ScraperService}
 * if multiple scrapers are required.
 */
public final class WebScraper implements Serializable {
    @Serial
    private static final long serialVersionUID = 5440710515833287425L;
    private static int globalID = 0;
    private final int ID;
    private Queue<String> unvisitedLinks;
    private final OptionHandler optionHandler;
    private final ContentHandler contentHandler;
    private transient SingleTaskService<WebScraper> service;
    private final String startURL;

    static {
        System.setProperty("sun.net.client.defaultReadTimeout", "5000");
        System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
    }

    WebScraper(String startURL, OptionHandler optionHandler, ContentHandler contentHandler, int nThreads) {
        this.startURL = startURL;
        this.optionHandler = optionHandler;
        this.contentHandler = contentHandler;
        unvisitedLinks = new ConcurrentLinkedQueue<>();
        service = getService(nThreads);
        ID = ++globalID;

    }

    /**
     * Starts this {@link WebScraper}.
     */
    public void start() {
        if(isRunning()) System.err.println(this + " is already running!");
        else {
            try {
                addUnvisitedLinks(getHTML(startURL), startURL);
                service.start(this);
                System.out.println(this + " STARTED");
            } catch (PageWithoutLinksException | InterruptedException e) {
                if(unvisitedLinks.isEmpty())
                    System.err.println("UNABLE TO START " + this);
                else service.start(this);
            }
        }
    }

    /**
     * Stops this {@link WebScraper}.
     */
    public void stop() {
        service.stop(this);
    }

    /**
     * Deserializes a {@link WebScraper} from the given {@link Path}.
     * @param path path to file.
     * @return Deserialized {@link WebScraper} instance from the provided {@link Path}.
     * @throws IOException if the file could not be opened for any reason.
     * @throws ClassNotFoundException if the class in the file is not a {@link WebScraper}.
     * @deprecated serialization is currently not working.
     * @see Serializable
     */
    @Deprecated
    static WebScraper deserialize(Path path) throws IOException, ClassNotFoundException {
        return  (WebScraper) new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(path.toFile()))).readObject();
    }

    /**
     * Serializes this {@link WebScraper} instance.
     * @return {@link Path} to the file it was saved to.
     * @deprecated serialization is currently not working.
     * @throws IOException if file could not be opened or created for any reason.
     * @see Serializable
     */
    @Deprecated
    private Path serialize() throws IOException {
        if(unvisitedLinks.isEmpty() && optionHandler.isPresent(DEBUG_MODE))
            System.err.println("Did not serialize, scraper reached end.");
        else {
            try {
                writeObject(new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(this.toString()))));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return Path.of("WebScraper_from " + startURL).toAbsolutePath();
    }

    boolean hasParser(HTMLParser parser) {
        return contentHandler.hasParser(parser);
    }

    @Override
    public String toString() {
        return "WebScraper_" + ID + "@" + getDomain(startURL);
    }

    /**
     * Captures the domain from the given URL.
     * @param url url to be parsed.
     * @return website domain.
     */
    private String getDomain(String url) {
        var m = Pattern.compile("(?<=\\.)([A-Za-z_]+?)(?=[.])").matcher(url);
        if(m.find()) return m.group(1);
        else throw new IllegalArgumentException("cannot parse main site");
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException, InterruptedException {
        out.defaultWriteObject();
        out.writeInt(service.size());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        service = getService(in.readInt());
    }

    /**
     * Makes this {@link WebScraper#contentHandler} save all contained {@link HTMLParser} cached data.
     * @throws IOException if any of the {@link HTMLParser}s could not be saved.
     */
    private void saveAllContent() throws IOException {
        contentHandler.saveAllContent();
        if (optionHandler.isPresent(DEBUG_MODE)) System.out.println("Content saved");
    }

    /**
     * @throws IOException if the file could not be opened or created for any reason.
     * @see LinkParser
     */
    private void saveLinks() throws IOException {
        contentHandler.saveUnvisitedLinks(this);
        contentHandler.getLinkParser().flushUnvisited(unvisitedLinks, this);
        if (optionHandler.isPresent(DEBUG_MODE))
            System.out.println("Links saved");
    }

    /**
     * Outputs to the console that this {@link WebScraper} is finalized.
     */
    private void printFinished() {
        System.out.println("-----------------");
        System.out.println(this + " FINISHED");
        System.out.println("-----------------");
    }

    /**
     * Tries to add unvisited links from the specified HTML to {@link WebScraper#unvisitedLinks}.
     * @param link link used to request this HTML. <br/>
     * (needed for {@link PageWithoutLinksException})
     * @param html page from which the links will be parsed.
     */
    private void tryAddingNewLinks(String link, String html) {
        try {
            addUnvisitedLinks(html, link);
        } catch(PageWithoutLinksException e) {
            if(optionHandler.isPresent(DEBUG_MODE)) System.err.println(e.getMessage());
        }
    }

    /**
     * Adds the specified {@link Option} to this {@link WebScraper}.
     * @param option option to be added.
     */
    public void addOption(Option option) {
        optionHandler.addOption(option);
    }

    private void printDebugMain(Set<HTMLParser> parsers) {
        for(var parser : parsers)
            if(optionHandler.isPresent(DEBUG_MODE)) {
                System.out.println(currentThread().getName());
                System.out.println("accumulated " + parser.NAME + ": " + parser.getTotal());
            }
    }

    /**
     * Retrieves the next link in {@link WebScraper#unvisitedLinks} queue.
     * @throws ReachedEndException if the queue is empty i.e. there is nowhere else to go.
     * @return the next link in sequence.
     */
    private synchronized String nextLink() throws ReachedEndException {
        if(unvisitedLinks.isEmpty()) {
            if(Files.exists(contentHandler.getLinkParser().pathToContent()))
                unvisitedLinks = contentHandler.loadUnvisitedLinks(this);
            else throw new ReachedEndException(this);
        }
        var link = unvisitedLinks.poll();
        contentHandler.addLink(link);
        printDebugNL();
        return link;
    }

    /**
     * Adds unvisited links from the specified HTML {@link String} to the {@link WebScraper#unvisitedLinks} queue.
     * @param html html to be parsed.
     * @param url link to specified HTML.
     * @throws PageWithoutLinksException if no links were parsed.
     */
    private void addUnvisitedLinks(String html, String url) throws PageWithoutLinksException {
        int UNVISITED_LINK_LIMIT = 5_000_000;
        if(unvisitedLinks.size() >= UNVISITED_LINK_LIMIT) cacheLinks();
        var links = contentHandler.getLinks(html);
        if (links.isEmpty()) throw new PageWithoutLinksException(this, url);
        links.stream().filter(contentHandler::linkNotVisited).forEach(unvisitedLinks::add);
    }

    private synchronized void cacheLinks() {
        contentHandler.getLinkParser().flushUnvisited(unvisitedLinks, this);
    }

    private void printDebugNL() {
        if(optionHandler.isPresent(DEBUG_MODE)) {
            System.out.println("visited links: " + contentHandler.getLinkParser().getTotal());
            System.out.println("unvisited links: " + unvisitedLinks.size());
        }
    }

    /**
     * Retrieves the HTML from the given URL.
     * @param url url to be used to request HTML.
     * @return HTML.
     */
    private String getHTML(String url) throws InterruptedException {
        var html = new StringBuilder(500_000);
        debugGetHTML(url);
        try (var reader = new BufferedInputStream(new URI(url).toURL().openStream())) {
            int b;
            while ((b = reader.read()) != -1)
                html.append((char) b);
        } catch (IOException | URISyntaxException e) {
            if (optionHandler.isPresent(DEBUG_MODE)) {
                System.err.println("Couldn't visit page:" + url);
                System.err.println(e.getMessage());
                ifTooManyRequestsErrorSleep(e);
            }
        }
        return html.toString();
    }

    /**
     * Makes the entrant {@link Thread} idle for 30 seconds if the target server is getting too many requests.
     * @param e {@link Exception} to be tested for code "429"
     */
    private void ifTooManyRequestsErrorSleep(Exception e) throws InterruptedException {
        if(e.getMessage().contains(" 429 ")) {
            System.out.println(this + " is sending too many requests");
            Thread.sleep(30_000);
        }
    }

    private void debugGetHTML(String url) {
        if(optionHandler.isPresent(DEBUG_MODE))
            System.out.println("visiting " + url);
    }

    /**
     * Tests if this {@link WebScraper} instance is currently running.
     * @return true if it is running.
     */
    public boolean isRunning() {
        return service.isRunning();
    }

    /**
     * @return this {@link WebScraper} instance state and collection analysis information.
     */
    public String getInfo() {
        var sb = new StringBuilder();
        appendNameAndState(sb);
        appendLinks(sb);
        appendContributions(sb);
        return sb.substring(0, sb.length() - 1);
    }

    private SingleTaskService<WebScraper> getService(int n) {
        return new SingleTaskService<>(n) {
            @Override
            public boolean condition(WebScraper webScraper) {
                return optionHandler.isPresent(UNLIMITED) || !contentHandler.allAreCollected();
            }

            @Override
            public void action(WebScraper webScraper) throws InterruptedException {
                var link = nextLink();
                String html = getHTML(link);
                if (unvisitedLinks.size() < CACHE_LIMIT)
                    tryAddingNewLinks(link, html);
                contentHandler.addAllNewContent(html);
                printDebugMain(contentHandler.getParsers());
            }

            private void signalService() {
               System.out.writeBytes("LIMIT REACHED, PLEASE STOP SERVICE WITH STOP_ALL\n".getBytes());
            }

            @Override
            public void close(WebScraper webScraper) {
                try {
                    if(contentHandler.allAreCollected()) signalService();
                    if (optionHandler.isPresent(SAVE_LINKS)) saveLinks();
                    if (optionHandler.isPresent(SAVE_PARSED_ELEMENTS)) saveAllContent();
                    if (optionHandler.isPresent(SERIALIZE_ON_CLOSE))
                        System.out.println("Scraper serialized... path to object -> " + serialize());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                printFinished();
            }
        };
    }

    private void appendLinks(StringBuilder sb) {
        sb.append("\tunvisited links: ").append(unvisitedLinks.size()).append('\n')
            .append("\tvisited links: ").append(contentHandler.getLinkParser().getTotal()).append('\n');
    }

    private void appendNameAndState(StringBuilder sb) {
        sb.append(this).append('\n')
                .append("\tstate: ").append(isRunning()? "running" : "not running").append('\n');
    }

    private void appendContributions(StringBuilder sb) {
        sb.append("\tcontributed:").append('\n');
        for(var content : contentHandler.getParsers()) {
            int contributed = contentHandler.getContributed(content);
            appendCollectionAnalysis(sb, content, contributed);
        }
    }

    private void appendCollectionAnalysis(StringBuilder sb, HTMLParser content, int contributed) {
        float percentage = (contributed / (float) Math.max(content.getTotal(), 1)) * 100;
        sb.append("\t\t").append(content).append(": ")
                .append(contributed)
                .append(" (").append(String.format("%.2f", percentage)).append("% of total)")
                .append('\n');
    }

}
