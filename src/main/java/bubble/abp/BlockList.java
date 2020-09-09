package bubble.abp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.ExpirationEvictionPolicy;
import org.cobbzilla.util.collection.ExpirationMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.cobbzilla.util.daemon.ZillaRuntime.hashOf;
import static org.cobbzilla.util.http.HttpContentTypes.isHtml;

@NoArgsConstructor @Accessors(chain=true) @Slf4j
public class BlockList {

    @Getter private final Set<String> rejectList = new HashSet<>();
    @Getter private final Set<BlockSpec> blacklist = new HashSet<>();
    @Getter private final Set<BlockSpec> whitelist = new HashSet<>();

    public void addToRejectList(String domain) { rejectList.add(domain); }
    public void addToRejectList(Collection<String> domains) { rejectList.addAll(domains); }

    public void addToBlacklist(BlockSpec spec) { blacklist.add(spec); }
    public void addToBlacklist(Collection<BlockSpec> specs) { blacklist.addAll(specs); }

    public void addToWhitelist(BlockSpec spec) { whitelist.add(spec); }
    public void addToWhitelist(Collection<BlockSpec> specs) { whitelist.addAll(specs); }

    public void merge(BlockList other) {
        addToWhitelist(other.getWhitelist());
        addToBlacklist(other.getBlacklist());
        addToRejectList(other.getRejectList());
    }

    public BlockDecision getDecision(String fqdn, String path) { return getDecision(fqdn, path, null, null, false); }

    public BlockDecision getDecision(String fqdn, String path, boolean primary) {
        return getDecision(fqdn, path, null, null, primary);
    }

    private final ExpirationMap<String, BlockDecision> decisionCache
            = new ExpirationMap<>(100, MINUTES.toMillis(10), ExpirationEvictionPolicy.atime);

    public BlockDecision getDecision(String fqdn, String path, String contentType, String referer, boolean primary) {
        final String cacheKey = hashOf(fqdn, path, contentType, referer, primary);
        return decisionCache.computeIfAbsent(cacheKey, k -> {
            for (BlockSpec allow : whitelist) {
                if (allow.matches(fqdn, path, contentType, referer)) {
                    return BlockDecision.ALLOW;
                }
            }
            final BlockDecision decision = new BlockDecision();
            for (BlockSpec block : blacklist) {
                if (block.matches(fqdn, path, contentType, referer)) {
                    if (!block.hasSelector()) return BlockDecision.BLOCK;
                    decision.add(block);

                } else if (block.hasSelector() && (!primary || isHtml(contentType))) {
                    decision.add(block);
                }
            }
            return decision;
        });
    }

    public BlockDecision getFqdnDecision(String fqdn) {
        for (BlockSpec allow : whitelist) {
            if (allow.matchesFqdn(fqdn)) return BlockDecision.ALLOW;
        }
        final BlockDecision decision = new BlockDecision();
        for (BlockSpec block : blacklist) {
            if (block.matchesFqdn(fqdn)) {
                if (!block.hasSelector()) return BlockDecision.BLOCK;
                decision.add(block);
            }
        }
        return decision;
    }

    @JsonIgnore public Set<BlockSpec> getBlacklistDomains() {
        return blacklist.stream().filter(BlockSpec::hasNoSelector).collect(Collectors.toSet());
    }

    @JsonIgnore public Set<BlockSpec> getWhitelistDomains() {
        return whitelist.stream().filter(BlockSpec::hasNoSelector).collect(Collectors.toSet());
    }

    public Set<String> getWhitelistDomainNames() {
        return getWhitelistDomains().stream()
                .filter(m -> m.getTarget().hasFullDomainBlock() || m.getTarget().hasPartialDomainBlock())
                .map(m -> m.getTarget().hasFullDomainBlock() ? m.getTarget().getFullDomainBlock() : m.getTarget().getPartialDomainBlock())
                .collect(Collectors.toSet());
    }

    @JsonIgnore public Set<String> getFullyBlockedDomains() {
        final Set<String> blockedDomains = new HashSet<>();
        final Set<BlockSpec> whitelistDomains = getWhitelistDomains();
        for (BlockSpec spec : getBlacklistDomains()) {
            if (whitelistDomains.contains(spec) || !spec.getTarget().hasFullDomainBlock()) continue;
            if (!spec.getTarget().hasConditions()) blockedDomains.add(spec.getTarget().getFullDomainBlock());
        }
        return blockedDomains;
    }

    @JsonIgnore public Set<String> getPartiallyBlockedDomains() {
        final Set<String> blockedDomains = new HashSet<>();
        final Set<BlockSpec> whitelistDomains = getWhitelistDomains();
        for (BlockSpec spec : getBlacklistDomains()) {
            if (whitelistDomains.contains(spec) || !spec.getTarget().hasPartialDomainBlock()) continue;
            blockedDomains.add(spec.getTarget().getPartialDomainBlock());
        }
        return blockedDomains;
    }

}
