/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.navigation.select.forms;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.mmapps.commons.util.StringUtil;

import org.apache.struts.action.*;
import org.mmbase.bridge.*;

import com.finalist.cmsc.mmbase.TreeUtil;
import com.finalist.cmsc.navigation.*;
import com.finalist.cmsc.navigation.select.SelectRenderer;
import com.finalist.cmsc.util.bundles.JstlUtil;
import com.finalist.tree.TreeInfo;
import com.finalist.tree.ajax.AjaxTree;
import com.finalist.tree.ajax.SelectAjaxRenderer;


public class SelectorAction extends com.finalist.cmsc.struts.SelectorAction {

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response, Cloud cloud) throws Exception {

        String action = request.getParameter("action");
        if(StringUtil.isEmpty(action)) {
            NavigationInfo info = new NavigationInfo(NavigationUtil.getNavigationInfo(cloud));
            cloud.setProperty("Selector" + NavigationInfo.class.getName(), info);
        }
        
        JstlUtil.setResourceBundle(request, "cmsc-site");
        return super.execute(mapping, form, request, response, cloud);
    }

    @Override
    protected String getChannelId(HttpServletRequest request, Cloud cloud) {
        String path = request.getParameter("path");
        if(!StringUtil.isEmpty(path)) {
            Node parentNode = NavigationUtil.getPageFromPath(cloud, path);
            if(parentNode != null) {
                return String.valueOf(parentNode.getNumber());
            }
        }
        return super.getChannelId(request, cloud);
    }
    
    protected Node getRootNode(Cloud cloud) {
        return null;
    }

    protected TreeInfo getTreeInfo(Cloud cloud) {
        TreeInfo info = (TreeInfo) cloud.getProperty("Selector" + NavigationInfo.class.getName());
        return info;
    }

    protected List<Node> getOpenChannels(Node channelNode) {
        if (PagesUtil.isPageType(channelNode)) {
            return NavigationUtil.getPathToRoot(channelNode);
        }
        return null;
    }

    protected AjaxTree getTree(HttpServletRequest request, HttpServletResponse response, Cloud cloud, TreeInfo info, String persistentid) {
        Node node = cloud.getNode(persistentid);
        if (!SiteUtil.isSite(node)) {
            node = NavigationUtil.getSiteFromPath(cloud, node.getStringValue(TreeUtil.PATH_FIELD));
        }

        NavigationTreeModel model = new NavigationTreeModel(node);
        SelectAjaxRenderer chr = new SelectRenderer(response, getLinkPattern(), getTarget());
        AjaxTree t = new AjaxTree(model, chr, info);
        t.setImgBaseUrl("../../gfx/icons/");
        return t;
    }
    
    protected List<String> getChildren(Cloud cloud, String path) {
        List<String> strings = new ArrayList<String>();
        if (StringUtil.isEmpty(path)) {
            NodeList sites = SiteUtil.getSites(cloud);
            for (Iterator<Node> iter = sites.iterator(); iter.hasNext();) {
                Node child = iter.next();
                strings.add(child.getStringValue(TreeUtil.PATH_FIELD));
            }
        }
        else {
            Node parentNode = NavigationUtil.getPageFromPath(cloud, path);
            if(parentNode != null) {
                NodeList children = NavigationUtil.getChildren(parentNode);
                for (Iterator<Node> iter = children.iterator(); iter.hasNext();) {
                    Node child = iter.next();
                    strings.add(child.getStringValue(TreeUtil.PATH_FIELD));
                }
            }
        }
        return strings;
    }
    
}
