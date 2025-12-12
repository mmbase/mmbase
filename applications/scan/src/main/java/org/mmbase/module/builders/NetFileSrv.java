/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;

import org.mmbase.module.builders.vwms.*;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;

/**
 * The NetFileServ builder contains information on services available to NetFile objects.
 * It contains a list of possible service/subservice tasks. These tasks are then attached
 * to entries in the VWMs builder, so it is possible to search for a VWM to handle a
 * service/subservice request.
 * The fields of NetFileServ are:<br />
 * <ul>
 * <li><code>service</code> : the main 'service' to be performed.
 *             Together with subservice, this determines the VWM that handles the transfer,
 *             i.e. 'pages/main' is handled by the {@link PageMaster} VWM.</li>
 * <li><code>subservice</code> : the subservice to perform. i.e. in PageMaster, 'main' determines mirror sites and
 *                schedules tasks for mirroring (by creating net netfile entries), while 'mirror'
 *                performs the actual transfer to a mirror<br />
 *                Often one VWM handles multiple subservices, but this is not a given.</li>
 * <li><code>options</code> : Currently unused (?)</li>
 *</ul>
 *
 * @author Daniel Ockeloen
 * @version $Id$
 */
public class NetFileSrv extends MMObjectBuilder {

    // Logger class
    private static Logger log = Logging.getLoggerInstance(NetFileSrv.class.getName());

    /**
    * Cache of VWMS as they are related to a service.
    */
    Hashtable<String, Object> service2bot=new Hashtable<String, Object>();

    /**
     * What should a GUI display for this node.
     * Returns a description of the service and subservice.
     * @param node The node to display
     * @return the display of the node as a <code>String</code>
     */
    @Override
    public String getGUIIndicator(MMObjectNode node) {
        String str=node.getStringValue("service");
        str+="/"+node.getStringValue("subservice");
        if (str.length()>15) {
            return str.substring(0,12)+"...";
        } else {
            return str;
        }
    }

    /**
     * Handles a change of a netfiles node.
     * Depending on the service/subservice of that node, a VWM (such as {@link PageMaster}) is invoked.
     * This method is called from the Netfiles builder whenever a local or remote node change occurs.
     * @param number Number of the node in the netfiles buidler than contain service request information.
     * @param ctype the type of change on that node ("c" : node was changed)
     * @return <code>true</code>
     */
    public boolean fileChange(String number,String ctype) {
        try {
            log.debug(number+" "+ctype);
            MMObjectNode node=getNode(number);
            String service=node.getStringValue("service");
            String subservice=node.getStringValue("subservice");
            String servicestr=service+"/"+subservice;
            Object bot=service2bot.get(servicestr);
            if (bot!=null) {
                if (bot instanceof VwmServiceInterface){
                     ((VwmServiceInterface)bot).fileChange(number,ctype);
                } else {
                    log.warn("Bot for "+servicestr+" is not a VwmServiceInterface.");
                }
            } else {
                // figure out the bot for this service/subservice
                bot=getAttachedBot(service,subservice);
                if (bot!=null) {
                    service2bot.put(servicestr,bot);
                    if (bot instanceof VwmServiceInterface) {
                        ((VwmServiceInterface)bot).fileChange(number,ctype);
                    } else {
                        log.warn("Bot for "+servicestr+" is not a VwmServiceInterface.");
                    }
                }
            }
        } catch (Exception e) {
            log.error("FileChange Exception : "+e);
            log.error(Logging.stackTrace(e));
        }
        return true;
    }


    /**
     * Handles a service request on a file.
     * Depending on the service/subservice, a VWM (such as {@link PageMaster}) is invoked.
     * This method is called from the {@link org.mmbase.module.scancache} module to handle page caching.
     * The VWM invoked is responsible for creating NetFile entries (if needed).
     * @param service the service to be performed
     * @param subservice the subservice to be performed
     * @param filename the filename to service
     * @return <code>true</code> if maintenance was performed, <code>false</code> otherwise
     */
    public boolean fileChange(String service,String subservice,String filename) {
        String servicestr=service+"/"+subservice;
        Object bot=service2bot.get(servicestr);
        if (bot!=null) {
            if (bot instanceof VwmServiceInterface) ((VwmServiceInterface)bot).fileChange(service,subservice,filename);
        } else {
            // figure out the bot for this service/subservice
            bot=getAttachedBot(service,subservice);
            if (bot!=null) {
                service2bot.put(servicestr,bot);
                if (bot instanceof VwmServiceInterface) ((VwmServiceInterface)bot).fileChange(service,subservice,filename);
            }
        }
        return true;
    }

    /**
     * Retrieve a vwm (a 'bot') for the service/subservice combination.
     * This is achieved by following the relations of NetFileServ entries to entries in the VWMs builder.
     * Note that while, theoretically, more vwms could be related, only one (the first) is returned.
     * @param service the service to search for
     * @param subservice the subservice to search for
     * @return an object that implements VWMInterface if successful, a dummy object otherwise.
     *         This system is a bit odd as getAttachedBot would ideally have VwmServiceInterface as
     *         a return type. Possibly change this later, though this also means changing the caching system.
     */
    public Object getAttachedBot(String service,String subservice) {
        Enumeration e=search("WHERE service='"+service+"' AND subservice='"+subservice+"'");

        if(!e.hasMoreElements()) {
            log.error("No entry with service="+service+" and subservice="+subservice+" found in netfilesrv");
        }
        while (e.hasMoreElements()) {
            MMObjectNode node=(MMObjectNode)e.nextElement();
            int number=node.getIntValue("number");
            Enumeration f=mmb.getInsRel().getRelated(""+number,"vwms");

            if(!f.hasMoreElements()) {
                log.error("No Vwms related to the service="+service+" and subservice="+subservice+" found in netfilesrv");
            }
            while (f.hasMoreElements()) {
                MMObjectNode vwmnode=(MMObjectNode)f.nextElement();
                Vwms vwms=(Vwms)mmb.getMMObject("vwms");
                if (vwms!=null) {
                    String name=vwmnode.getStringValue("name");
                    VwmServiceInterface vwm=(VwmServiceInterface)vwms.getVwm(name);
                    if (vwm!=null) {
                        return vwm;
                    } else {
                        log.error("Vwms "+name+" not loaded.");
                    }
                } else {
                    log.error("Builder vwms not loaded.");
                }
            }
        }
        return new Object(); // needed to fill a Dummy in the cache.
    }
}
