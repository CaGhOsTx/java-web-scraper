package carlos.webcrawler;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static carlos.webcrawler.Options.*;
import static java.lang.Thread.currentThread;
import static java.util.Collections.newSetFromMap;

public class WebScraper implements Serializable, AutoCloseable {
    @Serial
    private static final long serialVersionUID = 5440710515833287425L;
    private final Set<String> visitedLinks;
    private final Queue<String> unvisitedLinks;
    private final String LINK_PREFIX;
    private final OptionHandler<?> optionHandler;
    private final ContentHandler contentHandler;
    private transient ThreadPoolHandler threadPoolHandler;
    private boolean closed = false;


    WebScraper(String linkPrefix, OptionHandler<?> optionHandler, ContentHandler contentHandler, ThreadPoolHandler threadPoolHandler) {
        this.LINK_PREFIX = linkPrefix;
        this.optionHandler = optionHandler;
        this.contentHandler = contentHandler;
        this.threadPoolHandler = threadPoolHandler;
        visitedLinks = newSetFromMap(new ConcurrentHashMap<>());
        unvisitedLinks = new ConcurrentLinkedQueue<>();
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

    public boolean isFinished () {
        return closed;
    }

    public WebScraper startFrom(String startURL) {
        addUnvisitedLinksToQueue(visitAndRetrieveHTML(startURL));
        threadPoolHandler.setTask(this::main).start();
        return this;
    }

    private void saveAllContent() throws IOException {
        for(var ct : contentHandler.getTypes())
            contentHandler.saveContent(ct);
    }

    public Path getSavedContent(ContentType ct) {
        return contentHandler.getContentPath(ct);
    }

    private synchronized void flushContent() {
        for (var ct : contentHandler.getTypes()) {
            if (ct.getCollected() >= 1000000) {
                try {
                    saveAndClear(ct);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void saveAndClear(ContentType ct) throws IOException {
        contentHandler.saveContent(ct);
        ct.clearData();
    }

    private void saveLinks() throws IOException {
        contentHandler.saveContent(visitedLinks, contentHandler.getLink());
        contentHandler.saveContent(unvisitedLinks, contentHandler.getLink());
    }

    private void printFinished() {
        System.out.println("--------------");
        System.out.println(" FINISHED! :D");
        System.out.println("--------------");
    }

    private void main() {
        while (optionHandler.isTrue(UNLIMITED) || contentHandler.notAllAreCollected()) {
            String html = visitAndRetrieveHTML(LINK_PREFIX + nextLink());
            addUnvisitedLinksToQueue(html);
            for(var ct : contentHandler.getTypes()) {
                addContentDataAndUpdateCount(html, ct);
                printDebugMain(ct);
            }
            if (threadPoolHandler.shouldStop())
                break;
        }
    }

    private synchronized void addContentDataAndUpdateCount(String html, ContentType contentType) {
        if (!contentType.reachedLimit())
            contentType.addData(getContent(contentType, html));
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
            throw new IllegalStateException("Reached end!");
        var link = unvisitedLinks.poll();
        visitedLinks.add(link);
        printDebugRL();
        return link;
    }

    private synchronized void addUnvisitedLinksToQueue(String html) {
        contentHandler.getLinks(html).stream()
                .filter(s -> !visitedLinks.contains(s))
                .forEach(unvisitedLinks::add);
    }

    private void printDebugRL() {
        if(optionHandler.isTrue(DEBUG_MODE)) {
            System.out.println("visited links: " + visitedLinks.size());
            System.out.println("unvisited links: " + unvisitedLinks.size());
        }
    }

    private String visitAndRetrieveHTML(String url) {
        debugGetHTML(url);
        var sb = new StringBuilder();
        try (var reader = new BufferedInputStream(new URL(url).openStream())){
            int b;
            while((b = reader.read()) != -1)
                sb.append((char) b);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private void debugGetHTML(String url) {
        if(optionHandler.isTrue(DEBUG_MODE))
            System.out.println("visiting " + url);
    }

    public synchronized Set<String> getContent(ContentType gc, String html) {
        return contentHandler.getContent(gc, html);
    }

    @Override
    public void close() throws IOException {
        if(optionHandler.isTrue(DEBUG_MODE)) printFinished();
        if(optionHandler.isTrue(SAVE_LINKS)) saveLinks();
        if(optionHandler.isTrue(SAVE_CONTENT)) saveAllContent();
        closed = true;
    }
}
