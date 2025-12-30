/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase;

import java.io.*;
import java.util.jar.*;
import java.util.regex.*;
import java.net.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * MMBase version reporter. The only goal of this class is providing the current version of
 * MMBase. The function 'get' will return it as one String.
 *
 * @author Daniel Ockeloen
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class Version {

    private static final Logger log = Logging.getLoggerInstance(Version.class);

    private static final Pattern SCM = Pattern.compile("\\$URL: https://scm.mmbase.org/mmbase/(.*)");
    /**
     * Get Version Control tag
     * @return version Control tag
     * @since MMBase-1.9
     */
    public static String getTag() {
        String url = "$URL$";
        Matcher matcher = SCM.matcher(url);
        if (matcher.matches()) {
            String[] group = matcher.group(1).split("/", 3);
            if (group[0].equals("tags") || group[0].equals("branches")) {
                return group[1];
            } else {
                return group[0];
            }
        } else {
            return "trunk?";
        }
    }

    /**
     * Returns the 'name' part of the MMBase version. This will normally be 'MMBase'.
     * @return Name part of version
     * @since MMBase-1.6
     */
    public static String getName() {
        return "MMBase";
    }

    /**
     * Returns the major version number of this MMBase.
     * @return major version number
     * @since MMBase-1.6
     */
    public static int getMajor() {
        return 1;
    }
    /**
     * Returns the minor version number of this MMBase.
     * @return minor version number
     * @since MMBase-1.6
     */
    public static int getMinor() {
        return 9;
    }

    /**
     * Returns the patch level number of this MMBase.
     * @return patch level number
     * @since MMBase-1.6
     */
    public static int getPatchLevel() {
        return 7;
    }

    /**
     * Returns the build date of this MMBase. During the build, the
     * value of this is stored in builddate.properties.
     * @return build date of this MMBase
     *
     * @since MMBase-1.6
     */
    public static String getBuildDate() {
        Manifest man = getManifest();
        if (man != null) {
            return man.getAttributes("org/mmbase").getValue("Build-Date");
        } else {
            return "";
        }
    }

    /**
     * Returns the SubVersion version from which the current jar was built.
     *
     * @since MMBase-1.9.1
     */
    public static String getSCMRevision() {
        Manifest man = getManifest();
        if (man != null) {
            return man.getAttributes("org/mmbase").getValue("SCM-Revision");
        } else {
            return "?";
        }
    }

    /**
     * Returns the version number of this MMBase.
     * @return version number
     * @since MMBase-1.6
     */
    public static String getNumber() {
        return getMajor() + "." + getMinor() + "." + getPatchLevel() + (isRelease() ? "-" + getReleaseStatus() + " "  : ".")  + getBuildDate();
    }

    /**
     * Returns if this is a release version of MMBase. If this is false this MMBase is only a CVS snapshot.
     * @return is a release version
     * @since MMBase-1.6
     */
    public static boolean isRelease() {
        return true;
    };

    /**
     * A String describing the status of this release. Like 'final' or 'rc3'.
     * @return status of this release
     * @since MMBase-1.7
     */
    public static String getReleaseStatus() {
        return "rc2";
    };


    /**
     * Returns the version of this MMBase.
     * @return version of this MMBase
     * @since MMBase-1.6
     */
    public static String get() {
        String tag = getTag();
        if (tag.startsWith("MMBase")) {
            return tag + " " + getBuildDate() + " (r" + getSCMRevision() + ")";
        } else {
            return getName() + " " + getNumber() + " (r" + getSCMRevision() + ")";
        }
    }


    private static final String VERSION_CLASS = "org/mmbase/Version.class";
    private static Manifest manifest;
    private static boolean manifestLoaded = false;
    /**
     * Returns the Manifest of the jar in which this version is contained. Or <code>null</code> if
     * it is not in a jar.
     * @since MMBase-1.9.1
     */
    public static Manifest getManifest() {
        if (! manifestLoaded) {
            try {
                URL url = Version.class.getClassLoader().getResource(VERSION_CLASS);
                String u = url.toString();
                String[] parts = u.split("!", 2);
                if (parts.length == 2) {
                    URL jarUrl = new URL(parts[0] + "!/");
                    JarURLConnection jarConnection = (JarURLConnection)jarUrl.openConnection();
                    manifest = jarConnection.getManifest();
                }
            } catch (IOException ioe) {
                log.warn(ioe);
            }
            manifestLoaded = true;
        }
        return manifest;
    }

    /**
     * Prints the version of this mmbase on stdout.
     * can be usefull on command line:
     * <code>java -jar mmbase.jar<code>
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        System.out.println(get());
    }

    private Version() {
    }

}
