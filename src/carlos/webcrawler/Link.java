package carlos.webcrawler;

public class Link extends ContentType{

    boolean regionLocked;

    public Link(String NAME) {
        super(NAME);
        regionLocked = false;
    }

    void regionLock() {
        regionLocked = true;
    }

    @Override
    public String pattern() {
        return "(?<=href=\")https?://en\\.[A-Za-z0-9.%/:]+?(?=\")";
    }

    @Override
    public String transform(String html) {
        return html;
    }

    @Override
    public boolean onAddFilter(String s) {
        return true;
    }

    @Override
    public int limit() {
        return 0;
    }
}
