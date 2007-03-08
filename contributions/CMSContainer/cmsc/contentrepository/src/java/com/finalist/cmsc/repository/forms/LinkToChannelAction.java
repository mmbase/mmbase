/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.repository.forms;

import javax.servlet.http.HttpServletRequest;

import net.sf.mmapps.commons.util.StringUtil;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mmbase.bridge.*;

import com.finalist.cmsc.repository.RepositoryUtil;
import com.finalist.cmsc.struts.MMBaseFormlessAction;
import com.finalist.cmsc.services.publish.Publish;
import com.finalist.cmsc.services.workflow.Workflow;

import java.util.Enumeration;


public class LinkToChannelAction extends MMBaseFormlessAction {

    public ActionForward execute(ActionMapping mapping,
            HttpServletRequest request, Cloud cloud) throws Exception {

        String action = getParameter(request, "action");
        String channelnumber = getParameter(request, "channelnumber");
        Node channelNode = cloud.getNode(channelnumber);

        String objectnumber = getParameter(request, "objectnumber");


        if (action != null && action.equals("unlink")) {

           Node objectNode = cloud.getNode(objectnumber);

            if(RepositoryUtil.isCreationChannel(objectNode, channelNode)) {
                NodeList contentchannels = RepositoryUtil.getContentChannels(objectNode);
                if (contentchannels.size() <= 1) {
                    RepositoryUtil.removeContentFromChannel(objectNode, channelNode);
                    RepositoryUtil.removeCreationRelForContent(objectNode);
                    RepositoryUtil.addContentToChannel(objectNode, RepositoryUtil.getTrash(cloud));
                    
                    // unpublish and remove from workflow
                    Publish.remove(objectNode);
                    Workflow.remove(objectNode);
                }
                else {
                    String destinationnumber = getParameter(request, "destionationchannel");
                    if (!StringUtil.isEmpty(destinationnumber)) {
                        Node newCreationNode = cloud.getNode(destinationnumber);
                        
                        RepositoryUtil.removeContentFromChannel(objectNode, channelNode);
                        RepositoryUtil.removeCreationRelForContent(objectNode);
                        
                        if (RepositoryUtil.isTrash(newCreationNode)) {
                            RepositoryUtil.removeContentFromAllChannels(objectNode);
                            RepositoryUtil.addContentToChannel(objectNode, newCreationNode.getStringValue("number"));
                            
                            // unpublish and remove from workflow
                            Publish.remove(objectNode);
                            Workflow.remove(objectNode);    
                        }
                        else {
                            RepositoryUtil.addCreationChannel(objectNode, newCreationNode);
                        }
                    }
                    else {
                        addToRequest(request, "content", objectNode);
                        addToRequest(request, "creationchannel", channelNode);
                        addToRequest(request, "contentchannels", contentchannels);
                        addToRequest(request, "trashchannel", RepositoryUtil.getTrashNode(cloud));
                        return mapping.findForward("unlinkcreation");
                    }
                }
            }
            else {
                RepositoryUtil.removeContentFromChannel(objectNode, channelNode);
            }
            if (!Workflow.hasWorkflow(channelNode)) {
               Workflow.create(channelNode, "");
            }

        }
        else {
           // Link them all.

           Enumeration parameters = request.getParameterNames();
           while (parameters.hasMoreElements()) {
              String parameter = (String) parameters.nextElement();

              if (parameter.startsWith("link_")) {
                 String link = request.getParameter(parameter);
                 if (!Workflow.hasWorkflow(channelNode)) {
                    Workflow.create(channelNode, "");
                 }
                 RepositoryUtil.addContentToChannel(cloud.getNode(link), channelnumber);
              }
           }
        }

        String returnurl = request.getParameter("returnurl");

        if(returnurl != null) {
           return new ActionForward(returnurl, true);
        }
        String url = mapping.findForward(SUCCESS).getPath() + "?parentchannel=" + channelnumber;
        return new ActionForward(url, true);
    }

}
