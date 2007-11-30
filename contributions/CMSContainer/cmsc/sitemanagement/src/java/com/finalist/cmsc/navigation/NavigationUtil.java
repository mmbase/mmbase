/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.navigation;

import java.util.*;

import java.util.List;

import net.sf.mmapps.commons.bridge.*;
import net.sf.mmapps.commons.util.StringUtil;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.SearchUtil;

import com.finalist.cmsc.mmbase.TreeUtil;
import com.finalist.cmsc.security.*;
import com.finalist.cmsc.security.forms.RolesInfo;

public class NavigationUtil {

    public static final String NAVREL = "navrel";
    public static final String ALLOWREL = "allowrel";

    /**
     * The first key in this LinkedMap is the root nodemanager of the tree.
     */
    private static LinkedHashMap<String,String> treeManagers = new LinkedHashMap<String, String>();

    /**
     * Return node managers names and fields which are involved in the navigation tree
     * The key is the nodemanager name. The value is the field for the path fragment
     * The first element is the root node manager in the tree.
     * @return Map with nodemanager names and fields
     */
    public static LinkedHashMap<String, String> getTreeManagers() {
        return treeManagers;
    }

    /**
     * This method is used on startup of MMBase to fill the information about treeManagers 
     * and path fragment fields
     * This method is synchronized on the class (static method) to make sure only one managers is added
     * at the same time.
     * 
     * @param manager name of nodemanager which is used in the tree
     * @param fragmentFieldname name of field which is used in the path of a tree item
     * @param root This nodemanager maintains the nodes which are root tree items 
     */
    public static synchronized void registerTreeManager(String manager, String fragmentFieldname, boolean root) {
        if (root) {
            LinkedHashMap<String,String> tempManagers = new LinkedHashMap<String, String>(treeManagers);
            treeManagers.clear();
            treeManagers.put(manager, fragmentFieldname);
            treeManagers.putAll(tempManagers);
        }
        else {
            treeManagers.put(manager, fragmentFieldname);
        }
    }

    private NavigationUtil() {
        // utility
    }
    
    public static RelationManager getRelationManager(Cloud cloud) {
        return TreeUtil.getRelationManager(cloud, PagesUtil.PAGE, NAVREL);
    }

    public static void appendChild(Cloud cloud, String parent, String child) {
        TreeUtil.appendChild(cloud, parent, child, NAVREL);
    }

    public static void appendChild(Node parentNode, Node childNode) {
        TreeUtil.appendChild(parentNode, childNode, NAVREL);
    }

    public static Node getParent(Node node) {
        return TreeUtil.getParent(node, treeManagers, NAVREL);
    }

    public static Relation getParentRelation(Node node) {
        return TreeUtil.getParentRelation(node, TreeUtil.convertToList(treeManagers), NAVREL);
    }

    public static boolean isParent(Node sourcePage, Node destPage) {
        return TreeUtil.isParent(sourcePage, destPage, TreeUtil.convertToList(treeManagers), NAVREL);
    }

    public static String getFragmentFieldname(Node page) {
        return getFragmentFieldname(page.getNodeManager().getName());
    }

    public static String getFragmentFieldname(String page) {
        return TreeUtil.getFragmentFieldname(page, treeManagers);
    }

    /**
     * Find path to root
     * @param node - node
     * @return List with the path to the root. First item is the root and last is the node
     */
    public static List<Node> getPathToRoot(Node node) {
        return TreeUtil.getPathToRoot(node, treeManagers, NAVREL);
    }

    /**
     * Creates a string that represents the root path.
     * @param cloud - MMbase cloud
     * @param node - MMbase node
     * @return path to root
     */
    public static String getPathToRootString(Cloud cloud, String node) {
       return getPathToRootString(cloud.getNode(node));
    }

    /**
     * Creates a string that represents the root path.
     * @param node - MMbase node
     * @return path to root
     */
    public static String getPathToRootString(Node node) {
       return getPathToRootString(node, true);
    }

    /**
     * Creates a string that represents the root path.
     * @param node - MMbase node
     * @param includeRoot - include the root pathfragment
     * @return path to root
     */
    public static String getPathToRootString(Node node, boolean includeRoot) {
        return TreeUtil.getPathToRootString(node, treeManagers, NAVREL, includeRoot);
    }

    /**
     * Creates a string that represents the root path.
     * @param cloud - MMbase cloud
     * @param node - MMbase node
     * @return path to root
     */
    public static String[] getPathElementsToRoot(Cloud cloud, String node) {
       return getPathElementsToRoot(cloud.getNode(node));
    }

    /**
     * Creates a string that represents the root path.
     * @param node - MMbase node
     * @return path to root
     */
    public static String[] getPathElementsToRoot(Node node) {
       return getPathElementsToRoot(node, true);
    }

    /**
     * Creates a string that represents the root path.
     * @param node - MMbase node
     * @param includeRoot - include the root pathfragment
     * @return path to root
     */
    public static String[] getPathElementsToRoot(Node node, boolean includeRoot) {
        return TreeUtil.getPathElementsToRoot(node, treeManagers, NAVREL, includeRoot);
    }


    /**
     * Creates a string that represents the titles.
     * @param cloud - MMbase cloud
     * @param node - node number
     * @return titles of nodes in path
     */
    public static String getTitlesString(Cloud cloud, String node) {
       return getTitlesString(cloud, node, true);
    }

    /**
     * Creates a string that represents the titles.
     * @param cloud - MMbase cloud
     * @param node - node number
     * @param includeRoot - include the root node
     * @return titles of nodes in path
     */
    public static String getTitlesString(Cloud cloud, String node, boolean includeRoot) {
       return getTitlesString(cloud.getNode(node), includeRoot);
    }

    /**
     * Creates a string that represents the titles.
     * @param node - node number
     * @param includeRoot - include the root node
     * @return titles of nodes in path
     */
    public static String getTitlesString(Node node, boolean includeRoot) {
        return TreeUtil.getTitlesString(node, treeManagers, NAVREL, PagesUtil.TITLE_FIELD, includeRoot);
    }

    /**
     * Method that finds the Page node using a path as input.
     * @param cloud - MMbase cloud
     * @param path - path of page
     * @return node with page path
     */
    public static Node getPageFromPath(Cloud cloud, String path) {
        if (!StringUtil.isEmptyOrWhitespace(path)) {
            int index = path.indexOf(TreeUtil.PATH_SEPARATOR);
            if (index == -1) {
                Node site = SiteUtil.getSite(cloud, path);
                return site;
            }
            else {
                String sitename = path.substring(0, index);
                Node site = SiteUtil.getSite(cloud, sitename);
                if (site == null) {
                    return null;
                }
                return getPageFromPath(cloud, path, site, true);
            }
        }
        return null;
    }

   public static Node getSiteFromPath(Cloud cloud, String path) {
      if (!StringUtil.isEmptyOrWhitespace(path)) {
         int index = path.indexOf(TreeUtil.PATH_SEPARATOR);
         if (index == -1) {
            Node site = SiteUtil.getSite(cloud, path);
            if (site != null) {
               return site;
            }
            return null;
         } else {
            String sitename = path.substring(0, index);
            Node site = SiteUtil.getSite(cloud, sitename);
            if (site == null) {
               return null;
            }
            return site;
         }
      }
      return null;
   }


    /**
     * Method that finds the Page node using a path as input.
     * @param cloud - MMbase cloud
     * @param path - path of page
     * @param root - node to start search
     * @return node with page path
     */
    public static Node getPageFromPath(Cloud cloud, String path, Node root) {
         return getPageFromPath(cloud, path, root, true);
    }

    /**
     * Method that finds the Page node using a path as input.
     * @param cloud - MMbase cloud
     * @param path - path of page
     * @param root - node to start search
     * @param useCache - use path cache
     * @return node with page path
     */
    public static Node getPageFromPath(Cloud cloud, String path, Node root, boolean useCache) {
        Node node = TreeUtil.getTreeItemFromPath(cloud, path, root, treeManagers, NAVREL, useCache);
        return node;
    }

    public static NodeList getChildren(Node parentNode) {
        return TreeUtil.getChildren(parentNode, PagesUtil.PAGE, NAVREL);
     }

    public static NodeList getChildren(Node parentNode, String nodeManager) {
        return TreeUtil.getChildren(parentNode, nodeManager, NAVREL);
     }

    public static boolean hasChild(Node parentChannel, String fragment) {
        return TreeUtil.hasChild(parentChannel, fragment, treeManagers, NAVREL);
    }

    public static Node getChild(Node parentChannel, String fragment) {
        return TreeUtil.getChild(parentChannel, fragment, treeManagers, NAVREL);
    }

    
    public static void reorder(Node parent, String children) {
        RelationUtil.reorder(parent, children, NAVREL, PagesUtil.PAGE);
    }

    public static void reorder(Node parent, String[] children) {
        RelationUtil.reorder(parent, children, NAVREL, PagesUtil.PAGE);
    }

    public static void recalculateChildPositions(Node parent) {
        RelationUtil.recalculateChildPositions(parent, NAVREL, PagesUtil.PAGE);
    }


    public static NodeList getVisibleChildren(Node parentNode) {
        NodeList children = getOrderedChildren(parentNode);
        for (Iterator<Node> iter = children.iterator(); iter.hasNext();) {
            Node child = iter.next();
            if (!child.getBooleanValue(PagesUtil.VISIBLE_FIELD)) {
                iter.remove();
            }
        }
        return children;
    }


    public static NodeList getOrderedChildren(Node parentNode) {
        return SearchUtil.findRelatedOrderedNodeList(parentNode, null, NAVREL, NAVREL + ".pos");
     }

    public static int getLevel(String path) {
        return TreeUtil.getLevel(path);
    }

    public static int getChildCount(Node parent) {
        return TreeUtil.getChildCount(parent, "object", NAVREL);
    }

    public static void movePage(Node sourcePage, Node destPage) {
        if (!isParent(sourcePage, destPage)) {
            Relation parentRelation = getParentRelation(sourcePage);
            // NIJ-393, don't move if it is the same parent
            if (parentRelation.getSource().getNumber() != destPage.getNumber()) {
                appendChild(destPage, sourcePage);
                parentRelation.delete();
            }
        }
    }

    public static Node copyPage(Node sourcePage, Node destPage) {
        if (!isParent(sourcePage, destPage)) {
            Node newPage = PagesUtil.copyPage(sourcePage);
            appendChild(destPage, newPage);

            NodeList children = getOrderedChildren(sourcePage);
            for (Iterator<Node> iter = children.iterator(); iter.hasNext();) {
                Node childPage = iter.next();
                copyPage(childPage, newPage);
            }
            return newPage;
        }
        return null;
    }

    /**
     * Get the role for the user for a page
     *
     * @param group Node of group
     * @param page get role for this page
     * @return UserRole - rights of a user
     */
    public static UserRole getRole(Node group, Node page) {
        return getRole(group, page, false);
    }

    /**
     * Get the role for the user for a page
     *
     * @param cloud Cloud with user
     * @param page get role for this page
     * @return UserRole - rights of a user
     */
    public static UserRole getRole(Cloud cloud, int page) {
        return getRole(cloud, cloud.getNode(page), false);
    }

    /**
     * Get the role for the user for a page
     *
     * @param cloud Cloud with user
     * @param page get role for this page
     * @param rightsInherited inherit rights from parent chennal
     * @return UserRole - rights of a user
     */
    public static UserRole getRole(Cloud cloud, Node page, boolean rightsInherited) {
        TreeMap<String,UserRole> pagesWithRole = SecurityUtil.getLoggedInRoleMap(cloud, treeManagers, NAVREL);
        return SecurityUtil.getRole(page, rightsInherited, pagesWithRole);
    }

    /**
     * Get the role for the user for a page
     *
     * @param group Node of group
     * @param page get role for this page
     * @param rightsInherited inherit rights from parent chennal
     * @return UserRole - rights of a user
     */
    public static UserRole getRole(Node group, Node page, boolean rightsInherited) {
       // retrieve a TreeMap where the pages (keys) are ordered on level and path
       TreeMap<String,UserRole> pagesWithRole = SecurityUtil.getNewRolesMap();
       SecurityUtil.fillChannelsWithRole(group, pagesWithRole, treeManagers, NAVREL);
       return SecurityUtil.getRole(page, rightsInherited, pagesWithRole);
    }

    public static void setGroupRights(Cloud cloud, Node group, Map<Integer, UserRole> rights) {
        SecurityUtil.setGroupRights(cloud, group, rights, TreeUtil.convertToList(treeManagers));
    }

    public static void addRole(Cloud cloud, String pageNumber, Node group, Role role) {
        SecurityUtil.addRole(cloud, pageNumber, group, role, TreeUtil.convertToList(treeManagers));
    }

    public static void addRole(Cloud cloud, Node pageNode, Node group, Role role) {
        SecurityUtil.addRole(cloud, pageNode, group, role, TreeUtil.convertToList(treeManagers));
    }

    public static void deletePage(Node pageNode) {
        NodeList children = getOrderedChildren(pageNode);
        for (Iterator<Node> iter = children.iterator(); iter.hasNext();) {
            Node childPage = iter.next();
            deletePage(childPage);
        }
        PagesUtil.deletePage(pageNode);
        SecurityUtil.clearUserRoles(pageNode.getCloud(), TreeUtil.convertToList(treeManagers));
    }

    public static NavigationInfo getNavigationInfo(Cloud cloud) {
        NavigationInfo info = (NavigationInfo) cloud.getProperty(NavigationInfo.class.getName());
        if (info == null) {
            info = new NavigationInfo();
            cloud.setProperty(NavigationInfo.class.getName(), info);
            addPagesWithRoleToInfo(cloud, info);
            addAllSiteToInfo(cloud, info);
        }
        return info;
    }

	private static void addAllSiteToInfo(Cloud cloud, NavigationInfo info) {
		NodeList allSites = SiteUtil.getSites(cloud);
		for(NodeIterator i = allSites.nodeIterator(); i.hasNext(); ) {
			Node site = i.nextNode();
			info.expand(site.getNumber());
		}
		
	}

	private static void addPagesWithRoleToInfo(Cloud cloud, NavigationInfo info) {
		TreeMap<String,UserRole> pagesWithRole = SecurityUtil.getLoggedInRoleMap(cloud, treeManagers, NAVREL);
		for (String path : pagesWithRole.keySet()) {
		    Node page = getPageFromPath(cloud, path);
		    if(page != null) {
		       info.expand(page.getNumber());
		       addParentsToInfo(info, page);
		    }
		}
	}


	private static void addParentsToInfo(NavigationInfo info, Node page) {
   	   Node parent = NavigationUtil.getParent(page);
   	   if(parent != null) {
           info.expand(parent.getNumber());
           addParentsToInfo(info, parent);
   	   }
	}

	public static RolesInfo getRolesInfo(Cloud cloud, Node group) {
        RolesInfo info = new RolesInfo();
        TreeMap<String,UserRole> pagesWithRole = SecurityUtil.getRoleMap(treeManagers, NAVREL, group);
        for (String path : pagesWithRole.keySet()) {
            Node page = getPageFromPath(cloud, path);
            info.expand(page.getNumber());
        }
        return info;
    }

    /**
     * This is the method for a USER, the old ones want a GROUP...
     * (even although the are called getRoleForUser(..)
     * 
     * @param page page to get role for
     * @param user user to get role for
     * @return User Role
     */
    public static UserRole getUserRole(Node page, Node user) {
       TreeMap<String, UserRole> pagesWithRole = SecurityUtil.getNewRolesMap();
       SecurityUtil.getUserRoleMap(user, treeManagers, NAVREL, pagesWithRole);
        return SecurityUtil.getRole(page, true, pagesWithRole);
    }

    public static List<Node> getUsersWithRights(Node channel, Role requiredRole) {
        return SecurityUtil.getUsersWithRights(channel, requiredRole, TreeUtil.convertToList(treeManagers), NAVREL);
    }

}
