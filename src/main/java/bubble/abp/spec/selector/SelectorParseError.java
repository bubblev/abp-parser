package bubble.abp.spec.selector;

public class SelectorParseError extends RuntimeException {

    public SelectorParseError(String s) { super(s); }

    public static SelectorParseError parseError(String s) { return new SelectorParseError(s); }

}
