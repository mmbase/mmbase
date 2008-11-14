package com.finalist.newsletter.forms;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;

import com.finalist.cmsc.struts.MMBaseFormlessAction;
import com.finalist.newsletter.domain.EditionStatus;
import com.finalist.newsletter.util.NewsletterPublicationUtil;

public class NewsletterEditionApprovePublicationAction extends MMBaseFormlessAction{
   private static final String SUCCESS = "success";
   private static final String SAVE = "save";
   private static final String ACTION_CANCEL = "cancel";
   
   public ActionForward execute(ActionMapping mapping,
         HttpServletRequest request, Cloud cloud) throws Exception {
      int number = Integer.parseInt(getParameter(request, "number", true));

      if(isSaveAction(request)) {
         Node edition = cloud.getNode(number);
         request.setAttribute("action", "approve");
         if(!EditionStatus.APPROVED.value().equals(edition.getValue("process_status"))) {
            NewsletterPublicationUtil.approveEdition(edition);
         }
         if (StringUtils.isNotBlank(request.getParameter("forward"))) {
            ActionForward ret = new ActionForward(mapping.findForward("publicationedit").getPath() + "?newsletterId="
                     + request.getParameter("newsletterId"));
            ret.setRedirect(true);
            return ret;
         }
         return mapping.findForward(SUCCESS);
      }
      if(isCancelAction(request)) {
         String forwardPath = mapping.findForward("cancel").getPath();
         forwardPath = forwardPath.concat("?showpage=" + number);
        if (StringUtils.isNotBlank(request.getParameter("forward"))) {
            ActionForward ret = new ActionForward(mapping.findForward("publicationedit").getPath() + "?newsletterId="
                     + request.getParameter("newsletterId"));
            ret.setRedirect(true);
            return ret;
         }
         return new ActionForward(forwardPath);
      }
      request.setAttribute("action", "approve");
      return mapping.findForward("confirm");
   }
   private boolean isCancelAction(HttpServletRequest request) {
      return getParameter(request, ACTION_CANCEL) != null;
   }
   private boolean isSaveAction(HttpServletRequest request) {
      return getParameter(request, SAVE) != null;
   }
}
