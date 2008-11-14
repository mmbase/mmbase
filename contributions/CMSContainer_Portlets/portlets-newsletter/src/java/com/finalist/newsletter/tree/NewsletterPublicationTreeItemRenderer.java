package com.finalist.newsletter.tree;

import org.apache.commons.lang.StringUtils;
import org.mmbase.bridge.Node;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import com.finalist.cmsc.navigation.NavigationRenderer;
import com.finalist.cmsc.navigation.NavigationTreeItemRenderer;
import com.finalist.cmsc.navigation.NavigationUtil;
import com.finalist.cmsc.navigation.PagesUtil;
import com.finalist.cmsc.navigation.ServerUtil;
import com.finalist.cmsc.security.SecurityUtil;
import com.finalist.cmsc.security.UserRole;
import com.finalist.cmsc.services.publish.Publish;
import com.finalist.newsletter.domain.EditionStatus;
import com.finalist.newsletter.domain.Publication;
import com.finalist.newsletter.util.NewsletterPublicationUtil;
import com.finalist.tree.TreeElement;
import com.finalist.tree.TreeModel;
import com.finalist.util.module.ModuleUtil;


public class NewsletterPublicationTreeItemRenderer implements NavigationTreeItemRenderer {

   private static Logger log = Logging.getLoggerInstance(NewsletterPublicationTreeItemRenderer.class.getName());

   protected static final String FEATURE_WORKFLOW = "workflowitem";

   public void addParentOption(NavigationRenderer renderer, TreeElement element, String parentId) {
      // nothing
   }

   public TreeElement getTreeElement(NavigationRenderer renderer, Node parentNode, TreeModel model) {
      UserRole role = NavigationUtil.getRole(parentNode.getCloud(), parentNode, false);
      boolean secure = parentNode.getBooleanValue(PagesUtil.SECURE_FIELD);

      String name = parentNode.getStringValue(PagesUtil.TITLE_FIELD);
      String fragment = parentNode.getStringValue(NavigationUtil.getFragmentFieldname(parentNode));

      String id = String.valueOf(parentNode.getNumber());
      TreeElement element = renderer.createElement(parentNode, role, name, fragment, secure);

      if (SecurityUtil.isEditor(role)) {
         element.addOption(renderer.createTreeOption("edit_defaults.png", "site.newsletteredition.edit", "newsletter",
                  "../newsletter/NewsletterPublicationEdit.do?number=" + id));

         boolean isSingleApplication = true;
         boolean isPublished;
         isSingleApplication = ServerUtil.isSingle();
         if (isSingleApplication) {
           // NewsletterPublicationService publicationService = (NewsletterPublicationService) ApplicationContextFactory.getBean("publicationService");
            Publication.STATUS status = NewsletterPublicationUtil.getStatus(parentNode.getCloud(),parentNode.getNumber());
            isPublished = Publication.STATUS.DELIVERED.equals(status);
         } else {
            isPublished = Publish.isPublished(parentNode);
         }

         log.debug("Publication " + parentNode.getNumber() + "'s publication status:" + isPublished + " in single:" + isSingleApplication);

         if (SecurityUtil.isWebmaster(role) || (model.getChildCount(parentNode) == 0 && !isPublished)) {
            element.addOption(renderer.createTreeOption("delete.png", "site.newsletteredition.remove", "newsletter",
                     "../newsletter/NewsletterPublicationDelete.do?number=" + id));
            element.addOption(renderer.createTreeOption("type/email_error.png", "site.newsletteredition.publish", "newsletter",
                     "../newsletter/NewsletterPublicationPublish.do?number=" + id));
            element.addOption(renderer.createTreeOption("type/email_go.png", "site.newsletteredition.test", "newsletter",
                     "../newsletter/NewsletterPublicationTest.do?number=" + id));
         }

         if (NavigationUtil.getChildCount(parentNode) >= 2) {
            element.addOption(renderer.createTreeOption("reorder.png", "site.page.reorder", "reorder.jsp?parent=" + id));
         }

         if (SecurityUtil.isWebmaster(role) && ModuleUtil.checkFeature(FEATURE_WORKFLOW)) {
            element.addOption(renderer.createTreeOption("mail.png", "site.newsletteredition.publish", "newsletter",
                     "../workflow/publish.jsp?number=" + id));
         }
         if (SecurityUtil.isWebmaster(role)) {
            String status = NewsletterPublicationUtil.getEditionStatus(Integer.valueOf(id));
            if(EditionStatus.INITIAL.value().equals(status)) {
               element.addOption(renderer.createTreeOption("arrow_right.png", "site.newsletteredition.freeze", "newsletter",
                     "../newsletter/NewsletterEditionFreeze.do?number=" + id));
            }
            if(EditionStatus.FROZEN.value().equals(status)) {
               element.addOption(renderer.createTreeOption("arrow_undo.png", "site.newsletteredition.defrost", "newsletter",
                  "../newsletter/NewsletterEditionDefrost.do?number=" + id));
            }
         }
      }
      if(SecurityUtil.isWebmaster(role)){
         String status = NewsletterPublicationUtil.getEditionStatus(Integer.parseInt(id));
         if("approved".equalsIgnoreCase(status)){
            element.addOption(renderer.createTreeOption("status_onlive.png", "site.newsletteredition.revokeapproval", "newsletter","../newsletter/NewsletterEditionRevoke.do?number=" + id));
         }else if("frozen".equalsIgnoreCase(status)){
            element.addOption(renderer.createTreeOption("status_published.png", "site.newsletteredition.approve", "newsletter","../newsletter/NewsletterEditionApprove.do?number=" + id));
         }
      }
      element.addOption(renderer.createTreeOption("rights.png", "site.page.rights", "../usermanagement/pagerights.jsp?number=" + id));

      return element;
   }

   public boolean showChildren(Node parentNode) {
      return true;
   }

}