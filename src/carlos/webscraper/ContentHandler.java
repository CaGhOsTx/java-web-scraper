package carlos.webscraper;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;

import static carlos.webscraper.HTMLParser.saveContent;
import static java.util.stream.Collectors.toMap;

final class ContentHandler implements Serializable {
    @Serial
    private static final long serialVersionUID = 395515185246116492L;
    private int DATA_LIMIT = 1_000_000;
    private LinkParser linkParser;
    private final Map<HTMLParser, Integer> contributionsToParser;

    ContentHandler(HTMLParser... data) {
        contributionsToParser = Arrays.stream(data).collect(toMap(collectable -> collectable, i -> 0));
        linkParser = LinkParser.STANDARD();
    }

    void restrictLanguage(LanguagePattern languagePattern) {
        linkParser.addLanguageFilter(languagePattern);
    }

    void setCustomLinkParser(LinkParser linkParser) {
        this.linkParser = linkParser;
    }

    synchronized void addAllNewContent(String html) {
        contributionsToParser.replaceAll(appendNewContributions(html));
    }

    void setContentSaving(boolean saving) {
        for(var parser : contributionsToParser.keySet())
            parser.setShouldSave(saving);
    }

    private BiFunction<HTMLParser, Integer, Integer> appendNewContributions(String html) {
        return (parser, contribution) -> contributionsToParser.get(parser) + parser.addContentFrom(html, DATA_LIMIT);
    }

    public int getContributed(HTMLParser content) {
        return contributionsToParser.get(content);
    }

    Set<String> getLinks(String html) {
        return linkParser.getContent(html);
    }



    synchronized void saveAllContent() throws IOException {
        for(var parser : contributionsToParser.keySet())
            saveContent(parser);
    }

    synchronized boolean linkNotVisited(String link) {
        return !this.linkParser.alreadyVisited(link);
    }

    LinkParser getLinkParser() {
        return linkParser;
    }

    Set<HTMLParser> getContributionsToParser() {
        return contributionsToParser.keySet();
    }

    boolean notAllAreCollected() {
        return !contributionsToParser.keySet().stream().allMatch(content -> content.reachedLimit(DATA_LIMIT));
    }

    void addLink(String link) {
        linkParser.addVisitedLink(link, DATA_LIMIT);
    }

    void setLimit(int limit) {
        DATA_LIMIT = limit;
    }

    public boolean reachedLimit(HTMLParser HTMLParser) {
        return HTMLParser.reachedLimit(DATA_LIMIT);
    }

}
