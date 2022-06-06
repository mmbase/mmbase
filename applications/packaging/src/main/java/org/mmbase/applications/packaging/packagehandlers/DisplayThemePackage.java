/*
 *  This software is OSI Certified Open Source Software.
 *  OSI Certified is a certification mark of the Open Source Initiative.
 *  The license (Mozilla version 1.0) can be read at the MMBase site.
 *  See http://www.MMBase.org/license
 */
package org.mmbase.applications.packaging.packagehandlers;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.mmbase.applications.packaging.PackageManager;
import org.mmbase.applications.packaging.installhandlers.installStep;
import org.mmbase.applications.packaging.util.ExtendedDocumentReader;
import org.mmbase.module.core.MMBaseContext;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.w3c.dom.Element;

/**
 * DisplayThemePackage, Handler for theme packages
 *
 * @author     Daniel Ockeloen (MMBased)
 */
public class DisplayThemePackage extends BasicPackage implements PackageInterface {

    private static Logger log = Logging.getLoggerInstance(DisplayThemePackage.class);


    /**
     *Constructor for the DisplayThemePackage object
     */
    public DisplayThemePackage() { }


    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public boolean install() {
        boolean result = true;
        // needs to be set to false on a error
        try {

            // step1
            installStep step = getNextInstallStep();
            step.setUserFeedBack("display/theme installer started");
            setProgressBar(1000);
            // lets have 100 steps;

            // step 2
            step = getNextInstallStep();
            step.setUserFeedBack("receiving package ..");
            JarFile jf = getJarFile();
            if (jf != null) {
                step.setUserFeedBack("receiving package ... done (" + jf + ")");

                increaseProgressBar(100);
                // downloading is 10%

                // step 3
                step = getNextInstallStep();
                step.setUserFeedBack("checking dependencies ..");
                if (dependsInstalled(jf, step)) {

                    increaseProgressBar(100);
                    // downloading is 20%

                    step.setUserFeedBack("checking dependencies ... done");

                    // step 4
                    step = getNextInstallStep();
                    step.setUserFeedBack("installing theme ..");
                    installTheme(jf, step);
                    step.setUserFeedBack("installing theme ... done");

                    increaseProgressBar(100);
                    // downloading is 80%

                    // step 5
                    step = getNextInstallStep();
                    step.setUserFeedBack("updating mmbase registry ..");
                    updateRegistryInstalled();
                    increaseProgressBar(100);
                    // downloading is 90%
                    step.setUserFeedBack("updating mmbase registry ... done");
                } else {
                    step.setUserFeedBack("checking dependencies ... failed");
                    setState("failed");
                    result = false;
                }
            } else {
                step.setUserFeedBack("getting the mmp package...failed (server down or removed disk ? )");
                step.setType(installStep.TYPE_ERROR);
                try {
                    Thread.sleep(2000);
                } catch(Exception ee) {}
            }


            // step 6
            step = getNextInstallStep();
            step.setUserFeedBack("display/theme installer ended");
            increaseProgressBar(100);
            // downloading is 100%

        } catch (Exception e) {
            log.error("install crash on : " + this);
            result = false;
        }
        return result;
    }


    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public boolean uninstall() {
        try {

            // step1
            installStep step = getNextInstallStep();
            step.setUserFeedBack("display/theme uninstaller started");

            // step 3
            step = getNextInstallStep();
            step.setUserFeedBack("updating mmbase registry ..");
            updateRegistryUninstalled();
            step.setUserFeedBack("updating mmbase registry ... done");

            // step 4
            step = getNextInstallStep();
            step.setUserFeedBack("display/theme installer ended");

        } catch (Exception e) {
            log.error("install crash on : " + this);
        }
        return true;
    }


    /**
     *  Description of the Method
     *
     * @param  jf    Description of the Parameter
     * @param  step  Description of the Parameter
     * @return       Description of the Return Value
     */
    private boolean installTheme(JarFile jf, installStep step) {
    String themename = null;
        Enumeration e = jf.entries();
        increaseProgressBar(100);
        // downloading is 30%
        while (e.hasMoreElements()) {
            increaseProgressBar(1);
            // downloading is 1%
            ZipEntry zippy = (ZipEntry) e.nextElement();

            // this is just a demo version, the html package people should figure
            // out the real format.
            String name = zippy.getName();

            // only unpack all files except the theme file
            if (name.indexOf("META-INF")==-1) {
            String localfile = MMBaseContext.getHtmlRoot() + File.separator + "mmbase"+File.separator+"thememanager"+File.separator+name;
                if (name.indexOf("theme.xml")!=-1) {
                localfile = PackageManager.getConfigPath() + File.separator + "thememanager" + File.separator + "themes"+  File.separator+name;
            themename = name.substring(0,name.indexOf('/'));
            String dirname = MMBaseContext.getHtmlRoot() + File.separator + "mmbase"+File.separator+"thememanager"+File.separator+"images"+File.separator+themename;
                        File d = new File(dirname);
                        if (!d.exists()) {
                            d.mkdir();
            }
                dirname = PackageManager.getConfigPath() + File.separator + "thememanager" + File.separator + "themes"+  File.separator+themename;
                        d = new File(dirname);
                        if (!d.exists()) {
                            d.mkdir();
            }
            dirname = MMBaseContext.getHtmlRoot() + File.separator + "mmbase"+File.separator+"thememanager"+File.separator+"css"+File.separator+themename;
                        d = new File(dirname);
                        if (!d.exists()) {
                            d.mkdir();
            }
            }


                installStep substep = step.getNextInstallStep();

                // check if its a dir or a file
                if (zippy.isDirectory()) {
                    File d = new File(localfile);
                    if (!d.exists()) {
                        substep.setUserFeedBack("creating dir : " + localfile + ".. ");
                        d.mkdir();
                        substep.setUserFeedBack("creating dir : " + localfile + ".. done");
                    }
                } else {
                    substep.setUserFeedBack("creating file : " + localfile + ".. ");
                    try {
                        InputStream in = jf.getInputStream(zippy);
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(localfile));
                        int val;
                        while ((val = in.read()) != -1) {
                            out.write(val);
                        }
                        out.close();
                        substep.setUserFeedBack("creating file : " + localfile + ".. done");
                    } catch (IOException f) {
                        substep.setUserFeedBack("creating file : " + localfile + ".. failed");
                        f.printStackTrace();
                    }
                }
        }
        }
    if (themename != null) {
                installStep substep = step.getNextInstallStep();
                substep.setUserFeedBack("adding theme to manager ... ");
        if (addThemeName(themename)) {
                        substep.setUserFeedBack("adding theme to manager ... done ");
        } else {
                        substep.setUserFeedBack("adding theme to manager ... failed ");
        }
    }
        increaseProgressBar(100);
        // downloading is 70%
        return true;
    }

    private boolean addThemeName(String themename) {
        String themedir = PackageManager.getConfigPath() + File.separator + "thememanager" + File.separator;
        ExtendedDocumentReader reader = new ExtendedDocumentReader(themedir + "themes.xml", DisplayThemePackage.class);
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        body += "<!DOCTYPE themes PUBLIC \"//MMBase - themes //\" \"https://www.mmbase.org/dtd/themes_1_0.dtd\">\n";
        body += "<themes>\n";
        boolean found = false;
        for (Element n: reader.getChildElements("themes", "theme")) {
            String oldname = n.getAttribute("id");
            String oldfile = n.getAttribute("file");
            body += "\t<theme id=\"" + oldname + "\" file=\"" + oldfile + "\" />\n";
            if (themename.equals(oldname)) {
                found = true;
            }
        }

        if (!found) {
            body += "\t<theme id=\"" + themename + "\" file=\"themes/" + themename + "/theme.xml\" />\n";
            body += "</themes>\n";
            saveFile(themedir + "themes.xml", body);
        }
    return true;
    }


    /**
     *  Description of the Method
     *
     * @param  filename  Description of the Parameter
     * @param  value     Description of the Parameter
     * @return           Description of the Return Value
     */
    static boolean saveFile(String filename, String value) {
        File sfile = new File(filename);
        try {
            DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
            scan.writeBytes(value);
            scan.flush();
            scan.close();
        } catch (Exception e) {
            log.error(Logging.stackTrace(e));
        }
        return true;
    }

}

