/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib.pageflow;

import org.mmbase.bridge.jsp.taglib.ContextReferrerTag;
import javax.servlet.jsp.JspTagException;


/**
* Adds an extra parameter to the parent URL tag.
* 
* @author Michiel Meeuwissen
*/
public class ParamTag extends ContextReferrerTag {
    
    private String name = null;
           
    public void setName(String n) throws JspTagException {
        name = getAttributeValue(n);
    }

    public int doAfterBody() throws JspTagException {

        // find the parent URL-tag
        UrlTag urlTag = (UrlTag) findParentTag("org.mmbase.bridge.jsp.taglib.pageflow.UrlTag", null);

        // the value is the body context.
        
        urlTag.addParameter(name, bodyContent.getString());
        return SKIP_BODY;
    }

}
