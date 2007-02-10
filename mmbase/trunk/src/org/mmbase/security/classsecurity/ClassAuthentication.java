/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.classsecurity;

import java.util.*;
import java.util.regex.Pattern;
import java.net.URL;
import java.net.URLConnection;

import org.mmbase.security.SecurityException;
import org.mmbase.security.MMBaseCopConfig;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.xml.DocumentReader;
import org.w3c.dom.*;
import org.xml.sax.InputSource;


/**
 * Provides the static utility methods to authenticate by class. Default a file
 * security/&lt;classauthentication.xml&gt; is used for this. If using the wrapper authentication,
 * its configuration file, contains this configuration.
 *
 * @author   Michiel Meeuwissen
 * @version  $Id: ClassAuthentication.java,v 1.16 2007-02-10 16:22:38 nklasens Exp $
 * @see      ClassAuthenticationWrapper
 * @since    MMBase-1.8
 */
public class ClassAuthentication {
    private static final Logger log = Logging.getLoggerInstance(ClassAuthentication.class);

    public static final String PUBLIC_ID_CLASSSECURITY_1_0 = "-//MMBase//DTD classsecurity config 1.0//EN";
    public static final String DTD_CLASSSECURITY_1_0       = "classsecurity_1_0.dtd";
    static {
        XMLEntityResolver.registerPublicID(PUBLIC_ID_CLASSSECURITY_1_0, DTD_CLASSSECURITY_1_0, ClassAuthentication.class);
    }
    private static List<Login> authenticatedClasses = null;


    static ResourceWatcher watcher = null;

    /**
     * Stop watchin the config file, if there is watched one. This is needed when security
     * configuration switched to ClassAuthenticationWrapper (which will not happen very often).
     */

    static void stopWatching() {
        if (watcher != null) {
            watcher.exit();
        }
    }

    private ClassAuthentication() {
        //Static Utility class
    }

    /**
     * Reads the configuration file and instantiates and loads the wrapped Authentication.
     */
    protected static synchronized void load(String configFile) throws SecurityException {
        List<URL> resourceList = MMBaseCopConfig.securityLoader.getResourceList(configFile);
        log.info("Loading " + configFile + "( " + resourceList + ")");
        authenticatedClasses = new ArrayList();
        ListIterator<URL> it = resourceList.listIterator();
        while (it.hasNext()) it.next();
        while (it.hasPrevious()) {
            URL u = it.previous();
            try {
                URLConnection con = u.openConnection();
                if (! con.getDoInput()) continue;
                InputSource in = new InputSource(con.getInputStream());
                Document document = DocumentReader.getDocumentBuilder(true, // validate aggresively, because no further error-handling will be done
                                                                      new XMLErrorHandler(false, 0), // don't log, throw exception if not valid, otherwise big chance on NPE and so on
                                                                      new XMLEntityResolver(true, ClassAuthentication.class) // validate
                                                                      ).parse(in);

                NodeList authenticates = document.getElementsByTagName("authenticate");

                for (int i = 0; i < authenticates.getLength(); i ++) {
                    Node node = authenticates.item(i);
                    String clazz    = node.getAttributes().getNamedItem("class").getNodeValue();
                    String method   = node.getAttributes().getNamedItem("method").getNodeValue();
                    Node property   = node.getFirstChild();
                    Map<String, String> map = new HashMap();
                    while (property != null) {
                        if (property instanceof Element && property.getNodeName().equals("property")) {
                            String name     = property.getAttributes().getNamedItem("name").getNodeValue();
                            String value    = property.getAttributes().getNamedItem("value").getNodeValue();
                            map.put(name, value);
                        }
                        property = property.getNextSibling();
                    }
                    authenticatedClasses.add(new Login(Pattern.compile(clazz), method, Collections.unmodifiableMap(map)));
                }
            } catch (Exception e) {
                log.error(u + " " + e.getMessage(), e);
            }
        }

        { // last fall back, everybody may get the 'anonymous' cloud.
            Map<String, String> map = new HashMap();
            map.put("rank", "anonymous");
            authenticatedClasses.add(new Login(Pattern.compile(".*"), "class", Collections.unmodifiableMap(map)));
        }

        log.service("Class authentication: " + authenticatedClasses);

    }


    /**
     * Checks wether the (indirectly) calling class is authenticated by the
     * ClassAuthenticationWrapper (using a stack trace). This method can be called from an
     * Authentication implementation, e.g. to implement the 'class' application itself (if the
     * authentication implementation does understand the concept itself, then passwords can be
     * avoided in the wrappers' configuration file).
     *
     * @param application Only checks this 'authentication application'. Can be <code>null</code> to
     * check for every application.
     * @return A Login object if yes, <code>null</code> if not.
     */
    public static Login classCheck(String application) {
        if (authenticatedClasses == null) {
            synchronized(ClassAuthentication.class) { // if load is running this locks
                if (authenticatedClasses == null) { // if locked, load was running and this now skips, so load is not called twice.
                    String configFile = "classauthentication.xml";
                    load(configFile);
                    watcher = new ResourceWatcher(MMBaseCopConfig.securityLoader) {
                            public void onChange(String resource) {
                                load(resource);
                            }
                        };
                    watcher.add(configFile);
                    watcher.start();
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.trace("Class authenticating (" + authenticatedClasses + ")");
        }
        Throwable t = new Throwable();
        StackTraceElement[] stack = t.getStackTrace();

        for (Login n : authenticatedClasses) {
            if (application == null || application.equals(n.application)) {
                Pattern p = n.classPattern;
                for (StackTraceElement element : stack) {
                    String className = element.getClassName();
                    if (className.startsWith("org.mmbase.security.")) continue;
                    if (className.startsWith("org.mmbase.bridge.implementation.")) continue;
                    log.trace("Checking " + className);
                    if (p.matcher(className).matches()) {
                        if (log.isDebugEnabled()) {
                            log.debug("" + className + " matches! ->" + n + " " + n.getMap());
                        }
                        return n;
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Failed to authenticate " + Arrays.asList(stack) + " with " + authenticatedClasses);
        }
        return null;
    }

    /**
     * A structure to hold the login information.
     */
    public static class  Login {
        Pattern classPattern;
        String application;
        Map<String, String>    map;
        Login(Pattern p , String a, Map<String, String> m) {
            classPattern = p;
            application = a;
            map = m;
        }

        public Map<String, String> getMap() {
            return map;
        }
        public String toString() {
            return classPattern.pattern() + (application.equals("class") ? "" : ": " + application);
        }
    }

}
