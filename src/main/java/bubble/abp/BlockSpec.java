package bubble.abp;

import bubble.abp.selector.BlockSelector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpContentTypes;
import org.cobbzilla.util.string.StringUtil;

import java.util.ArrayList;
import java.util.List;

import static bubble.abp.selector.BlockSelector.buildSelector;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.HttpContentTypes.contentType;

@Slf4j @EqualsAndHashCode(of={"target", "domainExclusions", "typeMatches", "typeExclusions", "selector"})
public class BlockSpec {

    public static final String OPT_DOMAIN_PREFIX = "domain=";
    public static final String OPT_SCRIPT = "script";
    public static final String OPT_IMAGE = "image";
    public static final String OPT_STYLESHEET = "stylesheet";

    @JsonIgnore @Getter private String line;
    @Getter private BlockTarget target;

    @Getter private List<String> domainExclusions;
    @Getter private List<String> typeMatches;

    public boolean hasTypeMatches () { return !empty(typeMatches); }

    @Getter private List<String> typeExclusions;
    @Getter private List<String> otherOptions;

    @Getter private BlockSelector selector;
    public boolean hasSelector() { return selector != null; }
    public boolean hasNoSelector() { return !hasSelector(); }

    public BlockSpec(String line, BlockTarget target, List<String> options, BlockSelector selector) {
        this.line = line;
        this.target = target;
        this.selector = selector;
        if (options != null) {
            for (String opt : options) {
                if (opt.startsWith(OPT_DOMAIN_PREFIX)) {
                    processDomainOptions(opt.substring(OPT_DOMAIN_PREFIX.length()));

                } else if (opt.startsWith("~")) {
                    final String type = opt.substring(1);
                    if (isTypeOption(type)) {
                        if (typeExclusions == null) typeExclusions = new ArrayList<>();
                        typeExclusions.add(type);
                    } else {
                        if (otherOptions == null) otherOptions = new ArrayList<>();
                        otherOptions.add(opt);
                    }

                } else {
                    if (isTypeOption(opt)) {
                        if (typeMatches == null) typeMatches = new ArrayList<>();
                        typeMatches.add(opt);
                    } else {
                        if (otherOptions == null) otherOptions = new ArrayList<>();
                        otherOptions.add(opt);
                    }
                }
            }
        }
    }

    private void processDomainOptions(String option) {
        final String[] parts = option.split("\\|");
        for (String domainOption : parts) {
            if (domainOption.startsWith("~")) {
                if (domainExclusions == null) domainExclusions = new ArrayList<>();
                domainExclusions.add(domainOption.substring(1));
            } else {
                log.warn("ignoring included domain: "+domainOption);
            }
        }
    }

    public boolean isTypeOption(String type) {
        return type.equals(OPT_SCRIPT) || type.equals(OPT_IMAGE) || type.equals(OPT_STYLESHEET);
    }

    public static List<BlockSpec> parse(String line) {

        line = line.trim();
        int optionStartPos = line.indexOf('$');
        int selectorStartPos = line.indexOf("#");

        // sanity check that selectorStartPos > optionStartPos -- $ may occur AFTER ## if the selector contains a regex
        if (selectorStartPos != -1 && optionStartPos > selectorStartPos) optionStartPos = -1;

        final List<BlockTarget> targets;
        final List<String> options;
        final String selector;
        if (optionStartPos == -1) {
            if (selectorStartPos == -1) {
                // no options, no selector, entire line is the target
                targets = BlockTarget.parseBareLine(line);
                options = null;
                selector = null;
            } else {
                // no options, but selector present. split into target + selector
                targets = BlockTarget.parse(line.substring(0, selectorStartPos));
                options = null;
                selector = line.substring(selectorStartPos);
            }
        } else {
            if (selectorStartPos == -1) {
                // no selector, split into target + options
                targets = BlockTarget.parse(line.substring(0, optionStartPos));
                options = StringUtil.splitAndTrim(line.substring(optionStartPos+1), ",");
                selector = null;
            } else {
                // all 3 elements present
                targets = BlockTarget.parse(line.substring(0, optionStartPos));
                options = StringUtil.splitAndTrim(line.substring(optionStartPos + 1, selectorStartPos), ",");
                selector = line.substring(selectorStartPos);
            }
        }
        final List<BlockSpec> specs = new ArrayList<>();
        for (BlockTarget target : targets) specs.add(new BlockSpec(line, target, options, buildSelector(selector)));
        return specs;
    }

    public boolean matches(String fqdn, String path, String contentType) {
        if (contentType == null) contentType = contentType(contentType);

        if (target.hasDomainRegex() && target.getDomainPattern().matcher(fqdn).find()) {
            return checkDomainExclusionsAndType(fqdn, contentType);

        } else if (target.hasRegex()) {
            if (target.getRegexPattern().matcher(path).find()) {
                return checkDomainExclusionsAndType(fqdn, contentType);
            }
            final String full = fqdn + path;
            if (target.getRegexPattern().matcher(full).find()) {
                return checkDomainExclusionsAndType(fqdn, contentType);
            };
        }
        return false;
    }

    public boolean checkDomainExclusionsAndType(String fqdn, String contentType) {
        if (domainExclusions != null) {
            for (String domain : domainExclusions) {
                if (domain.equals(fqdn)) return false;
            }
        }
        if (typeExclusions != null) {
            for (String type : typeExclusions) {
                switch (type) {
                    case OPT_SCRIPT:
                        if (contentType.equals(HttpContentTypes.APPLICATION_JAVASCRIPT)) return false;
                        break;
                    case OPT_IMAGE:
                        if (contentType.startsWith(HttpContentTypes.IMAGE_PREFIX)) return false;
                        break;
                    case OPT_STYLESHEET:
                        if (contentType.equals(HttpContentTypes.TEXT_CSS)) return false;
                        break;
                }
            }
        }
        if (typeMatches != null) {
            for (String type : typeMatches) {
                switch (type) {
                    case OPT_SCRIPT:
                        if (contentType.equals(HttpContentTypes.APPLICATION_JAVASCRIPT)) return true;
                        break;
                    case OPT_IMAGE:
                        if (contentType.startsWith(HttpContentTypes.IMAGE_PREFIX)) return true;
                        break;
                    case OPT_STYLESHEET:
                        if (contentType.equals(HttpContentTypes.TEXT_CSS)) return true;
                        break;
                }
            }
            return false;
        }
        return true;
    }

}
