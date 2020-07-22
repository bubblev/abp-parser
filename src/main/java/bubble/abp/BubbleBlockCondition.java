package bubble.abp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import static org.cobbzilla.util.daemon.ZillaRuntime.shortError;

@NoArgsConstructor @Accessors(chain=true) @Slf4j
public class BubbleBlockCondition {

    @Getter @Setter private BubbleBlockConditionField field;
    @Getter @Setter private BubbleBlockConditionOperation operation;
    @Getter @Setter private String value;

    public BubbleBlockCondition(String condition) {
        final int firstSpace = condition.indexOf(" ");
        if (firstSpace == -1) throw new IllegalArgumentException("BubbleBlockCondition: invalid: '"+condition+"'");
        try {
            field = BubbleBlockConditionField.fromString(condition.substring(0, firstSpace));
        } catch (Exception e) {
            throw new IllegalArgumentException("BubbleBlockCondition: invalid field: '"+condition+"': "+shortError(e));
        }
        final int secondSpace = condition.indexOf(" ", firstSpace+1);
        if (secondSpace == -1) throw new IllegalArgumentException("BubbleBlockCondition: invalid: '"+condition+"'");
        try {
            operation = BubbleBlockConditionOperation.fromString(condition.substring(firstSpace+1, secondSpace));
        } catch (Exception e) {
            throw new IllegalArgumentException("BubbleBlockCondition: invalid operation: '"+condition+"': "+shortError(e));
        }
        value = condition.substring(secondSpace+1);
        if (value.length() == 0) throw new IllegalArgumentException("BubbleBlockCondition: invalid value: '"+condition+"'");
    }

    public static BubbleBlockCondition[] parse(String[] conditions) {
        final BubbleBlockCondition[] conds = new BubbleBlockCondition[conditions.length];
        for (int i=0; i<conditions.length; i++) {
            conds[i] = new BubbleBlockCondition(conditions[i]);
        }
        return conds;
    }

    public boolean matches(String fqdn, String path, String contentType, String referer) {
        switch (field) {
            case fqdn: return operation.matches(fqdn, value);
            case path: return operation.matches(path, value);
            case url: return operation.matches(fqdn + path, value);
            case content_type: return operation.matches(contentType, value);
            case referer: return operation.matches(referer, value);
            default: log.warn("matches: invalid field: "+field);
        }
        return false;
    }
}
