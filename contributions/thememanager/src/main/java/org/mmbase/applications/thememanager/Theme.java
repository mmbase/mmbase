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
import org.xml.sax.InputSource;
import org.w3c.dom.*;
import org.mmbase.module.core.*;
import org.mmbase.util.xml.*;
import org.mmbase.util.*;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * @author Daniel Ockeloen
 *
 */
public class Theme {

    // logger
    static private final Logger log = Logging.getLoggerInstance(Theme.class);
    private HashMap stylesheets;
    private HashMap stylesheetmanagers = new HashMap();
    private HashMap imagesets;
    private String mainid, themefilename;

    public static final String DTD_THEME_1_0 = "theme_1_0.dtd";
    public static final String DTD_ASSIGNED_1_0 = "assigned_1_0.dtd";

    public static final String PUBLIC_ID_THEME_1_0 = "-//MMBase//DTD theme config 1.0//EN";
    public static final String PUBLIC_ID_ASSIGNED_1_0 = "-//MMBase//DTD assigned config 1.0//EN";

    /**
     * Register the Public Ids for DTDs used by DatabaseReader This method is called by EntityResolver.
     */
    public static void registerPublicIDs() {
        EntityResolver.registerPublicID(PUBLIC_ID_THEME_1_0, DTD_THEME_1_0, Theme.class);
        EntityResolver.registerPublicID(PUBLIC_ID_ASSIGNED_1_0, DTD_ASSIGNED_1_0, Theme.class);
    }

    public Theme(String mainid, String themefilename, boolean create) {
        this.mainid = mainid;
        this.themefilename = themefilename;
        if (!create) {
            readTheme(themefilename);
        }
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
        HashMap subset = new HashMap();
        Iterator i = imagesets.entrySet().iterator();
        while (i.hasNext()) {
            log.debug("apparently there is an imageset");

            ImageSet is = (ImageSet) imagesets.get(((Map.Entry) i.next()).getKey());
            if (is.isRole(role)) {
                subset.put(is.getId(), is);
            }
        }
        return subset;
    }

    public ImageSet getImageSet(String id) {
        return (ImageSet) imagesets.get(id);
    }

    public String getStyleSheet(String id) {
        return (String) stylesheets.get(id);
    }

    public void addStyleSheet(String id, String value) {
        if (stylesheets == null){
            stylesheets = new HashMap();
        }
        stylesheets.put(id, value);
    }

    public void addImageSet(String id, ImageSet im) {
        if (imagesets == null){
            imagesets = new HashMap();
        }
        imagesets.put(id, im);
    }

    public int getStyleSheetsCount() {
        if (stylesheets != null) {
            return stylesheets.size();
        }
        return 0;
    }

    public int getImageSetsCount() {
        if (imagesets != null) {
            return imagesets.size();
        }
        return 0;
    }

    public void save() {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        body += "<!DOCTYPE theme PUBLIC \"//MMBase - theme//\" \"https://www.mmbase.org/dtd/theme_1_0.dtd\">\n";
        body += "<theme>\n";
        HashMap m = getStyleSheets();
        if (m != null) {
            Iterator keys = m.keySet().iterator();
            while (keys.hasNext()) {
                String k = (String) keys.next();
                String v = (String) m.get(k);
                if (v.equals("default")) {
                    body += "\t<stylesheet file=\"" + v + "\" />\n";
                } else {
                    body += "\t<stylesheet id=\"" + k + "\" file=\"" + v + "\" />\n";
                }
            }
        }
        m = getImageSets();
        if (m != null) {
            Iterator keys = m.keySet().iterator();
            while (keys.hasNext()) {
                String k = (String) keys.next();
                ImageSet im = (ImageSet) m.get(k);
                if (k.equals("default")) {
                    body += "\t<imageset role=\"" + im.getRole() + "\">\n";
                } else {
                    body += "\t<imageset id=\"" + k + "\" role=\"" + im.getRole() + "\">\n";
                }
                Iterator i3 = im.getImageIds();
                while (i3.hasNext()) {
                    String id = (String) i3.next();
                    String idf = im.getImage(id);
                    body += "\t\t<image id=\"" + id + "\" size=\"small\" file=\"" + idf + "\" />\n";
                }
                body += "\t</imageset>\n";
            }
        }
        body += "</theme>\n";
        try {
            Writer wr = ResourceLoader.getConfigurationRoot().getWriter(themefilename);
            wr.write(body);
            wr.flush();
            wr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getFileName() {
        if (themefilename.startsWith("thememanager/")){
            return themefilename.substring(13);
        }
        return themefilename;
    }

    public void readTheme(String themefilename) {
        stylesheets = new HashMap();
        imagesets = new HashMap();

        try {
            InputSource ris = ResourceLoader.getConfigurationRoot().getInputSource("thememanager/" + themefilename);
            DocumentReader reader = new DocumentReader(ris, Theme.class);
            if (reader != null) {
                // decode stylesheets
                for (Element n : ThemeManager.list(reader.getChildElements("theme", "stylesheet"))) {
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        String id = "default";
                        String stylefilename = null;

                        // decode name
                        org.w3c.dom.Node n3 = nm.getNamedItem("id");
                        if (n3 != null) {
                            id = n3.getNodeValue();
                        }
                        // decode filename
                        n3 = nm.getNamedItem("file");
                        if (n3 != null) {
                            stylefilename = n3.getNodeValue();
                        }
                        stylesheets.put(id, mainid + File.separator + stylefilename);
                    }
                }

                for (Element n : ThemeManager.list(reader.getChildElements("theme", "imageset"))) {
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        String id = "default";
                        String stylefilename = null;
                        String role = "";
                        // decode name
                        org.w3c.dom.Node n3 = nm.getNamedItem("id");
                        if (n3 != null) {
                            id = n3.getNodeValue();
                        }
                        org.w3c.dom.Node n4 = nm.getNamedItem("role");
                        if (n4 != null) {
                            role = n4.getNodeValue();
                        }
                        ImageSet is;
                        if (role.equals("")) {
                            is = new ImageSet(id);
                        } else {
                            is = new ImageSet(id, role);
                        }
                        for (Element n2 : ThemeManager.list(reader.getChildElements(n, "image"))) {
                            NamedNodeMap nm2 = n2.getAttributes();
                            if (nm2 != null) {
                                String imageid = null;
                                String imagefile = null;
                                n3 = nm2.getNamedItem("id");
                                if (n3 != null) {
                                    imageid = n3.getNodeValue();
                                }
                                n3 = nm2.getNamedItem("file");
                                if (n3 != null) {
                                    imagefile = n3.getNodeValue();
                                    is.setImage(imageid, imagefile);
                                }

                            }
                        }
                        imagesets.put(id, is);

                    }
                }

            }

        } catch (Exception e) {
            log.error("missing style file : " + themefilename);
        }
    }

    public StyleSheetManager getStyleSheetManager(String stylesheet) {
        Object o = stylesheetmanagers.get(stylesheet);
        if (o != null) {
            return (StyleSheetManager) o;
        } else {
            String filename = getStyleSheet(stylesheet);
            if (filename != null) {
                StyleSheetManager nm = new StyleSheetManager(filename);
                stylesheetmanagers.put(stylesheet, nm);
                return nm;
            }
        }
        return null;
    }

    public String getId() {
        return mainid;
    }

}
