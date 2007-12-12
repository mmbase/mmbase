/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.richtext.transformers;

import org.mmbase.util.transformers.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import java.util.regex.*;

import org.mmbase.util.StringObject;
import org.mmbase.util.ResourceLoader;
import org.mmbase.util.XSLTransformer;
import org.mmbase.richtext.Mmxf;


import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Like {@link org.mmbase.util.transformers.XmlField} but adds everything related to the MMXF doctype. This means basicly that it knows how to surround &lt;mmxf /&gt;
 *
 * @author Michiel Meeuwissen
 * @version $Id: XmlField.java,v 1.2 2006-01-02 16:57:46 michiel Exp $
 * @todo   THIS CLASS NEEDS A CONCEPT! It gets a bit messy.
 */

public class XmlField extends org.mmbase.util.transformers.XmlField {

    private static final Logger log = Logging.getLoggerInstance(XmlField.class);

    // can be decoded:
    public final static int RICH     = 1;
    public final static int POOR     = 2;
    public final static int BODY     = 3;
    public final static int XML      = 4;
    public final static int WIKI     = 12;

    // default doctype
    public final static String XML_DOCTYPE = "<!DOCTYPE mmxf PUBLIC \"" + Mmxf.DOCUMENTTYPE_PUBLIC + "\" \"" + Mmxf.DOCUMENTTYPE_SYSTEM + "\">\n";


    private final static String CODING = "UTF-8"; // This class only support UTF-8 now.

    // for validation only.
    private final static String XML_HEADER = "<?xml version=\"1.0\" encoding=\"" + CODING + "\"?>\n" + XML_DOCTYPE;
    private final static String XML_TAGSTART = "<mmxf version=\"1.1\" xmlns=\"" + Mmxf.NAMESPACE + "\">";
    private final static String XML_TAGEND   = "</mmxf>";




    private static Pattern wikiWrappingAnchor = Pattern.compile("\\[(\\w+):(.*?)\\]");
    private static Pattern wikiP = Pattern.compile("<p>\\[(\\w+)\\]");
    private static Pattern wikiSection = Pattern.compile("<section><h>\\[(\\w+)\\]");
    private static Pattern wikiAnchor = Pattern.compile("\\[(\\w+)\\]");

    static {
        org.mmbase.util.Encode.register(XmlField.class.getName());
    }

    public static String wikiToXML(String data) {
        Matcher wrappingAnchors = wikiWrappingAnchor.matcher(prepareDataString(data));
        data = wrappingAnchors.replaceAll("<a id=\"$1\">$2</a>");
        StringObject obj = new StringObject(data);
        handleRich(obj, true, false, true);
        handleFormat(obj, false);
        String string = obj.toString();
        Matcher ps = wikiP.matcher(string);
        string = ps.replaceAll("<p id=\"$1\">");
        Matcher sections = wikiSection.matcher(string);
        string = sections.replaceAll("<section id=\"$1\"><h>");
        Matcher anchors = wikiAnchor.matcher(string);
        string = anchors.replaceAll("<a id=\"$1\" />");
        return string;

    }

    /**
     *  chop of the mmxf xml tagstart and tagend:
     */

    final static private String xmlBody(String s) {
        return s.substring(XML_TAGSTART.length(), s.length() - XML_TAGEND.length());
    }

    public XmlField() {
        super();
    }
    public XmlField(int to) {
        super(to);
    }

    public Map transformers() {
        Map h = super.transformers();
        h.put("MMXF_RICH",  new Config(XmlField.class, RICH,  "Converts mmxf to enriched ASCII (can be reversed)"));
        h.put("MMXF_POOR",  new Config(XmlField.class, POOR,  "Converts mmxf to enriched ASCII (inversal will not produce <br />'s"));
        h.put("MMXF_ASCII", new Config(XmlField.class, ASCII, "Converts mmxf to ASCII (cannoted be reversed)"));
        h.put("MMXF_BODY",  new Config(XmlField.class, BODY,  "Takes away the surrounding mmxf tags (returns XML)"));
        h.put("MMXF_XHTML", new Config(XmlField.class, XHTML, "Converts mnxf to piece of XHTML"));
        h.put("MMXF_MMXF",  new Config(XmlField.class, XML,   "Only validates the XML with the DTD (when decoding)"));
        return h;
    }

    public String transform(String data) {
        switch (to) {
        case RICH :
        case POOR :
            return XSLTransform("mmxf2rich.xslt", data);
        case WIKI :
            return XSLTransform("2rich.xslt", data);
        case XHTML :
            return XSLTransform("mmxf2xhtml.xslt", data);
        case BODY :
            return xmlBody(data);
        default:
            return super.transform(data);
        }
    }

    public String transformBack(String r) {
        String result = null;
        try {
            switch (to) {
            case RICH :
                result = XML_TAGSTART + richToXML(r) + XML_TAGEND;
                // rich will not be validated... Cannot be used yet!!
                break;
            case POOR :
                result = XML_TAGSTART + poorToXML(r) + XML_TAGEND;
                validate(XML_HEADER + result);
                break;
            case WIKI :
                result = XML_TAGSTART + wikiToXML(r) + XML_TAGEND;
                validate(XML_HEADER + result);
                break;
            case BODY :
                result = XML_TAGSTART + r + XML_TAGEND;
                validate(XML_HEADER + result);
                break;
            case XML :
                result = r;
                validate(XML_HEADER + result);
                break;
            default:
                result = super.transformBack(r);
            }
        } catch (FormatException fe) {
            log.error(fe.toString() + " source: \n" + result);
        }
        return result;
    }

    public String getEncoding() {
        switch (to) {
        case RICH :
            return "MMXF_RICH";
        case POOR :
            return "MMXF_POOR";
        case ASCII :
            return "MMXF_ASCII";
        case XML :
            return "MMXF_MMXF";
        default :
            return super.getEncoding();
        }
    }
}
