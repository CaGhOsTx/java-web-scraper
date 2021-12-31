import carlos.webcrawler.*;

import java.io.IOException;
import java.util.regex.Pattern;

import static carlos.webcrawler.Options.*;

public class Main {



    public static String preprocess(String html) {
        var content = Pattern.compile("(?s)<div id=\"mw-content-text\".*<div id='mw-data-after-content'").matcher(html);
        if(content.find())
            return content.group();
        throw new IllegalStateException("Specified div could not be found");
    }

    public static void main(String[] args) throws IOException {
        ContentType link = new ContentType("link") {

            @Override
            public String pattern() {
                return "(?<=href=\"/wiki/)(?!.*:).+?(?=\")";
               // return "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)";
            }

            @Override
            public String transform(String html) {
                return preprocess(html);
            }

            @Override
            public boolean onAddFilter(String s) {
                return true;
            }

            @Override public int limit() {return 0;}
        };
        ContentType text = new ContentType("text") {

            String clearTags(String html) {
                return html.replaceAll("<.*?>", "");
            }

            @Override
            public String pattern() {
                return "[A-Z0-9][A-Za-z0-9, ]*?[!.?]";
            }
            @Override
            public String transform(String html) {
                return clearTags(preprocess(html));
            }

            @Override
            public boolean onAddFilter(String s) {
                var tmp = s.split("\\s+");
                return tmp.length >= 3 && tmp.length <= 10;
            }

            @Override public int limit() {return 10_000;}
        };
        try (var a = new WebScraperBuilder("https://en.wikipedia.org/wiki/Main_Page",
                "https://en.wikipedia.org/wiki/", link, text)
                .withOptions(SAVE_CONTENT, DEBUG_MODE, SAVE_LINKS)
                .withThreadPoolSize(5).build().start()) {
            try (var b = new WebScraperBuilder("https://en.wikipedia.org/wiki/Main_Page",
                    "https://en.wikipedia.org/wiki/", link, text)
                    .withOptions(SAVE_CONTENT, DEBUG_MODE, SAVE_LINKS)
                    .withThreadPoolSize(5).build().start()) {
                new HashComparator(b.getSavedContent(text)).start();
            }
            //new HashComparator(ws.getSavedContent(text)).start();
        }
    }
}