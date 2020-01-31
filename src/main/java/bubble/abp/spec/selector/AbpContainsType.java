package bubble.abp.spec.selector;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AbpContainsType {

    literal, regex, case_insensitive_regex, selector;

    @JsonCreator public static AbpContainsType fromString (String v) { return valueOf(v.toLowerCase()); }

}
