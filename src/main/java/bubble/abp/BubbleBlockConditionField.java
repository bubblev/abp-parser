package bubble.abp;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BubbleBlockConditionField {

    fqdn, path, url, referer, content_type;

    @JsonCreator public static BubbleBlockConditionField fromString (String v) { return valueOf(v.toLowerCase());
}
}
