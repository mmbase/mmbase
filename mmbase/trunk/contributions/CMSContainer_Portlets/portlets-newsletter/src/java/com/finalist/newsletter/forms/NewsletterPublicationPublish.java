/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package com.finalist.newsletter.forms;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mmbase.bridge.Cloud;

import com.finalist.cmsc.struts.MMBaseFormlessAction;
import com.finalist.newsletter.NewsletterPublisher;

public class NewsletterPublicationPublish extends MMBaseFormlessAction {

   @Override
   public ActionForward execute(ActionMapping mapping, HttpServletRequest request, Cloud cloud) throws Exception {

      String number = request.getParameter("number");
      if (number != null) {
         Thread publisher = new NewsletterPublisher(number);
         publisher.start();
      }
      ActionForward ret = mapping.findForward("SUCCESS");
      return ret;
   }
}
