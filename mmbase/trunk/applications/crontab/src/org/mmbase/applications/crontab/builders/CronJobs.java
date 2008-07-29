/*
 This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 */
package org.mmbase.applications.crontab.builders;

import org.mmbase.applications.crontab.*;
import org.mmbase.bridge.*;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import java.util.*;

/**
 * Builder that holds cronjobs and listens to changes.
 *  The builder also starts the CronDeamon. on startup the list of cronjobs is loaded into memory.
 *  <b>The builder uses the bridge to get a cloud using class security.</b>
 * @author Kees Jongenburger
 * @version $Id: CronJobs.java,v 1.7 2008-07-29 10:01:21 michiel Exp $
 */
public class CronJobs extends MMObjectBuilder implements Runnable {

    private static Logger log = Logging.getLoggerInstance(CronJobs.class);

    CronDaemon cronDaemon = null;

    public CronJobs() {
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
    }

    /**
     * This thread wait's for MMBase to be started and then adds all the crontEntries to the CronDaemon
     */
    public void run() {
        while (!MMBase.getMMBase().getState()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.warn("thread interrupted, cronjobs will not be loaded");
                return;
            }
        }

        cronDaemon = CronDaemon.getInstance();
        NodeIterator nodeIterator = getCloud().getNodeManager(getTableName()).getList(null, null, null).nodeIterator();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            CronEntry entry = null;
            try {
                entry = new NodeCronEntry(node);
                NodeList servers = node.getRelatedNodes("mmservers");
                if (servers.size() > 0) {
                    String machineName = MMBase.getMMBase().getMachineName();
                    boolean found = false;
                    for (int i=0; i<servers.size(); i++) {
                        Node server = servers.getNode(i);
                        String name = server.getStringValue("name");
                        if (name != null && name.equalsIgnoreCase(machineName)) {
                            log.service("Adding cron entry [" + entry + "] for server [" + name + "]");
                            cronDaemon.add(entry);
                            found = true;
                            break;
                        } else {
                            log.service("Ignoring related server [" + name + "], does not equal servername [" + machineName + "]");
                        }
                    }
                    if (!found) {
                        log.service("NOT Adding cron entry [" + entry + "], not related to server [" + machineName + "]");
                    }
                } else {
                    log.service("Adding cron entry [" + entry + "]");
                    cronDaemon.add(entry);
                }
            } catch (Exception e) {
                log.warn("did not add cronjob with id " + node.getNumber() + " because of error " + e.getMessage());
            }
        }
    }

    /**
     * Inserts a cronjob into the database, and create and adds a  cronEntry to the CronDeamon
     */
    public int insert(String owner, MMObjectNode objectNodenode) {
        int number = super.insert(owner, objectNodenode);
        try {
            if (cronDaemon != null) {
                Node node = getCloud().getNode(number);
                cronDaemon.add(new NodeCronEntry(node));
            }
        } catch (Exception e) {
            throw new RuntimeException("error while creating cron entry with id " + number + " error " + e.getMessage(), e);
        }
        return number;
    }

    /**
     * Commits a cronjob to the database.
     * On commit of a cronjob, the job is first removed from the cronDeamon and a new cronEntry is created and added to the CronDaemon.
     */
    public boolean commit(MMObjectNode node) {
        Set<String> changed = node.getChanged();
        boolean retval = super.commit(node);
        CronEntry entry = cronDaemon.getCronEntry("" + node.getNumber());
        if (entry == null) {
            log.warn("cron entry with ID " + node.getNumber() + " was not found. this usualy means it was invalid");
        } else {
            if (entry instanceof NodeCronEntry) {
                if (changed.contains("classfile") ||
                    changed.contains("name") ||
                    changed.contains("type")) {
                    log.debug("Changed fields " + changed);
                    cronDaemon.remove(entry);
                } else {
                    log.debug("Ignored " + node);
                    return retval;
                }
            } else {
                log.warn("How come, " + entry + " is not a node-entry");
                cronDaemon.remove(entry);
            }
        }
        try {
            log.debug("Replacing cronentry " + entry);
            Node n = getCloud().getNode(node.getNumber());
            cronDaemon.add(new NodeCronEntry(n));
        } catch (Exception e) {
            throw new RuntimeException("error while creating cron entry with id " + node.getNumber() + " error " + e.getMessage());
        }
        return retval;
    }

    /**
     * removes the node from the database and also removes it from the CronDaemon
     */
    public void removeNode(MMObjectNode objectNodenode) {
        String number = "" + objectNodenode.getNumber();
        super.removeNode(objectNodenode);
        CronEntry entry = cronDaemon.getCronEntry(number);
        if (entry != null) {
            cronDaemon.remove(entry);
        }
    }


    private Cloud getCloud() {
        return LocalContext.getCloudContext().getCloud("mmbase");
    }

    /**
     *	sets the default type and crontime (does not work)
     */
    public void setDefaults(MMObjectNode node) {
        super.setDefaults(node);
        node.setValue("type", CronEntry.Type.DEFAULT.ordinal());
        node.setValue("crontime", "*/5 * * * *");
    }

}
