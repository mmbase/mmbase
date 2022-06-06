/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */

package org.mmbase.applications.packaging;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;


import org.mmbase.applications.packaging.projects.Project;
import org.mmbase.applications.packaging.projects.creators.CreatorInterface;
import org.mmbase.applications.packaging.util.ExtendedDocumentReader;
import org.mmbase.util.xml.EntityResolver;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * project manager, keeps track of projects we are working on, Projects are
 * not a MMBase defined object they are more like projects inside a IDE
 * containers to keep track of the different projects you are working on.
 *
 * @author Daniel Ockeloen
 * @since MMBase-1.7
 */
public class ProjectManager {

    // create a logger for this class
    private static final Logger log = Logging.getLoggerInstance(ProjectManager.class);

    // state of this manager is running or not
    private static boolean state = false;

    // list of all the defined projects (xml file)
    private static Map<String, Project> projects = new HashMap<String, Project>();

    // list of all the defined creators this manager can work with (xml file)
    private static Map<String, CreatorInterface> creators = new HashMap<String, CreatorInterface>();

    // defines needed for the xml readers to find the dtd's
    public static final String DTD_PROJECTS_1_0 = "projects_1_0.dtd";
    public static final String DTD_CREATORS_1_0 = "creators_1_0.dtd";

    public static final String PUBLIC_ID_PROJECTS_1_0 = "-//MMBase//DTD projects config 1.0//EN";
    public static final String PUBLIC_ID_CREATORS_1_0 = "-//MMBase//DTD creators config 1.0//EN";

    static {
        EntityResolver.registerPublicID(PUBLIC_ID_PROJECTS_1_0, "DTD_PROJECTS_1_0", ProjectManager.class);
        EntityResolver.registerPublicID(PUBLIC_ID_CREATORS_1_0, "DTD_CREATORS_1_0", ProjectManager.class);
    }



    /**
    * start this manager, it reads all the defined creators and projects
    */
    public static synchronized void init() {
        if (!isRunning()) {
            readCreators();
            readProjects();
            state=true;
        }
    }

    /**
    * is this manager running ?
    *
    * @return true if running, false if its not
    *
    */
    public static boolean isRunning() {
        return state;
    }



    /**
     * get all the projects we have defined (xml file)
     *
     * @return projects
     */
    public static Iterator<Project> getProjects() {
        return projects.values().iterator();
    }


    /**
    * get a Project based on its name
    *
    * @return Project or null if not found
    */
    public static Project getProject(String name) {
        return projects.get(name);
    }


    /**
    * add a new project to the projectlist (xml file) changes
    * both the memory list and the file on disc.
    *
    * @return true if the add worked, false if something went wrong
    */
    public static boolean addProject(String name,String path) {
        Project pr = new Project(name,path);
        if (pr.save()) {
            projects.put(name,pr);
            save();
        }
        return true;
    }


    /**
    * change project name/path (xml file) changes
    * both the memory list and the file on disc.
    *
    * @return true if the change worked, false if something went wrong
    */
    public static boolean changeProjectSettings(String oldname,String newname,String newpath) {
        Project p = projects.get(oldname);
        if (p != null) {
            projects.remove(oldname);
            addProject(newname,newpath);
            save();
        }
        return true;
    }

    /**
    * delete a project to the projectlist (xml file) changes
    * both the memory list and the file on disc.
    *
    * @return true if the delete worked, false if something went wrong
    */
    public static boolean deleteProject(String name) {
        projects.remove(name);
        save();
        return true;
    }

    /**
    * read the Projects from disc, the file is defined in your
    * config directory. Uses a xml reader and the dtd's found as
    * resources.
    */
    public static void readProjects() {

        // XXX should use ResourceLoader here

        String filename = PackageManager.getConfigPath()+File.separator+"packaging"+File.separator+"projects.xml";
        File file = new File(filename);
        if(file.exists()) {
            ExtendedDocumentReader reader = new ExtendedDocumentReader(filename,ProjectManager.class);
            if(reader!=null) {

                // decode projects
                for (Element n: reader.getChildElements("projects", "project")) {
                    NamedNodeMap nm=n.getAttributes();
                    if (nm!=null) {
                        String name=null;
                        String path=null;

                        // decode name
                        org.w3c.dom.Node n3=nm.getNamedItem("name");
                        if (n3!=null) {
                            name=n3.getNodeValue();
                        }

                        // decode path
                        n3=nm.getNamedItem("path");
                        if (n3!=null) {
                            path=n3.getNodeValue();
                        }

                        if (path!=null && name!=null) {
                            Project p=new Project(name,path);
                            projects.put(name,p);
                        }
                    }
                }
            }
        } else {
            log.error("missing projects file : "+filename);
        }
    }


    /**
    * read the Creators from disc, the file is defined in your
    * config directory. Uses a xml reader and the dtd's found as
    * resources.
    */
    public static void readCreators() {
        creators = new HashMap<String, CreatorInterface>();

        // XXX Should use ResourceLoader here

        String filename = PackageManager.getConfigPath() + File.separator + "packaging" + File.separator + "creators.xml";

        File file = new File(filename);
        if(file.exists()) {
            ExtendedDocumentReader reader = new ExtendedDocumentReader(filename, ProjectManager.class);
            if(reader != null) {
                for (Element n: reader.getChildElements("creators", "creator")) {
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        String type = null;
                        String classname = null;

                        // decode type
                        org.w3c.dom.Node n2 = nm.getNamedItem("type");
                        if (n2 != null) {
                            type = n2.getNodeValue();
                        }

                        // decode the class
                        n2 = nm.getNamedItem("class");
                        if (n2 != null) {
                            classname = n2.getNodeValue();
                        }
                        try {
                            Class newclass = Class.forName(classname);
                            CreatorInterface cr = (CreatorInterface)newclass.newInstance();
                            cr.setType(type);
                            if (cr !=null) creators.put(type,cr);
                        } catch (Exception e) {
                            log.error("Can't create creator : "+classname+" for type "+type);
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            log.error("missing creator file : "+filename);
        }
    }

    /**
    * get all the defined creators we can use (xml file)
    *
    * @return creators
    */
    public static Map<String, CreatorInterface> getCreators() {
        return creators;
    }

    /**
    * get a creator based on its mimetype
    *
    * @return creator or null if non is found for the asked mimetype
    */
    public static CreatorInterface getCreatorByType(String type) {
        Object o = creators.get(type);
        if (o != null) return (CreatorInterface)o;
        return null;
    }


   /**
   * save/sync the projects file to disk.
   *
   * @return true if file saved/synced, false if something went wrong
   */
   public static boolean save() {
       String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
       body += "<!DOCTYPE projects PUBLIC \"-//MMBase/DTD projects config 1.0//EN\" \"https://www.mmbase.org/dtd/projects_1_0.dtd\">\n";
       body += "<projects>\n";
       Iterator<Project> e=projects.values().iterator();
       while (e.hasNext()) {
           Project pr = e.next();
           body += "\t<project name=\""+pr.getName()+"\" path=\""+pr.getPath()+"\" />\n";
       }
       body += "</projects>\n";
       String filename = PackageManager.getConfigPath()+File.separator+"packaging"+File.separator+"projects.xml";
       File sfile = new File(filename);
       try {
            DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
            scan.writeBytes(body);
            scan.flush();
            scan.close();
        } catch(Exception f) {
            log.error(Logging.stackTrace(f));
        }
        return true;
    }
}
