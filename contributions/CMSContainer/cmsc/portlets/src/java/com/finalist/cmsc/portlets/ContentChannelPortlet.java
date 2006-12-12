/*

 This software is OSI Certified Open Source Software.
 OSI Certified is a certification mark of the Open Source Initiative.

 The license (Mozilla version 1.0) can be read at the MMBase site.
 See http://www.MMBase.org/license

 */
package com.finalist.cmsc.portlets;

import java.io.IOException;
import java.util.*;

import javax.portlet.*;

import net.sf.mmapps.commons.util.StringUtil;

import org.mmbase.bridge.Node;

import com.finalist.cmsc.beans.om.ContentElement;
import com.finalist.cmsc.portalImpl.PortalConstants;
import com.finalist.cmsc.services.contentrepository.ContentRepository;
import com.finalist.cmsc.services.sitemanagement.SiteManagement;

/**
 * Portlet to edit content elements
 * 
 * @author Wouter Heijke
 */
public class ContentChannelPortlet extends AbstractContentPortlet {

    protected static final String DISPLAYTYPE_PARAM = "displayType";

    protected static final String USE_PAGING = "usePaging";
    protected static final String OFFSET = "pager.offset";
	protected static final String SHOW_PAGES = "showPages";
    protected static final String ELEMENTS_PER_PAGE = "elementsPerPage";
    protected static final String PAGES_INDEX = "pagesIndex";
    protected static final String INDEX_POSITION = "position";

    protected static final String ELEMENTS = "elements";
    protected static final String TYPES = "types";
    protected static final String TOTAL_ELEMENTS = "totalElements";

    protected static final String ARCHIVE = "archive";
    protected static final String USE_LIFECYCLE = "useLifecycle";

    protected static final String MAX_ELEMENTS = "maxElements";
    protected static final String DIRECTION = "direction";
    protected static final String ORDERBY = "orderby";
    protected static final String CONTENTCHANNEL = "contentchannel";

    protected static final String VIEW_TYPE = "viewtype";
    protected static final String DISPLAY_TYPE = "displaytype";

	private static final String YEAR = "year";
	private static final String MONTH = "month";
	private static final String DAY = "day";
	
	private static final String ARCHIVE_PAGE = "archivepage";
	private static final String START_INDEX = "startindex";
		
    

    protected void saveParameters(ActionRequest request, String portletId) {
        setPortletNodeParameter(portletId, CONTENTCHANNEL, request.getParameter(CONTENTCHANNEL));

        setPortletParameter(portletId, ORDERBY, request.getParameter(ORDERBY));
        setPortletParameter(portletId, DIRECTION, request.getParameter(DIRECTION));
        setPortletParameter(portletId, USE_LIFECYCLE, request.getParameter(USE_LIFECYCLE));
        setPortletParameter(portletId, ARCHIVE, request.getParameter(ARCHIVE));
        setPortletParameter(portletId, ELEMENTS_PER_PAGE, request.getParameter(ELEMENTS_PER_PAGE));
        setPortletParameter(portletId, MAX_ELEMENTS, request.getParameter(MAX_ELEMENTS));
        setPortletParameter(portletId, SHOW_PAGES, request.getParameter(SHOW_PAGES));
        setPortletParameter(portletId, USE_PAGING, request.getParameter(USE_PAGING));
        setPortletParameter(portletId, PAGES_INDEX, request.getParameter(PAGES_INDEX));
        setPortletParameter(portletId, INDEX_POSITION, request.getParameter(INDEX_POSITION));
        setPortletParameter(portletId, VIEW_TYPE, request.getParameter(VIEW_TYPE));
        setPortletParameter(portletId, ARCHIVE_PAGE, request.getParameter(ARCHIVE_PAGE));
        setPortletParameter(portletId, START_INDEX, request.getParameter(START_INDEX));        
    }

    protected void setEditResponse(ActionRequest request, ActionResponse response,
            HashMap<String, Node> nodesMap) throws PortletModeException {
        if (nodesMap.size() == 1) {
            String displayType  = request.getParameter(DISPLAYTYPE_PARAM);
            if (displayType == null || "detail".equals(displayType)) {
                Iterator nodesIt = nodesMap.values().iterator();
                Node n = (Node) nodesIt.next();
                response.setRenderParameter(ELEMENT_ID, String.valueOf(n.getNumber()));
            }
        }
        response.setPortletMode(PortletMode.VIEW);
    }

    @Override
    protected void doView(RenderRequest req, RenderResponse res) throws PortletException, IOException {
        PortletPreferences preferences = req.getPreferences();
        
        String channel = preferences.getValue(CONTENTCHANNEL, null);
        if (!StringUtil.isEmpty(channel)) {
            addContentElements(req);
            super.doView(req, res);
        }
    }

    @Override
    protected void doEdit(RenderRequest req, RenderResponse res) throws PortletException, IOException {
        String elementId = req.getParameter(ELEMENT_ID);
        if (StringUtil.isEmpty(elementId)) {
            addContentElements(req);
            PortletPreferences preferences = req.getPreferences();
            String channel = preferences.getValue(CONTENTCHANNEL, null);
            if (!StringUtil.isEmpty(channel)) {
                if (ContentRepository.mayEdit(channel)) {
                    super.doEdit(req, res);
                }
                else {
                    super.doView(req, res);
                }
            }
        }
        else {
            doEdit(req, res, elementId);
        }
    }
 
    protected void addContentElements(RenderRequest req) {
        String elementId = req.getParameter(ELEMENT_ID);
        if (StringUtil.isEmpty(elementId)) {
            PortletPreferences preferences = req.getPreferences();
            String portletId = preferences.getValue(PortalConstants.CMSC_OM_PORTLET_ID, null);
            List<String> contenttypes = SiteManagement.getContentTypes(portletId);
            
            String channel = preferences.getValue(CONTENTCHANNEL, null);            
            
            int offset = 0;               
            String currentOffset = req.getParameter(OFFSET);
            if (!StringUtil.isEmpty(currentOffset)) {
                offset = Integer.parseInt(currentOffset);                
            }            
            int startIndex = Integer.parseInt( preferences.getValue(START_INDEX, "1") ) - 1;
            if (startIndex > 0) {
            	offset = offset + startIndex ; 
            }            
            setAttribute(req, "offset", offset);          
            
            String orderby = preferences.getValue(ORDERBY, null);
            String direction = preferences.getValue(DIRECTION, null);
            String useLifecycle = preferences.getValue(USE_LIFECYCLE, null);
            String archive = preferences.getValue(ARCHIVE, null);
            
            int maxElements = Integer.parseInt( preferences.getValue(MAX_ELEMENTS, "-1") );
            if (maxElements <= 0) {
                maxElements = Integer.MAX_VALUE;
            }
            int elementsPerPage = Integer.parseInt( preferences.getValue(ELEMENTS_PER_PAGE, "-1") );
            if (elementsPerPage <= 0) {
                elementsPerPage = Integer.MAX_VALUE;
            }
            elementsPerPage = Math.min(elementsPerPage, maxElements);
            
            String filterYear = req.getParameter(YEAR);
            int year = (filterYear == null)?-1:Integer.parseInt(filterYear);
            
            String filterMonth = req.getParameter(MONTH);
            int month = (filterMonth == null)?-1:Integer.parseInt(filterMonth);

            String filterDay = req.getParameter(DAY);
            int day = (filterDay == null)?-1:Integer.parseInt(filterDay);
            
            int totalItems = ContentRepository.countContentElements(channel, contenttypes, orderby, direction, 
                    Boolean.valueOf(useLifecycle).booleanValue(), archive, offset, elementsPerPage, year, month, day);
            if (startIndex > 0) {
            	totalItems = totalItems - startIndex; 
            }
            
            List<ContentElement> elements = ContentRepository.getContentElements(channel, contenttypes, orderby, direction, 
                    Boolean.valueOf(useLifecycle).booleanValue(), archive, offset, elementsPerPage, year, month, day);
            
            setAttribute(req, ELEMENTS, elements);
            if (contenttypes != null && !contenttypes.isEmpty()) {
                setAttribute(req, TYPES, contenttypes);
            }
            setAttribute(req, TOTAL_ELEMENTS, Math.min(maxElements, totalItems));
            setAttribute(req, ELEMENTS_PER_PAGE, elementsPerPage);
    
            String pagesIndex = preferences.getValue(PAGES_INDEX, null);
            if (StringUtil.isEmpty(pagesIndex)) {
                setAttribute(req, PAGES_INDEX, "center");
            }

            String showPages = preferences.getValue(SHOW_PAGES, null);
            if (StringUtil.isEmpty(showPages)) {
                setAttribute(req, SHOW_PAGES, 10);
            }
            
            boolean usePaging = Boolean.valueOf( preferences.getValue(USE_PAGING, "true") );
            if (usePaging) {
                usePaging = totalItems > elementsPerPage;
            }
            setAttribute(req, USE_PAGING, usePaging);
            
            String indexPosition = preferences.getValue(INDEX_POSITION, null);
            if (StringUtil.isEmpty(indexPosition)) {
                setAttribute(req, INDEX_POSITION, "bottom");
            }
            String viewType = preferences.getValue(VIEW_TYPE, null);
            if (StringUtil.isEmpty(viewType)) {
                setAttribute(req, DISPLAY_TYPE, "list");
            }
            else {
                if ("oneDetail".equalsIgnoreCase(viewType)){
                    if (totalItems == 1) {
                        setAttribute(req, DISPLAY_TYPE, "detail");
                    }
                    else {
                        setAttribute(req, DISPLAY_TYPE, "list");
                    }
                }
                else {
                    setAttribute(req, DISPLAY_TYPE, viewType);
                }
            }
        }
        else {
            setMetaData(req, elementId);
        }
    }

    public int getOffset(int currentPage, int pageSize) {
        return ((currentPage - 1) * pageSize) + 1 ;
    }
    
}
