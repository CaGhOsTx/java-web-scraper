package carlos.webcrawler;

public enum Options {
    DEBUG_MODE(0),
    SAVE_LINKS(1),
    SAVE_CONTENT(2),
    UNLIMITED(3),
    RESTRICT_LANGUAGE(4),
    SERIALIZE_ON_CLOSE(5),
    LOAD_SERIALIZED(6);

    public final int position;

    Options(int position) {
        this.position = position;
    }
}
