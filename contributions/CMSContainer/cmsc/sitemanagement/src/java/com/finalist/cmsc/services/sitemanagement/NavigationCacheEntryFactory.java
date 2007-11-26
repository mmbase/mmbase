/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package com.finalist.cmsc.services.sitemanagement;

import java.io.Serializable;

import org.mmbase.bridge.*;
import org.mmbase.core.event.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import com.finalist.cmsc.beans.om.*;
import com.finalist.cmsc.navigation.*;

public class NavigationCacheEntryFactory extends MMBaseCacheEntryFactory {

   public NavigationCacheEntryFactory() {
      super(PagesUtil.PAGE);
   }

   /** MMbase logging system */
   private static Logger log = Logging.getLoggerInstance(NavigationCacheEntryFactory.class.getName());


   @Override
   protected Serializable loadEntry(Serializable key) throws Exception {
      Node node = getNode(key);
      for (NavigationItemManager manager : NavigationManager.getNavigationManagers()) {
         NavigationItem navigationItem = manager.loadNavigationItem((Integer) key, node);
         if (navigationItem != null) {
            return navigationItem;
         }
      }
      return null;
   }


   /*
    * [fp] return loadPage((Integer) key); } private Page loadPage(Integer key) {
    * Node pageNode = getNode(key); if (pageNode == null ||
    * !PagesUtil.isPageType(pageNode)) { log.debug("Page not found: " + key);
    * return null; } Page page = null; if (SiteUtil.isSite(pageNode)) { page =
    * (Page) MMBaseNodeMapper.copyNode(pageNode, Site.class); } else { page =
    * (Page) MMBaseNodeMapper.copyNode(pageNode, Page.class); } RelationList
    * rellist = PortletUtil.getPortletRelations(pageNode); RelationIterator r =
    * rellist.relationIterator(); while (r.hasNext()) { Relation relation =
    * r.nextRelation(); Node relatedPortletNode = relation.getDestination();
    * log.debug("portlet='" + relatedPortletNode.getNumber() + "' :" +
    * relatedPortletNode.getNodeManager().getName()); String layoutid =
    * relation.getStringValue(PortletUtil.LAYOUTID_FIELD);
    * page.addPortlet(layoutid, relatedPortletNode.getNumber()); }
    * loadLayout(pageNode, page); loadStylesheet(pageNode, page);
    * loadPageImages(pageNode, page); return page; } private void
    * loadLayout(Node pageNode, Page page) { Node layoutNode =
    * PagesUtil.getLayout(pageNode); if (layoutNode != null) {
    * page.setLayout(layoutNode.getNumber()); } else { log.error("NO LAYOUT"); } }
    * private void loadStylesheet(Node pageNode, Page page) { NodeList styleNode =
    * PagesUtil.getStylesheet(pageNode); if (!styleNode.isEmpty()) { for
    * (NodeIterator iter = styleNode.nodeIterator(); iter.hasNext();) { Node
    * stylesheetNode = iter.nextNode();
    * page.addStylesheet(stylesheetNode.getNumber()); } } } private void
    * loadPageImages(Node pageNode, Page page) { RelationList relations =
    * pageNode.getRelations(null, "images"); for(RelationIterator iter =
    * relations.relationIterator(); iter.hasNext();) { Relation relation =
    * iter.nextRelation(); String name = relation.getStringValue("name"); //
    * this is a bit of a hack, but saves on the loading of the actual node
    * String image = ""+relation.getStringValue("dnumber");
    * page.addPageImage(name, image); } }
    */
   @Override
   protected boolean isRelationEvent(RelationEvent event, String nodeType) {
      return super.isRelationEvent(event, nodeType) || super.isRelationEvent(event, SiteUtil.SITE);
   }


   @Override
   protected boolean isNodeEvent(NodeEvent event, String nodeType) {
      return super.isNodeEvent(event, nodeType) || super.isNodeEvent(event, SiteUtil.SITE);
   }


   @Override
   public void notify(RelationEvent event) {
      if (isRelationEvent(event)) {
         Integer key = getKey(event);
         if (key != null) {
            switch (event.getType()) {
               case Event.TYPE_CHANGE:
                  refreshEntry(key);
                  break;
               case Event.TYPE_DELETE:
                  // don't remove page entry when layout removed from page.
                  if (!event.getRelationDestinationType().equals(PagesUtil.LAYOUT)) {
                     deleteEntry(key);
                  }
                  break;
               case Event.TYPE_NEW:
                  refreshEntry(key);
                  break;
               case NodeEvent.TYPE_RELATION_CHANGE:
                  deleteEntry(key);
                  break;
               default:
                  break;
            }
         }
      }
   }

}
