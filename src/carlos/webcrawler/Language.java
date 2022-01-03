package carlos.webcrawler;

enum Language {
    ENGLISH("en"),
    CROATIAN("hr"),
    GERMAN("de"),
    FRENCH("fr"),
    SPANISH("es"),
    POLISH("po"),
    RUSSIAN("ru"),
    UKRAINIAN("uk"),
    ITALIAN("it"),
    DUTCH("nl");

    final String lang;

    Language(String lang) {
        this.lang = lang;
    }
}
