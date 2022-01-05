package carlos.webscraper;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import static carlos.webscraper.HTMLParser.CACHE_LIMIT;
import static carlos.webscraper.Option.*;
import static java.lang.Thread.currentThread;

public final class WebScraper implements Serializable {
    @Serial
    private static final long serialVersionUID = 5440710515833287425L;
    private static int globalID = 0;
    private final int id;
    private final Queue<String> unvisitedLinks;
    private final OptionHandler optionHandler;
    private final ContentHandler contentHandler;
    private transient ThreadHandler threadHandler;
    private final String startURL;

    WebScraper(String startURL, OptionHandler optionHandler, ContentHandler contentHandler, ThreadHandler threadHandler) {
        this.startURL = startURL;
        this.optionHandler = optionHandler;
        this.contentHandler = contentHandler;
        this.threadHandler = threadHandler;
        unvisitedLinks = new ConcurrentLinkedQueue<>();
        id = ++globalID;
    }

    public void start() {
        if(isRunning()) System.err.println(this + " is already running!");
        else {
            System.out.println(this + " STARTED");
            try {
                if(unvisitedLinks.isEmpty())
                    addUnvisitedLinksToQueue(visitAndRetrieveHTML(startURL), startURL);
                threadHandler.setTask(this::main).start();
            } catch (PageWithoutLinksException e) {
                System.err.println("UNABLE TO START " + this);
            }
        }
    }

    public void stop() {
        threadHandler.stop();
        while(isRunning()) {
            System.out.println("waiting for " + this + " to close...");
            synchronized(this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private synchronized void close() {
        try {
            if(optionHandler.isPresent(SAVE_LINKS)) saveLinks();
            if(optionHandler.isPresent(SAVE_PARSED_ELEMENTS)) saveAllContent();
            if(optionHandler.isPresent(SERIALIZE_ON_CLOSE))
                System.out.println("Scraper serialized... path to object -> " +  serialize());
        } catch (IOException e) {
            e.printStackTrace();
        }
        printFinished();
        synchronized (this) {
            this.notifyAll();
        }
    }

    static WebScraper deserialize(Path path) throws IOException, ClassNotFoundException {
        return  (WebScraper) new ObjectInputStream(new BufferedInputStream(new FileInputStream(path.toFile()))).readObject();
    }

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

    @Override
    public String toString() {
        return "WebScraper_" + id + "@" + getOriginalSite(startURL);
    }

    private String getOriginalSite(String originalURL) {
        var m = Pattern.compile("(?<=\\.)([A-Za-z_]+?)(?=[.])").matcher(originalURL);
        if(m.find()) return m.group(1);
        else throw new IllegalArgumentException("cannot parse main site");
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException, InterruptedException {
        out.defaultWriteObject();
        out.writeInt(threadHandler.size());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        threadHandler = new ThreadHandler(in.readInt());
        threadHandler.setTask(this::main);
    }

    private synchronized void saveAllContent() throws IOException {
        contentHandler.saveAllContent();
        if (optionHandler.isPresent(DEBUG_MODE)) System.out.println("Content saved");
    }

    private void saveLinks() throws IOException {
        contentHandler.getLinkParser().saveLinks(unvisitedLinks);
        if (optionHandler.isPresent(DEBUG_MODE))
            System.out.println("Links saved");
    }

    private void printFinished() {
        System.out.println("--------------");
        System.out.println(this + " FINISHED");
        System.out.println("--------------");
    }

    private void main() {
        while (optionHandler.isPresent(UNLIMITED) || contentHandler.notAllAreCollected()) {
            var link = nextLink();
            String html = visitAndRetrieveHTML(link);
            if(unvisitedLinks.size() < CACHE_LIMIT)
                tryToAddNewLinks(link, html);
            contentHandler.addAllNewContent(html);
            printDebugMain(contentHandler.getContributionsToParser());
            if (threadHandler.shouldStop())
                break;
        }
        printDebugClosingThread();
        if(threadHandler.isLastThread()) close();

    }

    private void printDebugClosingThread() {
        if(optionHandler.isPresent(DEBUG_MODE)) System.out.println(Thread.currentThread() + " stopping");
    }

    private void tryToAddNewLinks(String link, String html) {
        try {
            addUnvisitedLinksToQueue(html, link);
        } catch(PageWithoutLinksException e) {
            if(optionHandler.isPresent(DEBUG_MODE)) System.err.println(e.getMessage());
        }
    }

    public void addOption(Option option) {
        optionHandler.addOption(option);
    }

    private void printDebugMain(Set<HTMLParser> collectableDatumParsers) {
        for(var ct : collectableDatumParsers)
            if(optionHandler.isPresent(DEBUG_MODE)) {
                System.out.println(currentThread().getName());
                System.out.println("accumulated " + ct.NAME + ": " + ct.getTotal());
            }
    }

    private synchronized String nextLink() {
        if(unvisitedLinks.isEmpty()) {
            if(threadHandler.isLastThread()) close();
            throw new IllegalStateException(Thread.currentThread().getName() + " reached end!" + "(" + this + ")");
        }
        var link = unvisitedLinks.poll();
        contentHandler.addLink(link);
        printDebugRL();
        return link;
    }

    private synchronized void addUnvisitedLinksToQueue(String html, String url) throws PageWithoutLinksException {
        var links = contentHandler.getLinks(html);
        if (links.isEmpty()) throw new PageWithoutLinksException(url);
        links.stream().filter(contentHandler::linkNotVisited).forEach(unvisitedLinks::add);
    }

    private void printDebugRL() {
        if(optionHandler.isPresent(DEBUG_MODE)) {
            System.out.println("visited links: " + contentHandler.getLinkParser().getTotal());
            System.out.println("unvisited links: " + unvisitedLinks.size());
        }
    }

    private String visitAndRetrieveHTML(String url) {
        var sb = new StringBuilder();
        boolean error = true;
        while (error) {
            debugGetHTML(url);
            try (var reader = new BufferedInputStream(new URI(url).toURL().openStream())) {
                int b;
                error = false;
                while ((b = reader.read()) != -1)
                    sb.append((char) b);
            } catch (IOException | URISyntaxException e) {
                if (optionHandler.isPresent(DEBUG_MODE)) {
                    System.err.println(e.getMessage());
                    ifTooManyRequestsErrorSleep(e);
                    System.err.println("Couldn't visit page:" + url);
                }
                url = nextLink();
            }
        }
        return sb.toString();
    }

    private void ifTooManyRequestsErrorSleep(Exception e) {
        if(e.getMessage().contains(" 429 ")) {
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void debugGetHTML(String url) {
        if(optionHandler.isPresent(DEBUG_MODE))
            System.out.println("visiting " + url);
    }

    public boolean isRunning() {
        return !threadHandler.allTerminated();
    }

    public String getCollectedInfo() {
        var sb = new StringBuilder();
        appendNameAndState(sb);
        appendLinks(sb);
        appendContributions(sb);
        return sb.substring(0, sb.length() - 1);
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
        for(var content : contentHandler.getContributionsToParser()) {
            int contributed = contentHandler.getContributed(content);
            appendCollectionAnalysis(sb, content, contributed);
        }
    }

    private void appendCollectionAnalysis(StringBuilder sb, HTMLParser content, int contributed) {
        float percentage = content.getTotal() == 0 ? 0 : (contributed / (float) content.getTotal()) * 100;
        sb.append("\t\t").append(content).append(": ")
                .append(contributed)
                .append(" (").append(String.format("%.2f", percentage)).append("% of total)")
                .append('\n');
    }

    private final class PageWithoutLinksException extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 7244117343903569290L;
        private final String link;

        PageWithoutLinksException(String link) {
            this.link = link;
        }

        @Override
        public String getMessage() {
            return WebScraper.this + " page has no identifiable links! " + " LINK TO PAGE -> " + link;
        }
    }
}
