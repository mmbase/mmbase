/*
 *  This software is OSI Certified Open Source Software.
 *  OSI Certified is a certification mark of the Open Source Initiative.
 *  The license (Mozilla version 1.0) can be read at the MMBase site.
 *  See http://www.MMBase.org/license
 */
package org.mmbase.applications.packaging.projects;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.mmbase.applications.packaging.ProjectManager;
import org.mmbase.applications.packaging.projects.creators.CreatorInterface;
import org.mmbase.applications.packaging.util.ExtendedDocumentReader;
import org.mmbase.util.xml.EntityResolver;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * @javadoc
 * @author     Daniel Ockeloen
 */
public class Project {

    // logger
    private static Logger log = Logging.getLoggerInstance(Project.class);

    String path;
    String name;
    String basedir;
    HashMap<String, Target> targets = new HashMap<String, Target>();
    HashMap<String, Target> packagetargets = new HashMap<String, Target>();
    HashMap<String, Target> bundletargets = new HashMap<String, Target>();

    /**
     * DTD resource filename of the packaging DTD version 1.0
     */
    public final static String DTD_PACKAGING_1_0 = "packaging_1_0.dtd";

    /**
     * Public ID of the packaging DTD version 1.0
     */
    public final static String PUBLIC_ID_PACKAGING_1_0 = "-//MMBase//DTD packaging config 1.0//EN";

    /**
     * Register the Public Ids for DTDs used by DatabaseReader
     * This method is called by EntityResolver.
     */
    public static void registerPublicIDs() {
        EntityResolver.registerPublicID(PUBLIC_ID_PACKAGING_1_0, "DTD_PACKAGING_1_0", Project.class);
    }

    /**
     *Constructor for the Project object
     *
     * @param  name  Description of the Parameter
     * @param  path  Description of the Parameter
     */
    public Project(String name, String path) {
        this.name = name;
        this.path = path;
        basedir = expandBasedir(".");
        // set basedir to default
        readTargets();
    }

    /**
     *  Sets the name attribute of the Project object
     *
     * @param  name  The new name value
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *  Sets the path attribute of the Project object
     *
     * @param  path  The new path value
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     *  Gets the path attribute of the Project object
     *
     * @return    The path value
     */
    public String getPath() {
        return path;
    }

    public String getDir() {
        if (path!=null) {
            int pos=path.lastIndexOf("/");
            if (pos==-1) pos=path.lastIndexOf("\\");
            if (pos!=-1) return path.substring(0,pos+1);
        }
        return null;
    }

    /**
     *  Gets the name attribute of the Project object
     *
     * @return    The name value
     */
    public String getName() {
        return name;
    }

    /**
     *  Gets the baseDir attribute of the Project object
     *
     * @return    The baseDir value
     */
    public String getBaseDir() {
        return basedir;
    }

    /**
     *  Gets the targets attribute of the Project object
     *
     * @return    The targets value
     */
    public Iterator<Target> getTargets() {
        return targets.values().iterator();
    }

    /**
     *  Description of the Method
     *
     * @param  name  Description of the Parameter
     * @return       Description of the Return Value
     */
    public boolean deleteTarget(String name) {
        // bad bad bad
        targets.remove(name);
        packagetargets.remove(name);
        bundletargets.remove(name);
        save();
        return true;
    }

    /**
     *  Adds a feature to the BundleTarget attribute of the Project object
     *
     * @param  name  The feature to be added to the BundleTarget attribute
     * @param  type  The feature to be added to the BundleTarget attribute
     * @param  path  The feature to be added to the BundleTarget attribute
     * @return       Description of the Return Value
     */
    public boolean addBundleTarget(String name, String type, String path) {
        // several name tricks to make allow for faster creation by people
        if (name.equals("") || name.equals("[auto]")) {
            name = type.substring(type.indexOf("/") + 1);
        }
        if (path.equals("") || path.equals("[auto]")) {
            path = "packaging" + File.separator + getName() + "_" + type.replace('/','_') + ".xml";
            path = path.replace(' ', '_');
        }

        // check if the dirs are created, if not create them
        String dirsp = basedir + path.substring(0, path.lastIndexOf(File.separator));
        File dirs = new File(dirsp);
        if (!dirs.exists()) {
            dirs.mkdirs();
        }

        Target t = new Target(this,name);
        t.setBundle(true);
        t.setBaseDir(basedir);
        t.setPath(path);
        t.setType(type);
        // get the handler
        CreatorInterface cr = ProjectManager.getCreatorByType(type);
        if (cr != null) {
            t.setCreator(cr);
        }
        bundletargets.put(name, t);
        save();
        return true;
    }

    /**
     *  Adds a feature to the PackageTarget attribute of the Project object
     *
     * @param  name  The feature to be added to the PackageTarget attribute
     * @param  type  The feature to be added to the PackageTarget attribute
     * @param  path  The feature to be added to the PackageTarget attribute
     * @return       Description of the Return Value
     */
    public boolean addPackageTarget(String name, String type, String path) {
        CreatorInterface cr = ProjectManager.getCreatorByType(type);
        if (name.equals("") || name.equals("[auto]")) {
        if (cr != null) {
        name = cr.getDefaultTargetName();
        name = checkDubName(name);
        } else {
                name = type.substring(type.indexOf("/") + 1);
        }
        }
        if (path.equals("") || path.equals("[auto]")) {
            path = "packaging" + File.separator + getName() + "_" + type.replace('/','_') + ".xml";
            path = path.replace(' ', '_');
        path = checkDubFilename(path);
        }
        // check if the dirs are created, if not create them
        String dirsp = basedir + path.substring(0, path.lastIndexOf(File.separator));
        File dirs = new File(dirsp);
        if (!dirs.exists()) {
            dirs.mkdirs();
        }
        Target t = new Target(this,name);
        t.setPath(path);
        t.setBaseDir(basedir);
        t.setType(type);
        if (cr != null) {
            t.setCreator(cr);
            t.setDefaults();
        }
        packagetargets.put(name, t);
        save();
        return true;
    }

    /**
     *  Gets the target attribute of the Project object
     *
     * @param  name  Description of the Parameter
     * @return       The target value
     */
    public Target getTarget(String name) {
        Object o = targets.get(name);
        if (o != null) {
            return (Target) o;
        }
        o = packagetargets.get(name);
        if (o != null) {
            return (Target) o;
        }
        o = bundletargets.get(name);
        if (o != null) {
            return (Target) o;
        }
        return null;
    }

    /**
     *  Gets the target attribute of the Project object
     *
     * @param  name  Description of the Parameter
     * @return       The target value
     */
    public Target getTargetById(String id) {
    // find the target with the same package id
        Iterator<Target> e = packagetargets.values().iterator();
        while (e.hasNext()) {
            Target t = e.next();
            if (t.getId().equals(id)) {
            return t;
            }
        }
        return null;
    }

    /**
     *  Gets the packageTargets attribute of the Project object
     *
     * @return    The packageTargets value
     */
    public Iterator<Target> getPackageTargets() {
        return packagetargets.values().iterator();
    }

    /**
     *  Gets the bundleTargets attribute of the Project object
     *
     * @return    The bundleTargets value
     */
    public Iterator<Target> getBundleTargets() {
        return bundletargets.values().iterator();
    }

    /**
     *  Description of the Method
     */
    public void readTargets() {
        File file = new File(path);
        if (file.exists()) {
            ExtendedDocumentReader reader = new ExtendedDocumentReader(path, Project.class);
            if (reader != null) {

                org.w3c.dom.Node n2 = reader.getElementByPath("packaging");
                if (n2 != null) {
                    NamedNodeMap nm = n2.getAttributes();
                    if (nm != null) {
                        org.w3c.dom.Node n3 = nm.getNamedItem("basedir");
                        if (n3 != null) {
                            basedir = n3.getNodeValue();
                            if (basedir.equals(".")) {
                                basedir = expandBasedir(basedir);
                            }
                        }
                    }
                }
                // decode targets
                for (Element n: reader.getChildElements("packaging", "target")) {
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        String name = null;
                        String depends = null;

                        // decode name
                        org.w3c.dom.Node n3 = nm.getNamedItem("name");
                        if (n3 != null) {
                            name = n3.getNodeValue();
                        }
                        // decode depends
                        n3 = nm.getNamedItem("depends");
                        if (n3 != null) {
                            depends = n3.getNodeValue();
                        }

                        if (name != null) {
                            Target t = new Target(this,name);
                            if (depends != null) {
                                t.setDepends(depends);
                            }
                            targets.put(name, t);
                        }
                    }
                }
                // decode packagetargets
                for (Element n: reader.getChildElements("packaging", "package")) {
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        String name = null;
                        String type = null;
                        String path = null;
                        // decode name
                        org.w3c.dom.Node n3 = nm.getNamedItem("name");
                        if (n3 != null) {
                            name = n3.getNodeValue();
                        }
                        // decode path
                        n3 = nm.getNamedItem("file");
                        if (n3 != null) {
                            path = n3.getNodeValue();
                        }
                        // decode type
                        n3 = nm.getNamedItem("type");
                        if (n3 != null) {
                            type = n3.getNodeValue();
                        }

                        if (name != null) {
                            Target t = new Target(this,name);
                            if (path != null) {
                                t.setBaseDir(basedir);
                                t.setPath(path);
                            }
                            if (type != null) {
                                t.setType(type);
                                // get the handler
                                CreatorInterface cr = ProjectManager.getCreatorByType(type);
                                if (cr != null) {
                                    t.setCreator(cr);
                                }
                            }
                            packagetargets.put(name, t);
                        }
                    }
                }
                // decode bundletargets
                for (Element n: reader.getChildElements("packaging", "bundle")) {
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        String name = null;
                        String type = "bundle/basic";
                        String depends = null;
                        String path = null;

                        // decode name
                        org.w3c.dom.Node n3 = nm.getNamedItem("name");
                        if (n3 != null) {
                            name = n3.getNodeValue();
                        }
                        // decode type
                        n3 = nm.getNamedItem("type");
                        if (n3 != null) {
                            type = n3.getNodeValue();
                        }
                        // decode path
                        n3 = nm.getNamedItem("file");
                        if (n3 != null) {
                            path = n3.getNodeValue();
                        }
                        // decode depends
                        n3 = nm.getNamedItem("depends");
                        if (n3 != null) {
                            depends = n3.getNodeValue();
                        }
                        if (name != null) {
                            Target t = new Target(this,name);
                            t.setBundle(true);
                            if (depends != null) {
                                t.setDepends(depends);
                            }
                            if (path != null) {
                                t.setPath(path);
                                t.setBaseDir(basedir);
                            }
                            if (type != null) {
                                t.setType(type);
                                // get the handler
                                CreatorInterface cr = ProjectManager.getCreatorByType(type);
                                if (cr != null) {
                                    t.setCreator(cr);
                                }
                            }
                            bundletargets.put(name, t);
                        }
                    }
                }
            }
        } else {
            log.error("missing projects file : " + path);
        }
    }

    /**
     *  Description of the Method
     *
     * @param  basedir  Description of the Parameter
     * @return          Description of the Return Value
     */
    private String expandBasedir(String basedir) {
        File basefile = new File(path);
        if (basefile != null) {
            return basefile.getParent() + File.separator;
        }
        return basedir;
    }

    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public boolean save() {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        body += "<!DOCTYPE packaging PUBLIC \"-//MMBase/DTD packaging config 1.0//EN\" \"https://www.mmbase.org/dtd/packaging_1_0.dtd\">\n";
        body += "<packaging basedir=\".\">\n";
        Iterator<Target> e = packagetargets.values().iterator();
        while (e.hasNext()) {
            Target t = e.next();
            body += "\t<package name=\"" + t.getName() + "\" type=\"" + t.getType() + "\" file=\"" + t.getPath() + "\" />\n";
        }
        e = bundletargets.values().iterator();
        while (e.hasNext()) {
            Target t = e.next();
            body += "\t<bundle name=\"" + t.getName() + "\" type=\"" + t.getType() + "\" file=\"" + t.getPath() + "\" />\n";
        }
        body += "</packaging>\n";
        File sfile = new File(path);

        // check if the dirs are created, if not create them
        String dirsp = path.substring(0, path.lastIndexOf(File.separator));
        File dirs = new File(dirsp);
        if (!dirs.exists()) {
            dirs.mkdirs();
        }
        try {
            DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
            scan.writeBytes(body);
            scan.flush();
            scan.close();
        } catch (Exception f) {
            log.error("Can't save packaging file : " + path);
            return false;
        }
        return true;
    }

    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public boolean hasSyntaxErrors() {
        // performs several syntax checks to signal
        // the users in the gui tools on possible problems
        Iterator<Target> e = packagetargets.values().iterator();
        while (e.hasNext()) {
            Target t = e.next();
            if (t.hasSyntaxErrors()) {
                return true;
            }
        }
        e = bundletargets.values().iterator();
        while (e.hasNext()) {
            Target t = e.next();
            if (t.hasSyntaxErrors()) {
                return true;
            }
        }
        return false;
    }

    private String checkDubName(String name) {
        boolean dub =  true;
        int counter = 2;
        String newname =  name;
        while (dub) {
            Target t =  getTarget(newname);
            if (t != null) {
                newname = name + (counter++);
            } else {
                dub = false;
            }
        }
        return newname;
    }


    private String checkDubFilename(String filename) {
        boolean dub =  true;
        int counter = 2;
        String newfilename =  filename;
        while (dub) {
            File t=new File(basedir+File.separator+newfilename);
            if (t.exists()) {
                newfilename = filename.substring(0,filename.length()-4) + (counter++)+".xml";
            } else {
                dub = false;
            }
        }
        return newfilename;
    }

}

