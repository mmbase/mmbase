/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.repository.forms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import net.sf.mmapps.commons.bridge.RelationUtil;
import net.sf.mmapps.commons.util.StringUtil;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.mmbase.bridge.*;

import com.finalist.cmsc.repository.RepositoryUtil;
import com.finalist.cmsc.services.publish.Publish;
import com.finalist.cmsc.services.workflow.Workflow;
import com.finalist.cmsc.struts.MMBaseFormlessAction;


public class ChannelDelete extends MMBaseFormlessAction {

    @Override
    public ActionForward execute(ActionMapping mapping,
            HttpServletRequest request, Cloud cloud) throws Exception {
        
        String objectnumber = getParameter(request, "number", true);
        String action = getParameter(request, "remove");
        Node channelNode = cloud.getNode(objectnumber);

        if (StringUtil.isEmptyOrWhitespace(action)) {
            if (RepositoryUtil.hasCreatedContent(channelNode)) {
                return mapping.findForward("channeldeletewarning");
            }
            else {
                return mapping.findForward("channeldelete");
            }
        }
        else {
            if ("delete".equals(action)) {
                NodeList createdElements = RepositoryUtil.getCreatedElements(channelNode);
                for (Iterator<Node> iter = createdElements.iterator(); iter.hasNext();) {
                    Node objectNode = iter.next();
                    RepositoryUtil.removeContentFromChannel(objectNode, channelNode);
                    RepositoryUtil.removeCreationRelForContent(objectNode);
                    
                    RepositoryUtil.removeContentFromAllChannels(objectNode);
                    RepositoryUtil.addContentToChannel(objectNode, RepositoryUtil.getTrashNode(cloud));
                    
                    Publish.remove(objectNode);
                    Publish.unpublish(objectNode);
                    Workflow.remove(objectNode);
                }
                return mapping.findForward("channeldelete");
            }
            else if ("move".equals(action)) {
                // get relations of content elements to channels other then the creationchannel
                NodeList createdElements = RepositoryUtil.getCreatedElements(channelNode);
                
                for (Iterator<Node> iter = createdElements.iterator(); iter.hasNext();) {
                    Node elementNode = iter.next();
                    // get relations
                    RelationManager contentRelationManager = cloud.getRelationManager("contentrel");
                    NodeList relatedChannelsList = 
                        contentRelationManager.getList("(snumber != " + channelNode.getNumber() + " and dnumber = " + elementNode.getNumber() + ")", "number", "UP");
               
                    // loop through channel relations
                    Iterator<Node> iter2 = relatedChannelsList.iterator();
                    
                    if (iter2.hasNext()) {
                        Node relationNode = iter2.next();
                        Node newChannelNode = cloud.getNode(relationNode.getStringValue("snumber"));
                        
                        // move content element to the channel
                        RepositoryUtil.removeContentFromChannel(elementNode, channelNode);
                        RepositoryUtil.removeCreationRelForContent(elementNode);
                        RepositoryUtil.addCreationChannel(elementNode, newChannelNode);
        
                        // unpublish and remove from workflow
                        Publish.remove(elementNode);
                        Publish.unpublish(elementNode);
                        Workflow.remove(elementNode);                        
                    }
                }
                return mapping.findForward("channeldelete");
            }
            else if ("cancel".equals(action)) {
                return mapping.findForward(SUCCESS);
            }
            else {
                if (Workflow.hasWorkflow(channelNode)) {
                    Workflow.remove(channelNode);
                }
                Publish.remove(channelNode);
                Publish.unpublish(channelNode);
                Workflow.remove(channelNode);
                
                channelNode.delete(true);
                return mapping.findForward(SUCCESS);
            }
        }
    }
    
}
