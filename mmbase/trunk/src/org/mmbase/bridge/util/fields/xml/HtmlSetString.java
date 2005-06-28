/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.util.fields.xml;
import org.mmbase.bridge.util.fields.Processor;
import org.mmbase.bridge.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.*;

/**
 *
 * @author Michiel Meeuwissen
 * @version $Id: HtmlSetString.java,v 1.1 2005-06-28 14:19:54 michiel Exp $
 * @since MMBase-1.8
 */

public class HtmlSetString implements  Processor {
    private static final Logger log = Logging.getLoggerInstance(HtmlSetString.class);

    protected static final String PREF = "<p><![CDATA[";
    protected static final String POST = "]]></p>";
    public Object process(Node node, Field field, Object value) {
        if (value instanceof org.w3c.dom.Document) return value;
        log.debug("Getting " + field + " from " + node + " as a String");
        return Casting.toXML(PREF + Casting.toString(value) + POST);
    }
  
}
