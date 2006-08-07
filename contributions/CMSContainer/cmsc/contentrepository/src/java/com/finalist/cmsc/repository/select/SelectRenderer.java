/*

 This software is OSI Certified Open Source Software.
 OSI Certified is a certification mark of the Open Source Initiative.

 The license (Mozilla version 1.0) can be read at the MMBase site.
 See http://www.MMBase.org/license

 */
package com.finalist.cmsc.repository.select;

import javax.servlet.http.HttpServletResponse;

import org.mmbase.bridge.Node;

import com.finalist.cmsc.repository.RepositoryUtil;
import com.finalist.tree.ajax.SelectAjaxRenderer;

public class SelectRenderer extends SelectAjaxRenderer {

    public SelectRenderer(HttpServletResponse response, String linkPattern, String target) {
       super(response, linkPattern, target);
    }
    
    protected String getName(Node parentNode) {
        return parentNode.getStringValue(RepositoryUtil.NAME_FIELD);
    }

    protected String getFragment(Node parentNode) {
        return parentNode.getStringValue( RepositoryUtil.getFragmentFieldname(parentNode) );
    }
    
    public String getIcon(Object node) {
        Node n = (Node) node;
        return "type/" + n.getNodeManager().getName() + ".png";
    }
}
