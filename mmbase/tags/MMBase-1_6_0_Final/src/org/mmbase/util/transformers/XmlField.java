package org.mmbase.util.transformers;

import java.io.Reader;
import java.io.Writer;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.mmbase.util.StringObject;

import org.mmbase.module.core.MMBaseContext; 

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * XMLFields in MMBase. This class can encode such a field to several other formats. 
 *
 * @author Michiel Meeuwissen
 */

public class XmlField extends AbstractTransformer implements CharTransformer {


    private static Logger log = Logging.getLoggerInstance(XmlField.class.getName());

    // can be decoded:
    private final static int RICH   = 1;     
    private final static int POOR   = 2;
    private final static int BODY   = 3;
    private final static int XML    = 4;

    // cannot be decoded:
    private final static int ASCII  = 10;
    private final static int XHTML  = 11;


    private final static String CODING = "UTF-8"; // This class only support UTF-8 now.


    // for validation only.
    private final static String  XML_HEADER   = "<?xml version=\"1.0\" encoding=\"" + CODING + "\"?>\n<!DOCTYPE mmxf PUBLIC \"//NL//OMROEP//MMBASE//DTD//MMXF 1.0//\" \"http://www.mmbase.org/dtd/mmxf_1_0.dtd\">\n";   
    private final static String  XML_TAGSTART = "<mmxf>";
    private final static String  XML_TAGEND   = "</mmxf>";



    public final static boolean isXmlEncoded(String s) {
        return s.startsWith(XML_TAGSTART) && s.endsWith(XML_TAGEND);
    }


    /** 
     * Takes a string object, finds list structures and changes it to XLL
     *
     * @author Michiel Meeuwissen
     */

    private static void handleList(StringObject obj) {        
        // handle lists
        // make <ul> possible (not yet nested), with *-'s on the first char of line.
        int inList = 0; // if we want nesting possible, then an integer (rather then boolean) will be handy        
        int pos;
        if (obj.length() == 0) return;
        if (obj.charAt(0) == '-') { // hoo, we even _start_ with al list;
            obj.insert(0, "\n"); // in the loop \n- is deleted, so it must be there.
            pos = 0;
        } else {
            pos = obj.indexOf("\n-", 0); // search the first
        }

    listwhile:
        while (pos != -1) { 
            if (inList == 0) { // not yet in list
                inList++;        // now we are
                obj.delete(pos, 2); // delete \n-
                // remove spaces..
                while(pos < obj.length() && obj.charAt(pos) == ' ') obj.delete(pos, 1);
                obj.insert(pos, "\r<ul>\r<li>"); // insert 10 chars.
                pos += 10;
                
            } else {             // already in list               
                if (obj.charAt(pos + 1) != '-') { // end of list
                    obj.delete(pos, 1); // delete \n
                    obj.insert(pos, "</li>\r</ul>\n");
                    pos += 12;
                    inList--;
                } else {                      // not yet end
                    obj.delete(pos, 2); // delete \n-
                    // remove spaces..
                    while(pos < obj.length() && obj.charAt(pos) == ' ') obj.delete(pos, 1);
                    obj.insert(pos, "</li>\r<li>");
                    pos += 10;
                }
            }
            if (inList > 0) { // search for new line
                pos = obj.indexOf("\n", pos);
                if (pos == -1) break; // no new line found? End of list, of text.
                if (pos + 1 == obj.length()) { 
                    obj.delete(pos, 1);  break; // if end of text, simply remove the newline.
                }
                while (obj.charAt(pos + 1) == ' ') { // if next line starts with space, this new line does not count. This makes it possible to have some formatting in a <li>
                    pos = obj.indexOf("\n", pos + 1);
                    if (pos + 1 == obj.length()) { 
                        obj.delete(pos, 1);  break listwhile;  // nothing to do...
                    }
                }
            } else {             // search for next list
                pos = obj.indexOf("\n-", pos);
            }
        }
        // make sure that the list is closed:
        while (inList > 0) {
            obj.insert(obj.length(), "</li></ul>\n"); inList--; // always finish with a new line, it might be needed for the finding of paragraphs.
        } 
        
    }
    /**
     * If you want to add a _ in your text, that should be possible too...
     * Should be done last, because no tags can appear in <em>
     */
    private static void handleEmph(StringObject obj) { 

        obj.replace("__", "&#95;");
        
        // Emphasizing. This is perhaps also asking for trouble, because
        // people will try to use it like <font> or other evil
        // things. But basicly emphasizion is content, isn't it?
        boolean emph = false;
        int pos = obj.indexOf("_", 0);
        while (pos != -1) {
            obj.delete(pos, 1);
            if (! emph) {
                obj.insert(pos, "<em>");
                pos += 3;
                emph = true;
                int pos1 = obj.indexOf("_", pos);
                int pos2 = obj.indexOf("<", pos);// must be closed before next tag opens.
                pos = pos1 < pos2 ? pos1 : pos2;
            } else {
                obj.insert(pos, "</em>");
                pos += 4;
                emph = false;
                pos = obj.indexOf("_", pos); // search next opening.
            }            

        }
        
        if (emph) { // make sure it is closed on the end.
            // should neve happen when you e.g. used paragraphs.
            obj.insert(obj.length(), "</em>\r");
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
        int     requested_level;
        char    ch;
        int     level   = 0;      // start without being in section.
        int pos = obj.indexOf("<p>$", 0);
        while (pos != -1) {
            obj.delete(pos, 4); // remove <p>$

            requested_level = 1;
            // find requested level:
            while(true) {
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
            String add = "";
            for (;requested_level <= level; level--) {
                // same or higher level section
                add += "</section>";
            }             
            level++;
            for (;requested_level > level; level++) {
                add += "<section title=\"\">";
            }
            add += "<section title=\""; 

            obj.insert(pos, add);
            pos += add.length();
            
            // search end title of  header;

            while (true) { // oh yes, and don't allow _ in title.
                int pos1 = obj.indexOf("_", pos); 
                int pos2 = obj.indexOf("</p>", pos); 
                if (pos1 < pos2 && pos1 > 0 ) {
                    obj.delete(pos1, 1);
                } else {
                    pos = pos2;
                    break;
                }                
            }           
            if (pos == -1) break ; // not found, could not happen.
            // replace it.
            obj.delete(pos, 4);
            obj.insert(pos, "\">");
            pos += 2;
            pos = obj.indexOf("<p>$", pos); // search the next one.
        }
        // ready, close all sections still open.
        for(;level>0; level--) {
            obj.insert(obj.length(), "</section>");
        }
       
    }

    /**
     * Make <p> </p> tags.
     */
    private static void handleParagraphs(StringObject obj) { 
        // handle paragraphs:
        boolean inParagraph = true;
        while (obj.length() > 0 && obj.charAt(0) == '\n') obj.delete(0, 1); // delete starting newlines
        obj.insert(0, "<p>");
        int pos = obj.indexOf("\n\n", 3); // one or more empty lines.
        while (pos != -1) {
            while (obj.length() > pos && obj.charAt(pos) == '\n') obj.delete(pos, 1); // delete the new lines.

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
        if (inParagraph) {
            // read whole text, but stil in paragraph
            // if text ends with newline, take it away, because it then means </p> rather then <br />
            if (obj.charAt(obj.length() -1) == '\n') obj.delete(obj.length() - 1, 1);
            obj.insert(obj.length(), "</p>");
        }
    }

    /**
     * Removes all new lines and space which are too much.
     */
    private static void cleanupText(StringObject obj) {
        // remaining new lines have no meaning.        
        obj.replace(">\n", ">"); // don't replace by space if it is just after a tag, it could have a meaning then.
        obj.replace("\n", " "); // replace by space, because people could use it as word boundary.
        // remaing double spaces have no meaning as well:
        int pos = obj.indexOf(" ", 0);
        while (pos != -1) {
            pos++;
            while (obj.length() > pos && obj.charAt(pos) == ' ') obj.delete(pos, 1);
            pos = obj.indexOf(" ", pos);
        }
        // we used \r for non significant newlines:
        obj.replace("\r", "");
       
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
     *
     * @author Michiel Meeuwissen */

    public static String richToXML(String data, boolean format) {
        StringObject obj = new StringObject(Xml.XMLEscape(data));

        obj.replace("\r","");      // drop returns (\r), we work with newlines, \r will be used as a help.
        
        handleList(obj);
        handleParagraphs(obj);
        handleHeaders(obj);
        handleEmph(obj);

        // handle new remaining newlines.
        obj.replace("\n", "<br />\r");

        if (format) {
            obj.replace("\r", "\n");
        } else {
            cleanupText(obj);
        }               
        return obj.toString();
    }
    public static String richToXML(String data) {
        return richToXML(data, false);
    }
    /**
     * As richToXML but a little less rich. Which means that only one new line is non significant.
     */

    public static String poorToXML(String data, boolean format) {
        StringObject obj = new StringObject(Xml.XMLEscape(data));

        obj.replace("\r","");      // drop returns (\r), we work with newlines, \r will be used as a help.
        
        // the order _is_ important!
        handleList(obj);
        handleParagraphs(obj);
        handleHeaders(obj);
        handleEmph(obj);

        if (format) {
            obj.replace("\r", "\n");
        } else {
            cleanupText(obj);
        }
        return obj.toString();
    }


    public static String poorToXML(String data) {
        return poorToXML(data, false);
    }

    final static private String  xmlBody(String s) {
        // chop of the xml tagstart and tagend:
        return s.substring(XML_TAGSTART.length(), 
                           s.length() - XML_TAGEND.length());
    }

    /**
     * Base function for XSL conversions.
     *
     */

    private static String XSLTransform(String xslfile, String data) {
        try {
            String xslPath = 
                MMBaseContext.getConfigPath() + 
                File.separator + "xslt" +
                File.separator + xslfile; 

            javax.xml.transform.TransformerFactory tFactory = 
                javax.xml.transform.TransformerFactory.newInstance();

            //log.error("xslpath: " + xslPath);
            javax.xml.transform.Transformer transformer =                 
                tFactory.newTransformer(new javax.xml.transform.stream.StreamSource(new File(xslPath).getAbsoluteFile()));
            
            java.io.StringWriter res = new java.io.StringWriter();
            transformer.transform(new javax.xml.transform.stream.StreamSource(new java.io.StringReader(data)),
                                  new javax.xml.transform.stream.StreamResult(res));
            return res.toString();
        } catch (Exception e) {
            return "XSL transformation did not succeed: " + e.toString() + "\n" + data;
        }
    }


    static private void validate(String incoming) throws FormatException {
	try {

            log.debug("Validating " + incoming);
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
	    
      	    if (!resolver.hasDTD()) throw new FormatException("no doc-type specified for the xml");
	    if (errorHandler.errorOrWarning) throw new FormatException("error in xml: \n" + errorBuff.toString());	    
    	}
	catch(javax.xml.parsers.ParserConfigurationException pce) {           
	    throw new FormatException("[sax parser] not well formed xml: "+pce.toString());
	}	
	catch(org.xml.sax.SAXException se) {	
            se.printStackTrace();
	    //throw new FormatException("[sax] not well formed xml: "+se.toString() + "("+se.getMessage()+")");
	}
	catch(java.io.IOException ioe) {
	    throw new FormatException("[io] not well formed xml: "+ioe.toString());
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
      	    errorBuff.append("FATAL["+getLocationString(exc)+"]:" + exc.getMessage()+ "\n");   
      	    errorOrWarning = true;
    	}

    	// from org.xml.sax.ErrorHandler
    	public void error(org.xml.sax.SAXParseException exc) {
    	    errorBuff.append("Error["+getLocationString(exc)+"]: " + exc.getMessage()+ "\n");    
      	    errorOrWarning = true;
    	}

    	// from org.xml.sax.ErrorHandler
    	public void warning(org.xml.sax.SAXParseException exc) {
      	    errorBuff.append("Warning["+getLocationString(exc)+"]:" + exc.getMessage()+ "\n");   
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


    public Map transformers() {
        HashMap h = new HashMap();
        h.put("mmxf_rich".toUpperCase(),  new Config(XmlField.class, RICH, "Converts to enriched ASCII"));
        h.put("mmxf_poor".toUpperCase(),  new Config(XmlField.class, POOR));
        h.put("mmxf_ascii".toUpperCase(), new Config(XmlField.class, ASCII));
        h.put("mmxf_body".toUpperCase(),  new Config(XmlField.class, BODY));
        h.put("mmxf_xhtml".toUpperCase(), new Config(XmlField.class, XHTML, "Converts to piece of XHTML"));
        h.put("mmxf_mmxf".toUpperCase(), new Config(XmlField.class, XML, "Only validates the XML with the DTD (when decoding)"));
        return h;
    }

    /**
     * Takes an object, normally a string
     */
    public Writer transform(Reader r) {
        throw new UnsupportedOperationException("transform(Reader) is not yet supported");
    }
    public Writer transformBack(Reader r) {
        throw new UnsupportedOperationException("transformBack(Reader) is not yet supported");
    } 

    public String transform(String data) {
        switch(to){
        case RICH:      
        case POOR:        return XSLTransform("mmxf2rich.xsl", data);
        case ASCII:       return XSLTransform("mmxf2ascii.xsl", data);
        case XHTML:       return XSLTransform("mmxf2xhtml.xsl", data);
        case BODY:        return xmlBody(data);
        case XML:         return data;
        default: throw new UnsupportedOperationException("Cannot transform");
        }
    }
    public String transformBack(String r) {
        String result;
        switch(to){
        case RICH:    
            result = XML_TAGSTART + richToXML(r) + XML_TAGEND;
            // rich will not be validated... Cannot be used yet!!
            break;
        case POOR:    
            result =  XML_TAGSTART + poorToXML(r) + XML_TAGEND;
            break;
        case BODY:   
            result =  XML_TAGSTART + r + XML_TAGEND;
            break;            
        case XML:
            result = r;
            break;
        case ASCII:
        default: throw new UnsupportedOperationException("Cannot transform");           
        }
        try {
    	    validate(XML_HEADER + result);
	}
	catch(FormatException fe) {
	    throw new RuntimeException(fe.toString() + " source: \n" + result);
	}
        return result;
    }
    public String getEncoding() {
        switch(to){
        case RICH:  return "MMXF_RICH";
        case POOR:  return "MMXF_POOR";
        case ASCII: return "MMXF_ASCII";
        case XHTML: return "MMXF_XHTML";
        case BODY:  return "MMXF_BODY";
        case XML:   return "MMXF_MMXF";
        default: throw new UnsupportedOperationException("unknown encoding");
        }
    }
}
