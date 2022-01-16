package carlos.webscraper.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

enum Action {

    HELP("returns a list of all possible actions on this service."),
    NAME("returns the name of this service"),
    INFO("returns current scraping information."),
    START_ALL("starts all scrapers added to this service."),
    STOP_ALL("stops all scrapers added to this service."),
    TIME("prints time elapsed since scraping started."),
    SLS("returns names of contained web scrapers"),
    START("starts the scraper with the given name", "-n scraper_name"),
    STOP("stops the scraper with the given name", "-n scraper_name");

    final String info;
    final List<String> params;

    Action(String info, String... params) {
        this.info = info;
        this.params = Arrays.stream(params).toList();
    }

    Action(String info) {
        this.info = info;
        params = Collections.emptyList();
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
