package org.mmbase.util.transformers;

import java.util.*;
import java.util.regex.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.io.*;
import org.mmbase.util.functions.*;


import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Can be used to strip tags and attributes from HTML.
 *
 *
 * http://javafaq.nu/java-example-code-618.html
 * @author Michiel Meeuwissen
 * @version $Id: TagStripperFactory.java,v 1.9 2007-08-03 19:30:23 michiel Exp $
 * @since MMBase-1.8.4
 */
public class TagStripperFactory implements ParameterizedTransformerFactory  {

    private static final Logger log = Logging.getLoggerInstance(TagStripperFactory.class);


    private static final String NL_TOKEN = "XXXX_NL_XXXX";
    protected static final Parameter[] PARAMS = new Parameter[] {
        new Parameter<String>("tags", String.class, ""),  // allowed tags, default no tags are permitted.
        new Parameter<Boolean>("addbrs", Boolean.class, Boolean.FALSE),
        new Parameter<Boolean>("escapeamps", Boolean.class, Boolean.FALSE)
    };

    public Parameters createParameters() {
        return new Parameters(PARAMS);
    }



    /**
     * Creates a parameterized transformer.
     */
    public Transformer createTransformer(final Parameters parameters) {

        parameters.checkRequiredParameters();
        if (log.isDebugEnabled()) {
            log.debug("Creating transformer, with " + parameters);
        }
        
        final List<Tag> tagList;
        String tags = parameters.getString("tags").toUpperCase();
        if (tags.equals("XSS")) {
            tagList = XSS;
        } else if (tags.equals("")) {
            tagList = NONE;
        } else if (tags.equals("NONE")) {
            tagList = NONE;
        } else {
            throw new RuntimeException("Unknonw value for 'tags' parameter '" + tags + "'. Known are 'XSS': strip only cross-site scripting, and '': strip all tags.");
        }
        final Boolean addbrs = (Boolean) parameters.get("addbrs");
        final Boolean escapeamps = (Boolean) parameters.get("escapeamps");

        ParserGetter kit = new ParserGetter();
        final HTMLEditorKit.Parser parser = kit.getParser();
        ReaderTransformer trans = new ReaderTransformer() {
                public Writer transform(Reader r, Writer w) {
                    final TagStripper callback = new TagStripper(w, tagList);
                    callback.addBrs = addbrs;
                    callback.escapeAmps = escapeamps;
                    if (addbrs) {
                        r = new TransformingReader(r, new ChunkedTransformer(ChunkedTransformer.XMLTEXT) {
                                protected boolean replace(String string, Writer w, Status status) throws IOException {
                                    w.write(string.replaceAll("\n", NL_TOKEN));
                                    return false;
                                }
                                    
                                protected String base() { return "nl"; }
                                
                            });
                    }
                    try {
                        parser.parse(r, callback, true);
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                    return w;
                }
                public String toString() {
                    return tagList + " " + (addbrs ? "(adding brs)" : "");
                }
            };
        if (log.isDebugEnabled()) {
            log.debug("Created " + trans);
        }
        return trans;
    }


    /**
     * Enumeration for types of allowances
     */
    private enum Allows {
        YES,
        NO, 
        DONTKNOW
    }

    /**
     * 
     */
    private static abstract class Allowance {
        Allows allows(String p) {
            return Allows.DONTKNOW;
        }
    }

    private static final Allowance ALLOW_ALL    = new Allowance() { Allows allows(String p) { return Allows.YES; } public String toString() { return "ALL"; }};
    private static final Allowance DISALLOW_ALL = new Allowance() { Allows allows(String p) { return Allows.NO; } public String toString() { return "NONE"; }};

    private static class PatternAllowance extends Allowance {
        private final Pattern pattern;
        PatternAllowance(Pattern p) {
            pattern = p;
        }
        PatternAllowance(String s) {
            pattern = Pattern.compile(s);
        }
        Allows allows (String p) {
            return pattern.matcher(p).matches() ?  Allows.YES : Allows.DONTKNOW;
        }
        public String toString() {
            return pattern.toString();
        }
    }
    private static class PatternDisallowance extends Allowance {
        private final Pattern pattern;
        PatternDisallowance(Pattern p) {
            pattern = p;
        }

        PatternDisallowance(String s) {
            pattern = Pattern.compile(s);
        }
        Allows allows (String p) {
            return pattern.matcher(p).matches() ? Allows.NO : Allows.DONTKNOW;
        }
        public String toString() {
            return "!" + pattern.toString();
        }
    }
    private static class ChainedAllowance extends Allowance {
        private final List<Allowance> allowances = new ArrayList<Allowance>();
        void add(Allowance... alls) {
            for (Allowance a : alls) {
                allowances.add(a);
            }
        }
        Allows allows(String p) {
            for (Allowance a : allowances) {
                Allows allows = a.allows(p);
                if (allows != Allows.DONTKNOW) return allows;
            }
            return Allows.DONTKNOW;
        }
        public String toString() {
            return allowances.toString();
        }
    }


    private static class Attr {
        final Allowance key;
        final Allowance value;
        public Attr(Allowance k, Allowance v) {
            key = k; value = v;
        }        
        public Attr(Allowance k) {
            key = k; value = ALLOW_ALL;
        }        
        public Allows allows(String k, String v) {
            Allows ka = key.allows(k);
            if (ka == Allows.NO) return Allows.NO;
            Allows va = value == null ? Allows.YES : value.allows(v);
            if (va == Allows.NO) return Allows.NO;
            if (ka == Allows.YES && va == Allows.YES) return Allows.YES;
            return Allows.DONTKNOW;
        }
        public String toString() {
            return key.toString() + "=" + value;
        }
    }
    


    private static class Tag extends ChainedAllowance {
        private final List<Attr> attributes = new ArrayList<Attr>();
        public Tag(Allowance... wrapped) {
            super();
            add(wrapped);
        }
        public List<Attr> getAttributes() {
            return attributes;
        }
        public  boolean allowsAttribute(String k, String v) {
            //System.out.println("Checking " + k + "=" + v + " for " + this);
            for (Attr attr : attributes) {
                switch (attr.allows(k, v)) {
                case YES: return true;
                case NO: return false;
                }
            }
            return true;
        }
        public String toString() {
            return super.toString() + "(" + attributes + ")";
        }
    }


    protected static class TagStripper extends HTMLEditorKit.ParserCallback {
        private final Writer out;
        private final List<Tag> tags;
        boolean addImplied = false;
        boolean addBrs     = false;
        boolean escapeAmps = false;
        List<HTML.Tag> impliedTags = new ArrayList<HTML.Tag>();
        List<HTML.Tag> stack       = new ArrayList<HTML.Tag>();

        public TagStripper(Writer out, List<Tag> t) {
            this.out = out;
            tags = t;
        }

        public String toString() {
            return "" + tags + (addBrs ? "(replacing newlines)" : "");
        }
        protected Tag allowed(String tagName) {
            for (Tag tag : tags) {
                Allows a = tag.allows(tagName);
                switch (a) {
                case YES: return tag;
                case NO: return null;
                }
            }
            return null;
        }

        public void handleText(char[] text, int position) {          
            try {
                //System.out.println("Handling " + new String(text) + " for " + position);
                if (addBrs) {
                    String t = new String(text);
                    if (text[0] == '>') { // odd, otherwise <br /> ends up as <br />>
                        t = t.substring(1);
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("handling " + t);
                    }
                    
                    if (stack.get(0).isPreformatted()) {
                        t = t.replaceAll(NL_TOKEN, "\n");
                    } else {
                        t = t.replaceAll(NL_TOKEN, "<br class='auto' />");
                    }
                    if (escapeAmps) {
                        // see comment in handleAttributes
                        t = t.replaceAll("&", "&amp;");
                    }
                    out.write(t);

                } else {
                    String t;
                    if (text[0] == '>') { // odd, otherwise <br /> ends up as <br />>
                        t = new String(text).substring(1);
                    } else {
                        t = new String(text);
                    }
                    if (escapeAmps) {
                        // see comment in handleAttributes
                        t = t.replaceAll("&", "&amp;");
                    }
                    out.write(t);
                }
                out.flush();
            }
            catch (IOException e) {
                log.warn(e);
            }
        }

        protected Tag getTag(HTML.Tag tag, MutableAttributeSet attributes) {
            boolean implied = attributes.containsAttribute(IMPLIED, Boolean.TRUE);
            Tag t;
            if (! addImplied && implied) {
                t = null;
                impliedTags.add(tag);
            } else {
                t = allowed(tag.toString());
            }
            return t;
            
        }
        protected void handleAttributes(Tag t, MutableAttributeSet attributes) throws IOException {
            Enumeration en = attributes.getAttributeNames();
            while (en.hasMoreElements()) {
                Object attName =  en.nextElement();
                if (addBrs && attName.equals("nl_")) continue;
                AttributeSet set = attributes;
                Object value = attributes.getAttribute(attName);
                while (value == null && set.getResolveParent() != null) {
                    set = set.getResolveParent();
                    value = set.getAttribute(attName);
                }
                if (t.allowsAttribute("" + attName, "" + value)) {
                    out.write(' ');
                    out.write("" + attName);
                    out.write('=');
                    out.write('"');
                    String s = "" + value;
                    if (escapeAmps) {
                        // HTMLEditorKit translates all Iso1 entities to unicode.
                        // Escape remaining amps, to produce valid Xml.
                        s = s.replaceAll("&", "&amp;");
                    }
                    s = s.replaceAll("\"", "&quot;");
                    out.write(s);
                    out.write('"');
                }
            }
        }
        
        public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            try {
                stack.add(0, tag);
                Tag t = getTag(tag, attributes);
                
                if (t != null) {
                    out.write('<');
                    out.write(tag.toString());
                    handleAttributes(t, attributes);
                    out.write('>');
                } else {
                    out.write(' ');
                }
            }
            catch (IOException e) {
                log.warn(e);
            }
        }
        
        public void handleEndTag(HTML.Tag tag, int position) {
            stack.remove(0);
            try {
                String tagName = tag.toString();
                Tag t;
                boolean implied = impliedTags.contains(tag);
                if (! addImplied && implied) {
                    t = null;
                } else {
                    t = allowed(tagName);
                }
                if (t != null) {
                    out.write("</");
                    out.write(tagName);
                    out.write('>');
                } else {
                    out.write(' ');
                }
            }
            catch (IOException e) {
                log.warn(e);
            }
        }
        public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            //stack.remove(0);            
            //System.out.println("SIMPLE TAG " + tag);
            try {
                String tagName = tag.toString();
                Tag t = getTag(tag, attributes);
                if (t != null) {
                    out.write('<');
                    out.write(tagName);
                    handleAttributes(t, attributes);
                    out.write(" />"); 
                } else {
                    out.write(' ');
                }
            } catch (IOException e) {
                log.warn(e);
            }
            
        }
        public void handleError(String mes, int position) {
            log.debug(mes + " at " + position);
        }

        public void handleComment(char[] data, int pos) {
            try {
                out.write("<!-- " + new String(data) + " -->");
            } catch (IOException e) {
                log.warn(e);
            }
	}


        
        public void flush() {
            try {                
                out.flush();
                out.close();
            } catch (IOException e) {
                log.warn(e);
            }
        } 
    }

    protected static class ParserGetter extends HTMLEditorKit {
        // purely to make this method public
        public HTMLEditorKit.Parser getParser(){
            return super.getParser();
        }  
    }

    protected static final Attr EVENTS = new Attr(new PatternDisallowance("(?i)onclick|ondblclick|onmousedown|onmousemove|onmouseout|onmouseover|onmouseup|onload|onunload|onchange|onsubmit|onreset|onselect|onblur|onfocus|onkeydown|onkeyup|onkeypress"));

    // only strip cross-site-scripting
    public final static List<Tag> XSS = new ArrayList<Tag>();
    static {
        {
            Tag a = new Tag(new PatternAllowance("(?i)a"));
            a.getAttributes().add(new Attr(new PatternAllowance("(?i)href"), new PatternDisallowance("(?i)javascript:.*")));
            a.getAttributes().add(EVENTS);
            XSS.add(a);
        }            
        XSS.add(new Tag(new PatternDisallowance("(?i)script|embed|object|frameset"))); 

        {
            Tag all = new Tag(ALLOW_ALL);
            all.getAttributes().add(EVENTS);
            XSS.add(all);
        }
    }

    // strip all tags
    public final  static List<Tag> NONE = new ArrayList<Tag>();
    static {
        NONE.add(new Tag(DISALLOW_ALL));
    }



    public static void main(String[] args) {
        ParameterizedTransformerFactory factory = new TagStripperFactory();
        Parameters params = factory.createParameters();
        params.set("tags", "XSS");
        params.set("addbrs", Boolean.TRUE);
        params.set("escapeamps", Boolean.TRUE);
        CharTransformer transformer = (CharTransformer) factory.createTransformer(params);
        
        //        String source = "<p style=\"nanana\">allow this <b>but not this</b></p>";
//        String source = "<p style=nanana/>";
//        String source = "<p style=\"nanana\">text</p>";
        String source = "<P sTyle=\"nanana\">hoi hoi\n<br><table WIDTH=\"45\" height=99 border='1\"' fONt=bold styLe=\"n\\\"one\">\nbla bla bla</table></p>";
        //System.out.println("Source      = "+source);
        transformer.transform(new InputStreamReader(System.in), new OutputStreamWriter(System.out));
        //System.out.println("Destination = "+dest);

        org.mmbase.util.ThreadPools.filterExecutor.shutdown();

        
    }

}
