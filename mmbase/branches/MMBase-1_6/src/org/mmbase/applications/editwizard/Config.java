/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.editwizard;

import java.util.*;
import java.io.File;
import org.mmbase.util.xml.URIResolver;
import org.mmbase.applications.editwizard.SecurityException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mmbase.bridge.Cloud;
import org.mmbase.util.logging.*;
/**
 * This struct contains configuration information for the jsps. This
 * thing is put in the session. A subclass 'Configurator' can be used
 * to fill this struct.
 *
 * @author  Michiel Meeuwissen
 * @since   MMBase-1.6
 * @version $Id: Config.java,v 1.26.2.4 2003-05-01 17:28:08 vpro Exp $
 */

public class Config {

    // logging
    private static Logger log = Logging.getLoggerInstance(Config.class.getName());

    // protocol string to test referrer pages
    private final static String PROTOCOL = "http://";

    public String      sessionKey        = null;
    public URIResolver uriResolver       = null;
    public int         maxupload = 4 * 1024 * 1024; // 1 MByte max uploadsize
    public Stack       subObjects = new Stack(); // stores the Lists and Wizards.
    public String      sessionId;   // necessary if client doesn't accept cookies to store sessionid (this is appended to urls)
    public String      backPage;
    public String      templates;
    public String      language;

    static public abstract class SubConfig {
        public boolean debug = false;
        public String wizard;
        public String page;
        public Map   popups     = new HashMap(); // all popups now in use below this (key -> Config)
        
        public Map    attributes = new HashMap();

        public void setAttribute(String name, String value) {
            if (value!=null) {
                log.debug("storing "+name+" :"+value);
                attributes.put(name,value);
            }
        }
    }

    static public class WizardConfig extends SubConfig {
        public Wizard wiz;
        public String objectNumber;
        public String parentFid;
        public String parentDid;
        public String popupId;
    }
    static public class ListConfig extends SubConfig {
        public String title;
        public File   template;
        public String fields;
        public String startNodes;
        public String nodePath;
        public String constraints;
        public String orderBy;
        public String directions;
        public String searchDir;

        public String searchFields;
        public String searchValue="";
        public String searchType="like";
        public String baseConstraints;

        public int    age = -1;
        public int    start = 0;
        public boolean distinct;
        public int pagelength   = 50;
        public int maxpagecount = 10;
    }

    /**
     * To fill the Config struct, this 'Configurator' exists. You
     * could extend it to change wich query parameters must be used,
     * and what are the defaults and so on.
     */
    public abstract static class Configurator {
        private static Logger log = Logging.getLoggerInstance(Config.class.getName());

        protected HttpServletRequest request;
        protected HttpServletResponse response;
        private Config config;

        public HttpServletRequest getRequest() {
            return request;
        }

        public Configurator(HttpServletRequest req, HttpServletResponse res, Config c) throws WizardException {
            request = req;
            response = res;
            config  = c;

            config.sessionId = res.encodeURL("test.jsp").substring(8);
            log.debug("Sessionid : " + config.sessionId);

            

            if (config.language == null) {
                config.language = getParam("language", org.mmbase.bridge.LocalContext.getCloudContext().getDefaultLocale().getLanguage());
            }
            // The editwizard need to know the 'backpage' (for 'index' and 'logout' links).
            // It can be specified by a 'referrer' parameter. If this is missing the
            // 'Referer' http header is tried.
            if (config.backPage == null) {
                log.debug("No backpage. Getting from parameters");
                config.backPage = org.mmbase.util.Encode.decode("ESCAPE_URL_PARAM", getParam("referrer", "")).replace('\\', '/'); // this translations seems to be needed by some windows setups

                if (config.backPage.equals("")) {
                    log.debug("No backpage getting from header");
                    config.backPage = request.getHeader("Referer");
                }
                if (config.backPage == null) {
                    log.debug("No backpage setting to ''");
                    config.backPage = "";
                }
            }

            // if no 'uriResolver' is configured yet, then there is one created right now:
            // the uriResolver is used to find xml's and xsl's.
            if (config.uriResolver == null) {
                if (log.isDebugEnabled()) {
                    log.trace("creating uriresolver (backpage = " + config.backPage + ")");
                }
                URIResolver.EntryList extraDirs = new URIResolver.EntryList();

                /* Determin the 'referring' page, and add its directory to the URIResolver.
                   That means that xml can be placed relative to this page, and xsl's int xsl-dir.
                 */
                File refFile;
                // capture direct reference of http:// and shttp:// referers
                int protocolPos= config.backPage.indexOf(PROTOCOL);

                if (protocolPos >=0 ) { // given absolutely
                    String path =  config.backPage.substring(config.backPage.indexOf('/', protocolPos + PROTOCOL.length()));
                    // Using URL.getPath() would be nicer, but is not available in java 1.2
                    // suppose it is from the same server, web can find back the directory then:
                    refFile = new File(request.getRealPath(path.substring(request.getContextPath().length()))).getParentFile();

                    // TODO: What if it happened to be not from the same server?
                } else {
                    // Was given relatively, that's easy:
                    refFile = new File(request.getRealPath(config.backPage)).getParentFile();
                }
                if (refFile.exists()) {
                    extraDirs.add("ref:", refFile);
                }

                /* Optionally, you can indicate with a 'templates' option where the xml's and 
                   xsl must be searched (if they cannot be found in the referring dir).
                */
                config.templates = request.getParameter("templates");

                if (config.templates != null) {
                    File templatesDir = new File(request.getRealPath(config.templates));
                    try {
                        templatesDir = templatesDir.getCanonicalFile();
                    } catch (java.io.IOException e) {
                        throw new WizardException(e.toString());
                    }
                    if(! templatesDir.isDirectory()) {
                        throw new WizardException("Template directory not found : " + templatesDir);
                    }
                    extraDirs.add("templates:", templatesDir);
                }

                /**
                 * Then of course also the directory of editwizard installation must be added. This will allow for the 'basic' xsl's to be found,
                 * and also for 'library' editors.
                 */

                File jspFileDir = new File(request.getRealPath(request.getServletPath())).getParentFile(); // the directory of this jsp (list, wizard)
                File basedir    = new java.io.File(jspFileDir.getParentFile().getAbsolutePath(), "data"); // ew default data/xsls is in ../data then

                if (! config.language.equals("")) {
                    File i18n = new File(basedir, "i18n" + File.separator + config.language);
                    if (i18n.isDirectory()) {
                        extraDirs.add("i18n:", i18n);
                    } else {
                        if (! "en".equals(config.language)) { // english is default anyway
                            log.warn("Tried to internationalize the editwizard for language " + config.language + " for which support is lacking (" + i18n + " is not an existing directory)");
                        }
                    }
                }

                extraDirs.add("ew:", basedir);
                config.uriResolver = new URIResolver(jspFileDir, extraDirs);
            }
        }
        protected String getParam(String paramName) {
            if (request.getParameter(paramName) == null) return null;
            return request.getParameter(paramName);
        }

        protected String getParam(String paramName, String def) {
            if (request.getParameter(paramName) == null) {
                if (def == null) return null;
                return def.toString();
            }
            return getParam(paramName);
        }
        protected int  getParam(String paramName, int def) {
            String i = request.getParameter(paramName);
            if (i == null || i.equals("")) return def;
            return new Integer(i).intValue();
        }

        protected Integer getParam(String paramName, Integer def) {
            String i = request.getParameter(paramName);
            if (i == null || i.equals("")) return def;
            return new Integer(i);
        }

        protected boolean getParam(String paramName, boolean def) {
            if (request.getParameter(paramName) == null) return def;
            return new Boolean(request.getParameter(paramName)).booleanValue();
        }

        protected Boolean getParam(String paramName, Boolean def) {
            if (request.getParameter(paramName) == null) return def;
            return new Boolean(request.getParameter(paramName));
        }
        public String getBackPage(){
            if(config.subObjects.size() == 0) {
                return config.backPage;
            } else {
                return ((SubConfig) config.subObjects.peek()).page;
            }
        }

        public  ListConfig createList() {
            ListConfig l = new ListConfig();
            l.page = response.encodeURL(request.getServletPath() + "?proceed=yes");
            return l;
        }
        public Config.WizardConfig createWizard(Cloud cloud) throws SecurityException, WizardException {
            WizardConfig wizard = new WizardConfig();
            wizard.page = response.encodeURL(request.getServletPath() + "?proceed=yes");
            config(wizard); // determine the objectnumber and assign the wizard name.
            // wizard should now have a name!
            if (wizard.wizard == null) throw new WizardException("Wizardname may not be null, conigurated by class with name: " + this.getClass().getName());
            wizard.wiz = new Wizard(request.getContextPath(), config.uriResolver, wizard, cloud);
            wizard.wiz.setSessionId(config.sessionId);
            wizard.wiz.setSessionKey(config.sessionKey);
            wizard.wiz.setReferrer(config.backPage);
            wizard.wiz.setTemplatesDir(config.templates);
            return wizard;
        }
        public abstract void config(Config.ListConfig c);
        public abstract void config(Config.WizardConfig c);

    }

}
