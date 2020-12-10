package bubble.abp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static bubble.abp.BlockSpec.BUBBLE_BLOCK_SPEC_PREFIX;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.HttpSchemes.stripScheme;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.json.JsonUtil.jsonQuoteRegex;
import static org.cobbzilla.util.string.ValidationRegexes.isHostname;

@NoArgsConstructor @Accessors(chain=true) @EqualsAndHashCode(of={"domainRegex", "regex"})
public class BlockTarget {

    @Getter @Setter private String fullDomainBlock;
    public boolean hasFullDomainBlock () { return fullDomainBlock != null; }

    @Getter @Setter private String partialDomainBlock;
    public boolean hasPartialDomainBlock () { return partialDomainBlock != null; }

    @Getter @Setter private String domainRegex;
    public boolean hasDomainRegex() { return !empty(domainRegex); }
    @JsonIgnore @Getter(lazy=true) private final Pattern domainPattern = hasDomainRegex() ? Pattern.compile(getDomainRegex()) : null;

    @Getter @Setter private String regex;
    public boolean hasRegex() { return !empty(regex); }
    @JsonIgnore @Getter(lazy=true) private final Pattern regexPattern = hasRegex() ? Pattern.compile(getRegex()) : null;

    @Getter @Setter private BubbleBlockCondition[] conditions;
    public boolean hasConditions () { return !empty(conditions); }

    public boolean conditionsMatch(String fqdn, String path, String contentType, String referer) {
        if (!hasConditions()) return false;
        if (!fqdn.equalsIgnoreCase(partialDomainBlock)) return false;
        if (hasRegex() && !getRegexPattern().matcher(fqdn+path).matches()) return false;
        for (BubbleBlockCondition condition : conditions) {
            if (!condition.matches(fqdn, path, contentType, referer)) return false;
        }
        return true;
    }

    public static String hostOrNull(String hostPart) {
        return isHostname(hostPart) ? hostPart : null;
    }

    public static BlockTarget parseBubbleLine(String line) {
        if (!line.startsWith(BUBBLE_BLOCK_SPEC_PREFIX)) throw new IllegalArgumentException("parseBubbleLine: invalid line, expected "+BUBBLE_BLOCK_SPEC_PREFIX+" as first char: "+line);
        line = line.substring(BUBBLE_BLOCK_SPEC_PREFIX.length());
        final int conditionsStart = line.indexOf(BUBBLE_BLOCK_SPEC_PREFIX);
        if (conditionsStart == -1) throw new IllegalArgumentException("parseBubbleLine: invalid line, expected "+BUBBLE_BLOCK_SPEC_PREFIX+" to begin conditions: "+line);

        final String data = line.substring(0, conditionsStart);
        final BlockTarget target = parseTarget(data);
        final int slash = data.indexOf("/");
        if (slash == -1) {
            target.setPartialDomainBlock(data);
        } else {
            target.setPartialDomainBlock(data.substring(0, slash));
        }
        target.setConditions(BubbleBlockCondition.parse(json(line.substring(conditionsStart + 1), String[].class)));
        return target;
    }

    public static List<BlockTarget> parse(String data) {
        final List<BlockTarget> targets = new ArrayList<>();
        for (String part : data.split(",")) {
            targets.add(parseTarget(part));
        }
        return targets;
    }

    public static List<BlockTarget> parseBareLine(final String data) {
        if (data.contains("/")) {
            final String hostPart = data.substring(0, data.indexOf("/"));
            return data.startsWith("/") ? parse(data) : parse(data).stream()
                    .map(t -> t.setPartialDomainBlock(hostOrNull(hostPart)))
                    .collect(Collectors.toList());
        } else if (data.contains("|") || data.contains("^")) {
            return parse(data);
        }

        final List<BlockTarget> targets = new ArrayList<>();
        for (String part : data.split(",")) {
            targets.add(new BlockTarget()
                    .setDomainRegex(matchDomainOrAnySubdomains(part))
                    .setFullDomainBlock(hostOrNull(part)));
        }
        return targets;
    }

    private static BlockTarget parseTarget(String data) {
        String domainRegex = null;
        String regex = null;
        String fullBlock = null;
        if (data.startsWith("||")) {
            final int caretPos = data.indexOf("^");
            final String domain;
            if (caretPos != -1) {
                // domain match
                domain = data.substring(2, caretPos);
            } else {
                domain = data.substring(2);
            }
            domainRegex = matchDomainOrAnySubdomains(domain);
            fullBlock = hostOrNull(domain);

        } else if (data.startsWith("|") && data.endsWith("|")) {
            // exact match
            final String verbatimMatch = stripScheme(data.substring(1, data.length() - 1));
            regex = "^" + jsonQuoteRegex(verbatimMatch) + "$";

        } else if (data.startsWith("/")) {
            // path match, possibly regex
            if (data.endsWith("/") && (
                    data.contains("|") || data.contains("?")
                            || (data.contains("(") && data.contains(")"))
                            || (data.contains("{") && data.contains("}")))) {
                regex = data.substring(1, data.length()-1);

            } else if (data.contains("*")) {
                regex = parseWildcardMatch(data);
            } else {
                regex = "^" + jsonQuoteRegex(data) + ".*";
            }

        } else {
            if (data.contains("*")) {
                regex = parseWildcardMatch(data);
            } else {
                regex = "^" + jsonQuoteRegex(data) + ".*";
            }
        }
        return new BlockTarget()
                .setDomainRegex(domainRegex)
                .setRegex(regex)
                .setFullDomainBlock(regex == null ? fullBlock : null);
    }

    private static String parseWildcardMatch(String data) {
        final StringBuilder b = new StringBuilder("^");
        final StringTokenizer st = new StringTokenizer(data, "*", true);
        while (st.hasMoreTokens()) {
            final String token = st.nextToken();
            b.append(token.equals("*") ? ".*?" : token);
        }
        return b.append("$").toString();
    }

    private static String matchDomainOrAnySubdomains(String domain) {
        return ".*?"+jsonQuoteRegex(domain)+"$";
    }

}
