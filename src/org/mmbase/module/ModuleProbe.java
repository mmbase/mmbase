/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package org.mmbase.module;

import java.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * ModuleProbe is a deamon thread that periodically calls the maintenance() method
 * of the modules active in MMBase.
 * The number of milliseconds of the invocation period is approcimately 1 minute.
 *
 * @author Pierre van Rooden
 * @author Daniel Ockeloen
 * @version $Id: ModuleProbe.java,v 1.10 2005-03-16 13:09:19 pierre Exp $
 */
public class ModuleProbe extends Thread {

    private static final Logger log = Logging.getLoggerInstance(ModuleProbe.class);

    // Sleeptime (needs to be configurable?)
    private int invocationPeriod = 60*1000;

    public ModuleProbe() {
        super("ModuleProbe");
        setDaemon(true);
        log.service("Starting ModuleProbe");
    }

    /**
     * Periodically invoke the maintainance method of all active MMBase modules.
     */
    public void run() {
        while (true) {
            // sleep
            try {
                Thread.sleep(invocationPeriod);
            } catch (InterruptedException e){
                return;
            }
            // call each module's maintenance routine
            try {
                Iterator i = Module.getModules();
                while(i != null && i.hasNext()) {
                    Module module = (Module) i.next();
                    try {
                        module.maintainance();
                    } catch (RuntimeException e) {
                        log.error("Exception on maintainance call of " + module.getName() + " : " + e.getMessage());
                    }
                }
            } catch (ConcurrentModificationException cme) {
                log.debug("Module list changed - abort current probe, try again later");
            }
        }
    }
}
