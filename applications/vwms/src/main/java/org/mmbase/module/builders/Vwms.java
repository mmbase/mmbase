/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;

import org.mmbase.module.*;
import org.mmbase.module.Module;
import org.mmbase.module.builders.vwms.*;
import org.mmbase.module.core.*;
import org.mmbase.util.Mail;
import org.mmbase.util.logging.*;
import org.mmbase.applications.email.SendMail;

/**
 * Virtual WebMasterS (VWMS) are agents within MMBase.
 * To be able to start a VWMS the following things have to be done:<br />
 * - Create a VWMS that does the work<br />
 * - Start the VWMS in the VWMS-Builder.<br />
 * - Make a relation from the Vwm object to a MMserver.       <br />
 * - start the VWMS-Builder if it isn't already running.<br />
 *<br />
 * In the Vwms builder you have to insert the following information:<br />
 * Name: name of the Vwm<br />
 * Machine: machine on which the VwmM is running (wanted_cpu)<br />
 * Maintenance_Time: This is the interval time in which the Vwm is invoked<br />
 * State: inactive means Vwm is off. active means Vwm is on.<br />
 * Description: just a description<br />
 * ClassName: the classname of the actual VWM that is performing the task.<br />
 *
 * Note that currently, vwms are only started during startup.
 *
 * @application VWMs
 * @author Arjan Houtman
 * @author Rico Jansen
 * @author Pierre van Rooden (javadoc)
 * @version $Id$
 */
public class Vwms extends MMObjectBuilder implements MMBaseObserver {

    /**
     * Status value for a VWM that it is inactive
     */
    public static final int STATUS_INACTIVE = 1;
    /**
     * Status value for a VWM that it is active
     */
    public static final int STATUS_ACTIVE = 2;
    /**
     * Status value for a VWM that it is being refreshed (?)
     */
    public static final int STATUS_REFRESH = 3;

    // Logger
    private static Logger log = Logging.getLoggerInstance(Vwms.class.getName());

    /**
     * Cache of VWMs, by name.
     */
    Hashtable<String, VwmInterface> vwm_cache = new Hashtable<String, VwmInterface> ();

    /**
     * Parameter for determining the email domain of the 'sender' when sending error messages.
     */
    private String emailFromDomain;
    /**
     * Parameter for determining the return email address when sending error messages.
     */
    private String emailReturnPath;
    /**
     * Parameter for determining the email-addess of the recipient when sending error messages.
     */
    private String emailTo;

    /**
     * Initializes the vwms builder.
     * Doesn't do anything, which is odd as you would expect it to read the emailXXX properties.
     * This now happens in the {@link #sendMail} method.
     *
     * @return Always true.
     */
    public boolean init () {
        if (oType != -1) {
            return true;
        } else {
            boolean success = super.init ();
            if (success) {
                startVwms();
            }
            return success;
        }
    }

    /**
     * Returns gui information for a field.
     * Returns a descriptive value in the case of the 'status' field.
     * @param node The node to display
     * @param field the name field of the field to display
     * @return the display of the node's field as a <code>String</code>, null if not specified
     */
    @Override
    public String getGUIIndicator (String field, MMObjectNode node) {
      if (field.equals ("status")) {
          int val = node.getIntValue ("status");
          if (val==STATUS_INACTIVE) {
                return "inactive";
            } else if (val==STATUS_ACTIVE) {
                return "active";
            } else if (val==STATUS_REFRESH) {
                return "refresh";
            } else {
                return "unknown";
            }
        }
        return null;
    }

    /**
     * Starts all vwms whose 'wanted_cpu' field indicates they want to be run on this machine (either this particular one or all machines),
     * and that are marked as 'active'.
     * The builder tries to load the class (which should implement the VwmInterface),
     * instantiate a vwms using that class, and initialize it.
     * The vwm is also referred to as a 'bot'.
     * @deprecated Unused. Use startVwms() instead.
     */
    public void startVwmsByField() {
        Class newclass;
        log.debug("Vwms:startVwmsByField -> Vwms on machine "+getMachineName());
        for (Enumeration f=search("WHERE (wantedcpu='"+getMachineName()+"' OR wantedcpu='*') AND status="+STATUS_ACTIVE); f.hasMoreElements();) {
            MMObjectNode node=(MMObjectNode)f.nextElement();
            log.service("Vwms:startVwmsByField -> VWM="+node);
            String name = node.getStringValue("name");
            String classname=node.getStringValue("classname");
            try {
                log.service("Vwms:startVwmsByField -> Trying to create bot : "+name+" classname "+classname);
                newclass=Class.forName(classname);
                log.service("Vwms:startVwmsByField -> Loaded load class : "+newclass);
                VwmInterface vwm = (VwmInterface)newclass.newInstance();
                vwm.init(node,this);
                vwm_cache.put(name,vwm);
            } catch (Exception e) {
                log.error("Vwms:startVwmsByField -> Can't load class : "+name+" : "+e.getMessage());
                log.error(Logging.stackTrace(e));
            }
        }
    }

    /**
     * Starts all vwms whose 'entries are related to the current server (entry in mmservers),
     * and that are marked as 'active'.
     * The builder tries to load the class (which should implement the VwmInterface),
     * instantiate a vwms using that class, and initialize it.
     */
    public void startVwms() {
        Class newclass;
        // try to find my own node
        log.debug("Vwms:startVwms -> Vwms on machine "+getMachineName());
        MMServers bul=(MMServers)mmb.getMMObject("mmservers");
        bul.init(); // make sure mmservers is initialized
        MMObjectNode node=bul.getMMServerNode(getMachineName());
        if (node!=null) {
            for (Enumeration f=mmb.getInsRel().getRelated(node.getIntValue("number"),"vwms"); f.hasMoreElements();) {
                MMObjectNode vwmnode=(MMObjectNode)f.nextElement();
                log.service("Vwms:startVwms -> VWM="+vwmnode);
                String name = vwmnode.getStringValue("name");
                String classname=vwmnode.getStringValue("classname");
                try {
                    log.service("Vwms:startVwms -> Trying to create bot : "+name+" classname "+classname);
                    newclass=Class.forName(classname);
                    log.service("Vwms:startVwms -> Loaded load class : "+newclass);
                    VwmInterface vwm = (VwmInterface)newclass.newInstance();
                    vwm.init(vwmnode,this);
                    vwm_cache.put(name,vwm);
                } catch (Exception err) {
                    log.error("Vwms:startVwms -> Can't load class : "+name+" : "+err.getMessage());
                    log.error(Logging.stackTrace(err));
                }
            }
        }
    }

    /**
     * Passes a task to a vwm.
     * @param vwmname the name of the vwm to pass the task
     * @param node the node to apss as a task
     * @return <code>true</code> if the task was passed, <code>false</code> if the vwm did not exist.
     */
    public boolean putTask(String vwmname, MMObjectNode node) {
        boolean result = false;
        Vwm vwm=(Vwm)vwm_cache.get(vwmname);
        if (vwm!=null) {
            vwm.putTask(node);
            result = true;
        } else {
            log.error("Vwms : Could not find VWM : "+vwmname);
            result = false;
        }
        log.trace("vwmname("+vwmname+"), node("+node+"): result("+result+")");
        return result;
    }

    /**
     * Send mail, using this builder's email settings.
     * Passes its parameters,a s well as the field emailTo to the method that actually sends the mail.
     * The field emailTo can be null when passed the first time, which means that the first call would always fail (?)
     * @param who email address (?) of the sender
     * @param subject subject of the message
     * @param msg the message itself
     * @return <code>true</code> if the mail was send, <code>false</code> otherwise
     */
    public boolean sendMail(String who,String subject, String msg) {
        //using default to mailadres
        return sendMail(who,emailTo,subject,msg);
    }


    /**
     * Send mail, using this builder's email settings.
     * @param who email address (?) of the sender
     * @param to email address of the receiver
     * @param subject subject of the message
     * @param msg the message itself
     * @return <code>true</code> if the mail was send, <code>false</code> otherwise
     */
    public boolean sendMail(String who,String to,String subject, String msg) {

        SendMail sendmail=(SendMail)Module.getModule("sendmail");
        if (sendmail==null) {
            log.warn("sendmail module not active, cannot send email");
            return false;
        }

        // added a kinda weird check so it only checks settings when
        // needed, daniel.

        boolean result = false;
        if (emailTo==null) {
            // get email config and check it
               emailFromDomain = getInitParameter("fromdomain");
            if (emailFromDomain == null || emailFromDomain.equals("")) {
                log.warn(" missing init param from");
            } else if(emailFromDomain.equals("@yourcompany.nl")) {
                log.warn(" fromdomain init parameter is still default, please change!!!!");
            }
            emailReturnPath = getInitParameter("returnpath");
            if (emailReturnPath == null || emailReturnPath.equals("")) {
                log.warn(" missing init param returnpath");
            } else if(emailReturnPath.equals("youremail@yourcompany.nl")) {
                log.warn(" returnpath init parameter is still default, please change!!!!");
            }
            emailTo = getInitParameter("to");
            if (emailTo == null || emailTo.equals("")) {
                log.warn("missing init param subject");
            } else if(emailTo.equals("youremail@yourcompany.nl")) {
                log.warn(" to init parameter is still default, please change!!!!");
            }
        }

        String from="vwm_"+who+emailFromDomain;
        Mail mail=new Mail(to,from);
        mail.setSubject("Mail van VWM : "+who+ " : "+subject);
        mail.setDate();
        mail.setReplyTo(emailReturnPath); // should be from

        if (msg!=null && msg.length()>0) {
            mail.setText(msg);
        } else {
            mail.setText(subject);
        }
        if (sendmail.sendMail(mail)==false) {
            log.error("sending email failed");
            result = false;
        } else {
            log.info("email send");
            result = true;
        }
        log.trace("who("+who+"), to("+to+"), subject("+subject+"), msg.length("+msg.length()+")");
        return result;
    }

    /**
     * Retrieve a currently active vwm by name.
     * @param vwmname the name of the vwm to retrieve
     * @return a VwmInterface object, or null if the vwm does not exist or is not active.
     */
    public VwmInterface getVwm(String vwmname) {
        VwmInterface vwm=vwm_cache.get(vwmname);
        return vwm;
    }

    /**
     * Passes a remote change of a vwms node to the appropriate (active) vwm.
     * @param machine Name of the machine that changed the node.
     * @param number Number of the changed node as a <code>String</code>
     * @param builder type of the changed node
     * @param ctype command type, not very well documented
     * @return always <code>true</code>
     */
    @Override
    public boolean nodeRemoteChanged(String machine, String number, String builder, String ctype) {
        // signal to parent class
        super.nodeRemoteChanged(machine,number,builder,ctype);

        // always return true
        boolean result = true;
        if (ctype.equals("c")) {
            MMObjectNode node=getNode(number);
            if (node!=null) {
                String name=node.getStringValue("name");
                if (name!=null) {
                    VwmInterface vwm=getVwm(name);
                    if (vwm!=null) {
                        log.debug("Signalling vwm("+name+") that builder("+builder+") has a node("+number+") with ctype("+ctype+") from machine("+machine+")");
                        vwm.nodeRemoteChanged(machine,number,builder,ctype);
                    } else
                        log.debug("machine("+machine+"), number("+number+"), builder("+builder+"), ctype("+ctype+"): This vwm("+name+") is not locally installed, skipping..");
                } else
                    log.error("machine("+machine+"), number("+number+"), builder("+builder+"), ctype("+ctype+"): Got a vwmtask with no vwmname("+name+")!");
            } else
                log.error("machine("+machine+"), number("+number+"), builder("+builder+"), ctype("+ctype+"): This nodenumber("+number+") is not found!");
        }
        log.trace("machine("+machine+"), number("+number+"), builder("+builder+"), ctype("+ctype+"): result("+result+")");
        return result;
    }

    /**
     * Passes a local change of a vwms node to the appropriate (active) vwm.
     * @param machine Name of the machine that changed the node.
     * @param number Number of the changed node as a <code>String</code>
     * @param builder type of the changed node
     * @param ctype command type, not very well documented
     * @return always <code>true</code>
     */
    @Override
    public boolean nodeLocalChanged(String machine, String number, String builder, String ctype) {
        super.nodeLocalChanged(machine,number,builder,ctype);

        // always return true
        boolean result = true;
        if (ctype.equals("c")) {
            MMObjectNode node=getNode(number);
            if (node!=null) {
                String name=node.getStringValue("name");
                if (name!=null) {
                    log.debug("Signalling vwm("+name+") that builder("+builder+") has a node("+number+") with ctype("+ctype+") from machine("+machine+")");
                    VwmInterface vwm=getVwm(name);
                    if (vwm!=null) {
                        vwm.nodeLocalChanged(machine,number,builder,ctype);
                    } else
                        log.debug("machine("+machine+"), number("+number+"), builder("+builder+"), ctype("+ctype+"): This vwm("+name+") is not locally installed, skipping..");
                } else
                    log.error("machine("+machine+"), number("+number+"), builder("+builder+"), ctype("+ctype+"): Got a vwmtask with no vwmname("+name+")!");
            } else
                log.error("machine("+machine+"), number("+number+"), builder("+builder+"), ctype("+ctype+"): number("+number+") is not found!");
        }
        log.trace("machine("+machine+"), number("+number+"), builder("+builder+"), ctype("+ctype+"): result("+result+")");
        return result;
    }
}
