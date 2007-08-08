package com.finalist.cmsc.navigation;

import javax.servlet.http.*;

import net.sf.mmapps.commons.util.HttpUtil;

import org.mmbase.bridge.Node;

import com.finalist.cmsc.security.SecurityUtil;
import com.finalist.cmsc.security.UserRole;
import com.finalist.cmsc.util.bundles.JstlUtil;
import com.finalist.tree.*;
import com.finalist.util.module.ModuleUtil;

/**
 * Renderer of the Site management tree.
 * 
 * @author Nico Klasens (Finalist IT Group)
 */
public abstract class NavigationRenderer implements TreeCellRenderer {

    private static final String FEATURE_PAGEWIZARD = "pagewizarddefinition";
    private static final String FEATURE_RSSFEED = "rssfeed";
    private static final String FEATURE_WORKFLOW = "workflowitem";
    
	private String target = null;
    private HttpServletRequest request;
    private HttpServletResponse response;

    public NavigationRenderer(HttpServletRequest request, HttpServletResponse response, String target) {
        this.request = request;
        this.response = response;
        this.target = target;
    }

    /**
     * Render een node van de tree.
     * 
     * @see com.finalist.tree.TreeCellRenderer#getElement(TreeModel, Object, String)
     */
    public TreeElement getElement(TreeModel model, Object node, String id) {
        Node parentNode = (Node) node;
        if (id == null) {
            id = String.valueOf(parentNode.getNumber());
        }
        
        UserRole role = null;
        boolean isPage = false;
        boolean isSite = false;
        boolean isRssFeed = false;
        boolean secure = false;
        String name = parentNode.getStringValue("title");
        String fragmentFieldName = null;
        Node parentParentNode = null;
        if(ModuleUtil.checkFeature(FEATURE_RSSFEED) && RssFeedUtil.isRssFeedType(parentNode)) {
        	isRssFeed = true;
        	parentParentNode = parentNode.getRelatedNodes("page").getNode(0);
        	role = NavigationUtil.getRole(parentNode.getCloud(), parentParentNode, false);
        	fragmentFieldName = RssFeedUtil.FRAGMENT_FIELD;
        }
        else {
        	secure = parentNode.getBooleanValue(PagesUtil.SECURE_FIELD);
        	role = NavigationUtil.getRole(parentNode.getCloud(), parentNode, false);
        	isPage = PagesUtil.isPage(parentNode);
        	isSite = !isPage;
        
        	fragmentFieldName = isPage ? PagesUtil.TITLE_FIELD : SiteUtil.TITLE_FIELD;
        }
        String fragment = parentNode.getStringValue( fragmentFieldName);
        
        
        String action = null;
        if (ServerUtil.useServerName()) {
            String[] pathElements = NavigationUtil.getPathElementsToRoot(parentNode, true);

            action = HttpUtil.getWebappUri(request, pathElements[0], secure);
            for (int i = 1; i < pathElements.length; i++) {
                action += pathElements[i] + "/";
            }
            if (!request.getServerName().equals(pathElements[0])) {
                action = HttpUtil.addSessionId(request, action);
            }
            else {
                action = response.encodeURL(action);
            }
        }
        else {
            String path = null;

           	path = NavigationUtil.getPathToRootString(parentNode, true); 
            String webappuri = HttpUtil.getWebappUri(request, secure);
            action = response.encodeURL(webappuri + path);
        }
        
        TreeElement element = createElement(getIcon(node, role), id, name, fragment, action, target);
        
        if (role != null && SecurityUtil.isWriter(role)) {
            if (isPage) {
                if (SecurityUtil.isEditor(role)) {
                    String labelPageEdit = JstlUtil.getMessage(request, "site.page.edit");
                    element.addOption(createOption("edit_defaults.png", labelPageEdit,
                        getUrl("PageEdit.do?number=" + parentNode.getNumber()), target));
                    
                    if ((model.getChildCount(parentNode) == 0) || SecurityUtil.isWebmaster(role)) {
                        String labelPageRemove = JstlUtil.getMessage(request, "site.page.remove");
                        element.addOption(createOption("delete.png", labelPageRemove,
                                getUrl("PageDelete.do?number=" + parentNode.getNumber()), target));
                    }
                }
            }
            if (isSite) {
                if (SecurityUtil.isChiefEditor(role)) {
                    String labelSiteEdit = JstlUtil.getMessage(request, "site.site.edit");
                    element.addOption(createOption("edit_defaults.png", labelSiteEdit,
                            getUrl("SiteEdit.do?number=" + parentNode.getNumber()), target));

                    if ((model.getChildCount(parentNode) == 0) || SecurityUtil.isWebmaster(role)) {
                        String labelSiteRemove = JstlUtil.getMessage(request, "site.site.remove");
                        element.addOption(createOption("delete.png", labelSiteRemove,
                            getUrl("SiteDelete.do?number=" + parentNode.getNumber()), target));
                    }
                }
            }
            if (isRssFeed) {
                if (SecurityUtil.isEditor(role)) {
                    String labelSiteEdit = JstlUtil.getMessage(request, "site.rss.edit");
                    element.addOption(createOption("edit_defaults.png", labelSiteEdit,
                            getUrl("../rssfeed/RssFeedEdit.do?number=" + parentNode.getNumber()), target));

                    if ((model.getChildCount(parentNode) == 0) || SecurityUtil.isWebmaster(role)) {
                        String labelSiteRemove = JstlUtil.getMessage(request, "site.rss.remove");
                        element.addOption(createOption("delete.png", labelSiteRemove,
                            getUrl("../rssfeed/RssFeedDelete.do?number=" + parentNode.getNumber()), target));
                    }
                }
            }
            if (SecurityUtil.isEditor(role)) {
            	if (isPage || isSite) {
	                String labelPageNew = JstlUtil.getMessage(request, "site.page.new");
	                element.addOption(createOption("new.png", labelPageNew,
	                        getUrl("PageCreate.do?parentpage=" + parentNode.getNumber()), target));
	
	                if(ModuleUtil.checkFeature(FEATURE_RSSFEED)) {
	                   String labelRssNew = JstlUtil.getMessage(request, "site.rss.new");
	                   element.addOption(createOption("rss_new.png", labelRssNew,
	                         getUrl("../rssfeed/RssFeedCreate.do?parentpage=" + parentNode.getNumber()), target));
	                }
	                
	                if (NavigationUtil.getChildCount(parentNode) >= 2) {
	                    String labelPageReorder = JstlUtil.getMessage(request, "site.page.reorder");
	                    element.addOption(createOption("reorder.png", labelPageReorder, 
	                            getUrl("reorder.jsp?parent=" + parentNode.getNumber()), target));
	                }

	                if(ModuleUtil.checkFeature(FEATURE_PAGEWIZARD)) {
				        String labelPageWizard = JstlUtil.getMessage(request, "site.page.wizard");
				        element.addOption(createOption("wizard.png", labelPageWizard,
				            getUrl("../pagewizard/StartPageWizardAction.do?number=" + parentNode.getNumber()), target));
			        }
            	}
                
                if (SecurityUtil.isChiefEditor(role)) {
                    if (isPage || isRssFeed) {
                        String labelPageCut = JstlUtil.getMessage(request, "site.page.cut");
                        element.addOption(createOption("cut.png", labelPageCut, "javascript:cut('"
                                + parentNode.getNumber() + "');", null));
                        String labelPageCopy = JstlUtil.getMessage(request, "site.page.copy");
                        element.addOption(createOption("copy.png", labelPageCopy, "javascript:copy('"
                                + parentNode.getNumber() + "');", null));
                    }
                    if (isPage || isSite) {
	                    String labelPagePaste = JstlUtil.getMessage(request, "site.page.paste");
	                    element.addOption(createOption("paste.png", labelPagePaste, "javascript:paste('"
	                            + parentNode.getNumber() + "');", null));
                    }
                }
            }

            if (SecurityUtil.isWebmaster(role)) {
		        if(ModuleUtil.checkFeature(FEATURE_WORKFLOW)) {
                   String labelPublish = JstlUtil.getMessage(request, "site.page.publish");
                   element.addOption(createOption("publish.png", labelPublish,
                       getUrl("../workflow/publish.jsp?number=" + parentNode.getNumber()), target));
                   if (isPage || isSite) {
                	   String labelMassPublish = JstlUtil.getMessage(request, "site.page.masspublish");
                	   element.addOption(createOption("masspublish.png", labelMassPublish,
                			   getUrl("../workflow/masspublish.jsp?number=" + parentNode.getNumber()), target));
                   }
		        }
            }
            
            if (isPage || isSite) {
            	String labelPageRights = JstlUtil.getMessage(request, "site.page.rights");
            	element.addOption(createOption("rights.png", labelPageRights,
            			getUrl("../usermanagement/pagerights.jsp?number=" + parentNode.getNumber()), target));
            }
        }

        return element;
    }
    
    private void addGlobalOptions(Node parentNode, TreeElement element) {
        String labelPageRights = JstlUtil.getMessage(request, "site.page.rights");
        element.addOption(createOption("rights.png", labelPageRights,
            getUrl("../usermanagement/pagerights.jsp?number=" + parentNode.getNumber()), target));
    }

    private void addEditorOptions(Node parentNode, TreeElement element) {
        String labelPageNew = JstlUtil.getMessage(request, "site.page.new");
        element.addOption(createOption("new.png", labelPageNew,
                getUrl("PageCreate.do?parentpage=" + parentNode.getNumber()), target));
        
        if(ModuleUtil.checkFeature(FEATURE_RSSFEED)) {
            String labelRssNew = JstlUtil.getMessage(request, "site.rss.new");
            element.addOption(createOption("rss_new.png", labelRssNew,
                  getUrl("../rssfeed/RssFeedCreate.do?parentpage=" + parentNode.getNumber()), target));
        }
        
        if (NavigationUtil.getChildCount(parentNode) >= 2) {
            String labelPageReorder = JstlUtil.getMessage(request, "site.page.reorder");
            element.addOption(createOption("reorder.png", labelPageReorder, 
                    getUrl("reorder.jsp?parent=" + parentNode.getNumber()), target));
        }
    }

    private void addChiefEditorOptions(Node parentNode, boolean isPage, TreeElement element) {
        if (isPage) {
            String labelPageCut = JstlUtil.getMessage(request, "site.page.cut");
            element.addOption(createOption("cut.png", labelPageCut, "javascript:cut('"
                    + parentNode.getNumber() + "');", null));
            String labelPageCopy = JstlUtil.getMessage(request, "site.page.copy");
            element.addOption(createOption("copy.png", labelPageCopy, "javascript:copy('"
                    + parentNode.getNumber() + "');", null));
        }
        String labelPagePaste = JstlUtil.getMessage(request, "site.page.paste");
        element.addOption(createOption("paste.png", labelPagePaste, "javascript:paste('"
                + parentNode.getNumber() + "');", null));
    }

    private void addWebmasterOptions(Node parentNode, TreeElement element) {
        if(ModuleUtil.checkFeature(FEATURE_WORKFLOW)) {
           String labelPublish = JstlUtil.getMessage(request, "site.page.publish");
           element.addOption(createOption("publish.png", labelPublish,
               getUrl("../workflow/publish.jsp?number=" + parentNode.getNumber()), target));
           String labelMassPublish = JstlUtil.getMessage(request, "site.page.masspublish");
           element.addOption(createOption("masspublish.png", labelMassPublish,
               getUrl("../workflow/masspublish.jsp?number=" + parentNode.getNumber()), target));
        }

// Not yet ready for mainstream usage. Might become a handy tool for webmasters.
        
//        String labelMassModify = JstlUtil.getMessage(request, "site.page.massmodify");
//        element.addOption(createOption("massmodify.png", labelMassModify,
//            getUrl("MassModify.do?number=" + parentNode.getNumber()), target));
    }

    private String getUrl(String url) {
        return response.encodeURL(url);
    }

    public String getIcon(Object node, UserRole role) {
        Node n = (Node) node;
        return "type/" + n.getNodeManager().getName() + "_"+role.getRole().getName()+".png";
    }

    protected abstract TreeOption createOption(String icon, String label, String action, String target);

    protected abstract TreeElement createElement(String icon, String id, String name, String fragment, String action, String target);
    
}