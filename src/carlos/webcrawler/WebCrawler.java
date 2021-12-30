package carlos.webcrawler;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static carlos.webcrawler.Content.LINKS;
import static carlos.webcrawler.Content.SENTENCES;
import static java.util.Collections.newSetFromMap;

public final class WebCrawler {
    private final Set<String> visitedLinks;
    private Set<String> sentences;
    private final Queue<String> unvisitedLinks;
    private final int minSize, maxSize, sentenceLimit;
    static final String PREFIX = "https://en.wikipedia.org/wiki/";
    private final String startURL;
    private final boolean debug;

    WebCrawler (String startURL, int sentenceLimit, int minSize, int maxSize, boolean debug) {
        this.startURL = startURL;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.sentenceLimit = sentenceLimit;
        this.debug = debug;
        sentences = newSetFromMap(new ConcurrentHashMap<>(sentenceLimit));
        visitedLinks = newSetFromMap(new ConcurrentHashMap<>());
        unvisitedLinks = new ConcurrentLinkedQueue<>();
    }

    public WebCrawler start(int nThreads) {
        var startingLinks = findLinks(visitAndRetrieveHTML("Main_Page"));
        allocateAndStartThreads(startingLinks, nThreads);
        return this;
    }

    private void allocateAndStartThreads(List<String> startingLinks, int nThreads) {
        for(int i = 0; i < nThreads; i++)
            new Thread(() -> main(startingLinks)).start();
    }

    public List<String> getSentences() {
        while(sentences.size() < sentenceLimit) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("--------------");
        System.out.println("FILLED LIST!");
        System.out.println("--------------");
        return sentences.stream().toList();
    }

    public void writeResultantSentencesToFile(List<String> list, Path p) throws IOException {
        if(debug) System.out.println("Writing sentences to " + p.toString());
        try(var w = Files.newBufferedWriter(p)) {
            for(String sentence : list) {
                w.write(sentence);
                w.newLine();
            }
        }
        if(debug) System.out.println("Finished writing!");
    }

    public <T> void writeResultantList (List<T> list){
        try(var ObjectOS = new ObjectOutputStream(new FileOutputStream("result"))) {
            ObjectOS.writeObject(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @SuppressWarnings("unchecked")
    public List<String> readSerialList (){
        try(var ObjectIS = new ObjectInputStream(new FileInputStream("result"))) {
            return (List<String>) ObjectIS.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private void main(List<String> startingLinks) {
        List<String> links = startingLinks;
        while (sentences.size() < sentenceLimit) {
            String html = visitAndRetrieveHTML(randomLink(links));
            sentences.addAll(findSentences(html));
            printDebugMain(debug);
            var tmp = findLinks(html);
            if(tmp.isEmpty())
                main(links);
            links = tmp;
        }
    }

    private void printDebugMain(boolean debugMode) {
        if(debugMode) {
            System.out.println(Thread.currentThread().getName());
            System.out.println("accumulated: " + sentences.size());
        }
    }

    private String randomLink(List<String> links) {
        addUnvisitedLinksToQueue(links);
        if(unvisitedLinks.isEmpty())
            throw new IllegalStateException("Reached end (links) -> " + links);
        var link = unvisitedLinks.poll();
        visitedLinks.add(link);
        printDebugRL();
        return link;
    }

    private void addUnvisitedLinksToQueue(List<String> links) {
        links.stream().filter(s -> !visitedLinks.contains(s)).forEach(unvisitedLinks::add);
    }

    private void printDebugRL() {
        if(debug) {
            System.out.println("visited links: " + visitedLinks.size());
            System.out.println("unvisited links: " + unvisitedLinks.size());
        }
    }

    private List<String> findLinks(String html) {
        return LINKS.getContent(html, this::getContentDiv);
    }

    private String visitAndRetrieveHTML(String url) {
        debugGetHTML(url);
        var sb = new StringBuilder();
        try (var reader = new BufferedInputStream(new URL(PREFIX + url).openStream())){
            int b;
            while((b = reader.read()) != -1)
                sb.append((char) b);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private void debugGetHTML(String url) {
        if(debug)
            System.out.println("visiting " + url);
    }

    public Set<String> findSentences(String html) {
        return SENTENCES.getContent(html, this::getContentDiv).stream()
                .map(s -> s.split("\\s+"))
                .filter(s -> s.length >= minSize && s.length <= maxSize)
                .map(s -> String.join(" ", s))
                .collect(Collectors.toSet());
    }

    private boolean lastWordIsLikelyAPrefix(String[] words) {
        return words[words.length - 1].length() < 4;
    }

    private String getContentDiv(String html) {
        var content = Pattern.compile("(?s)<div id=\"mw-content-text\".*<div id='mw-data-after-content'").matcher(html);
        if(content.find())
            return content.group();
        throw new IllegalStateException("Specified div could not be found");
    }
}
