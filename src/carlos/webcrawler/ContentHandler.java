package carlos.webcrawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ContentHandler {
    private ContentType link;
    private final List<ContentType> types;
    Map<ContentType, Integer> filledCount;
    private boolean unlimited;
    
    public ContentHandler(ContentType link, ContentType... otherContent) {
        this.link = link;
        types = Arrays.stream(otherContent).toList();
        filledCount = new ConcurrentHashMap<>();
        types.forEach(ct -> filledCount.put(ct, 0));
    }

    void setUnlimited () {
        unlimited = true;
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

    void addCount(ContentType ct, int surplus) {
        filledCount.put(ct, filledCount.get(ct) + surplus);
    }

    void saveContent(Collection<String> content, ContentType ct) {
        var option = setOption(ct);
        try (var w = Files.newBufferedWriter(getContentPath(ct), option)) {
            for (var c : content) {
                w.write(c);
                w.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Path getContentPath(ContentType ct) {
        return Paths.get(this + "::" + ct.NAME + ".txt");
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

    boolean notAllAreCollected(Set<ContentType> contentMap) {
        return unlimited | !contentMap.stream().allMatch(ct -> ct.isCollected(filledCount.get(ct)));
    }

    public int getCount(ContentType ct) {
        return filledCount.get(ct);
    }
}
