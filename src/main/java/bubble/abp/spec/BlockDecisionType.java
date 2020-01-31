package bubble.abp.spec;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BlockDecisionType {

    block, allow, filter;

    @JsonCreator public static BlockDecisionType fromString (String v) { return valueOf(v.toLowerCase()); }

}
