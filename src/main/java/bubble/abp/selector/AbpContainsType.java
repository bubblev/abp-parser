package bubble.abp.selector;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AbpContainsType {

    literal, regex, selector;

    @JsonCreator public static AbpContainsType fromString (String v) { return valueOf(v.toLowerCase()); }

}
