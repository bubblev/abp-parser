package bubble.abp.selector;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AbpPropertyType {

    exact, wildcard, regex;

    @JsonCreator public static AbpPropertyType fromString (String v) { return valueOf(v.toLowerCase()); }

}
