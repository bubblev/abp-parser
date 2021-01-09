package bubble.abp;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.ExpirationEvictionPolicy;
import org.cobbzilla.util.collection.ExpirationMap;
import org.cobbzilla.util.string.ValidationRegexes;

import java.util.regex.Pattern;

@AllArgsConstructor @Slf4j
public enum BubbleBlockConditionOperation {

    eq       (String::equalsIgnoreCase),
    ne       ((input, value) -> !input.equalsIgnoreCase(value)),
    re_find  ((input, value) -> pattern(value).matcher(input).find()),
    re_exact ((input, value) -> pattern(value).matcher(input).matches()),
    contains ((input, value) -> value.contains(input));

    private static final ExpirationMap<String, Pattern> PATTERN_CACHE = new ExpirationMap<>(ExpirationEvictionPolicy.atime);

    private static Pattern pattern(String value) {
        return PATTERN_CACHE.computeIfAbsent(value, ValidationRegexes::safePattern);
    }

    private interface BubbleBlockConditionComparison { boolean matches(String input, String value); }

    private final BubbleBlockConditionComparison comparison;

    @JsonCreator public static BubbleBlockConditionOperation fromString (String v) { return valueOf(v.toLowerCase()); }

    public boolean matches(String input, String value) {
        final boolean matches = comparison.matches(input, value);
        if (log.isDebugEnabled()) log.debug("matches: "+input+" "+this+" "+value+" -> "+matches);
        return matches;
    }

}
