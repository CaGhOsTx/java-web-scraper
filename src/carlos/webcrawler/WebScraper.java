package carlos.webcrawler;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static carlos.webcrawler.ContentHandler.saveContent;
import static carlos.webcrawler.Options.*;
import static java.lang.Thread.currentThread;
import static java.util.Collections.newSetFromMap;

public final class WebScraper implements Serializable {
    @Serial
    private static final long serialVersionUID = 5440710515833287425L;
    static int ID;
    private final Set<String> visitedLinks;
    private final Queue<String> unvisitedLinks;
    private final OptionHandler optionHandler;
    private final ContentHandler contentHandler;
    private transient ThreadPoolHandler threadPoolHandler;
    int dataLimit;
    private static final int LINK_CACHE_LIMIT = 1_000_000, DATA_CACHE_LIMIT = 100_000;


    WebScraper(OptionHandler optionHandler, ContentHandler contentHandler, ThreadPoolHandler threadPoolHandler, int limit) {
        this.optionHandler = optionHandler;
        this.contentHandler = contentHandler;
        this.threadPoolHandler = threadPoolHandler;
        visitedLinks = newSetFromMap(new ConcurrentHashMap<>());
        unvisitedLinks = new ConcurrentLinkedQueue<>();
        ++ID;
        dataLimit = limit;
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        threadPoolHandler.close();
        out.defaultWriteObject();
        out.writeObject(threadPoolHandler.size());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        threadPoolHandler = new ThreadPoolHandler((int) in.readObject());
        threadPoolHandler.setTask(this::main).start();
    }

    public WebScraper startFrom(String startURL) {
        try {
            addUnvisitedLinksToQueue(visitAndRetrieveHTML(startURL), startURL);
        } catch(PageWithoutLinksException e) {
            System.err.println("UNABLE TO START " + this);
        }
        threadPoolHandler.setTask(this::main).start();
        return this;
    }

    private synchronized void saveAllContent() throws IOException {
        for(var ct : contentHandler.getTypes())
            saveContent(ct);
    }

    private synchronized void flushContent() {
        for (var ct : contentHandler.getTypes())
            if (ct.getData().size() >= DATA_CACHE_LIMIT)
                saveAndClear(ct);
    }

    private synchronized void saveAndClear(ContentType ct) {
        try {
            saveContent(ct);
            ct.clearData();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void saveLinks() throws IOException {
        contentHandler.saveLinks(visitedLinks);
        contentHandler.saveLinks(unvisitedLinks);
    }

    private void printFinished() {
        System.out.println("--------------");
        System.out.println(this + " FINISHED!");
        System.out.println("--------------");
    }

    private void main() {
        while (optionHandler.isTrue(UNLIMITED) || contentHandler.notAllAreCollected(dataLimit)) {
            var link = nextLink();
            String html = visitAndRetrieveHTML(link);
            if(unvisitedLinks.size() < LINK_CACHE_LIMIT)
                tryToAddNewLinks(link, html);
            for(var ct : contentHandler.getTypes()) {
                addContentDataAndUpdateCount(html, ct);
                printDebugMain(ct);
            }
            if (threadPoolHandler.shouldStop())
                break;
        }
        System.out.println(Thread.currentThread() + " stopping");
        if(threadPoolHandler.isLastThread()) close();
    }

    private void tryToAddNewLinks(String link, String html) {
        try {
            addUnvisitedLinksToQueue(html, link);
        } catch(PageWithoutLinksException e) {
            if(optionHandler.isTrue(DEBUG_MODE)) System.err.println(e.getMessage());
        }
    }

    private synchronized void addContentDataAndUpdateCount(String html, ContentType contentType) {
        if (!contentType.reachedLimit(dataLimit))
            contentType.addData(getContent(contentType, html), dataLimit);
        flushContent();
    }

    private void printDebugMain(ContentType ct) {
        if(optionHandler.isTrue(DEBUG_MODE)) {
            System.out.println(currentThread().getName());
            System.out.println("accumulated " + ct.NAME + ": " + ct.getCollected());
        }
    }

    private synchronized String nextLink() {
        if(unvisitedLinks.isEmpty())
            throw new IllegalStateException("Reached end!" + "(" + this + ")");
        var link = unvisitedLinks.poll();
        visitedLinks.add(link);
        printDebugRL();
        return link;
    }

    private synchronized void addUnvisitedLinksToQueue(String html, String url) throws PageWithoutLinksException {
        var links = contentHandler.getLinks(html);
        if (links.isEmpty()) throw new PageWithoutLinksException(url);
        links.stream().filter(s -> !visitedLinks.contains(s))
                .forEach(unvisitedLinks::add);
    }

    private void printDebugRL() {
        if(optionHandler.isTrue(DEBUG_MODE)) {
            System.out.println("visited links: " + visitedLinks.size());
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
                if (optionHandler.isTrue(DEBUG_MODE)) {
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
        if(e.getMessage().contains("429")) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void debugGetHTML(String url) {
        if(optionHandler.isTrue(DEBUG_MODE))
            System.out.println("visiting " + url);
    }

    public synchronized Set<String> getContent(ContentType gc, String html) {
        return contentHandler.getContent(gc, html);
    }

    public void stop() {
        threadPoolHandler.close();
    }
    public void close() {
        try {
            if(optionHandler.isTrue(DEBUG_MODE)) printFinished();
            if(optionHandler.isTrue(SAVE_LINKS)) saveLinks();
            if(optionHandler.isTrue(SAVE_CONTENT)) saveAllContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "WebScraper::" + ID;
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
            return WebScraper.this + " page has no identifiable links! " + " --LINK TO PAGE -> " + link;
        }
    }
}
