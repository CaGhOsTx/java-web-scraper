package carlos.webcrawler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class ContentHandler implements Serializable {
    @Serial
    private static final long serialVersionUID = 395515185246116492L;
    private final ContentType link;
    private final List<ContentType> types;

    public ContentHandler(ContentType link, ContentType... otherContent) {
        this.link = link;
        types = Arrays.stream(otherContent).toList();
    }
    
    Set<String> getLinks(String html) {
        return getContent(getLink(), html);
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
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

    void saveContent(ContentType ct) throws IOException {
        var option = setOption(ct);
        try (var w = Files.newBufferedWriter(getContentPath(ct), option)) {
            for (var data : ct.getData()) {
                w.write(data);
                w.newLine();
            }
        }
    }

    void saveContent(Collection<?> data, ContentType ct) throws IOException {
        var option = setOption(ct);
        try (var w = Files.newBufferedWriter(getContentPath(ct), option)) {
            for (var d : data) {
                w.write(d.toString());
                w.newLine();
            }
        }
    }

    Path getContentPath(ContentType ct) {
        return Paths.get(ct.getClass() + "@" + ct.hashCode() + "::" + ct.NAME + ".txt");
    }

    private StandardOpenOption setOption(ContentType ct) {
        if (Files.exists(getContentPath(ct)))
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
