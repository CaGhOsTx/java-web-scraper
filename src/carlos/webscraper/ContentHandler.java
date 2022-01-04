package carlos.webscraper;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.toMap;

final class ContentHandler implements Serializable {
    @Serial
    private static final long serialVersionUID = 395515185246116492L;
    private int DATA_LIMIT;
    private LinkParser linkParser;
    private final Map<HTMLParser, Integer> types;

    ContentHandler(HTMLParser... data) {
        types = Arrays.stream(data).collect(toMap(collectable -> collectable, i -> 0));
        linkParser = LinkParser.STANDARD;
    }

    void restrictLanguage(LanguagePattern languagePattern) {
        linkParser.addLanguageFilter(languagePattern);
    }

    void setCustomLinkType(LinkParser linkParser) {
        this.linkParser = linkParser;
    }

    synchronized void addAllNewContent(String html) {
        types.replaceAll((c, v) -> types.get(c) + c.addContentFrom(html, DATA_LIMIT));
    }

    public int getContributed(HTMLParser content) {
        return types.get(content);
    }

    Set<String> getLinks(String html) {
        return linkParser.getContent(html);
    }



    synchronized void saveAllContent() throws IOException {
        for(var content : types.keySet())
            content.saveContent();
    }

    synchronized boolean linkNotVisited(String link) {
        return !this.linkParser.contains(link);
    }

    LinkParser getLinkImpl() {
        return linkParser;
    }

    Map<HTMLParser, Integer> getContentTypeContributions() {
        return types;
    }
    Set<HTMLParser> getContentTypeSet() {
        return types.keySet();
    }

    boolean notAllAreCollected() {
        return !types.keySet().stream().allMatch(content -> content.reachedLimit(DATA_LIMIT));
    }

    void addLink(String link) {
        linkParser.addVisitedLink(link, DATA_LIMIT);
    }

    void setLimit(int limit) {
        this.DATA_LIMIT = limit;
    }

    public boolean reachedLimit(HTMLParser HTMLParser) {
        return HTMLParser.reachedLimit(DATA_LIMIT);
    }
}
