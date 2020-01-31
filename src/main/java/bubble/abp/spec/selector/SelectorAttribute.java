package bubble.abp.spec.selector;

import lombok.*;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.collection.NameAndValue;

import static bubble.abp.spec.selector.SelectorParseError.parseError;

@NoArgsConstructor @Accessors(chain=true)
@ToString(of={"name", "comparison", "value", "style"})
@EqualsAndHashCode(of={"name", "comparison", "value", "style"})
public class SelectorAttribute {

    @Getter @Setter private String name;
    @Getter @Setter private SelectorAttributeComparison comparison;
    @Getter @Setter private String value;
    @Getter @Setter private NameAndValue[] style;

    public static SelectorAttribute buildAttribute(String spec) throws SelectorParseError {
        final int eqPos = spec.indexOf('=');
        if (eqPos == -1) throw parseError("invalid attribute ('=' not found): "+spec);
        if (eqPos == 0) throw parseError("invalid attribute (nothing precedes '='): "+spec);

        final SelectorAttribute attr = new SelectorAttribute();
        switch (spec.charAt(eqPos-1)) {
            case '^':
                attr.setComparison(SelectorAttributeComparison.startsWith);
                attr.setName(spec.substring(0, eqPos-1));
                break;
            case '$':
                attr.setComparison(SelectorAttributeComparison.endsWith);
                attr.setName(spec.substring(0, eqPos-1));
                break;
            case '*':
                attr.setComparison(SelectorAttributeComparison.contains);
                attr.setName(spec.substring(0, eqPos-1));
                break;
            default:
                attr.setComparison(SelectorAttributeComparison.equals);
                attr.setName(spec.substring(0, eqPos));
                break;
        }
        final int openQuote = spec.indexOf("\"");
        if (openQuote == -1) throw parseError("invalid attribute (expected opening double-quote char following '='): "+spec);

        final int closeQuote = spec.indexOf("\"", openQuote+1);
        if (closeQuote == -1) throw parseError("invalid attribute (expected closing double-quote char after opening double-quote char): "+spec);

        attr.setValue(spec.substring(openQuote + 1, closeQuote));
        if (attr.getName().equalsIgnoreCase("style")) {
            attr.setStyle(buildStyles(spec.substring(openQuote + 1, closeQuote)));
        }
        return attr;
    }

    private static NameAndValue[] buildStyles(String spec) throws SelectorParseError {
        NameAndValue[] styles = new NameAndValue[0];
        for (String part : spec.split(";")) {
            styles = ArrayUtil.append(styles, buildStyle(part));
        }
        return styles;
    }

    private static NameAndValue buildStyle(String style) throws SelectorParseError {
        final int colonPos = style.indexOf(':');
        if (colonPos == -1) throw parseError("invalid style (expected colon char): "+style);
        if (colonPos == 0 || colonPos == style.length()-1) throw parseError("invalid style (no name or value): "+style);
        return new NameAndValue(style.substring(0, colonPos).trim(), style.substring(colonPos+1).trim());
    }
}
