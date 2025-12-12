/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;

import org.mmbase.module.core.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;

/**
 * Virtual Web Master task builder.
 * This builder holds the tasks that need to be performed by the various VWMs (which are registered in
 * the VWMS builder). It also runs a taskscheduler that periodically cehcks whether any new tasks need to be handled<br />
 * Task nodes have a number of fields that define if, when, and how a task is to be performed.
 * These fields are : <br/>
 * Requested Machine: the machine on which the task is to be performed (wantedcpu)<br />
 * VWM: the name of the vwm (as known in the VWMS builder) trhat should perform the task<br />
 * Status : the state of the task (i.e. requesting, claimed, error, done) <br />
 * Claiming Machine : the cpu that claimed the task for performance (claimedcpu)<br />
 * Wanted Time : time at which to start the task<br />
 * Changed Time : time at which the task was last changed<br />
 * Expire Time : time at which the task expires<br />
 * Task : name of the task<br />
 * Id : unknown<br />
 * Data : field for including properties/parameters<br />
 *
 * @application VWMs
 * @rename VwmTasks
 * @author Arjan Houtman
 * @author Pierre van Rooden (javadocs)
 * @version $Id$
 */
public class Vwmtasks extends MMObjectBuilder implements Runnable {
    /**
    * Status value for a task that requests to be performed
    */
    public static final int STATUS_REQUEST = 1;
    /**
    * Status value for a task that is being performed
    */
    public static final int STATUS_CLAIMED = 2;
    /**
    * Status value for a task that was succesfully performed
    */
    public static final int STATUS_DONE = 3;
    /**
    * Status value for a task that timed out
    */
    public static final int STATUS_TIMEOUT = 4;
    /**
    * Status value for a task that failed to be performed
    */
    public static final int STATUS_ERROR = 5;

    private static final Logger log = Logging.getLoggerInstance(Vwmtasks.class);

    /**
    * The interval (in seconds) between checks for new nodes by the task scheduler.
    */
    public static final int SLEEP_TIME= 37;

    // unused
    public boolean replaceCache=true;

    /**
     * Thread in which the task scheduler runms
     */
    Thread kicker;

    /**
     * Reference to the VWMs builder
     */
    Vwms vwms;

    // unused
    Hashtable vwm_cache = new Hashtable ();

    /**
     * Time (in seconds since 1/1/1970) that the builder last checked for new tasknodes to en handled
     */
    int lastchecked=0;

    public boolean init () {
        if (oType != -1) {
            return true;
        } else {
            boolean success = super.init ();
            if (success) {
                start();
            }
            return success;
        }
    }

    /**
     * Starts the thread for the task scheduler
     */
    public void start() {
        /* Start up the main thread */
        if (kicker == null) {
            kicker = new Thread(this,"VwmTasks");
            kicker.setDaemon(true);
            kicker.start();
        }
    }

    /**
     * Stops the thread for the task scheduler
     * Sets the kicker field to null, which causes the run method to terminate.
     */
    public void stop() {
        /* Stop thread */
        kicker.interrupt();
        kicker = null;
    }

    /**
     * VWM maintenance scheduler.
     * Calls the {@link #getVwmTasks} method, after which the thread sleeps
     * for a number of seconds as set in {@link #SLEEP_TIME}.
     */
    public void run() {
        log.info("Thread started, entering while loop");

        while (kicker!=null) {
            log.debug("Periodically sleep " + SLEEP_TIME
                      + " seconds and add all new vwmtasks that were created since last check ("
                      + lastchecked + ").");
            try {Thread.sleep(SLEEP_TIME*1000);} catch (InterruptedException e){return;}
            getVwmTasks();
        }
    }

    /**
     * This method is called just before an actual write to the database is performed.
     * It sets the last changed time to the current time (so the scheduler will select this node).
     * @param node The node to be committed.
     * @return the node to be committed (after changes have been made).
     */
    @Override
    public MMObjectNode preCommit(MMObjectNode node) {
        node.setValue("changedtime",(int)(System.currentTimeMillis()/1000));
        return node;
    }

    /**
     * What should a GUI display for this node.
     * Returns the contents of the task field (the task's description).
     * @param node The node to display
     * @return the display of the node as a <code>String</code>
     */
    @Override
    public String getGUIIndicator (MMObjectNode node) {
        String str = node.getStringValue ("task");
        if (str.length () > 15) {
            return str.substring (0,12) + "...";
        } else {
            return str;
        }
    }

    /**
     * What should a GUI display for this node/field combo.
     * Returns a descriptive text for the status field, or a formatted date struing in case of a time-value.
     * @param node The node to display
     * @param field the name field of the field to display
     * @return the display of the node's field as a <code>String</code>, null if not specified
     */
    @Override
    public String getGUIIndicator (String field, MMObjectNode node) {
        if (field.equals ("status")) {
            int val = node.getIntValue ("status");

            if (val==STATUS_REQUEST) {
                return "verzoek";               // return "request";
            } else if (val==STATUS_CLAIMED) {
                return "onderweg";              // return "claimed";
            } else if (val==STATUS_DONE) {
                return "gedaan";                // return "done";
            } else if (val==STATUS_TIMEOUT) {
                return "timeout";
            } else if (val==STATUS_ERROR) {
                return "error emailed";
            } else {
                return "unknown";
            }
        }
        return null;
    }

    /**
     * Task scheduling routine.
     * This routine selects all tasks that are to be performed.
     * In order to be selected, a task should be new or recently changed, it's status should indicate that it needs a request, and
     * the desired machine to execute the code should match this machine.
     * The tasks are passed to the VWM that is associated with the task.
     */
    protected void getVwmTasks() {
        if (mmb == null) {
            log.warn("No MMBase ");
            return;
        }
        String vwm,task;
        // get out alter ego Vwms Builder to pass the new tasks
        if (vwms == null) {
            vwms = (Vwms)mmb.getMMObject("vwms");
            vwms.init(); // make sure it is initialized
        }
        int checktime = lastchecked;
        lastchecked= (int)(System.currentTimeMillis()/1000);
        //Enumeration e=search("WHERE changedtime>"+checktime+" AND wantedcpu='"+getMachineName()+"' AND status=1");
        if (log.isDebugEnabled()) {
            log.debug("Search vwmtasks  WHERE changedtime>" + checktime
                      + " AND wantedcpu='" + getMachineName() + "'"
                      + " AND " + mmb.getStorageManagerFactory().getStorageIdentifier("status") + "=" + STATUS_REQUEST);
        }
        Enumeration e = search("WHERE changedtime>" + checktime
                               + " AND wantedcpu='" + getMachineName() + "'"
                               + " AND " + mmb.getStorageManagerFactory().getStorageIdentifier("status") + "=" + STATUS_REQUEST);

        for (MMObjectNode node=null; e.hasMoreElements();) {
            node = (MMObjectNode)e.nextElement();
            vwm  = node.getStringValue("vwm");
            task = node.getStringValue("task");
            if (log.isTraceEnabled()) {
                log.trace("Adding " + vwm + " tasknode " + node);
            }
            vwms.putTask(vwm,node);
        }
    }
}
