/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib.macro;

import javax.servlet.jsp.JspTagException;
import org.mmbase.bridge.jsp.taglib.ListConditionTag;
/**
*  shortcut for listContdition odd
*/
public class ListOddTag extends ListConditionTag{
	public int doStartTag() throws JspTagException{
		setValue("odd");
		return super.doStartTag();
	}
}
