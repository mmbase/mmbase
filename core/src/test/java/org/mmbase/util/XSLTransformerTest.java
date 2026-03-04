package org.mmbase.util;

import java.io.StringReader;
import java.io.StringWriter;
import static java.util.Objects.requireNonNull;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import junit.framework.TestCase;
import org.junit.Assert;

public class XSLTransformerTest extends TestCase {

    // Seems to be hit by https://issues.apache.org/jira/browse/XALANJ-2617
    public void testTransform() throws TransformerException {
        StringWriter sw = new StringWriter();
        Result result = new StreamResult(sw);
        XSLTransformer.transform(
            new StreamSource(new StringReader("<a><b>text\uD83D\uDC4D</b></a>")),
            new StreamSource(new StringReader("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
                "<xsl:output method=\"xml\" indent=\"yes\"/>" +
                "<xsl:template match=\"/\">" +
                "<root><xsl:value-of select=\"a/b\"/></root>" +
                "</xsl:template>" +
                "</xsl:stylesheet>")),
            result,
            null
        );
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>text\uD83D\uDC4D</root>\n", sw.toString());
    }

    /**
     * Not actually dependent on uri resolve and stuff. Just chekcing whether it will decently transform the docbook to html, and not throw an exception.
     */
    public void testDocbookBasic() {
        StringWriter sw = new StringWriter();
        Result result = new StreamResult(sw);
        try {
            XSLTransformer.transform(
                new StreamSource(requireNonNull(XSLTransformerTest.class.getResourceAsStream("/documentation.xml"))),
                new StreamSource(requireNonNull(XSLTransformerTest.class.getResourceAsStream("/org/mmbase/config/xslt/docbook2block.xslt"))),
                result,
                null
            );
            assertEquals("<div xmlns=\"http://www.w3.org/1999/xhtml\" class=\"mm_docbook\"><h1>MMBase Documentation Overview</h1><div id=\"introduction\">\n" +
                "    <h2>Introduction</h2>\n" +
                "    <p>foo</p>\n" +
                "    <p>bar</p>\n" +
                "\n" +
                "  </div></div>", sw.toString());
        } catch (TransformerException e) {
            Assert.fail("Should not throw exception, but got " + e.getMessage());
        }
    }


    /**
     * Not actually dependent on uri resolve and stuff. Just checking whether it will decently transform the docbook to html, and not throw an exception.
     */
    public void testDocbookUriResolver() {
        StringWriter sw = new StringWriter();
        Result result = new StreamResult(sw);
        try {
            XSLTransformer.transform(
                new StreamSource(requireNonNull(XSLTransformerTest.class.getResourceAsStream("/documentation.xml"))),
                new StreamSource(requireNonNull(XSLTransformerTest.class.getResourceAsStream("/org/mmbase/config/xslt/docbook2block.xslt"))),
                result,
                null
            );
            assertEquals("<div xmlns=\"http://www.w3.org/1999/xhtml\" class=\"mm_docbook\"><h1>MMBase Documentation Overview</h1><div id=\"introduction\">\n" +
                "    <h2>Introduction</h2>\n" +
                "    <p>foo</p>\n" +
                "    <p>bar</p>\n" +
                "\n" +
                "  </div></div>", sw.toString());
        } catch (TransformerException e) {
            Assert.fail("Should not throw exception, but got " + e.getMessage());
        }
    }
}
