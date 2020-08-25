package bubble.abp.spec;

import bubble.abp.BlockDecision;
import bubble.abp.BlockDecisionType;
import bubble.abp.BlockList;
import bubble.abp.BlockSpec;
import org.junit.Test;

import java.util.Arrays;

import static bubble.abp.BlockListSource.WHITELIST_PREFIX;
import static org.junit.Assert.assertEquals;

public class BlockListTest {

    public static final String BLOCK = BlockDecisionType.block.name();
    public static final String ALLOW = BlockDecisionType.allow.name();
    public static final String FILTER = BlockDecisionType.filter.name();

    public static final String[][] BLOCK_TESTS = {
            // rule                  // fqdn                 // path          // expected decision

            // bare hosts example (ala EasyList)
            {"example.com",           "example.com",          "/some_path",      BLOCK},
            {"example.com",           "foo.example.com",      "/some_path",      BLOCK},
            {"example.com",           "example.org",          "/some_path",      ALLOW},

            // block example.com and all subdomains
            {"||example.com^",        "example.com",          "/some_path",      BLOCK},
            {"||example.com^",        "foo.example.com",      "/some_path",      BLOCK},
            {"||example.com^",        "example.org",          "/some_path",      ALLOW},

            // block exact string
            {"|example.com/|",        "example.com",          "/",               BLOCK},
            {"|example.com/|",        "example.com",          "/some_path",      ALLOW},
            {"|example.com/|",        "foo.example.com",      "/some_path",      ALLOW},

            // block example.com, but not foo.example.com or bar.example.com
            {"||example.com^$domain=~foo.example.com|~bar.example.com",
                    "example.com",          "/some_path",      BLOCK},
            {"||example.com^$domain=~foo.example.com|~bar.example.com",
                    "foo.example.com",      "/some_path",      ALLOW},
            {"||example.com^$domain=~foo.example.com|~bar.example.com",
                    "bar.example.com",      "/some_path",      ALLOW},
            {"||example.com^$domain=~foo.example.com|~bar.example.com",
                    "baz.example.com",      "/some_path",      BLOCK},

            // block images and scripts on example.com, but not foo.example.com or bar.example.com
            {"||example.com^$image,script,domain=~foo.example.com|~bar.example.com",
                    "example.com",            "/some_path",      BLOCK},

            // test image blocking
            {"||example.com^$image,script,domain=~foo.example.com|~bar.example.com",
                    "example.com",            "/some_path.png",  BLOCK},
            {"||example.com^$image,script,domain=~foo.example.com|~bar.example.com",
                    "foo.example.com",        "/some_path.png",  ALLOW},
            {"||example.com^$image,script,domain=~foo.example.com|~bar.example.com",
                    "bar.example.com",        "/some_path.png",  ALLOW},
            {"||example.com^$image,script,domain=~foo.example.com|~bar.example.com",
                    "baz.example.com",         "/some_path.png", BLOCK},

            // test script blocking
            {"||example.com^$image,script,domain=~foo.example.com|~bar.example.com",
                    "example.com",            "/some_path.js",   BLOCK},
            {"||example.com^$image,script,domain=~foo.example.com|~bar.example.com",
                    "foo.example.com",        "/some_path.js",   ALLOW},
            {"||example.com^$image,script,domain=~foo.example.com|~bar.example.com",
                    "bar.example.com",        "/some_path.js",   ALLOW},
            {"||example.com^$image,script,domain=~foo.example.com|~bar.example.com",
                    "baz.example.com",         "/some_path.js",  BLOCK},

            // test stylesheet blocking
            {"||example.com^stylesheet,domain=~foo.example.com|~bar.example.com",
                    "example.com",            "/some_path.css",  BLOCK},
            {"||example.com^$stylesheet,domain=~foo.example.com|~bar.example.com",
                    "foo.example.com",        "/some_path.css",  ALLOW},
            {"||example.com^$stylesheet,domain=~foo.example.com|~bar.example.com",
                    "bar.example.com",        "/some_path.css",  ALLOW},
            {"||example.com^$stylesheet,domain=~foo.example.com|~bar.example.com",
                    "baz.example.com",         "/some_path.css", BLOCK},


            // path matching
            {"/foo",                  "example.com",          "/some_path",      ALLOW},
            {"/foo",                  "example.com",          "/foo",            BLOCK},
            {"/foo",                  "example.com",          "/foo/bar",        BLOCK},

            // path matching with wildcard
            {"/foo/*/img",            "example.com",          "/some_path",      ALLOW},
            {"/foo/*/img",            "example.com",          "/foo",            ALLOW},
            {"/foo/*/img",            "example.com",          "/foo/img",        ALLOW},
            {"/foo/*/img",            "example.com",          "/foo/x/img",      BLOCK},
            {"/foo/*/img",            "example.com",          "/foo/x/img.png",  ALLOW},
            {"/foo/*/img",            "example.com",          "/foo/x/y/z//img", BLOCK},
            {"/*img*",                "example.com",          "/foo/x/y/z/img",  BLOCK},
            {"/*img*",                "example.com",          "/foo/x/y/z/img/x",BLOCK},

            // path matching with regex
            {"/foo/(apps|ads)/img.+/",  "example.com",          "/foo/x/y/z//img",      ALLOW},
            {"/foo/(apps|ads)/img.+/",  "example.com",          "/foo/apps/img.png",    BLOCK},
            {"/foo/(apps|ads)/img.+/",  "example.com",          "/foo/ads/img.png",     BLOCK},
            {"/foo/(apps|ads)/img.+/",  "example.com",          "/foo/bar/ads/img.png", ALLOW},

            {"/(apps|ads)\\.example\\.(com|org)/",
                    "example.com",          "/ad.png",              ALLOW},
            {"/(apps|ads)\\.example\\.(com|org)/",
                    "ads.example.com",      "/ad.png",              BLOCK},
            {"/(apps|ads)\\.example\\.(com|org)/",
                    "apps.example.com",     "/ad.png",              BLOCK},
            {"/(apps|ads)\\.example\\.(com|org)/",
                    "ads.example.org",      "/ad.png",              BLOCK},
            {"/(apps|ads)\\.example\\.(com|org)/",
                    "apps.example.org",     "/ad.png",              BLOCK},
            {"/(apps|ads)\\.example\\.(com|org)/",
                    "ads.example.net",      "/ad.png",              ALLOW},
            {"/(apps|ads)\\.example\\.(com|org)/",
                    "apps.example.net",     "/ad.png",              ALLOW},

            // selectors
            {"example.com##.banner-ad", "example.com",          "/ad.png",              FILTER},

            // putting it all together
            {"||example.com^$domain=~foo.example.com|~bar.example.com##.banner-ad",
                    "example.com",          "/some_path",      FILTER},
            {"||example.com^$domain=~foo.example.com|~bar.example.com##.banner-ad",
                    "baz.example.com",      "/some_path",      FILTER},
            {"||example.com^$domain=~foo.example.com|~bar.example.com##.banner-ad",
                    "foo.example.com",      "/some_path",      FILTER},
            {"||example.com^$domain=~foo.example.com|~bar.example.com##.banner-ad",
                    "bar.example.com",      "/some_path",      FILTER},
    };

    @Test public void testRules () throws Exception {
        for (String[] test : BLOCK_TESTS) {
            final BlockDecisionType expectedDecision = BlockDecisionType.fromString(test[3]);
            final BlockList blockList = new BlockList();
            blockList.addToBlacklist(BlockSpec.parse(test[0]));
            assertEquals("testBlanketBlock: expected "+expectedDecision+" decision, test=" + Arrays.toString(test),
                    expectedDecision,
                    blockList.getDecision(test[1], test[2]).getDecisionType());
        }
    }

    public String[] SELECTOR_SPECS = {
            "||example.com##.banner-ad",
            "||foo.example.com##.more-ads",
    };

    @Test public void testMultipleSelectorMatches () throws Exception {
        final BlockList blockList = new BlockList();
        for (String line : SELECTOR_SPECS) {
            blockList.addToBlacklist(BlockSpec.parse(line));
        }
        BlockDecision decision;

        decision = blockList.getDecision("example.com", "/some_path");
        assertEquals("expected filter decision", BlockDecisionType.filter, decision.getDecisionType());
        assertEquals("expected 1 filter specs", 2, decision.getSpecs().size());

        decision = blockList.getDecision("foo.example.com", "/some_path");
        assertEquals("expected filter decision", BlockDecisionType.filter, decision.getDecisionType());
        assertEquals("expected 2 filter specs", 2, decision.getSpecs().size());
    }

    public static final String[][] CONDITIONAL_SPECS = {
// rule                                                                         // fqdn        // referer      // expect
{"~foo.bar.com~[\"referer_host ne bar.com\"]",                                  "foo.com",     "foo.com",      ALLOW},
{"~foo.bar.com~[\"referer_host ne bar.com\"]",                                  "bar.com",     "bar.com",      ALLOW},
{"~foo.bar.com~[\"referer_host ne bar.com\"]",                                  "foo.bar.com", "bar.com",      ALLOW},
{"~foo.bar.com~[\"referer_host ne bar.com\"]",                                  "foo.bar.com", "foo.com",      BLOCK},
{"~foo.bar.com~[\"referer_host ne bar.com\"]",                                  "bar.com",     "foo.com",      ALLOW},

{"~foo.bar.com~[\"referer_host ne bar.com\", \"referer_host ne www.bar.com\"]", "foo.bar.com", "bar.com",      ALLOW},
{"~foo.bar.com~[\"referer_host ne bar.com\", \"referer_host ne www.bar.com\"]", "foo.bar.com", "www.bar.com",  ALLOW},
{"~foo.bar.com~[\"referer_host ne bar.com\", \"referer_host ne www.bar.com\"]", "foo.bar.com", "baz.com",      BLOCK}
// todo: add more tests for other operators, regex_find vs regex_exact, more conditions, etc
    };

    @Test public void testConditionalMatches () throws Exception {
        for (String[] test : CONDITIONAL_SPECS) {
            final BlockList blockList = new BlockList();
            final String line = test[0];
            blockList.addToBlacklist(BlockSpec.parse(line));
            final BlockDecisionType expected = BlockDecisionType.fromString(test[3]);
            final String fqdn = test[1];
            final String referer = test[2];
            assertEquals("expected "+expected+" for test: fqdn="+fqdn+", referer="+referer+" for line: "+line,
                    expected,
                    blockList.getDecision(fqdn, "/", null, referer, true).getDecisionType());
        }
    }

    public static final String[][][] WHITELIST_CONDITIONAL_SPECS = {
    // rules
    {
        {"foo.bar.com"},
        {"@@foo.bar.com/baz"},
        {"@@~foo.bar.com/quux~[\"referer_host eq bar.com\"]"},
        {"@@~foo.bar.com/quux~[\"referer_host eq foo.bar.com\"]"},
    },
    // test
    {
        // fqdn          // path      // referer      // expect
        {"foo.com",      "/",         "foo.com",      ALLOW},
        {"foo.bar.com",  "/",         "bar.com",      BLOCK},
        {"foo.bar.com",  "/foo",      "bar.com",      BLOCK},
        {"foo.bar.com",  "/baz",      "bar.com",      ALLOW},
        {"foo.bar.com",  "/quux",     "baz.com",      BLOCK},
        {"foo.bar.com",  "/quux",     "bar.com",      ALLOW},
        {"foo.bar.com",  "/quux",     "foo.bar.com",  ALLOW},
    }
    };
    @Test public void testWhitelistConditionalMatches () throws Exception {
        final BlockList blockList = new BlockList();
        for (String[] rule : WHITELIST_CONDITIONAL_SPECS[0]) {
            final String line = rule[0];
            if (line.startsWith(WHITELIST_PREFIX)) {
                blockList.addToWhitelist(BlockSpec.parse(line.substring(WHITELIST_PREFIX.length())));
            } else {
                blockList.addToBlacklist(BlockSpec.parse(line));
            }
        }
        for (String[] test : WHITELIST_CONDITIONAL_SPECS[1]) {
            final BlockDecisionType expected = BlockDecisionType.fromString(test[3]);
            final String fqdn = test[0];
            final String path = test[1];
            final String referer = test[2];
            assertEquals("expected "+expected+" for test: fqdn="+fqdn+", path="+path+", referer="+referer,
                    expected,
                    blockList.getDecision(fqdn, path, null, referer, true).getDecisionType());
        }
    }
}
