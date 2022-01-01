package carlos.webcrawler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class ContentHandler implements Serializable {
    @Serial
    private static final long serialVersionUID = 395515185246116492L;

    private final Link link = new Link("link");
    private final List<ContentType> types;

    public ContentHandler(ContentType... otherContent) {
        types = Arrays.stream(otherContent).toList();
    }
    
    Set<String> getLinks(String html) {
        return getContent(getLink(), html);
    }

    void regionLockLinks() {
        link.regionLock();
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

    void saveContent(ContentType content) throws IOException {
        var option = getOption(content);
        try (var w = Files.newBufferedWriter(content.getPath(), option)) {
            for (var data : content.getData()) {
                w.write(data);
                w.newLine();
            }
        }
    }

    void saveLinks(Collection<?> data) throws IOException {
        try (var w = Files.newBufferedWriter(link.getPath(), getOption(link))) {
            for (var d : data) {
                w.write(d.toString());
                w.newLine();
            }
        }
    }

    private StandardOpenOption getOption(ContentType ct) {
        if (Files.exists(ct.getPath()))
            return StandardOpenOption.APPEND;
        return StandardOpenOption.CREATE;
    }

    ContentType getLink() {
        return link;
    }

    List<ContentType> getTypes() {
        return types;
    }

    boolean notAllAreCollected() {
        return !types.stream().allMatch(ContentType::reachedLimit);
    }

    public int getCount(ContentType ct) {
        return ct.getCollected();
    }
}
