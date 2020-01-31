package bubble.abp.spec;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @Accessors(chain=true)
public class BlockDecision {

    public static final BlockDecision BLOCK = new BlockDecision().setDecisionType(BlockDecisionType.block);
    public static final BlockDecision ALLOW = new BlockDecision().setDecisionType(BlockDecisionType.allow);

    @Getter @Setter BlockDecisionType decisionType = BlockDecisionType.allow;
    @Getter @Setter List<BlockSpec> specs;

    public BlockDecision add(BlockSpec spec) {
        if (specs == null) specs = new ArrayList<>();
        specs.add(spec);
        if (decisionType != BlockDecisionType.block && (spec.hasTypeMatches() || spec.hasSelector())) {
            decisionType = BlockDecisionType.filter;
        }
        return this;
    }

}
