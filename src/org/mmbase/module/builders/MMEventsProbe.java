/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

/**
 * @javadoc
 * @application Tools
 * @version $Id: MMEventsProbe.java,v 1.11 2008-06-12 11:23:11 michiel Exp $
 * @author Daniel Ockeloen
 */
class MMEventsProbe implements Runnable {

    Thread kicker = null;
    MMEvents parent = null;

    public MMEventsProbe(MMEvents parent) {
        this.parent = parent;
        init();
    }

    public void init() {
        this.start();
    }

    /**
     * Starts the admin Thread.
     */
    public void start() {
        if (kicker == null) {
            kicker = org.mmbase.module.core.MMBaseContext.startThread(this, "mmevents");
        }
    }

    /**
     * Stops the admin Thread.
     */
    public void stop() {
        // how is this ever called?

        // iow, why is there no MMObjectBuilder#shutdown method (this thing is started in MMEvents,
        // so it should also be stopped from that, isn't it?)

        // http://www.mmbase.org/jira/browse/MMB-1491
        kicker.interrupt();
        kicker = null;
    }

    /**
     * admin probe, try's to make a call to all the maintainance calls.
     */
    public void run() {
        while (kicker != null) {
            parent.probeCall();
            if (Thread.currentThread().isInterrupted()) break;
        }
    }
}
