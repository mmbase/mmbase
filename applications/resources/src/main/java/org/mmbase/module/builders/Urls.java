/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;

/**
 * The URL builder provides nice gui indications for objects with an 'url' field. Also it provides
 * the 'url' function wich also resolves urls which start with '/'.

 * @author Daniel Ockeloen
 * @author Michciel Meeuwissen
 * @version $Id$
 */
public class Urls extends MMObjectBuilder {
    private static final Logger log = Logging.getLoggerInstance(Urls.class);

    @Override
    public String getGUIIndicator(MMObjectNode node) {
        String str = node.getStringValue("url");
        if (str != null) {
            if (str.indexOf("http://") == 0) {
                str = str.substring(7);
            }
            str = org.mmbase.util.transformers.Xml.XMLEscape(str);
        }
        return str;
    }

    @Override
    public String getGUIIndicator(String field, MMObjectNode node) {
        if (field.equals("url")) {
            String url = node.getStringValue("url");
            if (url != null) {
                url = org.mmbase.util.transformers.Xml.XMLEscape(url);
                return "<a href=\"" + url + "\" class=\"mm_gui\" onclick=\"window.open(this.href); return false;\">" + url + "</a>";
            }
        }
        return null;
    }


    @Override
    public String getDefaultUrl(int src) {
        MMObjectNode node = getNode(src);
        return node.getStringValue("url");
    }


}
