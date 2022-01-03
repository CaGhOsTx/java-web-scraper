package carlos.webcrawler;

import java.io.*;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static carlos.webcrawler.StandardContentType.LINK;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.stream.Collectors.toMap;

public class ContentHandler implements Serializable {
    @Serial
    private static final long serialVersionUID = 395515185246116492L;

    private ContentType link;
    private final Map<ContentType, Integer> types;

    ContentHandler(ContentType... otherContent) {
        types = Arrays.stream(otherContent).collect(toMap(ct -> ct, i -> 0));
        link = LINK.get();
    }

    public void setCustomLinkRegex(String customLinkRegex) {
        link = new ContentType("link") {
            @Serial
            private static final long serialVersionUID = 1513148274869536319L;

            @Override
            public String pattern() {
                return customLinkRegex;
            }
        };
    }

    public void addData(ContentType ct, String html) {}

    Set<String> getLinks(String html) {
        return getContent(getLink(), html);
    }

    Set<String> getContent(ContentType ct, String html) {
        var matcher = ct.getPATTERN().matcher(ct.transform(html));
        Set<String> content = new HashSet<>();
        while(matcher.find()) {
            var group = matcher.group();
            if(ct.onAddFilter(group))
                content.add(group);
        }
        return content;
    }

    synchronized static void saveContent(ContentType content) throws IOException {
        if(content.getData().size() > 0) {
            var option = getOption(content);
            try (var w = newBufferedWriter(content.getPath(), option)) {
                for (var data : content.getData()) {
                    w.write(data);
                    w.newLine();
                }
            }
        }
    }

    synchronized void saveLinks(Collection<?> data) throws IOException {
        try (var w = newBufferedWriter(link.getPath(), getOption(link))) {
            for (var d : data) {
                w.write(d.toString());
                w.newLine();
            }
        }
    }

    private static StandardOpenOption getOption(ContentType ct) {
        if (exists(ct.getPath()))
            return StandardOpenOption.APPEND;
        return StandardOpenOption.CREATE;
    }

    ContentType getLink() {
        return link;
    }

    List<ContentType> getTypes() {
        return types;
    }

    boolean notAllAreCollected(int limit) {
        return !types.stream().allMatch(content -> content.reachedLimit(limit));
    }
}
