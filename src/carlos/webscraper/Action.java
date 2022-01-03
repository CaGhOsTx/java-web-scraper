package carlos.webscraper;

enum Action {
    HELP("returns a list of all possible actions on this service."),
    NAME("returns the name of this service"),
    INFO("returns current scraping information."),
    START("starts all scrapers added to this service."),
    STOP("stops all scrapers added to this service."),
    TIME("prints time elapsed since scraping started."),
    SERIALIZE("stops and serializes this service.");

    final String info;

    Action(String info) {
        this.info = info;
    }

    static Action fromString(String s) {
        for(var action : values()) {
            if(s.equalsIgnoreCase(action.toString()))
                return action;
        }
        throw new IllegalStateException("Action " + s + " doesn't exist, type help to get a list of actions.");
    }
}
