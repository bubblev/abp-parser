package bubble.abp.selector;

import com.fasterxml.jackson.annotation.JsonCreator;

import static bubble.abp.selector.SelectorParseError.parseError;

public enum SelectorType {

    id, tag, cls, id_and_cls, tag_and_cls;

    @JsonCreator public static SelectorType fromString (String v) { return valueOf(v.toLowerCase()); }

    public static String setType(String spec, BlockSelector sel) throws SelectorParseError {
        if (spec.startsWith("?")) {
            sel.setAbpEnabled(true);
            spec = spec.substring(1);
        }
        if (!spec.startsWith("#")) throw parseError("invalid prefix: "+spec);
        return setNextType(spec.substring(1), sel);
    }

    public static String setNextType(String spec, BlockSelector sel) throws SelectorParseError {
        if (spec.startsWith("#")) {
            sel.setType(id);
            spec = spec.substring(1);

        } else if (spec.startsWith(".")) {
            sel.setType(cls);
            spec = spec.substring(1);

        } else {
            sel.setType(tag);
        }
        return spec;
    }
}
