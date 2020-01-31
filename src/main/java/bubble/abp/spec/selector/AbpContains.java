package bubble.abp.spec.selector;

import lombok.*;
import lombok.experimental.Accessors;

@NoArgsConstructor @Accessors(chain=true)
@EqualsAndHashCode @ToString
public class AbpContains {

    @Getter @Setter private String value;
    @Getter @Setter private AbpContainsType type;
    @Getter @Setter private BlockSelector selector;

    public static AbpContains build(String spec) throws SelectorParseError {
        if (spec.startsWith("/") && (spec.endsWith("/") || spec.endsWith("/i"))) {
            return new AbpContains()
                    .setValue(spec.substring(1, spec.length()-1))
                    .setType(spec.endsWith("/i") ? AbpContainsType.case_insensitive_regex : AbpContainsType.regex);
        }
        if (spec.contains("[") && spec.contains("]")) {
            return new AbpContains()
                    .setSelector(BlockSelector.buildNextSelector(spec))
                    .setType(AbpContainsType.selector);
        }
        return new AbpContains().setType(AbpContainsType.literal).setValue(spec);
    }
}
