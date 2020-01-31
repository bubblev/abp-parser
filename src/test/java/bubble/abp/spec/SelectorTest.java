package bubble.abp.spec;

import bubble.abp.spec.selector.*;
import org.cobbzilla.util.collection.NameAndValue;
import org.junit.Test;

import java.io.InputStream;

import static bubble.abp.spec.selector.SelectorAttributeComparison.*;
import static bubble.abp.spec.selector.SelectorOperator.encloses;
import static bubble.abp.spec.selector.SelectorOperator.next;
import static bubble.abp.spec.selector.SelectorType.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.shortError;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SelectorTest {

    public static final Object[][] SELECTOR_TESTS = new Object[][] {
            {"###foo", new BlockSelector().setType(id).setName("foo")},
            {"##.foo", new BlockSelector().setType(cls).setName("foo")},
            {"##foo", new BlockSelector().setType(tag).setName("foo")},

            {"##foo > .bar", new BlockSelector().setType(tag).setName("foo").setOperator(encloses)
                    .setNext(new BlockSelector().setType(cls).setName("bar"))},
            {"##foo + .bar", new BlockSelector().setType(tag).setName("foo").setOperator(next)
                    .setNext(new BlockSelector().setType(cls).setName("bar"))},

            {"##foo > bar", new BlockSelector().setType(tag).setName("foo").setOperator(encloses)
                    .setNext(new BlockSelector().setType(tag).setName("bar"))},
            {"##foo + bar", new BlockSelector().setType(tag).setName("foo").setOperator(next)
                    .setNext(new BlockSelector().setType(tag).setName("bar"))},

            {"##.foo[href=\"bar\"]", new BlockSelector().setType(cls).setName("foo").setAttributes(new SelectorAttribute[] {
                    new SelectorAttribute().setName("href").setComparison(equals).setValue("bar")
            })},
            {"##.foo[href^=\"bar\"]", new BlockSelector().setType(cls).setName("foo").setAttributes(new SelectorAttribute[] {
                    new SelectorAttribute().setName("href").setComparison(startsWith).setValue("bar")
            })},
            {"##.foo[href$=\"bar\"]", new BlockSelector().setType(cls).setName("foo").setAttributes(new SelectorAttribute[] {
                    new SelectorAttribute().setName("href").setComparison(endsWith).setValue("bar")
            })},
            {"##.foo[href*=\"bar\"]", new BlockSelector().setType(cls).setName("foo").setAttributes(new SelectorAttribute[] {
                    new SelectorAttribute().setName("href").setComparison(contains).setValue("bar")
            })},

            {"##.foo[href=\"bar\"][target^=\"_blank\"]", new BlockSelector().setType(cls).setName("foo").setAttributes(new SelectorAttribute[] {
                    new SelectorAttribute().setName("href").setComparison(equals).setValue("bar"),
                    new SelectorAttribute().setName("target").setComparison(startsWith).setValue("_blank")
            })},

            {"##.foo[href=\"bar\"][target^=\"_blank\"][style*=\"width:300px;height:100px\"]", new BlockSelector().setType(cls).setName("foo").setAttributes(new SelectorAttribute[] {
                    new SelectorAttribute().setName("href").setComparison(equals).setValue("bar"),
                    new SelectorAttribute().setName("target").setComparison(startsWith).setValue("_blank"),
                    new SelectorAttribute().setName("style").setComparison(contains).setValue("width:300px;height:100px")
                            .setStyle(new NameAndValue[] {
                            new NameAndValue("width", "300px"),
                            new NameAndValue("height", "100px")
                    })
            })},

            {"##foo > .bar[src*=\"advert\"][height=\"30\"] + img > #ad_foo[src$=\".png\"]", new BlockSelector().setType(tag).setName("foo")
                    .setOperator(encloses)
                    .setNext(new BlockSelector().setType(cls).setName("bar").setAttributes(
                    new SelectorAttribute[] {
                            new SelectorAttribute().setName("src").setComparison(contains).setValue("advert"),
                            new SelectorAttribute().setName("height").setComparison(equals).setValue("30")
                    })
                    .setOperator(next)
                    .setNext(new BlockSelector().setType(tag).setName("img")
                            .setOperator(encloses)
                            .setNext(new BlockSelector().setType(id).setName("ad_foo").setAttributes(
                                    new SelectorAttribute[] {
                                            new SelectorAttribute().setName("src").setComparison(endsWith).setValue(".png")
                                    }))
                    ))
            },
    };

    public static final Object[][] ABP_TESTS = new Object[][] {
            {"#?#div:-abp-properties(width:300px;height:250px;)", new BlockSelector()
                    .setAbpEnabled(true).setType(tag).setName("div").setAbp(
                    new AbpClause()
                            .setType(AbpClauseType.properties)
                            .setProperties(new AbpProperty[] {
                                    new AbpProperty().setName("width").setType(AbpPropertyType.exact).setValue("300px"),
                                    new AbpProperty().setName("height").setType(AbpPropertyType.exact).setValue("250px")
                            }))
            },

            {"#?#div:-abp-has(> div > img.advert)", new BlockSelector()
                    .setAbpEnabled(true).setType(tag).setName("div").setAbp(
                            new AbpClause()
                    .setType(AbpClauseType.has)
                    .setSelector(new BlockSelector().setType(tag).setName("div").setOperator(encloses)
                            .setNext(new BlockSelector().setType(tag_and_cls).setName("img").setCls("advert"))))},

            {"#?#div:-abp-has(> span:-abp-contains(Advertisement))", new BlockSelector()
                    .setAbpEnabled(true).setType(tag).setName("div").setAbp(
                    new AbpClause().setType(AbpClauseType.has)
                            .setSelector(new BlockSelector().setType(tag).setName("span").setAbp(
                                    new AbpClause().setType(AbpClauseType.contains).setContains(
                                            new AbpContains().setType(AbpContainsType.literal).setValue("Advertisement")
                                    ))))},

            {"#?#div > img:-abp-properties(/width: 3[2-8]px;/)", new BlockSelector()
            .setAbpEnabled(true).setType(tag).setName("div").setOperator(encloses).setNext(
                    new BlockSelector().setType(tag).setName("img").setAbp(
                            new AbpClause().setType(AbpClauseType.properties).setProperties(new AbpProperty[]{
                                    new AbpProperty().setType(AbpPropertyType.regex).setValue("width: 3[2-8]px;")
                            })))},

            {"#?#.panel:-abp-contains(a[href*=\"example.com\"])", new BlockSelector()
            .setAbpEnabled(true).setType(cls).setName("panel").setAbp(
                    new AbpClause().setType(AbpClauseType.contains).setContains(
                            new AbpContains().setType(AbpContainsType.selector).setSelector(
                                    new BlockSelector().setType(tag).setName("a").setAttributes(new SelectorAttribute[]{
                                            new SelectorAttribute().setName("href").setComparison(contains).setValue("example.com")
                                    }))))}
    };

    public static final Object[][] ODD_TESTS = new Object[][] {
            {"#?#.cls-content span[id^=\"i-\"]:-abp-contains(Automatic updates)", new BlockSelector()
            .setAbpEnabled(true).setType(cls).setName("cls-content").setOperator(encloses).setNext(
                    new BlockSelector().setType(tag).setName("span").setAttributes(new SelectorAttribute[]{
                            new SelectorAttribute().setName("id").setComparison(startsWith).setValue("i-")
                    }).setAbp(new AbpClause().setType(AbpClauseType.contains).setContains(
                            new AbpContains().setType(AbpContainsType.literal).setValue("Automatic updates")
                    ))
            )},

            {"#?#b:-abp-has(a[target^=\"reimage\"])", new BlockSelector()
                    .setAbpEnabled(true).setType(tag).setName("b").setAbp(
                            new AbpClause().setType(AbpClauseType.has).setSelector(
                                    new BlockSelector().setType(tag).setName("a").setAttributes(new SelectorAttribute[]{
                                            new SelectorAttribute().setName("target").setComparison(startsWith).setValue("reimage")
                                    }))
            )},

            {"##div[style^=\"float: none; margin:10px \"]", new BlockSelector()
            .setType(tag).setName("div").setAttributes(new SelectorAttribute[]{
                    new SelectorAttribute().setName("style").setComparison(startsWith)
                            .setValue("float: none; margin:10px ").setStyle(new NameAndValue[]{
                            new NameAndValue("float", "none"),
                            new NameAndValue("margin", "10px")
                    })
            })},

            {"#?#.cls-content ol:-abp-contains(Download Foo)", new BlockSelector()
                    .setAbpEnabled(true).setType(cls).setName("cls-content").setOperator(encloses).setNext(
                    new BlockSelector().setType(tag).setName("ol").setAbp(
                            new AbpClause().setType(AbpClauseType.contains).setContains(
                                    new AbpContains().setType(AbpContainsType.literal).setValue("Download Foo")
                            )))}
    };
    @Test public void testSelectorParsing () throws Exception { runTests(SELECTOR_TESTS); }
    @Test public void testAbpParsing () throws Exception { runTests(ABP_TESTS); }
    @Test public void testOddParsing () throws Exception { runTests(ODD_TESTS); }

    private void runTests(Object[][] tests) throws SelectorParseError {
        for (Object[] test : tests) {
            final String spec = test[0].toString();
            final BlockSelector sel = BlockSelector.buildSelector(spec);
            assertEquals("BlockSelector object is incorrect for spec: "+spec, test[1], sel);
        }
    }

    @Test public void testComplexList () throws Exception {
        try {
            final BlockListSource source = new BlockListSource() {
                @Override public InputStream getUrlInputStream() { return loadResourceAsStream("AntiMalwareABP.txt"); }
            }.download();
            assertEquals("error parsing some lines", 0, source.getBlockList().getWhitelist().size());
            assertEquals("error parsing some lines", 553, source.getBlockList().getBlacklist().size());
            System.out.println("saved lists!");
        } catch (Exception e) {
            fail("testComplexList: badness: "+shortError(e));
        }
    }

}
