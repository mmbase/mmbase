/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package com.finalist.cmsc.subsite.forms;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.mmapps.commons.util.StringUtil;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;

import com.finalist.cmsc.repository.RepositoryUtil;
import com.finalist.cmsc.struts.MMBaseAction;
import com.finalist.cmsc.subsite.util.SubSiteUtil;

public class PPContentAction extends MMBaseAction {

   @Override
   public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	         HttpServletResponse response, Cloud cloud) throws Exception {

	   int personalpage = Integer.parseInt(request.getParameter("personalpage"));
	  
	  Node ppNode = cloud.getNode(personalpage);
	  Node ppChannel = SubSiteUtil.getPersonalpageChannel(ppNode);
	  
	  if (ppChannel != null) {
	     String orderby = request.getParameter("orderby");
	     String direction = request.getParameter("direction");
	     if (StringUtil.isEmpty(orderby)) {
	        orderby = null;
	     }
	     if (StringUtil.isEmpty(direction)) {
	        direction = null;
	     }
         NodeList elements = RepositoryUtil.getLinkedElements(ppChannel, null, orderby, direction, false, -1, -1, -1, -1, -1);
         addToRequest(request, "elements", elements);
         
         String url = "/editors/repository/content.jsp?parentchannel=" + ppChannel.getNumber();
         request.setAttribute("returnUrl", url);
         return new ActionForward(url);
      }
	  
      //return super.execute(mapping, form, request, response, cloud);
      return mapping.findForward(SUCCESS);
   }

}
