/*

 This software is OSI Certified Open Source Software.
 OSI Certified is a certification mark of the Open Source Initiative.

 The license (Mozilla version 1.0) can be read at the MMBase site.
 See http://www.MMBase.org/license

 */
package com.finalist.cmsc.taglib.navigation;

import java.util.Iterator;
import java.util.List;

import com.finalist.cmsc.beans.om.Page;
import com.finalist.cmsc.services.sitemanagement.SiteManagement;
import com.finalist.cmsc.taglib.AbstractListTag;

/**
 * path of pages
 */
public class PathTag extends AbstractListTag<Page> {

    private static final String MODE_ALL = "all";
    private static final String MODE_HIDDEN = "hidden";
    private static final String MODE_MENU = "menu";
    
    private String mode = MODE_MENU;

	protected List<Page> getList() {
		String path = getPath();
        
        List<Page> pages = SiteManagement.getListFromPath(path);
        
        if (pages != null ) {
            if (MODE_MENU.equalsIgnoreCase(mode) || MODE_ALL.equalsIgnoreCase(mode)) {
                boolean hideChildren = false;
                for (Iterator<? extends Page> iter = pages.iterator(); iter.hasNext();) {
                    Page page = iter.next();
                    if (hideChildren || !page.isInmenu()) {
                        iter.remove();
                        hideChildren = true;
                    }
                }
            }
            if (MODE_HIDDEN.equalsIgnoreCase(mode) || MODE_ALL.equalsIgnoreCase(mode)) {
                boolean showChildren = false;
                for (Iterator<? extends Page> iter = pages.iterator(); iter.hasNext();) {
                    Page page = iter.next();
                    if (showChildren || page.isInmenu()) {
                        iter.remove();
                        showChildren = true;
                    }
                }
            }
        }

        return pages;
	}
}
