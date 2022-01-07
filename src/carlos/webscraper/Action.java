package carlos.webscraper;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

enum Action {

    HELP("returns a list of all possible actions on this service."),
    NAME("returns the name of this service"),
    INFO("returns current scraping information."),
    START("starts all scrapers added to this service."),
    STOP("stops all scrapers added to this service."),
    TIME("prints time elapsed since scraping started."),
    @Deprecated
    SERIALIZE("stops and serializes this service."),
    LIST("returns names of contained web scrapers"),
    START_SCRAPER("starts the scraper defined by given options"),
    STOP_SCRAPER("stops the scraper with the given name"),
    KILL("shuts this service down forcefully");

    final String info;
    Consumer<ScraperService> consumer;
    BiConsumer<ScraperService, List<String>> multiConsumer;

    Action(String info) {
        this.info = info;
    }

    static Action fromString(String s) {
        s = s.split(" ")[0];
        for(var action : values()) {
            if(s.equalsIgnoreCase(action.toString()))
                return action;
        }
        throw new IllegalStateException("Action " + s + " doesn't exist, type help to get a list of actions.");
    }

    public static List<String> parseOptions(String s) {
        if(!s.contains("-")) {
            System.out.println("Missing -, Query syntax: action -o1 -o2 -o3 ...");
            return Collections.emptyList();
        }
        return Pattern.compile("-(.+) ?").matcher(s).results().map(m -> m.group(1)).toList();
    }
}
