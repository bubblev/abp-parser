package bubble.abp.selector;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.StringTokenizer;

import static bubble.abp.selector.SelectorParseError.parseError;
import static org.cobbzilla.util.json.JsonUtil.jsonQuoteRegex;

@NoArgsConstructor @Accessors(chain=true)
@EqualsAndHashCode @ToString
public class AbpProperty {

    @Getter @Setter private String name;
    @Getter @Setter private AbpPropertyType type;
    @Getter @Setter private String value;

    public static AbpProperty buildProperty(String spec) throws SelectorParseError {
        final int colonPos = spec.indexOf(':');
        if (colonPos == -1) throw parseError("invalid abp property (expecting colon): "+spec);
        if (colonPos == 0) throw parseError("invalid abp property (expecting name): "+spec);
        if (colonPos == spec.length()) throw parseError("invalid abp property (expecting value): "+spec);
        String value = spec.substring(colonPos + 1);
        if (value.contains("*")) {
            final StringBuilder regex = new StringBuilder();
            final StringTokenizer st = new StringTokenizer(value, "*", true);
            while (st.hasMoreTokens()) {
                final String tok = st.nextToken();
                if (tok.equals("*")) {
                    regex.append(".+?");
                } else {
                    regex.append(jsonQuoteRegex(tok));
                }
            }
            return new AbpProperty()
                    .setName(spec.substring(0, colonPos))
                    .setType(AbpPropertyType.wildcard)
                    .setValue(regex.toString());

        } else {
            return new AbpProperty()
                    .setName(spec.substring(0, colonPos))
                    .setType(AbpPropertyType.exact)
                    .setValue(value);
        }
    }
}
