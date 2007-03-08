/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.services.search;

import java.util.*;

import javax.servlet.ServletConfig;

import net.sf.mmapps.commons.bridge.GenericNodeList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.storage.search.RelationStep;
import org.mmbase.storage.search.Step;

import com.finalist.cmsc.beans.om.*;
import com.finalist.cmsc.navigation.*;
import com.finalist.cmsc.repository.ContentElementUtil;
import com.finalist.cmsc.repository.RepositoryUtil;
import com.finalist.cmsc.services.Properties;
import com.finalist.cmsc.services.sitemanagement.SiteManagement;


public class SearchServiceMMBaseImpl extends SearchService {

    private static Log log = LogFactory.getLog(SearchServiceMMBaseImpl.class);

    @Override
    protected void init(ServletConfig aConfig, Properties aProperties) throws Exception {      
        log.info("SearchServiceMMBaseImpl STARTED");
    }
    
    @Override
    public PageInfo findDetailPageForContent(Node content) {
        NodeList pages = findPagesForContent(content, null);
        if (!pages.isEmpty()) {
            Node pageQueryNode = null;
            if (pages.size() == 1) {
                pageQueryNode = pages.getNode(0);
            }
            else {
                for (Iterator<Node> iter = pages.iterator(); iter.hasNext();) {
                    Node pageNode = iter.next();
                    String key = pageNode.getStringValue(PortletUtil.NODEPARAMETER + "." + PortletUtil.KEY_FIELD);
                    if ("contentelement".equals(key)) {
                        pageQueryNode = pageNode;
                        break;
                    }
                }
            }
            if (pageQueryNode != null) {
                // It was one page match or the content is placed in a detail portlet
                return getPageInfo(pageQueryNode, true);
            }
            else {
                // The homepage (Site object) has a lower preference than a page deeper in the tree 
                List<Node> pageNodes = new ArrayList<Node>();
                for (Iterator<Node> iter = pages.iterator(); iter.hasNext();) {
                    Node pageNode = iter.next();
                    Page page = SiteManagement.getPage(pageNode.getIntValue(PagesUtil.PAGE + ".number"));
                    if (page != null && !(page instanceof Site)) {
                        pageNodes.add(pageNode);
                    }
                }
                if (pageNodes.isEmpty()) {
                    // all matches are sites
                    filterPageQueryNodes(pages, content);
                    if (!pages.isEmpty()) {
                        pageQueryNode = pages.getNode(0);
                    }
                    
                    return getPageInfo(pageQueryNode, true);
                }
                else {
                    // find page which is most suitable to use as detail page
                    filterPageQueryNodes(pageNodes, content);
                    if (!pageNodes.isEmpty()) {
                        pageQueryNode = pageNodes.get(0);
                    }
                    return getPageInfo(pageQueryNode, true);
                }
            }
        }
        return null;
    }

    private void filterPageQueryNodes(List<Node> pages, Node content) {
        for (Iterator<Node> iter = pages.iterator(); iter.hasNext();) {
            Node pageQueryNode = iter.next();
            boolean keep = evaluatePageQueryNode(pageQueryNode, content);
            if (! keep) {
                iter.remove();
            }
        }
    }
    
    private boolean evaluatePageQueryNode(Node pageQueryNode, Node content) {
        Page page = SiteManagement.getPage(pageQueryNode.getIntValue(PagesUtil.PAGE + ".number"));
        String key = pageQueryNode.getStringValue(PortletUtil.NODEPARAMETER + "." + PortletUtil.KEY_FIELD);
        if ("contentchannel".equals(key)) {
            String portletWindowName = pageQueryNode.getStringValue(PortletUtil.PORTLETREL + "." + PortletUtil.LAYOUTID_FIELD);
            Integer portletId = page.getPortlet(portletWindowName);
            Portlet portlet = SiteManagement.getPortlet(portletId);
            if (portlet != null) {
                return evaluateArchive(portlet, content);
            }
        }
        return true;
    }

    private boolean  evaluateArchive(Portlet portlet, Node content) {
        String archive = portlet.getParameterValue("archive");
        return ContentElementUtil.matchArchive(content, archive);
    }

    @Override
    public List<PageInfo> findAllDetailPagesForContent(Node content) {
        List<PageInfo> result = new ArrayList<PageInfo>();
        
        List<PageInfo> infos = findAllDetailPages(content);
        
        // The homepage (Site object) has a lower preference than a page deeper in the tree
        // For detail pages skip the homepage
        for (PageInfo info : infos) {
            Page page = SiteManagement.getPage(info.getPageNumber());
            if (page != null && !(page instanceof Site)) {
                result.add(info);
            }
        }
        // No pages left then reset
        if (result.isEmpty()) {
            result = infos;
        }
        
        
        
        return result;
    }

    private List<PageInfo> findAllDetailPages(Node content) {
        NodeList pages = findPagesForContent(content, null);
        
        List<PageInfo> infos = new ArrayList<PageInfo>();
        for (Iterator<Node> iter = pages.iterator(); iter.hasNext();) {
            Node pageNode = iter.next();
            PageInfo pageInfo = getPageInfo(pageNode, true);
            if (pageInfo != null && !infos.contains(pageInfo)) {
                infos.add(pageInfo);
            }
        }
        
        
        
        return infos;
    }
    
    @Override
    public List<PageInfo> findPagesForContentElement(Node content) {
        return findPagesForContentElement(content, null);
    }

    @Override
    public List<PageInfo> findPagesForContentElement(Node content, Node channel) {
        NodeList pages = findPagesForContent(content, channel);
        
        List<PageInfo> infos = new ArrayList<PageInfo>();
        for (Iterator<Node> iter = pages.iterator(); iter.hasNext();) {
            Node pageNode = iter.next();
            PageInfo pageInfo = getPageInfo(pageNode, false);
            if (pageInfo != null) {
                infos.add(pageInfo);
            }
        }
        return infos;
    }
    
    private PageInfo getPageInfo(Node pageQueryNode, boolean clicktopage) {
        Page page = SiteManagement.getPage(pageQueryNode.getIntValue(PagesUtil.PAGE + ".number"));
        if (page != null) {
            String portletWindowName = pageQueryNode.getStringValue(PortletUtil.PORTLETREL + "." + PortletUtil.LAYOUTID_FIELD);
            
            if (clicktopage) {
                String key = pageQueryNode.getStringValue(PortletUtil.NODEPARAMETER + "." + PortletUtil.KEY_FIELD);
                if ("contentchannel".equals(key)) {
                    Integer portletId = page.getPortlet(portletWindowName);
                    Portlet portlet = SiteManagement.getPortlet(portletId);
                    
                    if (portlet != null) {
                        String pageNumber = portlet.getParameterValue("page");
                        if (pageNumber != null) {
                            page = SiteManagement.getPage(Integer.valueOf(pageNumber));
                            portletWindowName = portlet.getParameterValue("window");
                        }
                    }
                }
            }
            // Check if a portlet exists on this position
            Integer portletId = page.getPortlet(portletWindowName);
            if (portletId == -1) {
                return null;
            }

            String pagePath = SiteManagement.getPath(page, !ServerUtil.useServerName());
            PageInfo pageInfo = new PageInfo(page.getId(), pagePath, portletWindowName);
            return pageInfo;
        }
        return null;
    }
    
    public boolean hasContentPages(Node content) {
       NodeList pages = findPagesForContent(content, null); 
       return (pages != null && pages.size() > 0);
    }
    
    private NodeList findPagesForContent(Node content, Node channel) {
        NodeList channels;
        Cloud cloud = content.getCloud();
        
        if (channel != null) {
            channels = cloud.createNodeList();
            channels.add(channel);
        }
        else {
            channels = RepositoryUtil.getContentChannels(content);
        }

        if (content != null) {
            channels.add(content);
        }
        
        Query query = createPagesForContentQuery(cloud, channels);
        
        NodeList pages = cloud.getList(query);
        if (pages.isEmpty()) {
            if (content != null) {
                channels.remove(content);
            }
            NodeList collectionchannels = new GenericNodeList();
            for (Iterator<Node> iter = channels.iterator(); iter.hasNext();) {
                Node contentchannel = iter.next();
                NodeList cc = RepositoryUtil.getCollectionChannels(contentchannel);
                if (!cc.isEmpty()) {
                    collectionchannels.addAll(cc);
                }
            }
            if (!collectionchannels.isEmpty()) {
                Query collectionquery = createPagesForContentQuery(cloud, collectionchannels);
                pages = cloud.getList(collectionquery);
            }
        }
        if (content != null) {
            filterPageQueryNodes(pages, content);
        }
        
        return pages;
    }

    private Query createPagesForContentQuery(Cloud cloud, NodeList channels) {
        NodeManager parameterManager = cloud.getNodeManager(PortletUtil.NODEPARAMETER);
        NodeManager portletManager = cloud.getNodeManager(PortletUtil.PORTLET);
        NodeManager pageManager = cloud.getNodeManager(PagesUtil.PAGE);

        Query query = cloud.createQuery();
        Step parameterStep = query.addStep(parameterManager);
        RelationStep step2 = query.addRelationStep(portletManager, PortletUtil.PARAMETERREL, "SOURCE");
        RelationStep step4 = query.addRelationStep(pageManager, PortletUtil.PORTLETREL, "SOURCE");
        Step pageStep = step4.getNext();

        query.addField(parameterStep, parameterManager.getField(PortletUtil.KEY_FIELD));
        query.addField(parameterStep, parameterManager.getField(PortletUtil.VALUE_FIELD));
        query.addField(step4, cloud.getRelationManager(PortletUtil.PORTLETREL).getField(PortletUtil.LAYOUTID_FIELD));
        query.addField(pageStep, pageManager.getField("number"));
        
        SearchUtil.addNodesConstraints(query, parameterManager.getField(PortletUtil.VALUE_FIELD), channels);
        return query;
    }
    
    @Override
    public Set<Node> findContentElementsForPage(Node page) {
        return findContentElementsForPage(page, false);
    }

    @Override
    public Set<Node> findDetailContentElementsForPage(Node page) {
        return findContentElementsForPage(page, true);
    }

    private Set<Node> findContentElementsForPage(Node page, boolean detailOnly) {
        Set<Node> result = new HashSet<Node>();
        if (page != null) {
            Cloud cloud = page.getCloud();

            Page pageObject = SiteManagement.getPage(page.getNumber());
            Collection<Integer> portlets = pageObject.getPortlets();
            for (Integer portletId : portlets) {
                Portlet portlet = SiteManagement.getPortlet(portletId);
                
                if (detailOnly && !isDetailPortlet(portlet)) {
                    continue;
                }
                
                List<Object> parameters = portlet.getPortletparameters();
                for (Iterator<Object> iter = parameters.iterator(); iter.hasNext();) {
                    Object param = iter.next();
                    if (param instanceof NodeParameter) {
                        String value = ((NodeParameter) param).getValueAsString();
                        if (value != null) {
                            Node found = cloud.getNode(value);
                            if (RepositoryUtil.isContentChannel(found)) {
                                NodeList elements = RepositoryUtil.getLinkedElements(found);
                                for (Iterator<Node> iterator = elements.iterator(); iterator.hasNext();) {
                                    Node contentElement = iterator.next();
                                    if (evaluateArchive(portlet, contentElement)) {
                                        result.add(contentElement);
                                    }
                                }
                            } else { 
                                if (ContentElementUtil.isContentElement(found)) {
                                    if (evaluateArchive(portlet, found)) {
                                        result.add(found);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        log.debug("found: '" + result.size() + "' elements for page: '" + page.getNumber() + "'");
        return result;
    }

    private boolean isDetailPortlet(Portlet portlet) {
        String contentchannel = portlet.getParameterValue("contentchannel");
        if (contentchannel != null) {
            String pageNumber = portlet.getParameterValue("page");
            if (pageNumber != null) {
                return false;
            }
        }
        return true;
    }

    
    @Override
    public Set<Node> findLinkedSecondaryContent(Node contentElement, String nodeManager) {
        Set<Node> result = findLinkedSecondaryContent(contentElement, nodeManager, "posrel", "DESTINATION");
        result.addAll(findLinkedSecondaryContent(contentElement, nodeManager, "inlinerel", "DESTINATION"));
        
        return result;        
    }
    
    public Set<Node> findLinkedSecondaryContent(Node contentElement, String nodeManager, String relType, String direction) {
    	Set<Node> result = new HashSet<Node>();
    	
    	NodeList ceList = contentElement.getRelatedNodes(nodeManager, relType, direction);
    	Iterator<Node> ceIt = ceList.iterator();
    	while (ceIt.hasNext()) {
    		Node node = ceIt.next();
    		log.debug("Linked content (" + nodeManager + ") '" + node.getNumber() + "'");
    		result.add(node);
    	}
    	return result;
    }
    @Override
    public String getPortletWindow(int pageId, String elementNumber) {
        Cloud cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase");
        Node node = cloud.getNode(elementNumber);
        if (ContentElementUtil.isContentElement(node)) {
            List<PageInfo> infos = findAllDetailPages(node);
            
            if (!infos.isEmpty()) {
                for (PageInfo pageInfo : infos) {
                    if (pageId == pageInfo.getPageNumber()) {
                        return pageInfo.getWindowName();
                    }
                }
            }
        }

        return null;
    }

}
