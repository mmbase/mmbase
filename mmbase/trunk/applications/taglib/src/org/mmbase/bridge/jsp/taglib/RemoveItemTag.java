/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.jsp.taglib;
import org.mmbase.bridge.jsp.taglib.util.Attribute;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspException;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Removes the current item from the list. In a 'reuse' of the list, the item will not be present any
 * more.
 *
 * @author Michiel Meeuwissen
 * @version $Id: RemoveItemTag.java,v 1.2 2003-08-27 21:33:36 michiel Exp $ 
 */

public class RemoveItemTag extends ListReferrerTag {

    private static final Logger log = Logging.getLoggerInstance(RemoveItemTag.class);

    public int doStartTag() throws JspTagException{
        ListProvider list = getList();
        list.remove();
        return SKIP_BODY;
    }


}
