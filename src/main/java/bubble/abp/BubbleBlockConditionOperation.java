package bubble.abp;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import org.cobbzilla.util.collection.ExpirationEvictionPolicy;
import org.cobbzilla.util.collection.ExpirationMap;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

@AllArgsConstructor
public enum BubbleBlockConditionOperation {

    eq       (String::equalsIgnoreCase),
    ne       ((input, value) -> !input.equalsIgnoreCase(value)),
    re_find  ((input, value) -> pattern(value).matcher(input).find()),
    re_exact ((input, value) -> pattern(value).matcher(input).matches()),
    contains ((input, value) -> value.contains(input));

    private static final ExpirationMap<String, Pattern> PATTERN_CACHE = new ExpirationMap<>(ExpirationEvictionPolicy.atime);
    private static Pattern pattern(String value) {
        return PATTERN_CACHE.computeIfAbsent(value, k -> Pattern.compile(k, CASE_INSENSITIVE));
    }

    private interface BubbleBlockConditionComparison { boolean matches(String input, String value); }

    private final BubbleBlockConditionComparison comparison;

    @JsonCreator public static BubbleBlockConditionOperation fromString (String v) { return valueOf(v.toLowerCase()); }

    public boolean matches(String input, String value) { return comparison.matches(input, value); }

}
