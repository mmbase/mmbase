/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.taglib.form;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import net.sf.mmapps.commons.util.StringUtil;

public class SelectTag extends SimpleTagSupport {

    public String var;

    public String selected;
    
    public String defaultValue;
    
    public void setVar(String var) {
        this.var = var;
    }
    
    @Override
    public void doTag() throws JspException, IOException {
        PageContext ctx = (PageContext) getJspContext();
        HttpServletRequest request = (HttpServletRequest) ctx.getRequest();

        selected = (String) request.getAttribute(var);
        
        if (StringUtil.isEmpty(selected)) {
            selected = defaultValue;
        }
        ctx.getOut().print("<select name=\"" + var + "\">");
        JspFragment frag = getJspBody();
        if (frag != null) {
            frag.invoke(null);
        }
        ctx.getOut().print("</select>");
    }
    
    public String getSelected() {
        return selected;
    }

    
    public String getDefault() {
        return defaultValue;
    }

    public void setDefault(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
}