package com.finalist.newsletter.tree;

import org.mmbase.bridge.Node;

import com.finalist.cmsc.navigation.NavigationRenderer;
import com.finalist.cmsc.navigation.NavigationTreeItemRenderer;
import com.finalist.cmsc.navigation.NavigationUtil;
import com.finalist.cmsc.navigation.PagesUtil;
import com.finalist.cmsc.security.SecurityUtil;
import com.finalist.cmsc.security.UserRole;
import com.finalist.cmsc.services.publish.Publish;
import com.finalist.tree.TreeElement;
import com.finalist.tree.TreeModel;
import com.finalist.util.module.ModuleUtil;

public class NewsletterTreeItemRenderer implements NavigationTreeItemRenderer {

   protected static final String FEATURE_PAGEWIZARD = "pagewizarddefinition";
   protected static final String FEATURE_WORKFLOW = "workflowitem";

   public void addParentOption(NavigationRenderer renderer, TreeElement element, String parentId) {
      element.addOption(renderer.createTreeOption("mail.png", "site.newsletter.new", "newsletter",
            "../newsletter/NewsletterCreate.do?parentnewsletter=" + parentId));
   }

   public TreeElement getTreeElement(NavigationRenderer renderer, Node parentNode, TreeModel model) {
      UserRole role = NavigationUtil.getRole(parentNode.getCloud(), parentNode, false);
      boolean secure = parentNode.getBooleanValue(PagesUtil.SECURE_FIELD);

      String name = parentNode.getStringValue(PagesUtil.TITLE_FIELD);
      String fragment = parentNode.getStringValue(NavigationUtil.getFragmentFieldname(parentNode));

      String id = String.valueOf(parentNode.getNumber());
      TreeElement element = renderer.createElement(parentNode, role, name, fragment, secure);

      if (SecurityUtil.isEditor(role)) {
         element.addOption(renderer.createTreeOption("edit_defaults.png", "site.newsletter.edit", "newsletter",
               "../newsletter/NewsletterEdit.do?number=" + id));
         element.addOption(renderer.createTreeOption("mail.png", "site.newsletterpublication.new.blank", "newsletter",
               "../newsletter/NewsletterPublicationCreate.do?parent=" + id + "&copycontent=false"));
         element.addOption(renderer.createTreeOption("mail.png", "site.newsletterpublication.new.withcontent", "newsletter",
               "../newsletter/NewsletterPublicationCreate.do?parent=" + id + "&copycontent=true"));

         if (SecurityUtil.isWebmaster(role) || (model.getChildCount(parentNode) == 0 && !Publish.isPublished(parentNode))) {
            element.addOption(renderer.createTreeOption("delete.png", "site.newsletter.remove", "newsletter",
                  "../newsletter/NewsletterDelete.do?number=" + id));
         }

         if (NavigationUtil.getChildCount(parentNode) >= 2) {
            element.addOption(renderer.createTreeOption("reorder.png", "site.page.reorder", "reorder.jsp?parent=" + id));
         }

         if (SecurityUtil.isChiefEditor(role)) {
            element.addOption(renderer.createTreeOption("cut.png", "site.page.cut", "javascript:cut('" + id + "');"));
            element.addOption(renderer.createTreeOption("copy.png", "site.page.copy", "javascript:copy('" + id + "');"));
            element.addOption(renderer.createTreeOption("paste.png", "site.page.paste", "javascript:paste('" + id + "');"));
         }

         if (SecurityUtil.isWebmaster(role) && ModuleUtil.checkFeature(FEATURE_WORKFLOW)) {
            element.addOption(renderer.createTreeOption("publish.png", "site.newsletter.publish", "newsletter", "../workflow/publish.jsp?number="
                  + id));
            element.addOption(renderer.createTreeOption("masspublish.png", "site.newsletter.masspublish", "newsletter",
                  "../workflow/masspublish.jsp?number=" + id));
         }
      }
      element.addOption(renderer.createTreeOption("rights.png", "site.page.rights", "../usermanagement/pagerights.jsp?number=" + id));

      return element;
   }

   public boolean showChildren(Node parentNode) {
      return true; // Always show sub-items
   }

}
