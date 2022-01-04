package carlos.webscraper;

import java.io.IOException;
import java.io.Serial;
import java.util.Queue;
import java.util.Set;

import static java.nio.file.Files.newBufferedWriter;

public abstract class LinkParser extends HTMLParser {
    public static final LinkParser STANDARD = new LinkParser() {
        @Serial
        private static final long serialVersionUID = -7245878207925294233L;

        @Override
        public String pattern() {
            return "(?<=href=\")https?://[A-Za-z0-9./:_()\\[\\]{}-]+?(?=\")";
        }
    };
    @Serial
    private static final long serialVersionUID = 1303388778823614737L;
    private LanguagePattern languagePattern;

    public LinkParser() {
        super("link");
    }

    @Override
    public abstract String pattern();

    @Override
    public boolean onAddFilter(String s) {
        return languagePattern == null || languagePattern.LANG_PATTERN.matcher(s).find();
    }
    final synchronized void saveLinks(Queue<String> links) throws IOException {
        try (var w = newBufferedWriter(pathToContent(), openOption())) {
            w.write("Unvisited links:");
            for (var link : links) {
                w.write(link);
                w.newLine();
            }
            w.write("visited links");
            for(var link : data){
                w.write(link);
                w.newLine();
            }
        }
    }

    synchronized void addVisitedLink(String link, int limit) {
        int previousSize = data.size();
        data.add(link);
        if(newDataOverflowsLimit(limit)) trimToSize(limit);
        if(saving && cacheOverflowing()) resetCache();
        collected += data.size() - previousSize;
    }

    @Override
    synchronized int addData(Set<String> newData, int limit) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("only one link can be visited at a time!");
    }

    boolean contains(String link) {
        return data.contains(link);
    }

    void addLanguageFilter(LanguagePattern languagePattern) {
        this.languagePattern = languagePattern;
    }
}
