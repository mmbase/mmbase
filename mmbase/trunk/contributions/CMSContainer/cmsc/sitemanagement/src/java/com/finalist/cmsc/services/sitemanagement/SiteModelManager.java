/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package com.finalist.cmsc.services.sitemanagement;

import java.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import com.finalist.cmsc.beans.om.*;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCacheManager;

public class SiteModelManager extends SelfPopulatingCacheManager {

   private static final String PORTLET_DEFINITION_CACHE = PortletDefinition.class.getName();
   private static final String LAYOUT_CACHE = Layout.class.getName();
   private static final String STYLESHEET_CACHE = Stylesheet.class.getName();
   private static final String VIEW_CACHE = View.class.getName();
   private static final String NAVIGATION_CACHE = NavigationItem.class.getName();
   private static final String PORTLET_CACHE = Portlet.class.getName();

   private SiteCache siteCache;

   private PortletDefinition selectDefinition;

   /** MMbase logging system */
   private static Logger log = Logging.getLoggerInstance(SiteModelManager.class.getName());


   public SiteModelManager() throws CacheException {
      super();

      selectDefinition = new PortletDefinition();
      selectDefinition.setTitle("CMSC PortletSelectPortlet");
      selectDefinition.setDescription("CMSC PortletSelectPortlet");
      selectDefinition.setDefinition("portletselectportlet");
      selectDefinition.setType("system");
   }


   @Override
   protected void doSetupCaches() throws CacheException {
      PortletDefinitionCacheEntryFactory portletDefinitionFactory = new PortletDefinitionCacheEntryFactory();
      SelfPopulatingCache portletdefs = createSelfPopulatingCache(PORTLET_DEFINITION_CACHE, portletDefinitionFactory);
      portletDefinitionFactory.cacheToRefresh(portletdefs);

      PortletCacheEntryFactory portletFactory = new PortletCacheEntryFactory();
      SelfPopulatingCache portlets = createSelfPopulatingCache(PORTLET_CACHE, portletFactory);
      portletFactory.cacheToRefresh(portlets);

      LayoutCacheEntryFactory layoutFactory = new LayoutCacheEntryFactory();
      SelfPopulatingCache layouts = createSelfPopulatingCache(LAYOUT_CACHE, layoutFactory);
      layoutFactory.cacheToRefresh(layouts);

      StyleSheetCacheEntryFactory styleSheetFactory = new StyleSheetCacheEntryFactory();
      SelfPopulatingCache stylesheets = createSelfPopulatingCache(STYLESHEET_CACHE, styleSheetFactory);
      styleSheetFactory.cacheToRefresh(stylesheets);

      ViewCacheEntryFactory viewFactory = new ViewCacheEntryFactory();
      SelfPopulatingCache views = createSelfPopulatingCache(VIEW_CACHE, viewFactory);
      viewFactory.cacheToRefresh(views);

      NavigationCacheEntryFactory navigationFactory = new NavigationCacheEntryFactory();
      SelfPopulatingCache navigationItems = createSelfPopulatingCache(NAVIGATION_CACHE, navigationFactory);
      navigationFactory.cacheToRefresh(navigationItems);

      siteCache = new SiteCache();
   }


   public void resetSiteCache() {
      siteCache = new SiteCache();
   }


   public boolean hasPage(String path) {
      if (path != null && path.length() > 0) {
         Integer pageId = siteCache.getPage(path);
         return pageId != null;
      }
      return false;
   }


   public NavigationItem getNavigationItem(String path) {
      if (path != null && path.length() > 0) {
         Integer id = siteCache.getPage(path);
         if (id != null) {
            return getNavigationItem(id);
         }
         else {
            log.debug("NavigationItem not found for path " + path);
         }
      }
      return null;
   }


   public NavigationItem getNavigationItem(int id) {
      try {
         return (NavigationItem) getCache(NAVIGATION_CACHE).get(id);
      }
      catch (CacheException e) {
         log.info("" + e.getMessage(), e);
      }
      return null;
   }


   public Site getSite(String path) {
      if (path != null && path.length() > 0) {
         try {
            Integer pageId = siteCache.getSite(path);
            if (pageId != null) {
               return (Site) getCache(NAVIGATION_CACHE).get(pageId);
            }
            else {
               log.debug("Site not found for path " + path);
            }
         }
         catch (CacheException e) {
            log.info("" + e.getMessage(), e);
         }
      }
      return null;
   }


   public String getSite(Page page) {

      return siteCache.getSite(page);
   }


   public List<Site> getSites() {
      List<Site> sites = new ArrayList<Site>();
      try {
         List<Integer> siteIds = siteCache.getSites();
         for (Integer siteId : siteIds) {
            Site site = (Site) getCache(NAVIGATION_CACHE).get(siteId);
            if (site != null) {
               sites.add(site);
            }
         }
      }
      catch (CacheException e) {
         log.info("" + e.getMessage(), e);
      }
      return sites;
   }


   public List<Page> getPagesForPath(String path) {
      List<Page> pages = new ArrayList<Page>();
      if (path != null && path.length() > 0) {
         try {
            List<Integer> pageIds = siteCache.getPagesForPath(path);
            for (Integer pageId : pageIds) {
               Page page = (Page) getCache(NAVIGATION_CACHE).get(pageId);
               if (page != null) {
                  pages.add(page);
               }
            }
         }
         catch (CacheException e) {
            log.info("" + e.getMessage(), e);
         }
      }
      return pages;
   }


   public List<Page> getChildren(Page findpage) {
      List<Page> pages = new ArrayList<Page>();
      if (findpage != null) {
         try {
            List<Integer> pageIds = siteCache.getChildren(findpage);
            for (Integer pageId : pageIds) {
               NavigationItem navigationItem = (NavigationItem) getCache(NAVIGATION_CACHE).get(pageId);
               if (navigationItem != null && navigationItem instanceof Page) {
                  pages.add((Page) navigationItem);
               }
            }
         }
         catch (CacheException e) {
            log.info("" + e.getMessage(), e);
         }
      }
      return pages;
   }


   public View getView(int id) {
      if (id > 0) {
         try {
            return (View) getCache(VIEW_CACHE).get(new Integer(id));
         }
         catch (CacheException e) {
            log.info("" + e.getMessage(), e);
         }
      }
      return null;
   }


   public Stylesheet getStylesheet(int id) {
      if (id > 0) {
         try {
            return (Stylesheet) getCache(STYLESHEET_CACHE).get(new Integer(id));
         }
         catch (CacheException e) {
            log.info("" + e.getMessage(), e);
         }
      }
      return null;
   }


   public Layout getLayout(int id) {
      if (id > 0) {
         try {
            return (Layout) getCache(LAYOUT_CACHE).get(new Integer(id));
         }
         catch (CacheException e) {
            log.info("" + e.getMessage(), e);
         }
      }
      return null;
   }


   public PortletDefinition getPortletDefinition(int id) {
      if (id <= 0) {
         return selectDefinition;
      }
      try {
         return (PortletDefinition) getCache(PORTLET_DEFINITION_CACHE).get(new Integer(id));
      }
      catch (CacheException e) {
         log.info("" + e.getMessage(), e);
      }
      return null;
   }


   public Portlet getPortlet(int id) {
      if (id <= 0) {
         Portlet empty = new Portlet();
         empty.setTitle("CMSC PortletSelectPortlet");
         empty.setDefinition(-1);
         empty.setId(-1);
         return empty;
      }
      try {
         return (Portlet) getCache(PORTLET_CACHE).get(new Integer(id));
      }
      catch (CacheException e) {
         log.info("" + e.getMessage(), e);
      }
      return null;
   }


   public List<View> getViews(String screenId, String layoutId) {
      List<Integer> defs = getDefinitions(screenId, layoutId);

      List<View> views = new ArrayList<View>();

      for (Integer definitionId : defs) {
         PortletDefinition definition = getPortletDefinition(definitionId);
         List<View> defViews = getViews(definition);
         views.addAll(defViews);
      }
      Collections.sort(views);
      return views;
   }


   public List<View> getViews(String definitionId) {
      PortletDefinition definition = getPortletDefinition(Integer.parseInt(definitionId));
      List<View> views = getViews(definition);
      return views;
   }


   public List<View> getViews(PortletDefinition definition) {
      List<View> views = new ArrayList<View>();

      List<Integer> viewIds = definition.getAllowedViews();
      for (Integer viewId : viewIds) {
         View view = getView(viewId);
         views.add(view);
      }
      Collections.sort(views);
      return views;
   }


   public List<PortletDefinition> getSingletonPortlets(String screenId, String layoutId) {
      List<PortletDefinition> definitions = new ArrayList<PortletDefinition>();
      List<Integer> defs = getDefinitions(screenId, layoutId);

      for (Integer definitionId : defs) {
         PortletDefinition definition = getPortletDefinition(definitionId);
         if (definition.isSingle()) {
            definitions.add(definition);
         }
      }
      Collections.sort(definitions);
      return definitions;
   }


   public List<PortletDefinition> getPortletDefintions(String screenId, String layoutId) {
      List<PortletDefinition> definitions = new ArrayList<PortletDefinition>();
      List<Integer> defs = getDefinitions(screenId, layoutId);

      for (Integer definitionId : defs) {
         PortletDefinition definition = getPortletDefinition(definitionId);
         if (definition.isMultiple()) {
            definitions.add(definition);
         }
      }
      Collections.sort(definitions);
      return definitions;
   }


   private List<Integer> getDefinitions(String screenId, String layoutId) {
      Page page = (Page) getNavigationItem(Integer.parseInt(screenId));
      Layout layout = getLayout(page.getLayout());
      return layout.getAllowedDefinitions(layoutId);
   }


   public String getPath(Page page, boolean includeRoot) {
      return siteCache.getPath(page, includeRoot);
   }


   public List<String> getContentTypes(String portletId) {
      Portlet p = getPortlet(Integer.parseInt(portletId));
      if (p == null) {
         throw new IllegalArgumentException("Portlet not found with id " + portletId);
      }
      List<String> types = new ArrayList<String>();

      PortletDefinition d = getPortletDefinition(p.getDefinition());
      List<String> dTypes = d.getContenttypes();

      View v = getView(p.getView());
      if (v != null) {
         List<String> vTypes = v.getContenttypes();
         if (!vTypes.isEmpty()) {
            for (Iterator<String> iter = vTypes.iterator(); iter.hasNext();) {
               String type = iter.next();
               if (dTypes.isEmpty() || dTypes.contains(type)) {
                  types.add(type);
               }
            }
         }
      }
      if (types.isEmpty() && !dTypes.isEmpty()) {
         types.addAll(dTypes);
      }

      return types;
   }


   public void clearPortlet(String portletId) {
      try {
         getCache(PORTLET_CACHE).put(Integer.valueOf(portletId), null);
      }
      catch (CacheException e) {
         log.info("" + e.getMessage(), e);
      }
   }


   public void clearPage(String pageId) {
      clearPage(Integer.valueOf(pageId));
   }


   public void clearPage(int pageId) {
      try {
         getCache(NAVIGATION_CACHE).put(pageId, null);
      }
      catch (CacheException e) {
         log.info("" + e.getMessage(), e);
      }
   }


   public Set<String> getPagePositions(int pageId) {
      Page page = (Page) getNavigationItem(pageId);
      int layoutid = page.getLayout();
      Layout layout = getLayout(layoutid);
      return layout.getNames();
   }

   /*
    * [FP] public RssFeed getRssFeed(String path) { if (path != null &&
    * path.length() > 0) { Integer rssFeed = siteCache.getPage(path); if
    * (rssFeed != null) { return getRssFeed(rssFeed); } else { log.debug("Page
    * not found for path " + path); } } return null; } public RssFeed
    * getRssFeed(int rssFeedId) { try { return (RssFeed)
    * getCache(NAVIGATION_CACHE).get(rssFeedId); } catch (CacheException e) {
    * log.info("" + e.getMessage(), e); } return null; }
    */
}
