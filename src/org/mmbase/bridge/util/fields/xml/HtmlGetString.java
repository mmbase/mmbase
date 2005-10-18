/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.util.fields.xml;

import org.mmbase.bridge.util.fields.Processor;
import org.mmbase.bridge.*;

/**
 * Currently is like FieldGetString.
 * @author Michiel Meeuwissen
 * @version $Id: HtmlGetString.java,v 1.3 2005-10-18 11:34:59 michiel Exp $
 * @since MMBase-1.8
 */

public class HtmlGetString implements  Processor {

    private static final int serialVersionUID = 1;

    private Processor processor = new FieldGetString();

    public Object process(Node node, Field field, Object value) {
        return processor.process(node, field, value);
    }

    public String toString() {
        return "get_HTML";
    }
}
