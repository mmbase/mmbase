/*
 * 
 * This software is OSI Certified Open Source Software. OSI Certified is a certification mark of the Open Source
 * Initiative.
 * 
 * The license (Mozilla version 1.0) can be read at the MMBase site. See http://www.MMBase.org/license
 */
package com.finalist.cmsc.repository.forms;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mmbase.bridge.*;

import com.finalist.cmsc.repository.RepositoryUtil;
import com.finalist.cmsc.struts.MMBaseFormlessAction;
import com.finalist.cmsc.services.publish.Publish;
import com.finalist.cmsc.services.workflow.Workflow;

import java.util.*;

public class AssetDeleteAction extends MMBaseFormlessAction {

   @Override
   public ActionForward execute(ActionMapping mapping, HttpServletRequest request, Cloud cloud) throws Exception {

      //String action = getParameter(request, "action");
      String channelnumber = getParameter(request, "channelnumber");
      Node channelNode = cloud.getNode(channelnumber);
      String objectnumber = getParameter(request, "objectnumber");

      Node objectNode = cloud.getNode(objectnumber);

      RepositoryUtil.removeAssetFromChannel(objectNode, channelNode);
      RepositoryUtil.removeCreationRelForAsset(objectNode);
      RepositoryUtil.addAssetToChannel(objectNode, RepositoryUtil.getTrashNode(cloud));

      String returnurl = request.getParameter("returnurl");

      if (returnurl != null) {
         return new ActionForward(returnurl, true);
      }
      String url = mapping.findForward(SUCCESS).getPath() + "?parentchannel=" + channelnumber;
      return new ActionForward(url, true);
   }
}
