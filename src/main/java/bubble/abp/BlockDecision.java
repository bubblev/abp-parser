package bubble.abp;

import bubble.abp.selector.BlockSelector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @Accessors(chain=true)
public class BlockDecision {

    public static final BlockDecision BLOCK = new BlockDecision().setDecisionType(BlockDecisionType.block);
    public static final BlockDecision ALLOW = new BlockDecision().setDecisionType(BlockDecisionType.allow);

    @Getter @Setter BlockDecisionType decisionType = BlockDecisionType.allow;
    @Getter @Setter List<BlockSpec> specs;
    public boolean hasSpecs () { return !empty(specs); }

    public BlockDecision add(BlockSpec spec) {
        if (specs == null) specs = new ArrayList<>();
        specs.add(spec);
        if (decisionType != BlockDecisionType.block && (spec.hasTypeMatches() || spec.hasSelector())) {
            decisionType = BlockDecisionType.filter;
        }
        return this;
    }

    @JsonIgnore public Set<BlockSelector> getSelectors() { return BlockSelector.getSelectors(specs); }

    @Override public String toString () {
        return "BlockDecision{"+decisionType+(hasSpecs() ? ", "+specs.size()+" specs" : "")+"}";
    }
}
