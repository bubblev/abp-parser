package bubble.abp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.cobbzilla.util.http.HttpContentTypes.isHtml;

@NoArgsConstructor @Accessors(chain=true) @Slf4j
public class BlockList {

    @Getter private Set<BlockSpec> blacklist = new HashSet<>();
    @Getter private Set<BlockSpec> whitelist = new HashSet<>();

    public void addToWhitelist(BlockSpec spec) {
        whitelist.add(spec);
    }

    public void addToWhitelist(List<BlockSpec> specs) {
        for (BlockSpec spec : specs) addToWhitelist(spec);
    }

    public void addToBlacklist(BlockSpec spec) {
        blacklist.add(spec);
    }

    public void addToBlacklist(List<BlockSpec> specs) {
        for (BlockSpec spec : specs) addToBlacklist(spec);
    }

    public void merge(BlockList other) {
        for (BlockSpec allow : other.getWhitelist()) {
            addToWhitelist(allow);
        }
        for (BlockSpec block : other.getBlacklist()) {
            addToBlacklist(block);
        }
    }

    public BlockDecision getDecision(String fqdn, String path) { return getDecision(fqdn, path, null, false); }

    public BlockDecision getNonPrimaryDecision(String fqdn, String path) { return getDecision(fqdn, path, null, false); }
    public BlockDecision getPrimaryDecision(String fqdn, String path) { return getDecision(fqdn, path, null, true   ); }

    public BlockDecision getNonPrimaryDecision(String fqdn, String path, String contentType) { return getDecision(fqdn, path, contentType, false); }
    public BlockDecision getPrimaryDecision(String fqdn, String path, String contentType) { return getDecision(fqdn, path, contentType, true); }

    public BlockDecision getDecision(String fqdn, String path, boolean primary) {
        return getDecision(fqdn, path, null, primary);
    }

    public BlockDecision getDecision(String fqdn, String path, String contentType, boolean primary) {
        for (BlockSpec allow : whitelist) {
            if (allow.matches(fqdn, path, contentType)) return BlockDecision.ALLOW;
        }
        final BlockDecision decision = new BlockDecision();
        for (BlockSpec block : blacklist) {
            if (block.matches(fqdn, path, contentType)) {
                if (!block.hasSelector()) return BlockDecision.BLOCK;
                decision.add(block);

            } else if (block.hasSelector() && (!primary || isHtml(contentType))) {
                decision.add(block);
            }
        }
        return decision;
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

    @JsonIgnore public Set<String> getFullyBlockedDomains() {
        final Set<String> blockedDomains = new HashSet<>();
        final Set<BlockSpec> whitelistDomains = getWhitelistDomains();
        for (BlockSpec spec : getBlacklistDomains()) {
            if (whitelistDomains.contains(spec) || !spec.getTarget().hasFullDomainBlock()) continue;
            blockedDomains.add(spec.getTarget().getFullDomainBlock());
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
