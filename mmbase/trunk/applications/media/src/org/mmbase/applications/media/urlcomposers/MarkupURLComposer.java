/*
  
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
  
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
  
*/

package org.mmbase.applications.media.urlcomposers;

import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import org.mmbase.applications.media.Format;
import org.mmbase.applications.media.builders.MediaFragments;
import java.util.*;
import java.net.*;


/**
 * Produces links to (jsp) templates which can present a media
 * fragment. These templates can e.g. produce pieces of HTML (for use
 * with object tag), or perhaps a better example are
 * SMIL-jsp-templates ( with response.setHeader("Content-Type",
 * "application/smil");)
 *
 * Depends on a 'template' to be linked to the fragment, or to one of
 * its parent fragments.
 *
 * @author Michiel Meeuwissen
 * @version $Id: MarkupURLComposer.java,v 1.4 2003-02-25 23:54:31 michiel Exp $
 * @since MMBase-1.7
 */
public class MarkupURLComposer extends FragmentURLComposer { 
    private static Logger log = Logging.getLoggerInstance(MarkupURLComposer.class.getName());
    
    public MarkupURLComposer(MMObjectNode provider, MMObjectNode source, MMObjectNode fragment, Map info) {
        super(provider, source, fragment, info);
     
    }


    /**
     * Typical for a 'MarkupURLComposer' is that it should have a
     * 'template'. It could have been called a 'TemplateURLComposer',
     * but that sounds to general..
     */

    protected MMObjectNode getTemplate() {
        return (MMObjectNode) getInfo().get("template");
    }

    /**
     * This composer can only do something if it has a template. The
     * URLComposerFactory arranges this, but if somewhy it doesn't, it still works.
     *
     * More importantly, it could also do checks to link 'markup
     * language type' to source format type.  Currently is checked
     * that the source-format must be Real, if the templates language
     * is SMIL (this is perhaps too limited).
     */

    public boolean canCompose() {
        MMObjectNode template = getTemplate();
        if (template == null) return false;
        Format sourceFormat = Format.get(source.getIntValue("format")); 
        if (getFormat() == Format.SMIL && !( sourceFormat == Format.RM || sourceFormat == Format.RA)) return false;
        return true;
    }

    protected StringBuffer  getURLBuffer() {
        MMObjectNode template = getTemplate();
        if (template != null) { 
            String url = template.getStringValue("url");
            StringBuffer buf = new StringBuffer(url + "fragment=" + fragment.getNumber() + "&format=" +  Format.get(source.getIntValue("format")));            
            if (url.indexOf("://") < 0) {
                if (! url.startsWith("/")) {
                    buf.insert(0, Config.templatesDir);
                }
                buf.insert(0, "http://" + Config.host);
            }
            return buf;
        } else {
            return new StringBuffer("[Could not compose]"); // should not happen
        }
        
    }
    public String getGUIIndicator(Map options) {
        Locale locale = (Locale) options.get("locale");
        Format sourceFormat = Format.get(source.getIntValue("format")); 
        return super.getGUIIndicator(options) + " (" + sourceFormat.getGUIIndicator(locale) + ")";
    }


    public String getDescription(Map options) { 
        Locale locale = (Locale) options.get("locale");
        ResourceBundle m = ResourceBundle.getBundle("org.mmbase.applications.media.urlcomposers.resources.markupurlcomposer", locale);
        String url = getURL() + "&amp;language=" + locale.getLanguage();
        MMObjectNode template = getTemplate();
        if (template.getStringValue("mimetype").equals("text/html")) {
            return template.getStringValue("name") + "<br />" + template.getStringValue("description") + "<br />" + m.getString("object") + ":<br /><nobr>&lt;object data='" + url + "' type='text/html'&gt;&lt/object&gt;</nobr>";
        } else {
            return template.getStringValue("name") + "<br />" + template.getStringValue("description");
        }
    }


    /**
     * Depends on mimetype of the template to return the format of this urlcomposer.
     */

    public Format  getFormat()   { 
        MMObjectNode template = getTemplate();
        if (template == null) return Format.HTML;
        String mimetype = template.getStringValue("mimetype");
        if (mimetype.equals("application/smil")) {
            return Format.SMIL;
        } else {
            return Format.HTML; 
        }
    } 

}
