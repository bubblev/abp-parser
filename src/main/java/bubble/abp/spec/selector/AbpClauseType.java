package bubble.abp.spec.selector;

import com.fasterxml.jackson.annotation.JsonCreator;

import static bubble.abp.spec.selector.SelectorParseError.parseError;

public enum AbpClauseType {

    properties, has, contains;

    public static final String ABP_CLAUSE_PREFIX = ":-abp-";

    @JsonCreator public static AbpClauseType fromString (String v) { return valueOf(v.toLowerCase()); }

    public static AbpClauseType fromSpec(String spec) throws SelectorParseError {
        if (!spec.startsWith(ABP_CLAUSE_PREFIX)) throw parseError("invalid abp clause: "+spec);
        return valueOf(spec.substring(ABP_CLAUSE_PREFIX.length()));
    }
}
