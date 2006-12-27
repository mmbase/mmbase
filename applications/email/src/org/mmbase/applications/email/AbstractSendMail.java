/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.email;

import java.util.*;

import org.mmbase.module.*;
import org.mmbase.util.*;

/**
 * This module provides mail functionality
 *
 * @application Mail
 * @author Michiel Meeuwissen
 */
abstract public class AbstractSendMail extends WatchedReloadableModule implements SendMailInterface {

    /**
     * Send mail without extra headers
     */
    public boolean sendMail(String from, String to, String data) {
        return sendMail(from, to, data, null);
    }

    /**
     * Send mail
     */
    public boolean sendMail(Mail mail) {
        return sendMail(mail.from, mail.to, mail.text, mail.headers);
    }

    /**
     * checks the e-mail address
     */
    public String verify(String name) {
        throw new UnsupportedOperationException("cannot verify e-mail");
    }

    /**
     * gives all the members of a mailinglist
     */
    public List expand(String name) {
        throw new UnsupportedOperationException("cannot expand e-mail");
    }


}
