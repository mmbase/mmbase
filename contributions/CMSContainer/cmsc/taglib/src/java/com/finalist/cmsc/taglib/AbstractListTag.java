/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.taglib;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import net.sf.mmapps.commons.util.StringUtil;


/**
 * Register a list as attribute in the request.
 */
public abstract class AbstractListTag<E> extends CmscTag {

	public Object origin;
	
	/**
	 * JSP variable name.
	 */
	public String var;
	
	public void doTag() {
		PageContext ctx = (PageContext) getJspContext();
		HttpServletRequest request = (HttpServletRequest) ctx.getRequest();
		
		List<E> list = getList();
        // handle result
		setAttribute(request, list);
	}

    protected void setAttribute(HttpServletRequest request, List<E> list) {
		if (!StringUtil.isEmpty(var)) {
			// put in variable
			if (list != null) {
				request.setAttribute(var, list);
			} else {
				request.removeAttribute(var);
			}
		}
    }

	public void setVar(String var) {
		this.var = var;
	}
    public String getVar() {
        return var;
    }

	public void setOrigin(Object origin) {
		this.origin = origin;
	}

	protected abstract List<E> getList();

}
