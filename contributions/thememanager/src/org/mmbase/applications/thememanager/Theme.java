/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.applications.thememanager;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import org.mmbase.module.core.*;

import org.mmbase.util.*;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * @author Daniel Ockeloen
 * 
 */
public class Theme {
 
    // logger
    static private Logger log = Logging.getLoggerInstance(Theme.class.getName()); 
    private HashMap stylesheets;
    private HashMap imagesets;
    private String mainid;


    public static final String DTD_THEME_1_0 = "theme_1_0.dtd";
    public static final String DTD_ASSIGNED_1_0 = "assigned_1_0.dtd";

    public static final String PUBLIC_ID_THEME_1_0 = "-//MMBase//DTD theme config 1.0//EN";
    public static final String PUBLIC_ID_ASSIGNED_1_0 = "-//MMBase//DTD assigned config 1.0//EN";

    /**
     * Register the Public Ids for DTDs used by DatabaseReader
     * This method is called by XMLEntityResolver.
     */
    public static void registerPublicIDs() {
        XMLEntityResolver.registerPublicID(PUBLIC_ID_THEME_1_0, DTD_THEME_1_0, Theme.class);
        XMLEntityResolver.registerPublicID(PUBLIC_ID_ASSIGNED_1_0, DTD_ASSIGNED_1_0, Theme.class);
    }

   public Theme(String mainid,String themefilename) {
	this.mainid=mainid;
	readTheme(themefilename);	
   }


   public String getStyleSheet() {
	return getStyleSheet("default");
   }

   public HashMap getStyleSheets() {
	return stylesheets;
   }

   public HashMap getImageSets() {
	return imagesets;
   }

   public HashMap getImageSets(String role) {
       log.debug("getting imagesets with role = " + role);
	HashMap subset=new HashMap();
        Iterator i=imagesets.entrySet().iterator();
        while (i.hasNext()) {
            log.debug("apparently there is an imageset");

             ImageSet is=(ImageSet)imagesets.get(((Map.Entry)i.next()).getKey());
             if (is.isRole(role)) {
	     	subset.put(is.getId(),is);
	     }
	}
	return subset;
   }

   public ImageSet getImageSet(String id) {
	return (ImageSet)imagesets.get(id);
   }

   public String getStyleSheet(String id) {
	return (String)stylesheets.get(id);
   }

   public int getStyleSheetsCount() {
	if (stylesheets!=null) {
		return stylesheets.size();
	}
	return 0;
   }


   public int getImageSetsCount() {
	if (imagesets!=null) {
		return imagesets.size();
	}
	return 0;
   }

    public void readTheme(String themefilename) {
       stylesheets=new HashMap();
       imagesets=new HashMap();

       String filename = MMBaseContext.getConfigPath()+File.separator+"thememanager"+File.separator+themefilename;

        File file = new File(filename);
        if (file.exists()) {
            	XMLBasicReader reader = new XMLBasicReader(filename,Theme.class);
            	if(reader!=null) {
			// decode stylesheets
            		for(Enumeration ns=reader.getChildElements("theme","stylesheet");ns.hasMoreElements(); ) {
                		Element n=(Element)ns.nextElement();
   		               	NamedNodeMap nm=n.getAttributes();
                    		if (nm!=null) {
					String id="default";
					String stylefilename=null;
				
					// decode name
                        		org.w3c.dom.Node n3=nm.getNamedItem("id");
                        		if (n3!=null) {
						id=n3.getNodeValue();
					}
					// decode filename
                        		n3=nm.getNamedItem("file");
                        		if (n3!=null) {
						stylefilename=n3.getNodeValue();
					}
					stylesheets.put(id,mainid+File.separator+stylefilename);	
			    	}
			}

            		for(Enumeration ns=reader.getChildElements("theme","imageset");ns.hasMoreElements(); ) {
                		Element n=(Element)ns.nextElement();
   		               	NamedNodeMap nm=n.getAttributes();
                    		if (nm!=null) {
					String id="default";
					String stylefilename=null;
                    String role = "";				
					// decode name
                        		org.w3c.dom.Node n3=nm.getNamedItem("id");
                    if (n3!=null) {
					    id=n3.getNodeValue();
					}
                    org.w3c.dom.Node n4 = nm.getNamedItem("role");
                    if (n4 != null) {
                        role = n4.getNodeValue();
                    }
                    ImageSet is;
                    if (role.equals(""))
 {
                        is=new ImageSet(id);
                    } else {
                        is = new ImageSet(id,role);
                    } 
            				for(Enumeration ns2=reader.getChildElements(n,"image");ns2.hasMoreElements(); ) {
					
                			Element n2=(Element)ns2.nextElement();
   		                	NamedNodeMap nm2=n2.getAttributes();
                    			if (nm2!=null) {
						String imageid=null;
						String imagefile=null;
                        			n3=nm2.getNamedItem("id");
                        			if (n3!=null) {
							imageid=n3.getNodeValue();
						}
                        			n3=nm2.getNamedItem("file");
                        			if (n3!=null) {
							imagefile=n3.getNodeValue();
							is.setImage(imageid,imagefile);
						}
						
					}
					}
					imagesets.put(id,is);	

			    	}
			}

		}

	} else {
            log.error("missing style file : "+filename);
	}
    }

}
