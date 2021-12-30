package carlos.webcrawler;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Content {
    LINKS("(?<=href=\"/wiki/)(?!.*:).+?(?=\")"),
    SENTENCES("[A-Z][A-Za-z0-9, ]*?[!.?]");
    final Pattern PATTERN;

    Content(String regex) {
        PATTERN = Pattern.compile(regex);
    }

    List<String> getContent(String html) {
        if(this == SENTENCES)
            html = clearTags(html);
        var matcher = PATTERN.matcher(html);
        if(!matcher.find())
            return Collections.emptyList();
        return Stream.generate(matcher::group).takeWhile(s -> matcher.find()).collect(Collectors.toList());
    }

    private String clearTags(String html) {
        return html.replaceAll("<.*?>", "");
    }

    List<String> getContent(String html, Function<String, String> htmlTransformer) {
        return getContent(htmlTransformer.apply(html));
    }
}
