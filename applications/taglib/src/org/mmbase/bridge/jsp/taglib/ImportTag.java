/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;

import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.JspTagException;

/**
* The importtag takes its body, and writes it to the context.
* 
* @author Michiel Meeuwissen
*/
public class ImportTag extends CloudReferrerTag {

    public int doAfterBody() throws JspTagException{
        findCloudProvider().register(getId(), bodyContent.getString());
        return SKIP_BODY;
    }

}
