package com.finalist.newsletter.util;

import java.util.ArrayList;
import java.util.List;

import net.sf.mmapps.commons.bridge.RelationUtil;
import net.sf.mmapps.modules.cloudprovider.CloudProviderFactory;

import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.NodeManager;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import com.finalist.cmsc.navigation.NavigationUtil;
import com.finalist.cmsc.navigation.PagesUtil;
import com.finalist.newsletter.CloneUtil;

public abstract class NewsletterPublicationUtil {

   private static Logger log = Logging.getLoggerInstance(NewsletterPublicationUtil.class.getName());

   public static Node createPublication(String newsletterNumber) {
      if (newsletterNumber == null) {
         return(null);
      }
      Cloud cloud = CloudProviderFactory.getCloudProvider().getCloud();
      Node newsletterNode = cloud.getNode(newsletterNumber);
      log.debug("Creating a new publication for newsletter " + newsletterNode.getNumber());

      Node publicationNode = CloneUtil.cloneNode(newsletterNode, "newsletterpublication");
      if (publicationNode != null) {
         log.debug("Creation of publication node successfull. Now copying themes and content");
         NavigationUtil.appendChild(newsletterNode, publicationNode);
         Node layoutNode = PagesUtil.getLayout(newsletterNode);
         PagesUtil.linkPortlets(publicationNode, layoutNode);

         NodeList newsletterThemeList = newsletterNode.getRelatedNodes("newslettertheme");
         if (newsletterThemeList != null) {
            log.debug("Found " + newsletterThemeList.size() + " themes for newsletter " + newsletterNode.getNumber());
            for (int i = 0; i < newsletterThemeList.size(); i++) {
               Node oldThemeNode = newsletterThemeList.getNode(i);
               Node newThemeNode = CloneUtil.cloneNode(oldThemeNode, "newsletterpublicationtheme");
               if (newThemeNode != null) {
                  log.debug("Theme " + oldThemeNode.getNumber() + " copied");
                  NodeList content = SearchUtil.findRelatedNodeList(oldThemeNode, null, "newslettercontent");
                  if (content != null && content.size() > 0) {
                     log.debug("Found " + content.size() + " content items for theme " + oldThemeNode.getNumber());
                     for (int a = 0; a < content.size(); a++) {
                        Node contentNode = content.getNode(a);
                        RelationUtil.createRelation(newThemeNode, contentNode, "newslettercontent");
                        log.debug("Copied content node " + contentNode.getNumber() + " to theme " + newThemeNode.getNumber());
                     }
                  }
               }
            }
         }
         return (publicationNode);
      }
      return (null);
   }

   // Delete a publication, only if not yet published
   public static void deletePublication(String publicationNumber) {
      Cloud cloud = CloudProviderFactory.getCloudProvider().getCloud();

      NodeManager publicationThemeManager = cloud.getNodeManager("newsletterpublicationtheme");
      Node publicationNode = cloud.getNode(publicationNumber);
      NodeList publicationThemeList = publicationNode.getRelatedNodes(publicationThemeManager);

      for (int i = 0; i < publicationThemeList.size(); i++) {
         Node publicationThemeNode = publicationThemeList.getNode(i);
         publicationThemeNode.deleteRelations();
         publicationThemeNode.delete();
      }
      publicationNode.deleteRelations();
      publicationNode.delete();
      NavigationUtil.deletePage(publicationNode);

   }

   public static List<String> getAllThemesForPublication(String publicationNumber) {
      List<String> themes = new ArrayList<String>();
      Cloud cloud = CloudProviderFactory.getCloudProvider().getCloud();
      Node newsletterNode = cloud.getNode(publicationNumber);
      NodeList themeList = newsletterNode.getRelatedNodes("newslettertheme");
      for (int i = 0; i < themeList.size(); i++) {
         Node themeNode = themeList.getNode(i);
         String theme = themeNode.getStringValue("number");
         themes.add(theme);
      }
      return (themes);
   }

}