/*
 
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
 
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 
 */
package org.mmbase.bridge.jsp.taglib.pageflow;

import org.mmbase.bridge.jsp.taglib.util.Attribute;
import javax.servlet.jsp.JspTagException;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Like IncludeTag, but an entire tree of files is being probed to find the one
 * that has the most specified value.
 *
 * This is a taglib-implementation of the 'TREEPART' command, but renamed to
 * 'Treeinclude' for esthetic reasons :)
 * A full description of this command can be found in the mmbase-taglib.xml file.
 *
 * @author Johannes Verelst
 */
public class TreeIncludeTag extends IncludeTag {
    
    private static Logger log = Logging.getLoggerInstance(TreeIncludeTag.class.getName());
    protected Attribute objectList = Attribute.NULL;
    private TreeHelper th = new TreeHelper();
    
    public int doStartTag() throws JspTagException {        
        if (objectList == Attribute.NULL) {
            throw new JspTagException("Attribute 'objectlist' was not specified");
        }
        return super.doStartTag();
    }

    protected String getPage() throws JspTagException {        
        String orgPage = super.getPage();
        String treePage = th.findTreeFile(orgPage, objectList.getString(this), pageContext.getSession());
        if (log.isDebugEnabled()) {
            log.debug("Retrieving page '" + treePage + "'");
        }
        if (treePage == null) throw new JspTagException("Could not find page " + orgPage);
        return treePage;
    }

    public void doAfterBodySetValue() throws JspTagException {
        // sigh, we would of course prefer to extend, but no multiple inheritance possible in Java..
        th.setCloud(getCloud());
    
        // Let IncludeTag do the rest of the work
        includePage();
    }
    
    public void setObjectlist(String p) throws JspTagException {
        objectList = getAttribute(p);
    }

        
}
