package carlos.webscraper;

import java.util.regex.Pattern;

public enum LanguagePattern {
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

    final Pattern LANG_PATTERN;

    LanguagePattern(String lang) {
        this.LANG_PATTERN = Pattern.compile(String.format("\\.%s[./]|[./]%s\\.|/%s/", lang, lang, lang));
    }

}
