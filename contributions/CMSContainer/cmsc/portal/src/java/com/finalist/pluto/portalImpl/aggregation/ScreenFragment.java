package com.finalist.pluto.portalImpl.aggregation;

import java.io.IOException;
import java.util.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.finalist.cmsc.beans.om.*;
import com.finalist.cmsc.portalImpl.PortalConstants;

/**
 * @author Wouter Heijke
 */
public class ScreenFragment extends AbstractFragment {
	private static Log log = LogFactory.getLog(ScreenFragment.class);
    
    private List<Fragment> children = new ArrayList<Fragment>();
	
	private Page page;
    private Layout layout;

	public ScreenFragment(ServletConfig config, Page page, Layout layout) throws Exception {
		super(null, config, null);
		this.page = page;
        this.layout = layout; 
        log.debug("Create - page: " + page.getId() + " layout: " + page.getLayout());
	}

    public void processAction(HttpServletRequest request, HttpServletResponse response, String actionFragment) throws IOException {
        Fragment fragment = getFragment(actionFragment);
        if (fragment instanceof PortletFragment) {
            PortletFragment pf = (PortletFragment) fragment;

            setupRequest(request);
            pf.processAction(request, response);
        }
    }

	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setupRequest(request);
	    
	    Iterator<Fragment> portlets = this.getChildFragments().iterator();
        while(portlets.hasNext()) {
            Fragment portlet = portlets.next();
            // let the Portlet do it's thing
            portlet.service(request, response);
        }
        
		if (page != null) {
			if (layout != null) {
                log.debug("using layout:'" + layout.getResource() + "' for page:'" + page.getTitle() + "'");

                request.setAttribute(PortalConstants.FRAGMENT, this);
				RequestDispatcher rd = getMainRequestDispatcher(layout.getResource());
				rd.include(request, response);
                request.removeAttribute(PortalConstants.FRAGMENT);
            } else {
				log.error("No layout for Screen");
			}
		} else {
			log.error("No page for ScreenFragment");
		}
	}

    public void setupRequest(HttpServletRequest request) {
        request.setAttribute(PortalConstants.CMSC_OM_PAGE_ID, String.valueOf(getPage().getId()));
    }

	public Page getPage() {
		return page;
	}
    
    public Layout getLayout() {
        return layout;
    }

	public String getKey() {
		return this.getId();
	}

   public Collection<Fragment> getChildFragments() {
        return children;
    }

    public void addChild(Fragment child) {
        children.add(child);
    }

    public Fragment getFragment(String id) {
        Fragment fragment = null;
        Iterator<Fragment> iterator = this.getChildFragments().iterator();
        while (iterator.hasNext()) {
            Fragment tmp = iterator.next();
            if (tmp != null) {
                if (tmp.getKey().equalsIgnoreCase(id)) {
                    fragment = tmp;
                    break;
                }
            }
        }
        log.debug("getFragment: '" + id + "':'" + fragment + "'");
        return fragment;
    }
}
