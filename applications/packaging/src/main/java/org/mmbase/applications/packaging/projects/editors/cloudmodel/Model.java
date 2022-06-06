/*
 *  This software is OSI Certified Open Source Software.
 *  OSI Certified is a certification mark of the Open Source Initiative.
 *  The license (Mozilla version 1.0) can be read at the MMBase site.
 *  See http://www.MMBase.org/license
 */
package org.mmbase.applications.packaging.projects.editors.cloudmodel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mmbase.applications.packaging.util.ExtendedDocumentReader;
import org.mmbase.util.xml.EntityResolver;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * DisplayHtmlPackage, Handler for html packages
 *
 * @author Daniel Ockeloen (MMBased)
 */
public class Model {

    private static Logger log = Logging.getLoggerInstance(Model.class);

    /**
     * Description of the Field
     */
    public final static String DTD_PACKAGING_CLOUD_MODEL_1_0 = "packaging_cloud_model_1_0.dtd";

    public final static String DTD_BUILDER_1_1 = "builder_1_1.dtd";

    /**
     * Description of the Field
     */
    public final static String PUBLIC_ID_PACKAGING_CLOUD_MODEL_1_0 = "-//MMBase//DTD packaging_cloud_model config 1.0//EN";

    public final static String PUBLIC_ID_BUILDER_1_1 = "-//MMBase//DTD builder config 1.1//EN";

    private ArrayList<NeededBuilder> neededbuilders = new ArrayList<NeededBuilder>();

    private ArrayList<NeededRelDef> neededreldefs = new ArrayList<NeededRelDef>();

    private ArrayList<AllowedRelation> allowedrelations = new ArrayList<AllowedRelation>();

    private String path;

    /**
     * Description of the Method
     */
    public static void registerPublicIDs() {
        EntityResolver.registerPublicID(PUBLIC_ID_PACKAGING_CLOUD_MODEL_1_0,
                "DTD_PACKAGING_CLOUD_MODEL_1_0", Model.class);
        EntityResolver.registerPublicID(PUBLIC_ID_BUILDER_1_1, "DTD_BUILDER_1_1", Model.class);
    }

    public Model(String modelfilename) {
        modelfilename = modelfilename.replace('/', File.separatorChar);
        modelfilename = modelfilename.replace('\\', File.separatorChar);
        this.path = modelfilename;
        readModel(modelfilename);
    }

    public Iterator<NeededBuilder> getNeededBuilders() {
        return neededbuilders.iterator();
    }

    public NeededBuilder getNeededBuilder(String buildername) {
        Iterator<NeededBuilder> nbl = getNeededBuilders();
        while (nbl.hasNext()) {
            NeededBuilder nb = nbl.next();
            if (nb.getName().equals(buildername)) { return nb; }
        }
        return null;
    }

    public Iterator<NeededRelDef> getNeededRelDefs() {
        return neededreldefs.iterator();
    }

    public Iterator<AllowedRelation> getAllowedRelations() {
        return allowedrelations.iterator();
    }

    public boolean addNeededBuilder(String builder, String maintainer, String version) {
        NeededBuilder nb = new NeededBuilder();
        nb.setName(builder);
        nb.setMaintainer(maintainer);
        nb.setVersion(version);
        neededbuilders.add(nb);
        writeModel();
        return true;
    }

    public boolean deleteNeededBuilder(String builder, String maintainer, String version) {
        Iterator<NeededBuilder> nbl = getNeededBuilders();
        while (nbl.hasNext()) {
            NeededBuilder nb = nbl.next();
            if (nb.getName().equals(builder) && nb.getMaintainer().equals(maintainer)
                    && nb.getVersion().equals(version)) {
                neededbuilders.remove(nb);
                writeModel();
                return true;
            }
        }
        return false;
    }

    public boolean addNeededRelDef(String source, String target, String direction,
            String guisourcename, String guitargetname, String builder) {
        NeededRelDef nr = new NeededRelDef();
        nr.setSource(source);
        nr.setTarget(target);
        nr.setDirection(direction);
        nr.setGuiSourceName(guisourcename);
        nr.setGuiTargetName(guitargetname);
        nr.setBuilderName(builder);
        neededreldefs.add(nr);
        writeModel();
        return true;
    }

    public boolean deleteNeededRelDef(String source, String target, String direction,
            String guisourcename, String guitargetname, String builder) {
        Iterator<NeededRelDef> nrl = getNeededRelDefs();
        while (nrl.hasNext()) {
            NeededRelDef nr = nrl.next();
            if (nr.getSource().equals(source) && nr.getTarget().equals(target)
                    && nr.getDirection().equals(direction)
                    && nr.getGuiSourceName().equals(guitargetname)
                    && nr.getGuiTargetName().equals(guitargetname)
                    && nr.getBuilderName().equals(builder)) {
                allowedrelations.remove(nr);
                writeModel();
                return true;
            }
        }
        return true;
    }

    public boolean addAllowedRelation(String from, String to, String type) {
        AllowedRelation ar = new AllowedRelation();
        ar.setFrom(from);
        ar.setTo(to);
        ar.setType(type);
        allowedrelations.add(ar);
        writeModel();
        return true;
    }

    public boolean deleteAllowedRelation(String from, String to, String type) {
        Iterator<AllowedRelation> arl = getAllowedRelations();
        while (arl.hasNext()) {
            AllowedRelation ar = arl.next();
            if (ar.getFrom().equals(from) && ar.getTo().equals(to) && ar.getType().equals(type)) {
                allowedrelations.remove(ar);
                writeModel();
                return true;
            }
        }
        return false;
    }

    private void readModel(String path) {
        File file = new File(path);
        if (file.exists()) {
            ExtendedDocumentReader reader = new ExtendedDocumentReader(path, Model.class);
            if (reader != null) {
                for (Element n: reader.getChildElements("cloudmodel.neededbuilderlist","builder")) {
                    String name = reader.getElementValue(n);
                    NeededBuilder nb = new NeededBuilder();
                    nb.setName(name);
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        org.w3c.dom.Node n2 = nm.getNamedItem("version");
                        if (n2 != null) {
                            nb.setVersion(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("maintainer");
                        if (n2 != null) {
                            nb.setMaintainer(n2.getNodeValue());
                        }
                    }
                    neededbuilders.add(nb);
                    // try to find if this is defined in a real file
                    readBuilder(nb);
                }
                for (Element n: reader.getChildElements("cloudmodel.neededreldeflist","reldef")) {
                    NeededRelDef nr = new NeededRelDef();
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        org.w3c.dom.Node n2 = nm.getNamedItem("source");
                        if (n2 != null) {
                            nr.setSource(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("target");
                        if (n2 != null) {
                            nr.setTarget(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("direction");
                        if (n2 != null) {
                            nr.setDirection(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("guisourcename");
                        if (n2 != null) {
                            nr.setGuiSourceName(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("guitargetname");
                        if (n2 != null) {
                            nr.setGuiTargetName(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("builder");
                        if (n2 != null) {
                            nr.setBuilderName(n2.getNodeValue());
                        }
                    }
                    neededreldefs.add(nr);
                }
                for (Element n: reader.getChildElements("cloudmodel.allowedrelationlist","relation")) {
                    AllowedRelation ar = new AllowedRelation();
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        org.w3c.dom.Node n2 = nm.getNamedItem("from");
                        if (n2 != null) {
                            ar.setFrom(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("to");
                        if (n2 != null) {
                            ar.setTo(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("type");
                        if (n2 != null) {
                            ar.setType(n2.getNodeValue());
                        }
                    }
                    allowedrelations.add(ar);
                }
            }
        }
    }

    private void readBuilder(NeededBuilder nb) {
        String builderpath = path.substring(0, path.length() - 4) + "/" + nb.getName() + ".xml";
        File file = new File(builderpath);
        if (file.exists()) {
            ExtendedDocumentReader reader = new ExtendedDocumentReader(builderpath, Model.class);
            if (reader != null) {
                org.w3c.dom.Node n = reader.getElementByPath("builder");
                if (n != null) {
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        org.w3c.dom.Node n2 = nm.getNamedItem("name");
                        if (n2 != null) {
                            nb.setName(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("maintainer");
                        if (n2 != null) {
                            nb.setMaintainer(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("version");
                        if (n2 != null) {
                            nb.setVersion(n2.getNodeValue());
                        }
                        n2 = nm.getNamedItem("extends");
                        if (n2 != null) {
                            nb.setExtends(n2.getNodeValue());
                        }
                    }
                }
                n = reader.getElementByPath("builder.status");
                if (n != null) {
                    org.w3c.dom.Node n2 = n.getFirstChild();
                    if (n2 != null) {
                        nb.setStatus(n2.getNodeValue());
                    }
                }
                n = reader.getElementByPath("builder.searchage");
                if (n != null) {
                    org.w3c.dom.Node n2 = n.getFirstChild();
                    if (n2 != null) {
                        nb.setSearchAge(n2.getNodeValue());
                    }
                }
                n = reader.getElementByPath("builder.classfile");
                if (n != null) {
                    org.w3c.dom.Node n2 = n.getFirstChild();
                    if (n2 != null) {
                        nb.setClassName(n2.getNodeValue());
                    }
                }
                for (Element n4: reader.getChildElements("builder.names", "singular")) {
                    String name = reader.getElementValue(n4);
                    NamedNodeMap nm = n4.getAttributes();
                    if (nm != null) {
                        String language = "unknown";
                        org.w3c.dom.Node n2 = nm.getNamedItem("xml:lang");
                        if (n2 != null) {
                            language = n2.getNodeValue();
                        }
                        nb.setSingularName(language, name);
                    }
                }
                for (Element n4: reader.getChildElements("builder.names", "plural")) {
                    String name = reader.getElementValue(n4);
                    NamedNodeMap nm = n4.getAttributes();
                    if (nm != null) {
                        String language = "unknown";
                        org.w3c.dom.Node n2 = nm.getNamedItem("xml:lang");
                        if (n2 != null) {
                            language = n2.getNodeValue();
                        }
                        nb.setPluralName(language, name);
                    }
                }
                for (Element n4: reader.getChildElements("builder.descriptions", "description")) {
                    String description = reader.getElementValue(n4);
                    NamedNodeMap nm = n4.getAttributes();
                    if (nm != null) {
                        String language = "unknown";
                        org.w3c.dom.Node n2 = nm.getNamedItem("xml:lang");
                        if (n2 != null) {
                            language = n2.getNodeValue();
                        }
                        nb.setDescription(language, description);
                    }
                }
                for (Element n4: reader.getChildElements("builder.fieldlist", "field")) {
                    decodeField(nb, n4);
                }
            }
        }
    }

    private void decodeField(NeededBuilder nb, Element n) {
        NeededBuilderField nbf = new NeededBuilderField();
        org.w3c.dom.NodeList l = n.getElementsByTagName("description");
        for (int i = 0; i < l.getLength(); i++) {
            org.w3c.dom.Node n2 = l.item(i);
            NamedNodeMap nm = n2.getAttributes();
            if (nm != null) {
                org.w3c.dom.Node n3 = nm.getNamedItem("xml:lang");
                if (n3 != null) {
                    nbf.setDescription(n3.getNodeValue(), n2.getFirstChild().getNodeValue());
                }
            }
        }
        l = n.getElementsByTagName("guiname");
        for (int i = 0; i < l.getLength(); i++) {
            org.w3c.dom.Node n2 = l.item(i);
            NamedNodeMap nm = n2.getAttributes();
            if (nm != null) {
                org.w3c.dom.Node n3 = nm.getNamedItem("xml:lang");
                if (n3 != null) {
                    nbf.setGuiName(n3.getNodeValue(), n2.getFirstChild().getNodeValue());
                }
            }
        }
        l = n.getElementsByTagName("guitype");
        if (l.getLength() > 0) {
            org.w3c.dom.Node n2 = l.item(0);
            if (n2 != null) {
                nbf.setGuiType(n2.getFirstChild().getNodeValue());
            }
        }
        l = n.getElementsByTagName("input");
        if (l.getLength() > 0) {
            org.w3c.dom.Node n2 = l.item(0);
            if (n2 != null) {
                try {
                    nbf.setEditorInputPos(Integer.parseInt(n2.getFirstChild().getNodeValue()));
                }
                catch (Exception e) {
                    log.info("builder.field.editor.input not a number");
                }
            }
        }
        l = n.getElementsByTagName("list");
        if (l.getLength() > 0) {
            org.w3c.dom.Node n2 = l.item(0);
            if (n2 != null) {
                try {
                    nbf.setEditorListPos(Integer.parseInt(n2.getFirstChild().getNodeValue()));
                }
                catch (Exception e) {
                    log.info("builder.field.editor.list not a number");
                }
            }
        }
        l = n.getElementsByTagName("search");
        if (l.getLength() > 0) {
            org.w3c.dom.Node n2 = l.item(0);
            if (n2 != null) {
                try {
                    nbf.setEditorSearchPos(Integer.parseInt(n2.getFirstChild().getNodeValue()));
                }
                catch (Exception e) {
                    log.info("builder.field.editor.search not a number");
                }
            }
        }
        l = n.getElementsByTagName("name");
        if (l.getLength() > 0) {
            org.w3c.dom.Node n2 = l.item(0);
            if (n2 != null) {
                nbf.setDBName(n2.getFirstChild().getNodeValue());
            }
        }
        l = n.getElementsByTagName("type");
        if (l.getLength() > 0) {
            org.w3c.dom.Node n2 = l.item(0);
            if (n2 != null) {
                nbf.setDBType(n2.getFirstChild().getNodeValue());
            }
            NamedNodeMap nm = n2.getAttributes();
            if (nm != null) {
                org.w3c.dom.Node n3 = nm.getNamedItem("state");
                if (n3 != null) {
                    nbf.setDBState(n3.getNodeValue());
                }
                n3 = nm.getNamedItem("size");
                if (n3 != null) {
                    try {
                        nbf.setDBSize(Integer.parseInt(n3.getNodeValue()));
                    }
                    catch (Exception e) {
                        log.info("builder.field.editor.search not a number");
                    }
                }
                n3 = nm.getNamedItem("key");
                if (n3 != null) {
                    if (n3.getNodeValue().equals("true") || n3.getNodeValue().equals("TRUE")) {
                        nbf.setDBKey(true);
                    }
                    else {
                        nbf.setDBKey(false);
                    }
                }
                n3 = nm.getNamedItem("notnull");
                if (n3 != null) {
                    if (n3.getNodeValue().equals("true") || n3.getNodeValue().equals("TRUE")) {
                        nbf.setDBNotNull(true);
                    }
                    else {
                        nbf.setDBNotNull(false);
                    }
                }
            }
        }
        nb.addField(nbf);

    }

    public boolean writeModel() {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        body += "<!DOCTYPE cloudmodel PUBLIC \"-//MMBase/DTD cloudmodel 1.0//EN\" \"https://www.mmbase.org/dtd/cloudmodel_1_0.dtd\">\n";

        body += "<cloudmodel>\n";
        body += "\t<neededbuilderlist>\n";
        Iterator<NeededBuilder> nbl = getNeededBuilders();
        while (nbl.hasNext()) {
            NeededBuilder nb = nbl.next();
            body += "\t\t<builder maintainer=\"" + nb.getMaintainer() + "\" version=\""
                    + nb.getVersion() + "\">" + nb.getName() + "</builder>\n";
            writeBuilder(nb);
        }
        body += "\t</neededbuilderlist>\n\n";

        body += "\t<neededreldeflist>\n";
        Iterator<NeededRelDef> rdl = getNeededRelDefs();
        while (rdl.hasNext()) {
            NeededRelDef nr = rdl.next();
            body += "\t\t<reldef source=\"" + nr.getSource() + "\" target=\"" + nr.getTarget()
                    + "\" direction=\"" + nr.getDirection() + "\" guisourcename=\""
                    + nr.getGuiSourceName() + "\" guitargetname=\"" + nr.getGuiTargetName()
                    + "\" builder=\"" + nr.getBuilderName() + "\" />\n";
        }
        body += "\t</neededreldeflist>\n\n";

        body += "\t<allowedrelationlist>\n";
        Iterator<AllowedRelation> arl = getAllowedRelations();
        while (arl.hasNext()) {
            AllowedRelation ar = arl.next();
            body += "\t\t<relation from=\"" + ar.getFrom() + "\" to=\"" + ar.getTo() + "\" type=\""
                    + ar.getType() + "\" />\n";

        }
        body += "\t</allowedrelationlist>\n\n";

        body += "</cloudmodel>\n";

        // check if the dirs are created, if not create them
        String dirsp = path.substring(0, path.lastIndexOf(File.separator));
        File dirs = new File(dirsp);
        if (!dirs.exists()) {
            dirs.mkdirs();
        }

        // write back to disk
        try {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path),
                    "UTF8"));
            out.write(body);
            out.flush();
            out.close();
        }
        catch (Exception e) {
            log.error(Logging.stackTrace(e));
        }
        return true;
    }

    private boolean writeBuilder(NeededBuilder nb) {
        String builderpath = path.substring(0, path.length() - 4) + "/" + nb.getName() + ".xml";
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        body += "<!DOCTYPE builder PUBLIC \"-//MMBase/DTD builder 1.1//EN\" \"https://www.mmbase.org/dtd/builder_1_1.dtd\">\n";

        body += "<builder name=\"" + nb.getName() + "\" maintainer=\"" + nb.getMaintainer()
                + "\" version=\"" + nb.getVersion() + "\" extends=\"" + nb.getExtends() + "\">\n";
        body += "\t<status>" + nb.getStatus() + "</status>\n";
        if (!nb.getClassName().equals("Dummy")) {
            body += "\t<classfile>" + nb.getClassName() + "</classfile>\n";
        }
        body += "\t<searchage>" + nb.getSearchAge() + "</searchage>\n";
        body += "\t<names>\n";
        body += "\t\t<!-- singles per language as defined by ISO 639 -->\n";
        HashMap<String, String> sn = nb.getSingularNames();
        Iterator<Map.Entry<String, String>> snk = sn.entrySet().iterator();
        while (snk.hasNext()) {
            Map.Entry<String, String> entry = snk.next();
            String key = entry.getKey();
            String value = entry.getValue();
            body += "\t\t<singular xml:lang=\"" + key + "\">" + value + "</singular>\n";
        }
        body += "\t\t<!-- singles per language as defined by ISO 639 -->\n";
        HashMap<String, String> pn = nb.getPluralNames();
        Iterator<Map.Entry<String, String>> pnk = pn.entrySet().iterator();
        while (pnk.hasNext()) {
            Map.Entry<String, String> entry = pnk.next();
            String key = entry.getKey();
            String value = entry.getValue();
            body += "\t\t<plural xml:lang=\"" + key + "\">" + value + "</plural>\n";
        }
        body += "\t</names>\n";
        body += "\t<!-- <descriptions> small description of the builder for human reading -->\n";
        body += "\t<descriptions>\n";
        Map<String, String> de = nb.getDescriptions();
        Iterator<Map.Entry<String, String>> dek = de.entrySet().iterator();
        while (dek.hasNext()) {
            Map.Entry<String, String> entry = dek.next();
            String key = entry.getKey();
            String value = entry.getValue();
            body += "\t\t<description xml:lang=\"" + key + "\">" + value + "</description>\n";
        }
        body += "\t</descriptions>\n";
        body += "\t<fieldlist>\n";
        Iterator<NeededBuilderField> fl = nb.getFields();
        int pos = 3;
        while (fl.hasNext()) {
            NeededBuilderField nbf = fl.next();
            body += "\t\t<!-- POS " + (pos++) + " : <field> '" + nbf.getDBName() + "'  -->\n";
            body += "\t\t<field>\n";
            body += "\t\t\t<descriptions>\n";

            de = nbf.getDescriptions();
            dek = de.entrySet().iterator();
            while (dek.hasNext()) {
                Map.Entry<String, String> entry = dek.next();
                String key = entry.getKey();
                String value = entry.getValue();
                body += "\t\t\t\t<description xml:lang=\"" + key + "\">" + value
                        + "</description>\n";
            }
            body += "\t\t\t</descriptions>\n";
            body += "\t\t\t<gui>\n";
            de = nbf.getGuiNames();
            dek = de.entrySet().iterator();
            while (dek.hasNext()) {
                Map.Entry<String, String> entry = dek.next();
                String key = entry.getKey();
                String value = entry.getValue();
                body += "\t\t\t\t<guiname xml:lang=\"" + key + "\">" + value + "</guiname>\n";
            }
            body += "\t\t\t\t<guitype>" + nbf.getGuiType() + "</guitype>\n";
            body += "\t\t\t</gui>\n";
            body += "\t\t\t<!-- editor related  -->\n";
            body += "\t\t\t<editor>\n";
            body += "\t\t\t\t<positions>\n";
            body += "\t\t\t\t\t<!-- position in the input area of the editor -->\n";
            body += "\t\t\t\t\t<input>" + nbf.getEditorInputPos() + "</input>\n";
            body += "\t\t\t\t\t<!-- position in list area of the editor -->\n";
            body += "\t\t\t\t\t<list>" + nbf.getEditorListPos() + "</list>\n";
            body += "\t\t\t\t\t<!-- position in search area of the editor -->\n";
            body += "\t\t\t\t\t<search>" + nbf.getEditorSearchPos() + "</search>\n";

            body += "\t\t\t\t</positions>\n";
            body += "\t\t\t</editor>\n";
            body += "\t\t\t<!-- database related  -->\n";
            body += "\t\t\t<db>\n";
            body += "\t\t\t\t<!-- name of the field in the database -->\n";
            body += "\t\t\t\t<name>" + nbf.getDBName() + "</name>\n";
            body += "\t\t\t\t<!-- MMBase datatype and demands on it -->\n";
            if (nbf.getDBSize() == -1) {
                body += "\t\t\t\t<type state=\"" + nbf.getDBState() + "\" notnull=\""
                        + nbf.getDBNotNull() + "\" key=\"" + nbf.getDBKey() + "\">"
                        + nbf.getDBType() + "</type>\n";
            }
            else {
                body += "\t\t\t\t<type state=\"" + nbf.getDBState() + "\" size=\""
                        + nbf.getDBSize() + "\" notnull=\"" + nbf.getDBNotNull() + "\" key=\""
                        + nbf.getDBKey() + "\">" + nbf.getDBType() + "</type>\n";
            }
            body += "\t\t\t</db>\n";
            body += "\t\t</field>\n";
        }
        body += "\t</fieldlist>\n";
        body += "</builder>\n";

        // check if the dirs are created, if not create them
        String dirsp = builderpath.substring(0, builderpath.lastIndexOf(File.separator));
        File dirs = new File(dirsp);
        if (!dirs.exists()) {
            dirs.mkdirs();
        }

        // write back to disk
        try {
            Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(builderpath), "UTF8"));
            out.write(body);
            out.flush();
            out.close();
        }
        catch (Exception e) {
            log.error(Logging.stackTrace(e));
        }
        return true;
    }

}
