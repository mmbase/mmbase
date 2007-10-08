/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
sQuery
*/
package org.mmbase.notifications;
import org.mmbase.bridge.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A notification implementation which sends using mmbase-email.jar
 *
 * @author Michiel Meeuwissen
 * @version $Id: EmailNotification.java,v 1.2 2007-10-08 16:55:17 michiel Exp $
 **/
public  class EmailNotification extends Notification {

    private static final Logger log = Logging.getLoggerInstance(EmailNotification.class);

    public void send(Node recipient, Node notifyable) {
        String address = recipient.getFunctionValue("email", null).toString();
        log.service("Sending notification email to " + address);
        NodeManager emails = recipient.getCloud().getNodeManager("email");
        Node email = emails.createNode();
        email.setStringValue("to", address);
        email.setStringValue("subject", notifyable.getStringValue("title"));
        email.setStringValue("body", notifyable.getStringValue("message"));
        email.commit();
        email.getFunctionValue("startmail", null);

    }


}
