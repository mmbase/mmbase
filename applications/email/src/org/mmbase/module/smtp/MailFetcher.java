/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.module.smtp;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;
import org.mmbase.bridge.*;
import java.util.*;
import java.io.*;
import org.mmbase.applications.crontab.*;
import javax.mail.*;
import javax.mail.search.*;
import javax.mail.event.*;
import javax.mail.internet.*;

/**
 * The abstraction of a class that somehow fetches mail. It is not defined how these fetchers are
 * used. Current implementations create Fetchers using crontab {@link PopFetcher} or are driven by a
 * seperate thread ({@link SMTPFetcher} is instantiated by {@link SMTPListener}), which in turn is
 * bootstrapped by a module {@link SMTPModule}.
 *

 * @version $Id: MailFetcher.java,v 1.3 2007-11-09 18:26:23 michiel Exp $
 */
public abstract class MailFetcher  {
    private static final Logger log = Logging.getLoggerInstance(MailFetcher.class);


    MailFetcher() {
    }

    public MailHandler getHandler() {
        return MailHandler.Factory.getInstance();
    }



}
