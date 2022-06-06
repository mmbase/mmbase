/*

 This software is OSI Certified Open Source Software.
 OSI Certified is a certification mark of the Open Source Initiative.

 The license (Mozilla version 1.0) can be read at the MMBase site.
 See http://www.MMBase.org/license

 */
package org.mmbase.applications.multilanguagegui;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.*;
import org.mmbase.module.core.*;

import java.net.*;
import java.io.*;
import java.util.*;

import org.xml.sax.InputSource;

import org.mmbase.util.*;
import org.mmbase.util.xml.*;
import org.mmbase.bridge.*;
import org.mmbase.bridge.implementation.*;

import org.w3c.dom.*;

/**
 *
 * @author Dani&euml;l Ockeloen
 * @javadoc
 */

public class MultiLanguageGui {

    private static final Logger log = Logging.getLoggerInstance(MultiLanguageGui.class);
    private static Hashtable languageguisets;
    private static Hashtable setfilenames = new Hashtable();
    private static CloudContext context;
    private static Cloud cloud;

    public static final String DTD_LANGUAGEGUISETS_1_0 = "languageguisets_1_0.dtd";
    public static final String DTD_LANGUAGEGUISET_1_0 = "languageguiset_1_0.dtd";

    public static final String PUBLIC_ID_LANGUAGEGUISETS_1_0 = "-//MMBase//DTD MMBase - languageguisets 1.0//EN";
    public static final String PUBLIC_ID_LANGUAGEGUISET_1_0 = "-//MMBase//DTD MMBase - languageguiset 1.0//EN";


    // used to make code compile agains both 1.8 and 1.9
    protected static Iterable<Element> list(final Object o) {
        if (o instanceof Iterator) {
            return new Iterable<Element>() {
                public Iterator<Element> iterator() {
                    return (Iterator<Element>) o;
                }
            };
        } else {
            return (Iterable<Element>) o;
        }
    }
    /**
     * Register the Public Ids for DTDs used by DatabaseReader This method is called by EntityResolver.
     */
    public static void registerPublicIDs() {
        EntityResolver.registerPublicID(PUBLIC_ID_LANGUAGEGUISETS_1_0, DTD_LANGUAGEGUISETS_1_0, MultiLanguageGui.class);
        EntityResolver.registerPublicID(PUBLIC_ID_LANGUAGEGUISET_1_0, DTD_LANGUAGEGUISET_1_0, MultiLanguageGui.class);
    }

    public static void readSets() {
        languageguisets = new Hashtable();
        String languageguisetsfile = "languageguisets.xml";
        try {
            InputSource is = ResourceLoader.getConfigurationRoot().getInputSource("multilanguagegui/" + languageguisetsfile);
            DocumentReader reader = new DocumentReader(is, MultiLanguageGui.class);
            if (reader != null) {
                for (Element n : list(reader.getChildElements("languageguisets", "languageguiset"))) {

                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        String setfile = null;
                        String setname = null;

                        // decode filename
                        org.w3c.dom.Node n3 = nm.getNamedItem("file");
                        if (n3 != null) {
                            setfile = File.separator + n3.getNodeValue();
                            n3 = nm.getNamedItem("name");
                            if (n3 != null) {
                                decodeLanguageGuiSet(setfile, n3.getNodeValue());
                                setfilenames.put(n3.getNodeValue(), setfile);
                            }
                        }

                    }
                }
            } else {
                log.error("Can't read/parse langaugeguiset : " + languageguisetsfile);
            }
        } catch (Exception e) {
            log.error("Can't open languageguisets : " + languageguisetsfile);
        }
    }

    private static void decodeLanguageGuiSet(String filename, String setname) {
        try {
            InputSource is = ResourceLoader.getConfigurationRoot().getInputSource("multilanguagegui" + filename);
            DocumentReader reader = new DocumentReader(is, MultiLanguageGui.class);

            Hashtable languageguiset = new Hashtable();
            for (Element element : list(reader.getChildElements("languageguiset", "keyword"))) {
                String name = reader.getElementAttributeValue(element, "name");
                if (name != null) {
                    Hashtable keyword = new Hashtable();

                    for (Element translation_element : list(reader.getChildElements(element, "translation"))) {
                        String language = reader.getElementAttributeValue(translation_element, "language");
                        String value = reader.getElementAttributeValue(translation_element, "value");
                        keyword.put(language, value);

                    }
                    languageguiset.put(name, keyword);
                }
            }
            languageguisets.put(setname, languageguiset);
        } catch (Exception e) {
            log.error("Can't read languageguiset : " + filename);
        }
    }

    public static String getConversion(String name, String lang) {
        if (languageguisets == null){
            readSets();
        }
        int pos = name.indexOf(".");
        if (pos != -1) {
            String setname = name.substring(0, pos);
            String keyword = name.substring(pos + 1);

            Hashtable set = (Hashtable) languageguisets.get(setname);

            // this is kinda a weird way of creating auto edit links
            // needs to be replaced with a better system.
            if (lang.equals("edit")) {
                String url = "";
                if (set != null) {
                    if (set.containsKey(keyword)) {
                        url = "<a href=\"/mmbase-webapp/mlg/keyword.jsp?keyword=" + keyword + "&setname=" + setname
                                + "\" target=\"mlg\" >*" + name + "*</a>";
                    } else {
                        url = "<a href=\"/mmbase-webapp/mlg/set.jsp?wantedkeyword=" + keyword + "&setname=" + setname
                                + "\" target=\"mlg\" >*" + name + "*</a>";
                    }
                } else {
                    url = "<a href=\"/mmbase-webapp/mlg/index.jsp?askedset=" + setname + "\" target=\"mlg\" >*" + name + "*</a>";
                }
                return url;
            }

            if (set != null) {
                Hashtable kset = (Hashtable) set.get(keyword);
                if (kset != null) {
                    String result = (String) kset.get(lang);
                    if (result != null) {
                        return result;
                    } else {
                        result = (String) kset.get("df");
                        if (result != null) {
                            return result;
                        } else {
                            return "no match/default on keyword : " + keyword + " on set : " + setname;
                        }
                    }
                } else {
                    return "missing keyword : " + keyword + " on set : " + setname;
                }
            } else {
                return "missing set : " + setname;
            }
        } else {
            return "missing set name 'set.keyword'";
        }
    }

    public List getSets() {
        if (languageguisets == null){
            readSets();
        }
        VirtualBuilder builder = new VirtualBuilder(MMBase.getMMBase());

        // create a result list
        List list = new ArrayList();

        Enumeration e = languageguisets.keys();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            MMObjectNode virtual = builder.getNewNode("admin");
            virtual.setValue("name", s);
            list.add(virtual);
        }
        return list;
    }

    public List getKeywords(String setname, String searchkey, String language) {
        if (languageguisets == null){
            readSets();
        }
        VirtualBuilder builder = new VirtualBuilder(MMBase.getMMBase());

        // create a result list
        List list = new ArrayList();

        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            Enumeration e = set.keys();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                MMObjectNode virtual = builder.getNewNode("admin");
                if (searchkey.equals("*") || s.indexOf(searchkey) != -1) {
                    Hashtable keywordset = (Hashtable) set.get(s);
                    String v = (String) keywordset.get(language);
                    virtual.setValue("name", s);
                    virtual.setValue("value", v);
                    list.add(virtual);
                }
            }
        }
        return list;
    }

    public List getKeywordPerLanguage(String setname, String language) {
        if (languageguisets == null) readSets();
        VirtualBuilder builder = new VirtualBuilder(MMBase.getMMBase());

        // create a result list
        List list = new ArrayList();

        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            Enumeration e = set.keys();
            while (e.hasMoreElements()) {
                String keyword = (String) e.nextElement();
                MMObjectNode virtual = builder.getNewNode("admin");
                virtual.setValue("keyword", keyword);
                Hashtable keywordset = (Hashtable) set.get(keyword);
                if (keywordset != null) {
                    String value = (String) keywordset.get(language);
                    if (value == null) {
                        value = (String) keywordset.get("df");
                    }
                    virtual.setValue("translation", value);
                } else {
                    virtual.setValue("translation", "*missing mlg keyword (" + setname + "/" + keyword + "/" + language + ")*");
                }
                list.add(virtual);
            }
        }
        return list;
    }

    public List getLanguagesInSet(String setname) {
        if (languageguisets == null) readSets();
        ArrayList list = new ArrayList();
        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            ArrayList ll = new ArrayList();
            Enumeration e = set.keys();
            while (e.hasMoreElements()) {
                HashMap map = new HashMap();
                String keyword = (String) e.nextElement();
                Hashtable keywordset = (Hashtable) set.get(keyword);
                if (keywordset != null) {
                    Enumeration e2 = keywordset.keys();
                    while (e2.hasMoreElements()) {
                        String lang = (String) e2.nextElement();
                        if (!ll.contains(lang)){
                            ll.add(lang);
                        }
                    }
                }
            }
            Iterator i = ll.iterator();
            while (i.hasNext()) {
                HashMap map = new HashMap();
                map.put("name", (String) i.next());
                list.add(map);
            }
        }

        return list;
    }

    public String getTranslation(String setname, String keyword, String language) {
        if (languageguisets == null){
            readSets();
        }
        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            Hashtable keywordset = (Hashtable) set.get(keyword);
            if (keywordset != null) {
                String value = (String) keywordset.get(language);
                if (value == null) {
                    value = (String) keywordset.get("df");
                }
                return value;
            } else {
                return "*missing mlg keyword (" + setname + "/" + keyword + "/" + language + ")*";
            }
        } else {
            return "*missing mlg set*";
        }
    }

    public List getTranslations(String setname, String keyword) {
        if (languageguisets == null){
            readSets();
        }
        VirtualBuilder builder = new VirtualBuilder(MMBase.getMMBase());

        // create a result list
        List list = new ArrayList();

        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            Hashtable keywordset = (Hashtable) set.get(keyword);
            if (keywordset != null) {

                Enumeration e = keywordset.keys();
                while (e.hasMoreElements()) {
                    String s = (String) e.nextElement();
                    MMObjectNode virtual = builder.getNewNode("admin");
                    virtual.setValue("name", s);
                    String v = (String) keywordset.get(s);
                    virtual.setValue("value", v);
                    list.add(virtual);
                }
            }
        }
        return list;
    }

    public boolean addKeyword(String setname, String keyword) {
        if (languageguisets == null){
            readSets();
        }
        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            set.put(keyword, new Hashtable());
        }

        String filename = (String) setfilenames.get(setname);
        if (filename != null) {
            saveFile(filename, createSetXML(setname));
        }
        filename = "multilanguagegui" + File.separator + "languageguisets.xml";
        saveFile(filename, createSetsXML());
        return true;
    }

    public boolean addSet(String setname) {
        if (languageguisets == null){
            readSets();
        }
        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set == null) {
            languageguisets.put(setname, new Hashtable());
            String setfile = "multilanguagegui" + File.separator + "sets" + File.separator + setname + ".xml";
            setfilenames.put(setname, setfile);
        }

        String filename = (String) setfilenames.get(setname);
        if (filename != null) {
            saveFile(filename, createSetXML(setname));
        }
        filename = "multilanguagegui" + File.separator + "languageguisets.xml";
        saveFile(filename, createSetsXML());
        return true;
    }

    public boolean addLanguage(String setname, String keyword, String language, String value) {
        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            Hashtable keywordset = (Hashtable) set.get(keyword);
            if (keywordset != null) {
                keywordset.put(language, value);
            }
        }

        String filename = (String) setfilenames.get(setname);
        if (filename != null) {
            saveFile(filename, createSetXML(setname));
        }
        return true;
    }

    public boolean changeLanguage(String setname, String keyword, String language, String value) {
        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            Hashtable keywordset = (Hashtable) set.get(keyword);
            if (keywordset != null) {
                keywordset.put(language, value);
            }
        }

        String filename = (String) setfilenames.get(setname);
        if (filename != null) {
            saveFile(filename, createSetXML(setname));
        }
        return true;
    }

    public boolean removeLanguage(String setname, String keyword, String language) {
        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            Hashtable keywordset = (Hashtable) set.get(keyword);
            if (keywordset != null) {
                keywordset.remove(language);
            }
        }

        String filename = (String) setfilenames.get(setname);
        if (filename != null) {
            saveFile(filename, createSetXML(setname));
        }
        return true;
    }

    public boolean removeKeyword(String setname, String keyword) {
        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            set.remove(keyword);
        }

        String filename = (String) setfilenames.get(setname);
        if (filename != null) {
            saveFile(filename, createSetXML(setname));
        }
        return true;
    }

    private String createSetXML(String setname) {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        body += "<!DOCTYPE languageguiset PUBLIC \"//MMBase - languageguiset //\" \"https://www.mmbase.org/dtd/languageguiset_1_0.dtd\">\n";

        Hashtable set = (Hashtable) languageguisets.get(setname);
        if (set != null) {
            body += "<languageguiset>\n";
            Enumeration e = set.keys();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                body += "\t<keyword name=\"" + s + "\">\n";
                Hashtable translations = (Hashtable) set.get(s);
                Enumeration e2 = translations.keys();
                while (e2.hasMoreElements()) {
                    String l = (String) e2.nextElement();
                    String v = (String) translations.get(l);
                    body += "\t\t<translation language=\"" + l + "\" value=\"" + v + "\" />\n";
                }
                body += "\t</keyword>\n";
            }

            body += "</languageguiset>\n";
        }
        return body;
    }

    private String createSetsXML() {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        body += "<!DOCTYPE languageguisets PUBLIC \"//MMBase - languageguisets //\" \"https://www.mmbase.org/dtd/languageguisets_1_0.dtd\">\n";
        body += "<languageguisets>\n";
        Enumeration e = languageguisets.keys();
        while (e.hasMoreElements()) {
            String setname = (String) e.nextElement();
            body += "\t<languageguiset name=\"" + setname + "\" file=\"sets" + File.separator + setname + ".xml\" />\n";
        }
        body += "</languageguisets>\n";
        return body;
    }

    static boolean saveFile(String filename, String body) {
        try {
            Writer wr = ResourceLoader.getConfigurationRoot().getWriter(filename);
            wr.write(body);
            wr.flush();
            wr.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
