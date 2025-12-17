/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.implementation;
import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.Module;
import org.mmbase.module.core.*;
import org.mmbase.security.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This is the base class for {@link org.mmbase.bridge.LocalContext} (which is probably its only
 * descendant). Some of the (static) members are package, and hence are easily accessible by other
 * implementors of the 'local' cloud in the current package org.mmbase.bridge.implementation.
 *
 * @author Rob Vermeulen
 * @author Pierre van Rooden
 * @version $Id$
 */
public abstract class BasicCloudContext implements CloudContext {
    private static final Logger log = Logging.getLoggerInstance(BasicCloudContext.class);

    /**
    * Link to the mmbase root
    */
    static MMBase mmb = null;

    /**
    * Transaction Manager to keep track of transactions
    */
    static TransactionManager transactionManager = null;

    /**
     * @javadoc
     * Temporary Node Manager for storing node during edits
     */
    static TemporaryNodeManager tmpObjectManager = null;

    // map of clouds by name
    private static final Set<String> localClouds = new HashSet<String>();

    // map of modules by name
    private static final Map<String, Module> localModules = new HashMap<String, Module>();

    /**
     *  constructor to call from the MMBase class
     *  (protected, so cannot be reached from a script)
     */
    protected BasicCloudContext() {
    }

    /**
     * @throws NotFoundException If mmbase not running and cannot be started because mmbase.config missing
     * @throws BridgeException   If mmbase not running and cannot be started (but mmbase.config was specified)
     */
    protected boolean check() {
        if(mmb == null) {
            synchronized(org.mmbase.module.core.MMBase.class) { // this is the object which MMBase.getMMBase is synchronizing too
                // obtained lock
                if (mmb == null) { // if run in the mean time by other thread, then skip
                    Iterator<org.mmbase.module.Module> i = org.mmbase.module.Module.getModules();
                    // check if MMBase is already running
                    if (i == null) {
                        // build the error message, since it has very litle overhead (only entered once incase of startup)
                        // MMBase may only be started from the bridge when the property mmbase.config was provided
                        if (java.lang.System.getProperty("mmbase.config") == null) {
                            // when mmbase.config is empty fill it with current working dir + /config
                            // this way there is no need to provide the info on the commandline
                            // java.lang.System.setProperty("mmbase.config", java.lang.System.getProperty("user.dir") + java.io.File.separatorChar + "config");
                            throw new NotFoundException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + " : no property mmbase.config found)");
                        }
                        // when MMBase is not running, try to start it!
                        try {
                            // init the MMBaseContext,...
                            org.mmbase.module.core.MMBaseContext.init();
                            // try to start MMBase now,...
                            org.mmbase.module.core.MMBase.getMMBase();
                            // now re-assign the values agina
                            i = org.mmbase.module.Module.getModules();
                        } catch(java.lang.Exception ex) {
                            log.error("Error while trying to start MMBase from the bridge: " + ex.getMessage(), ex);
                        }
                        // if still null,.. give error!
                        if(i == null) {
                            return false;
                        }
                    }
                    // get the core module!
                    MMBase m = org.mmbase.module.core.MMBase.getMMBase();
                    // create module list
                    while(i.hasNext()) {
                        Module mod = ModuleHandler.getModule(i.next(), this);
                        localModules.put(mod.getName(), mod);
                    }

                    transactionManager = TransactionManager.getInstance();
                    tmpObjectManager = transactionManager.getTemporaryNodeManager();

                    // set all the names of all accessable clouds..
                    localClouds.add("mmbase");

                    mmb = m;
                }
            }
        }
        return true;
    }

    public ModuleList getModules() {
        if (!check()) throw new BridgeException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + ")");
        ModuleList ml = new BasicModuleList(localModules.values());
        return ml;
    }

    public Module getModule(String moduleName) throws NotFoundException {
        if (!check()) throw new BridgeException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + ")");
        Module mod = localModules.get(moduleName);
        if (mod == null) {
            throw new NotFoundException("Module '" + moduleName + "' does not exist.");
        }
        return mod;
    }

    public boolean hasModule(String moduleName) {
        if (!check()) throw new BridgeException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + ")");
        return localModules.get(moduleName) != null;
    }


    protected void checkExists(String cloudName) throws NotFoundException  {
        if (!check()) throw new BridgeException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + ")");
        if ( !localClouds.contains(cloudName) ) {
            throw new NotFoundException("Cloud '" + cloudName + "' does not exist.");
        }
        if (mmb == null) {
            throw new NotFoundException("MMBase is not yet initialized (null)");
        }
        if (mmb.isShutdown()) {
            throw new NotFoundException("MMBase is shut down");
        }
        if (! mmb.getState()) {
            throw new NotFoundException("MMBase is not yet initialized: " + mmb.getMMBaseState());
         }
    }
    public Cloud getCloud(String cloudName) {
        checkExists(cloudName);
        return getCloud(cloudName, "anonymous", null);
    }

    public Cloud getCloud(String cloudName, String authenticationType, Map<String, ?> loginInfo) throws NotFoundException  {
        checkExists(cloudName);
        UserContext userContext = mmb.getMMBaseCop().getAuthentication().login(authenticationType, loginInfo, null);
        if (userContext == null) {
            log.debug("Login failed");
            throw new java.lang.SecurityException("Login invalid (login-module: " + authenticationType + "  on " + BasicCloudContext.mmb.getMMBaseCop().getAuthentication());
        }
        // end authentication...

        if (userContext.getAuthenticationType() == null) {
            log.warn("Security implementation did not set 'authentication type' in the user object.");
        }
        return getCloud(cloudName, userContext);
    }

    public Cloud getCloud(String cloudName, UserContext user) throws NotFoundException {
        checkExists(cloudName);
       return new BasicCloud(cloudName, user, this);
    }

    public StringList getCloudNames() {
        if (!check()) throw new BridgeException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + ")");
        return new BasicStringList(localClouds);
    }

    /**
     * @return String describing the encoding.
     * @since MMBase-1.6
     */
    public String getDefaultCharacterEncoding() {
        if (!check()) throw new BridgeException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + ")");
        return mmb.getEncoding();
    }

    public java.util.Locale getDefaultLocale() {
        if (!check()) throw new BridgeException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + ")");
        return mmb.getLocale();
    }

    public java.util.TimeZone getDefaultTimeZone() {
        if (!check()) throw new BridgeException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + ")");
        return mmb.getTimeZone();
    }

    public FieldList createFieldList() {
        return new BasicFieldList();
    }

    public NodeList createNodeList() {
        return new BasicNodeList();
    }

    public RelationList createRelationList() {
        return new BasicRelationList();
    }

    public NodeManagerList createNodeManagerList() {
        return new BasicNodeManagerList();
    }

    public RelationManagerList createRelationManagerList() {
        return new BasicRelationManagerList();
    }

    public ModuleList createModuleList() {
        return new BasicModuleList();
    }

    public StringList createStringList() {
        return new BasicStringList();
    }

    public AuthenticationData getAuthentication() throws NotFoundException {
        if (!check()) throw new BridgeException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + ")");
        // checkExists(cloudName);
        MMBaseCop cop = mmb.getMMBaseCop();
        if (cop == null) {
            throw new NotFoundException("MMBase not yet initialized");
        } else {
            return cop.getAuthentication();
        }
    }

    public ActionRepository getActionRepository() throws NotFoundException {
        if (!check()) throw new BridgeException("MMBase has not been started, and cannot be started by this Class. (" + getClass().getName() + ")");
        // checkExists(cloudName);
        MMBaseCop cop = mmb.getMMBaseCop();
        if (cop == null) {
            throw new NotFoundException("MMBase not yet initialized");
        } else {
            return cop.getActionRepository();
        }
    }


    public boolean isUp() {
        return mmb != null && mmb.getState() && check();
    }

    public CloudContext assertUp() {
        // TODO implement with some nice notify-mechanism.
        CloudContext ctx = LocalContext.getCloudContext();
        while (!MMBaseContext.isInitialized() || ! isUp()) {

            if (mmb != null && mmb.isShutdown()) break;
            try {
                if (Thread.currentThread().isInterrupted()) {
                    return this;
                }
                try {
                    check();
                    log.debug("Sleeping 10 secs");
                } catch (NotFoundException nfe) {
                    log.service(nfe.getMessage() + ". Waiting 10 secs.");
                }
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                return this;
            }
        }
        return this;
    }


}
