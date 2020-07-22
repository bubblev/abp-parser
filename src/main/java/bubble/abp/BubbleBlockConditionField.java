package bubble.abp;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BubbleBlockConditionField {

    host, path, url, referer_host, referer_url, content_type;

    @JsonCreator public static BubbleBlockConditionField fromString (String v) { return valueOf(v.toLowerCase());
}
}
