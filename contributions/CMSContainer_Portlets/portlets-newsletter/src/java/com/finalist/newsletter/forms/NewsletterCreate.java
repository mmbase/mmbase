/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package com.finalist.newsletter.forms;

import javax.servlet.http.HttpServletRequest;

import net.sf.mmapps.commons.bridge.RelationUtil;
import net.sf.mmapps.commons.util.StringUtil;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeManager;

import com.finalist.cmsc.navigation.NavigationUtil;
import com.finalist.cmsc.navigation.PagesUtil;
import com.finalist.cmsc.struts.MMBaseFormlessAction;
import com.finalist.newsletter.util.NewsletterPublicationUtil;

public class NewsletterCreate extends MMBaseFormlessAction {

   @Override
   public ActionForward execute(ActionMapping mapping, HttpServletRequest request, Cloud cloud) throws Exception {

      String parentnewsletter = getParameter(request, "parentnewsletter", true);
      String action = getParameter(request, "action");

      if (StringUtil.isEmptyOrWhitespace(action)) {
         request.getSession().setAttribute("parentnewsletter", parentnewsletter);

         ActionForward ret = new ActionForward(mapping.findForward("openwizard").getPath() + "?action=create" + "&contenttype=newsletter"
               + "&returnurl=" + mapping.findForward("returnurl").getPath());
         ret.setRedirect(true);
         return ret;
      } else {
         if ("save".equals(action)) {
            String ewnodelastedited = getParameter(request, "ewnodelastedited");
            NavigationUtil.appendChild(cloud, parentnewsletter, ewnodelastedited);

            Node newNewsletter = cloud.getNode(ewnodelastedited);
            Node layoutNode = PagesUtil.getLayout(newNewsletter);
            PagesUtil.linkPortlets(newNewsletter, layoutNode);
            request.getSession().removeAttribute("parentnewsletter");

            // Create a default term for this newsletter
            
            NewsletterPublicationUtil.createDefaultTerm(newNewsletter);
           

            addToRequest(request, "showpage", ewnodelastedited);
            ActionForward ret = mapping.findForward(SUCCESS);
            return ret;
         }
         request.getSession().removeAttribute("parentnewsletter");
         ActionForward ret = mapping.findForward(CANCEL);
         return ret;
      }
   }

}
