/*
 *  This software is OSI Certified Open Source Software.
 *  OSI Certified is a certification mark of the Open Source Initiative.
 *  The license (Mozilla version 1.0) can be read at the MMBase site.
 *  See http://www.MMBase.org/license
 */
package org.mmbase.applications.packaging.projects.creators;

import org.mmbase.bridge.*;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.*;
import org.mmbase.module.builders.Versions;
import org.mmbase.applications.packaging.*;
import org.mmbase.applications.packaging.projects.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

import org.w3c.dom.*;

/**
 * DisplayThemePackage, Handler for theme packages
 *
 * @author     Daniel Ockeloen (MMBased)
 * @created    July 20, 2004
 */
public class DisplayThemeCreator extends BasicCreator implements CreatorInterface {

    private static Logger log = Logging.getLoggerInstance(DisplayThemeCreator.class);

    /**
     *  Description of the Field
     */
    public final static String DTD_PACKAGING_DISPLAY_THEME_1_0 = "packaging_display_theme_1_0.dtd";
    /**
     *  Description of the Field
     */
    public final static String PUBLIC_ID_PACKAGING_DISPLAY_THEME_1_0 = "-//MMBase//DTD packaging_display_theme config 1.0//EN";


    /**
     *  Description of the Method
     */
    public static void registerPublicIDs() {
        XMLEntityResolver.registerPublicID(PUBLIC_ID_PACKAGING_DISPLAY_THEME_1_0, "DTD_PACKAGING_DISPLAY_THEME_1_0", DisplayThemeCreator.class);
    }


    /**
     *Constructor for the DisplayThemeCreator object
     */
    public DisplayThemeCreator() {
        cl = DisplayThemeCreator.class;
        prefix = "packaging_display_theme";
    }


    /**
     *  Description of the Method
     *
     * @param  target      Description of the Parameter
     * @param  newversion  Description of the Parameter
     * @return             Description of the Return Value
     */
    public boolean createPackage(Target target, int newversion) {

        clearPackageSteps();

        // step1
        packageStep step = getNextPackageStep();
        step.setUserFeedBack("display/theme packager started");

        String basedir = target.getBaseDir() + getItemStringValue(target, "basedir");
        String include = getItemStringValue(target, "include");
        String exclude = getItemStringValue(target, "exclude");

        step = getNextPackageStep();
        step.setUserFeedBack("used basedir : " + basedir);
        step = getNextPackageStep();
        step.setUserFeedBack("used include : " + include);
        step = getNextPackageStep();
        step.setUserFeedBack("used exclude : " + exclude);

        String newfilename = getBuildPath() + getName(target).replace(' ', '_') + "@" + getMaintainer(target) + "_display_theme_" + newversion;
        try {
            JarOutputStream jarfile = new JarOutputStream(new FileOutputStream(newfilename + ".tmp"), new Manifest());

            step = getNextPackageStep();
            step.setUserFeedBack("creating package.xml file...");
            createPackageMetaFile(jarfile, target, newversion);
            step.setUserFeedBack("creating package.xml file...done");
            step = getNextPackageStep();
            step.setUserFeedBack("creating depends.xml file...");
            createDependsMetaFile(jarfile, target);
            step.setUserFeedBack("creating depends.xml file...done");

            int filecount = addFiles(jarfile, basedir, include, exclude, "display", "theme");
            if (filecount == 0) {
                step = getNextPackageStep();
                step.setUserFeedBack("did't add any display files, no files found");
                step.setType(packageStep.TYPE_WARNING);
            }
            jarfile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        step = getNextPackageStep();
        step.setUserFeedBack("display/theme packager ended : " + getErrorCount() + " errors and " + getWarningCount() + " warnings");

        // update the build file to reflect the last build, should only be done if no errors
        if (getErrorCount() == 0) {
            File f1 = new File(newfilename + ".tmp");
            File f2 = new File(newfilename + ".mmp");
            if (f1.renameTo(f2)) {
                updatePackageTime(target, new Date(), newversion);
                target.save();
            }
        }
        return true;
    }


    /**
     *  Description of the Method
     *
     * @param  target  Description of the Parameter
     * @return         Description of the Return Value
     */
    public boolean decodeItems(Target target) {
        super.decodeItems(target);
        decodeStringItem(target, "include");
        decodeStringItem(target, "basedir");
        decodeStringItem(target, "exclude");
        return true;
    }


    /**
     *  Gets the xMLFile attribute of the DisplayThemeCreator object
     *
     * @param  target  Description of the Parameter
     * @return         The xMLFile value
     */
    public String getXMLFile(Target target) {
        String body = getDefaultXMLHeader(target);
        body += getDefaultXMLMetaInfo(target);
        body += "\t<basedir>" + getItemStringValue(target, "basedir") + "</basedir>\n";
        body += "\t<include>" + getItemStringValue(target, "include") + "</include>\n";
        body += "\t<exclude>" + getItemStringValue(target, "exclude") + "</exclude>\n";
        body += getPackageDependsXML(target);
        body += getRelatedPeopleXML("initiators", "initiator", target);
        body += getRelatedPeopleXML("supporters", "supporter", target);
        body += getRelatedPeopleXML("developers", "developer", target);
        body += getRelatedPeopleXML("contacts", "contact", target);
        body += getDefaultXMLFooter(target);
        return body;
    }


    /**
     *  Sets the defaults attribute of the DisplayThemeCreator object
     *
     * @param  target  The new defaults value
     */
    public void setDefaults(Target target) {
        target.setItem("basedir", "thememanager/");
        target.setItem("include", "*");
        target.setItem("exclude", "CVS");
    }

}

