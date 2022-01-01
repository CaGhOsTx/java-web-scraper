package carlos.webcrawler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.newBufferedWriter;

public class ContentHandler implements Serializable {
    @Serial
    private static final long serialVersionUID = 395515185246116492L;

    private ContentType link;
    private final List<ContentType> types;

    public ContentHandler(ContentType... otherContent) {
        types = Arrays.stream(otherContent).toList();
        link = LinkType.STANDARD;
    }
    
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
        return !types.stream().allMatch(ct -> ct.reachedLimit(limit));
    }

    public int getCount(ContentType ct) {
        return ct.getCollected();
    }

    public void setCustomLinkRegex(String customLinkRegex) {
        link = new LinkType(customLinkRegex);
    }
}
