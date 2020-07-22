package bubble.abp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import static org.cobbzilla.util.daemon.ZillaRuntime.shortError;
import static org.cobbzilla.util.http.HttpSchemes.SCHEME_HTTP;
import static org.cobbzilla.util.http.HttpSchemes.SCHEME_HTTPS;

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
        if (log.isDebugEnabled()) log.debug("matches(fqdn="+fqdn+", path="+path+", contentType="+contentType+", referer="+referer+") with field="+field+", operation="+operation);
        switch (field) {
            case host:         return operation.matches(fqdn, value);
            case path:         return operation.matches(path, value);
            case url:          return operation.matches(fqdn + path, value);
            case content_type: return operation.matches(contentType, value);
            case referer_host: return operation.matches(toFqdn(referer), value);
            case referer_url:  return operation.matches(referer, value);
            default: log.warn("matches: invalid field: "+field);
        }
        return false;
    }

    private String toFqdn(String referer) {
        if (referer.startsWith(SCHEME_HTTPS)) {
            referer = referer.substring(SCHEME_HTTPS.length());
        } else if (referer.startsWith(SCHEME_HTTP)) {
            referer = referer.substring(SCHEME_HTTP.length());
        }
        final int slashPos = referer.indexOf("/");
        return slashPos == -1 ? referer : referer.substring(0, slashPos);
    }

}
