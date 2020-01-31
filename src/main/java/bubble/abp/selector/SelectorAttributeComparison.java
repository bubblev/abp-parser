package bubble.abp.selector;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SelectorAttributeComparison {

    equals, startsWith, endsWith, contains;

    @JsonCreator public static SelectorAttributeComparison fromString (String v) { return valueOf(v.toLowerCase()); }

}
