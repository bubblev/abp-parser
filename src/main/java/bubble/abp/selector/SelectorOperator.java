package bubble.abp.selector;

import com.fasterxml.jackson.annotation.JsonCreator;

import static bubble.abp.selector.SelectorParseError.parseError;

public enum SelectorOperator {

    next, encloses;

    @JsonCreator public static SelectorOperator fromString (String v) { return valueOf(v.toLowerCase()); }

    public static String setOperator(String spec, BlockSelector sel) throws SelectorParseError {
        spec = spec.trim();
        if (spec.length() == 0) throw parseError("unexpected end of spec");
        switch (spec.charAt(0)) {
            case '+':
                sel.setOperator(next);
                if (spec.charAt(1) != ' ') throw parseError("expected space after + operator: "+spec);
                spec = spec.substring(2);
                break;
            case '>':
                sel.setOperator(encloses);
                if (spec.charAt(1) != ' ') throw parseError("expected space after > operator: "+spec);
                spec = spec.substring(2);
                break;
            default:
                // non-standard syntax, roll with it, assume they mean to enclose
                sel.setOperator(encloses);
                if (!Character.isAlphabetic(spec.charAt(0))) throw parseError("expected alphabetic char: "+spec);
                break;
        }
        return spec;
    }
}
