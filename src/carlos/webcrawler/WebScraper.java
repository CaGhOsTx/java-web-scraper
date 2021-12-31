package carlos.webcrawler;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static carlos.webcrawler.Options.*;
import static java.util.Collections.newSetFromMap;

public class WebScraper implements Serializable {
    private final Set<String> visitedLinks;
    private final Map<ContentType, Set<String>> contentMap;
    private final Queue<String> unvisitedLinks;
    private final String LINK_PREFIX;
    private final String startURL;
    private final OptionHandler<?> optionHandler;
    private final ContentHandler contentHandler;
    private final ThreadPoolHandler threadPoolHandler;


    WebScraper(String startURL, String linkPrefix, OptionHandler<?> optionHandler, ContentHandler contentHandler, ThreadPoolHandler threadPoolHandler) {
        this.startURL = startURL;
        this.LINK_PREFIX = linkPrefix;
        this.optionHandler = optionHandler;
        this.contentHandler = contentHandler;
        this.threadPoolHandler = threadPoolHandler;
        contentMap = initialiseContentMap();
        visitedLinks = newSetFromMap(new ConcurrentHashMap<>());
        unvisitedLinks = new ConcurrentLinkedQueue<>();
    }

    private Map<ContentType, Set<String>> initialiseContentMap() {
        var map = new ConcurrentHashMap<ContentType, Set<String>>();
        for(var ct : contentHandler.getTypes())
            map.put(ct, newSetFromMap(new ConcurrentHashMap<>()));
        return map;
    }

    public static WebScraper load(Path p) throws IOException, ClassNotFoundException {
        try(var ois = new ObjectInputStream(Files.newInputStream(p))) {
            return (WebScraper) ois.readObject();
        }
    }

    public WebScraper start() {
        addLinks(visitAndRetrieveHTML(startURL));
        threadPoolHandler.setTask(this::main);
        waitForProcessToFinish();
        if(optionHandler.isTrue(DEBUG_MODE)) printFinished();
        if(optionHandler.isTrue(SAVE_LINKS)) saveLinks();
        if(optionHandler.isTrue(SAVE_CONTENT)) saveAllContent();
        return this;
    }

    private void waitForProcessToFinish() {
        while(!threadPoolHandler.allTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            flushContent();
        }
    }

    private void saveAllContent() {
        for(var ct : contentMap.keySet())
            contentHandler.saveContent(contentMap.get(ct), ct);
    }

    public Path getSavedContent(ContentType ct) {
        return contentHandler.getContentPath(ct);
    }

    private void flushContent() {
        for (var ct : contentMap.keySet()) {
            var contentData = contentMap.get(ct);
            if (contentHandler.getCount(ct) >= 1000000) {
                contentHandler.saveContent(contentData, ct);
                contentData.clear();
            }
        }
    }

    private void saveLinks() {
        contentHandler.saveContent(visitedLinks, contentHandler.getLink());
        contentHandler.saveContent(unvisitedLinks, contentHandler.getLink());
    }

    private void printFinished() {
        System.out.println("--------------");
        System.out.println("FINISHED! :D");
        System.out.println("--------------");
    }

    public void writeObject(String fileName) {
        try(var oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void main() {
        while (contentHandler.notAllAreCollected(contentMap.keySet())) {
            String html = visitAndRetrieveHTML(LINK_PREFIX + nextLink());
            addLinks(html);
            for(var ct : contentMap.keySet()) {
                addContentDataAndUpdateCount(html, ct);
                printDebugMain(ct);
            }
        }
    }

    private synchronized void addContentDataAndUpdateCount(String html, ContentType ct) {
        var contentData = contentMap.get(ct);
        int countBef = contentData.size();
        if (contentData.size() < ct.limit()) {
            contentData.addAll(addContent(ct, html));
            contentHandler.addCount(ct, contentMap.get(ct).size() - countBef);
        }
    }

    private void printDebugMain(ContentType ct) {
        if(optionHandler.isTrue(DEBUG_MODE)) {
            System.out.println(Thread.currentThread().getName());
            System.out.println("accumulated " + ct.NAME + ": " + contentHandler.getCount(ct));
        }
    }

    public synchronized void addLinks(String html) {
        addUnvisitedLinksToQueue(addContent(contentHandler.getLink(), html));
    }

    private synchronized String nextLink() {
        if(unvisitedLinks.isEmpty())
            throw new IllegalStateException("Reached end!");
        var link = unvisitedLinks.poll();
        visitedLinks.add(link);
        printDebugRL();
        return link;
    }

    private synchronized void addUnvisitedLinksToQueue(Set<String> links) {
        links.stream().filter(s -> !visitedLinks.contains(s)).forEach(unvisitedLinks::add);
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

    public synchronized Set<String> addContent(ContentType gc, String html) {
        return contentHandler.getContent(gc, html);
    }
}
