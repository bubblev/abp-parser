package bubble.abp.spec.selector;

import lombok.*;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;

import java.util.StringTokenizer;

import static bubble.abp.spec.selector.SelectorAttributeParseState.*;
import static bubble.abp.spec.selector.SelectorParseError.parseError;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @Accessors(chain=true)
@ToString(of={"type", "abpEnabled", "name", "cls", "attributes", "abp", "operator", "next"})
@EqualsAndHashCode(of={"type", "abpEnabled", "name", "cls", "attributes", "abp", "operator", "next"})
public class BlockSelector {

    @Getter @Setter private String error;

    @Getter @Setter private SelectorType type;
    @Getter @Setter private Boolean abpEnabled;
    public boolean abpEnabled() { return abpEnabled != null && abpEnabled; }

    @Getter private String name;
    public BlockSelector setName(String n) throws SelectorParseError {
        final int dotPos = n.indexOf('.');
        if (dotPos == -1) {
            name = n;
        } else {
            if (dotPos == 0 || dotPos == n.length()-1) throw parseError("invalid name: "+n);
            if (type == null) throw parseError("type not set, cannot set name: "+n);
            switch (type) {
                case id:
                    name = n.substring(0, dotPos);
                    cls = n.substring(dotPos + 1);
                    type = SelectorType.id_and_cls;
                    break;
                case tag:
                    name = n.substring(0, dotPos);
                    cls = n.substring(dotPos + 1);
                    type = SelectorType.tag_and_cls;
                    break;
                case cls:
                    name = n;
                    cls = n;
                    break;
                default: throw parseError("cannot add class to type "+type);
            }
        }
        return this;
    }

    @Getter @Setter private String cls;
    @Getter @Setter private SelectorAttribute[] attributes;
    @Getter @Setter private AbpClause abp;
    @Getter @Setter private SelectorOperator operator;
    @Getter @Setter private BlockSelector next;

    public static BlockSelector buildSelector(String spec) throws SelectorParseError {
        if (empty(spec)) return null;

        if (spec.charAt(0) != '#') throw parseError("expected spec to begin with '#'");
        spec = spec.substring(1);

        final BlockSelector sel = new BlockSelector();
        spec = SelectorType.setType(spec, sel);

        return _buildSelector(spec, sel);
    }

    public static BlockSelector buildNextSelector(String spec) throws SelectorParseError {
        if (empty(spec)) return null;

        final BlockSelector sel = new BlockSelector();
        spec = SelectorType.setNextType(spec, sel);
        return _buildSelector(spec, sel);
    }

    private static BlockSelector _buildSelector(String spec, BlockSelector sel) throws SelectorParseError {
        boolean nameSet = false;
        int bracketPos = spec.indexOf('[');
        int spacePos = spec.indexOf(' ');
        int abpPos = spec.indexOf(":-");
        if ((spacePos != -1 && bracketPos > spacePos) || (abpPos != -1 && bracketPos > abpPos)) {
            bracketPos = -1;
        }
        if (bracketPos != -1) {
            sel.setName(spec.substring(0, bracketPos));
            spec = spec.substring(bracketPos);
            nameSet = true;
            final StringTokenizer st = new StringTokenizer(spec, "[] ", true);
            SelectorAttributeParseState state = seeking_open_bracket;
            String attrSpec = "";
            int charsConsumed = 0;
            while (st.hasMoreTokens()) {
                final String tok = st.nextToken();
                switch (tok) {
                    case "[":
                        if (state != seeking_open_bracket) throw parseError("invalid attribute (expecting open bracket): "+spec);
                        charsConsumed += tok.length();
                        state = seeking_close_bracket;
                        attrSpec = "";
                        break;

                    case "]":
                        if (state != seeking_close_bracket) throw parseError("invalid attribute (expecting close bracket): "+spec);
                        state = seeking_open_bracket;
                        if (empty(attrSpec)) throw parseError("invalid attribute: "+spec);
                        sel.attributes = ArrayUtil.append(sel.attributes, SelectorAttribute.buildAttribute(attrSpec));
                        charsConsumed += tok.length();
                        break;

                    case " ":
                        if (state == seeking_open_bracket) {
                            state = finished;
                        } else {
                            attrSpec += tok;
                            charsConsumed += tok.length();
                        }
                        break;

                    default:
                        if (state == seeking_open_bracket) {
                            state = finished;
                            break;
                        }
                        attrSpec += tok;
                        charsConsumed += tok.length();
                        break;
                }
                if (state == finished) break;
            }
            spec = spec.substring(charsConsumed);
        }
        if (spec.trim().length() == 0) {
            return sel;
        }

        abpPos = spec.indexOf(":-");
        spacePos = spec.indexOf(' ');
        if (abpPos != -1 && spacePos != -1 && abpPos > spacePos) abpPos = -1;
        if (abpPos != -1) {
            abpPos = spec.indexOf(":-");
            if (!nameSet) sel.setName(spec.substring(0, abpPos));
            nameSet = true;

            int openParen = spec.indexOf("(");
            if (openParen == -1) throw parseError("found abp clause but no open paren: "+spec);
            if (openParen == spec.length()-1) throw parseError("found abp clause but open paren has no closing paren: "+spec);
            final AbpClauseType abpClauseType = AbpClauseType.fromSpec(spec.substring(abpPos, openParen));
            spec = spec.substring(openParen);
            int nestCount = 0;
            final StringTokenizer st = new StringTokenizer(spec, "()", true);
            boolean done = false;
            final StringBuilder abpSpec = new StringBuilder();
            while (st.hasMoreTokens()) {
                final String tok = st.nextToken();
                abpSpec.append(tok);
                switch (tok) {
                    case "(":
                        nestCount++;
                        break;
                    case ")":
                        nestCount--;
                        if (nestCount == 0) done = true;
                        break;
                }
                if (done) break;
            }
            sel.setAbp(AbpClause.buildAbpClause(abpClauseType, abpSpec.toString()));
            spec = spec.substring(abpSpec.length());
        }

        if (spec.trim().length() == 0) return sel;

        if (!nameSet) {
            spacePos = spec.indexOf(' ');
            if (spacePos == -1) {
                sel.setName(spec);
                return sel;

            } else {
                sel.setName(spec.substring(0, spacePos));
                spec = spec.substring(spacePos).trim();
            }
        }

        if (!empty(spec)) {
            spec = SelectorOperator.setOperator(spec, sel);
            sel.setNext(buildNextSelector(spec));
        }
        return sel;
    }

}
