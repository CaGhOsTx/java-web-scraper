package carlos.webscraper;

import java.util.regex.Pattern;

/**
 * Patterns for restricting {@link LinkParser} to a certain language.
 * <h2>Supported languages:</h2>
 * <ul>
 *     <li>ENGLISH</li>
 *     <li>CROATIAN</li>
 *     <li>GERMAN</li>
 *     <li>FRENCH</li>
 *     <li>SPANISH</li>
 *     <li>POLISH</li>
 *     <li>RUSSIAN</li>
 *     <li>UKRAINIAN</li>
 *     <li>ITALIAN</li>
 *     <li>DUTCH</li>
 * </ul>
 * For use with {@link WebScraperBuilder#restrictLanguage(LanguagePattern)}
 * @author Carlos Milkovic
 * @version 1.0
 * @see WebScraperBuilder
 */
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

    final Pattern PATTERN;

    LanguagePattern(String lang) {
        this.PATTERN = Pattern.compile(String.format("\\.%s[./]|[./]%s\\.|/%s/", lang, lang, lang));
    }

}
