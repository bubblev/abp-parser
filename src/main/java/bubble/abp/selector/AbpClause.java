package bubble.abp.selector;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.ArrayUtil;

import static bubble.abp.selector.SelectorParseError.parseError;

@NoArgsConstructor @Accessors(chain=true)
@EqualsAndHashCode @ToString @Slf4j
public class AbpClause {

    @Getter @Setter private AbpClauseType type;
    @Getter @Setter private AbpContains contains;
    @Getter @Setter private AbpProperty[] properties;
    @Getter @Setter private BlockSelector selector;

    public static AbpClause buildAbpClause(AbpClauseType type, String spec) throws SelectorParseError {
        final AbpClause abp = new AbpClause().setType(type);
        if (!spec.startsWith("(") || !spec.endsWith(")")) throw parseError("expected abp clause to begin with open paren and end with close paren: "+spec);
        spec = spec.substring(1, spec.length()-1);
        switch (type) {
            case contains:
                return abp.setContains(AbpContains.build(spec));

            case properties:
                if (spec.startsWith("/") && spec.endsWith("/")) {
                    return abp.setProperties(new AbpProperty[]{
                            new AbpProperty()
                                    .setType(AbpPropertyType.regex)
                                    .setValue(spec.substring(1, spec.length()-1))
                    });
                }
                AbpProperty[] props = new AbpProperty[0];
                for (String prop : spec.split(";")) {
                    props = ArrayUtil.append(props, AbpProperty.buildProperty(prop));
                }
                return abp.setProperties(props);

            case has:
                if (!spec.startsWith(">")) {
                    log.info("expected abp-has argument to begin with > :"+spec);
                } else {
                    spec = spec.substring(1);
                }
                return abp.setSelector(BlockSelector.buildNextSelector(spec.trim()));
        }
        throw parseError("invalid abp clause type: "+spec);
    }

}
