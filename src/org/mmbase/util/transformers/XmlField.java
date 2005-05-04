package org.mmbase.util.transformers;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.mmbase.util.StringObject;
import org.mmbase.util.ResourceLoader;


import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * XMLFields in MMBase. This class can encode such a field to several other formats.
 *
 * @author Michiel Meeuwissen
 * @version $Id: XmlField.java,v 1.30 2005-05-04 22:46:01 michiel Exp $
 * @todo   THIS CLASS NEEDS A CONCEPT! It gets a bit messy.
 */

public class XmlField extends ConfigurableStringTransformer implements CharTransformer {

    private static final Logger log = Logging.getLoggerInstance(XmlField.class);

    // can be decoded:
    public final static int RICH     = 1;
    public final static int POOR     = 2;
    public final static int BODY     = 3;
    public final static int XML      = 4;
    public final static int POORBODY = 5;
    public final static int RICHBODY = 6;

    // cannot yet be encoded even..
    public final static int HTML_INLINE    = 7;
    public final static int HTML_BLOCK     = 8;
    public final static int HTML_BLOCK_BR  = 9;
    public final static int HTML_BLOCK_NOSURROUNDINGP     = 10;
    public final static int HTML_BLOCK_BR_NOSURROUNDINGP  = 11;

    // default doctype
    public final static String XML_DOCTYPE = "<!DOCTYPE mmxf PUBLIC \"-//MMBase//DTD mmxf 1.0//EN\" \"http://www.mmbase.org/dtd/mmxf_1_0.dtd\">\n";

    // cannot be decoded:
    public final static int ASCII = 51;
    public final static int XHTML = 52;

    private final static String CODING = "UTF-8"; // This class only support UTF-8 now.

    // for validation only.
    private final static String XML_HEADER = "<?xml version=\"1.0\" encoding=\"" + CODING + "\"?>\n" + XML_DOCTYPE;
    private final static String XML_TAGSTART = "<mmxf>";
    private final static String XML_TAGEND   = "</mmxf>";

    public final static boolean isXmlEncoded(String s) {
        return s.startsWith(XML_TAGSTART) && s.endsWith(XML_TAGEND);
    }


    /**
     * Takes a string object, finds list structures and changes those to XML
     */

    private static void handleList(StringObject obj) {
        // handle lists
        // make <ul> possible (not yet nested), with -'s on the first char of line.
        int inList = 0; // if we want nesting possible, then an integer (rather then boolean) will be handy
        int pos = 0;
        if (obj.length() < 3) {
            return;
        }
        if (obj.charAt(0) == '-' && obj.charAt(1) != '-') { // hoo, we even _start_ with a list;
            obj.insert(0, "\n"); // in the loop \n- is deleted, so it must be there.
        } else {
            while (true) {
                pos = obj.indexOf("\n-", pos); // search the first
                if (pos == -1 || obj.length() <= pos + 2) break;
                if (obj.charAt(pos + 2) != '-') break;
                pos += 2;
            }
        }

        listwhile : while (pos != -1) {
            if (inList == 0) { // not yet in list
                inList++; // now we are
                obj.delete(pos, 2); // delete \n-
                // remove spaces..
                while (pos < obj.length() && obj.charAt(pos) == ' ') {
                    obj.delete(pos, 1);
                }
                obj.insert(pos, "\r<ul>\r<li>"); // insert 10 chars.
                pos += 10;

            } else { // already in list
                if (obj.charAt(pos + 1) != '-') { // end of list
                    obj.delete(pos, 1); // delete \n
                    obj.insert(pos, "</li>\r</ul>\n");
                    pos += 12;
                    inList--;
                } else { // not yet end
                    obj.delete(pos, 2); // delete \n-
                    // remove spaces..
                    while (pos < obj.length() && obj.charAt(pos) == ' ')
                        obj.delete(pos, 1);
                    obj.insert(pos, "</li>\r<li>");
                    pos += 10;
                }
            }
            if (inList > 0) { // search for new line
                pos = obj.indexOf("\n", pos);
                if (pos == -1)
                    break; // no new line found? End of list, of text.
                if (pos + 1 == obj.length()) {
                    obj.delete(pos, 1);
                    break; // if end of text, simply remove the newline.
                }
                while (obj.charAt(pos + 1) == ' ') {
                    // if next line starts with space, this new line does not count. This makes it possible to have some formatting in a <li>
                    pos = obj.indexOf("\n", pos + 1);
                    if (pos + 1 == obj.length()) {
                        obj.delete(pos, 1);
                        break listwhile; // nothing to do...
                    }
                }
            } else { // search for next list
                while (true) {
                    pos = obj.indexOf("\n-", pos);
                    if (pos == -1 || obj.length() <= pos + 2) break;
                    if (obj.charAt(pos + 2) != '-') break; // should not start with two -'s, because this is some seperation line
                    pos += 2;
                }
            }
        }
        // make sure that the list is closed:
        while (inList > 0) { // lists in lists not already supported, but if we will...
            obj.insert(obj.length(), "</li></ul>\n");
            inList--; // always finish with a new line, it might be needed for the finding of paragraphs.
        }

    }
    /**
     * If you want to add a _ in your text, that should be possible too...
     * Should be done last, because no tags can appear in <em>
     */
    // test cases:
    // I cite _m_pos_! -> <mmxf><p>I cite <em>m_pos</em>!</p></mmxf>

    private static void handleEmph(StringObject obj) {

        obj.replace("__", "&#95;"); // makes it possible to escape underscores

        // Emphasizing. This is perhaps also asking for trouble, because
        // people will try to use it like <font> or other evil
        // things. But basicly emphasizion is content, isn't it?

        int posEmphOpen = obj.indexOf("_", 0);
        int posTagOpen = obj.indexOf("<", 0); // must be closed before next tag opens.


        OUTER:
        while (posEmphOpen != -1) {

            if (posTagOpen > 0 &&
                posTagOpen < posEmphOpen) { // ensure that we are not inside existing tags
                int posTagClose = obj.indexOf(">", posTagOpen);
                if (posTagClose == -1) break;
                posEmphOpen = obj.indexOf("_", posTagClose);
                posTagOpen  = obj.indexOf("<", posTagClose);
                continue;
            }

            if (posEmphOpen + 1 >= obj.length()) break; // no use, nothing can follow

            if ((posEmphOpen > 0 && Character.isLetterOrDigit(obj.charAt(posEmphOpen - 1))) ||
                (! Character.isLetterOrDigit(obj.charAt(posEmphOpen + 1)))) {
                // _ is inside a word, ignore that.
                // or not starting a word
                posEmphOpen = obj.indexOf("_", posEmphOpen + 1);
                continue;
            }

            // now find closing _.
            int posEmphClose = obj.indexOf("_", posEmphOpen + 1);
            if (posEmphClose == -1) break;
            while((posEmphClose + 1) < obj.length() &&
                  (Character.isLetterOrDigit(obj.charAt(posEmphClose + 1)))
                  ) {
                posEmphClose = obj.indexOf("_", posEmphClose + 1);
                if (posEmphClose == -1) break OUTER;
            }

            if (posTagOpen > 0
                && posEmphClose > posTagOpen) {
                posEmphOpen = obj.indexOf("_", posTagOpen); // a tag opened before emphasis close, ignore then too, and re-search
                continue;
            }

            // realy do replacing now
            obj.delete(posEmphClose, 1);
            obj.insert(posEmphClose,"</em>");
            obj.delete(posEmphOpen, 1);
            obj.insert(posEmphOpen, "<em>");
            posEmphClose += 7;

            posEmphOpen = obj.indexOf("_", posEmphClose);
            posTagOpen  = obj.indexOf("<", posEmphClose);

        }

        obj.replace("&#95;", "_");
    }

    /**
     * Some paragraphs are are really \sections. So this handler can
     * be done after handleParagraphs. It will search the paragraphs
     * which are really headers, and changes them. A header, in our
     * 'rich' text format, is a paragraph starting with one or more $.
     * If there are more then one, the resulting <section> tags are
     * going to be nested.
     *
     */

    private static void handleHeaders(StringObject obj) {
        // handle headers
        int requested_level;
        char ch;
        int level = 0; // start without being in section.
        int pos = obj.indexOf("<p>$", 0);
        while (pos != -1) {
            obj.delete(pos, 4); // remove <p>$

            requested_level = 1;
            // find requested level:
            while (true) {
                ch = obj.charAt(pos);
                if (ch == '$') {
                    requested_level++;
                    obj.delete(pos, 1);
                } else {
                    if (ch == ' ') {
                        obj.delete(pos, 1);
                    }
                    break;
                }
            }
            StringBuffer add = new StringBuffer();
            for (; requested_level <= level; level--) {
                // same or higher level section
                add.append("</section>");
            }
            level++;
            for (; requested_level > level; level++) {
                add.append("<section>");
            }
            add.append("<section><h>");

            obj.insert(pos, add.toString());
            pos += add.length();

            // search end title of  header;

            while (true) { // oh yes, and don't allow _ in title.
                int pos1 = obj.indexOf("_", pos);
                int pos2 = obj.indexOf("</p>", pos);
                if (pos1 < pos2 && pos1 > 0) {
                    obj.delete(pos1, 1);
                } else {
                    pos = pos2;
                    break;
                }
            }
            if (pos == -1)
                break; // not found, could not happen.
            // replace it.
            obj.delete(pos, 4);
            obj.insert(pos, "</h>");
            pos += 2;
            pos = obj.indexOf("<p>$", pos); // search the next one.
        }
        // ready, close all sections still open.
        for (; level > 0; level--) {
            obj.insert(obj.length(), "</section>");
        }

    }


    /**
     * Make <p> </p> tags.
     * @param leaveExtraNewLines (defaults to false) if false, 2 or more newlines starts a new p. If true, every 2 newlines starts new p, and every extra new line simply stays (inside the p).
     * @param surroundingP (defaults to true) wether the surrounding &lt;p&gt; should be included too.
     */
    private static void handleParagraphs(StringObject obj, boolean leaveExtraNewLines, boolean surroundingP) {
        // handle paragraphs:
        boolean inParagraph = true;
        while (obj.length() > 0 && obj.charAt(0) == '\n') {
            obj.delete(0, 1); // delete starting newlines
        }
        if (surroundingP) {
            obj.insert(0, "<p>");
        }
        int pos = obj.indexOf("\n\n", 3); // one or more empty lines.
        while (pos != -1) {
            // delete the 2 new lines of the p.
            obj.delete(pos, 2);

            if (leaveExtraNewLines) {
                while (obj.length() > pos && obj.charAt(pos) == '\n') {
                    pos++;
                }
            } else {
                while (obj.length() > pos && obj.charAt(pos) == '\n') {
                    obj.delete(pos, 1); // delete the extra new lines too
                }
            }

            if (inParagraph) { // close the previous paragraph.
                obj.insert(pos, "</p>");
                inParagraph = false;
                pos += 4;
            }
            // next paragraph.
            obj.insert(pos, "\r<p>");
            pos += 4;
            inParagraph = true;
            pos = obj.indexOf("\n\n", pos); // search end of next paragraph
        }
        if (inParagraph) { // in current impl. this is always true

            // read whole text, but stil in paragraph
            // if text ends with newline, take it away, because it then means </p> rather then <br />
            if (obj.length() > 0) {
                if (obj.charAt(obj.length() - 1) == '\n') {
                    obj.delete(obj.length() - 1, 1);
                }
            }
            if (surroundingP) {
                obj.insert(obj.length(), "</p>");
            }
        }
    }

    /**
     * Removes all new lines and space which are too much.
     */
    private static void cleanupText(StringObject obj) {
        // remaining new lines have no meaning.
        obj.replace(">\n", ">"); // don't replace by space if it is just after a tag, it could have a meaning then.
        obj.replace("\n", " "); // replace by space, because people could use it as word boundary.
        // remaining double spaces have no meaning as well:
        int pos = obj.indexOf(" ", 0);
        while (pos != -1) {
            pos++;
            while (obj.length() > pos && obj.charAt(pos) == ' ') {
                obj.delete(pos, 1);
            }
            pos = obj.indexOf(" ", pos);
        }
        // we used \r for non significant newlines:
        obj.replace("\r", "");

    }

    /**
     * Only escape, clean up.
     * @since MMBase-1.7
     */
    private static void handleFormat(StringObject obj, boolean format) {
        if (format) {
            obj.replace("\r", "\n");
        } else {
            cleanupText(obj);
        }
    }
    private static StringObject prepareData(String data) {
        StringObject obj = new StringObject(Xml.XMLEscape(data));
        obj.replace("\r", ""); // drop returns (\r), we work with newlines, \r will be used as a help.
        return obj;
    }


    private static void handleRich(StringObject obj, boolean sections, boolean leaveExtraNewLines, boolean surroundingP) {
        // the order _is_ important!
        handleList(obj);
        handleParagraphs(obj, leaveExtraNewLines, surroundingP);
        if (sections) {
            handleHeaders(obj);
        }
        handleEmph(obj);
    }

    private static void handleNewlines(StringObject obj) {
        obj.replace("</ul>\n", "</ul>"); // otherwise we will wind up with the silly "</ul><br />" the \n was necessary for </ul></p>
        obj.replace("\n", "<br />\r");  // handle new remaining newlines.
    }

    /**
     * Defines a kind of 'rich' text format. This is a way to easily
     * type structured text in XML.  The XML tags which can be
     * produced by this are all HTML as well.
     *
     * This is a generalisation of the MMBase html() functions which
     * do similar duties, but hopefully this one is better, and more
     * powerfull too.
     *
     * The following things are recognized:
     * <ul>
     *  <li> Firstly, XMLEscape is called.</li>
     *  <li> A line starting with an asterix (*) will start an unnumberd
     *       list. The first new line not starting with a space or an other
     *       asterix will end the list </li>
     *  <li> Underscores are translated to the emphasize HTML-tag</li>
     *  <li> You can create a header tag by by starting a line with a dollar signs</li>
     *  <li> A paragraph can be begun (and ended) with an empty line.</li>
     * </ul>
     *
     * Test with commandline: java org.mmbase.util.Encode RICH_TEXT (reads from STDIN)
     *
     * @param data text to convert
     * @param format if the resulting XML must be nicely formatted (default: false)
     * @return the converted text
     */

    public static String richToXML(String data, boolean format) {
        StringObject obj = prepareData(data);
        handleRich(obj, true, true, true);
        handleNewlines(obj);
        handleFormat(obj, format);
        return obj.toString();
    }
    public static String richToXML(String data) {
        return richToXML(data, false);
    }
    /**
     * As richToXML but a little less rich. Which means that only one new line is non significant.
     * @see #richToXML
     */

    public static String poorToXML(String data, boolean format) {
        StringObject obj = prepareData(data);
        handleRich(obj, true, false, true);
        handleFormat(obj, format);
        return obj.toString();
    }

    public static String poorToXML(String data) {
        return poorToXML(data, false);
    }
    /**
     * So poor, that it actually generates pieces of XHTML 1.1 blocks (so, no use of sections).
     *
     * @see #richToXML
     * @since MMBase-1.7
     */

    public static String richToHTMLBlock(String data, boolean multipibleBrs, boolean surroundingP) {
        StringObject obj = prepareData(data);
        handleRich(obj, false, multipibleBrs, surroundingP);   // no <section> tags, leave newlines if multipble br's requested
        handleNewlines(obj);
        handleFormat(obj, false);
        return obj.toString();
    }


    public static String richToHTMLBlock(String data) {
        return richToHTMLBlock(data, false, true);
    }

    /**
     * So poor, that it actually generates pieces of XHTML 1.1 inlines (so, no use of section, br, p).
     *
     * @since MMBase-1.7
     */
    public static String poorToHTMLInline(String data) {
        StringObject obj = prepareData(data);
        // don't add newlines.
        handleFormat(obj, false);
        handleEmph(obj);
        return obj.toString();
    }



    /**
     *  chop of the mmxf xml tagstart and tagend:
     */

    final static private String xmlBody(String s) {
        return s.substring(XML_TAGSTART.length(), s.length() - XML_TAGEND.length());
    }

    /**
     * Base function for XSL conversions.
     */

    private static String XSLTransform(String xslfile, String data) {
        try {
            TransformerFactory tFactory = javax.xml.transform.TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(new StreamSource(ResourceLoader.getConfigurationRoot().getResourceAsStream("xslt/" + xslfile)));
            java.io.StringWriter res = new java.io.StringWriter();
            transformer.transform(new StreamSource(new StringReader(data)), new StreamResult(res));
            return res.toString();
        } catch (Exception e) {
            return "XSL transformation did not succeed: " + e.toString() + "\n" + data;
        }
    }

    static private void validate(String incoming) throws FormatException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Validating " + incoming);
            }
            javax.xml.parsers.DocumentBuilderFactory dfactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();

            // turn validating on..
            dfactory.setValidating(true);
            dfactory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder documentBuilder = dfactory.newDocumentBuilder();

            // in order to find the dtd.....
            org.mmbase.util.XMLEntityResolver resolver = new org.mmbase.util.XMLEntityResolver();
            documentBuilder.setEntityResolver(resolver);

            // in order to log our xml-errors
            StringBuffer errorBuff = new StringBuffer();
            ErrorHandler errorHandler = new ErrorHandler(errorBuff);
            documentBuilder.setErrorHandler(errorHandler);
            // documentBuilder.init();
            java.io.InputStream input = new java.io.ByteArrayInputStream(incoming.getBytes(CODING));
            documentBuilder.parse(input);

            if (!resolver.hasDTD())
                throw new FormatException("no doc-type specified for the xml");
            if (errorHandler.errorOrWarning)
                throw new FormatException("error in xml: \n" + errorBuff.toString());
        } catch (javax.xml.parsers.ParserConfigurationException pce) {
            throw new FormatException("[sax parser] not well formed xml: " + pce.toString());
        } catch (org.xml.sax.SAXException se) {
            log.debug("", se);
            //throw new FormatException("[sax] not well formed xml: "+se.toString() + "("+se.getMessage()+")");
        } catch (java.io.IOException ioe) {
            throw new FormatException("[io] not well formed xml: " + ioe.toString());
        }
    }

    static class FormatException extends java.lang.Exception {
        FormatException(String msg) {
            super(msg);
        }
    }

    // Catch any errors or warnings,....
    static class ErrorHandler implements org.xml.sax.ErrorHandler {
        boolean errorOrWarning;
        StringBuffer errorBuff;

        ErrorHandler(StringBuffer errorBuff) {
            super();
            this.errorBuff = errorBuff;
            errorOrWarning = false;
        }

        // all methods from org.xml.sax.ErrorHandler
        // from org.xml.sax.ErrorHandler
        public void fatalError(org.xml.sax.SAXParseException exc) {
            errorBuff.append("FATAL[" + getLocationString(exc) + "]:" + exc.getMessage() + "\n");
            errorOrWarning = true;
        }

        // from org.xml.sax.ErrorHandler
        public void error(org.xml.sax.SAXParseException exc) {
            errorBuff.append("Error[" + getLocationString(exc) + "]: " + exc.getMessage() + "\n");
            errorOrWarning = true;
        }

        // from org.xml.sax.ErrorHandler
        public void warning(org.xml.sax.SAXParseException exc) {
            errorBuff.append("Warning[" + getLocationString(exc) + "]:" + exc.getMessage() + "\n");
            errorOrWarning = true;
        }

        // helper methods
        /**
         * Returns a string of the location.
         */
        private String getLocationString(org.xml.sax.SAXParseException ex) {
            StringBuffer str = new StringBuffer();
            String systemId = ex.getSystemId();
            if (systemId != null) {
                int index = systemId.lastIndexOf('/');
                if (index != -1) {
                    systemId = systemId.substring(index + 1);
                }
                str.append(systemId);
            }
            str.append(" line:");
            str.append(ex.getLineNumber());
            str.append(" column:");
            str.append(ex.getColumnNumber());
            return str.toString();
        }
    }

    public XmlField() {
        super();
    }
    public XmlField(int to) {
        super(to);
    }

    public Map transformers() {
        Map h = new HashMap();
        h.put("MMXF_RICH",  new Config(XmlField.class, RICH,  "Converts mmxf to enriched ASCII (can be reversed)"));
        h.put("MMXF_POOR",  new Config(XmlField.class, POOR,  "Converts mmxf to enriched ASCII (inversal will not produce <br />'s"));
        h.put("MMXF_ASCII", new Config(XmlField.class, ASCII, "Converts mmxf to ASCII (cannoted be reversed)"));
        h.put("MMXF_BODY",  new Config(XmlField.class, BODY,  "Takes away the surrounding mmxf tags (returns XML)"));
        h.put("MMXF_BODY_RICH", new Config(XmlField.class, RICHBODY, "Like MMXF_RICH, but returns decodes without mmxf tags"));
        h.put("MMXF_BODY_POOR", new Config(XmlField.class, POORBODY, "Like MMXF_POOR, but returns decoded without mmxf tags"));
        h.put("MMXF_HTML_INLINE", new Config(XmlField.class, HTML_INLINE, "Decodes only escaping and with <em>"));
        h.put("MMXF_HTML_BLOCK", new Config(XmlField.class,  HTML_BLOCK, "Decodes only escaping and with <em>, <p>, <br /> (only one) and <ul>"));
        h.put("MMXF_HTML_BLOCK_BR", new Config(XmlField.class,  HTML_BLOCK_BR, "Decodes only escaping and with <em>, <p>, <br /> (also multiples) and <ul>"));
        h.put("MMXF_HTML_BLOCK_NOSURROUNDINGP", new Config(XmlField.class,  HTML_BLOCK_NOSURROUNDINGP, "Decodes only escaping and with <em>, <p>, <br /> (only one) and <ul>"));
        h.put("MMXF_HTML_BLOCK_BR_NOSURROUNDINGP", new Config(XmlField.class,  HTML_BLOCK_BR_NOSURROUNDINGP, "Decodes only escaping and with <em>, <p>, <br /> (also multiples) and <ul>"));

        h.put("MMXF_XHTML", new Config(XmlField.class, XHTML, "Converts to piece of XHTML"));
        h.put("MMXF_MMXF",  new Config(XmlField.class, XML,   "Only validates the XML with the DTD (when decoding)"));
        return h;
    }

    public String transform(String data) {
        switch (to) {
            case RICH :
            case POOR :
                return XSLTransform("mmxf2rich.xslt", data);
            case RICHBODY :
            case POORBODY :
                return XSLTransform("mmxf2rich.xslt", XML_TAGSTART + data + XML_TAGEND);
            case ASCII :
                return XSLTransform("mmxf2ascii.xslt", data);
            case XHTML :
                return XSLTransform("mmxf2xhtml.xslt", data);
            case BODY :
                return xmlBody(data);
            case XML :
                return data;
            case HTML_BLOCK:
            case HTML_BLOCK_BR:
            case HTML_INLINE:
                throw new UnsupportedOperationException("Cannot transform");
            default :
                throw new UnknownCodingException(getClass(), to);
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
            case RICHBODY :
                result = richToXML(r);
                // rich will not be validated... Cannot be used yet!!
                break;
            case POORBODY :
                result = poorToXML(r);
                break;
            case BODY :
                result = XML_TAGSTART + r + XML_TAGEND;
                validate(XML_HEADER + result);
                break;
            case XML :
                result = r;
                validate(XML_HEADER + result);
                break;
            case HTML_BLOCK:
                result = richToHTMLBlock(r);
                break;
            case HTML_BLOCK_BR:
                result = richToHTMLBlock(r, true, true);
                break;
            case HTML_BLOCK_NOSURROUNDINGP:
                result = richToHTMLBlock(r, false, false);
                break;
            case HTML_BLOCK_BR_NOSURROUNDINGP:
                result = richToHTMLBlock(r, true, false);
                break;
            case HTML_INLINE:
                result = poorToHTMLInline(r);
                break;
            case ASCII :
                throw new UnsupportedOperationException("Cannot transform");
            default :
                throw new UnknownCodingException(getClass(), to);
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
        case RICHBODY :
            return "MMXF_BODY_RICH";
        case POORBODY :
            return "MMXF_BODY_POOR";
        case HTML_BLOCK :
            return "MMXF_HTML_BLOCK";
        case HTML_BLOCK_BR :
            return "MMXF_HTML_BLOCK_BR";
        case HTML_BLOCK_NOSURROUNDINGP :
            return "MMXF_HTML_BLOCK_NOSURROUNDINGP";
        case HTML_BLOCK_BR_NOSURROUNDINGP :
            return "MMXF_HTML_BLOCK_BR_NOSURROUNDINGP";
        case HTML_INLINE :
            return "MMXF_HTML_INLINE";
        case ASCII :
            return "MMXF_ASCII";
        case XHTML :
            return "MMXF_XHTML";
        case BODY :
            return "MMXF_BODY";
        case XML :
            return "MMXF_MMXF";
        default :
            throw new UnknownCodingException(getClass(), to);
        }
    }
}
