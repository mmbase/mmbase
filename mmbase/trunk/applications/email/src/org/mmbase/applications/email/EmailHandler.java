/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.email;

import java.util.*;
import java.net.*;
import java.io.*;

import org.mmbase.module.core.*;
import org.mmbase.module.*;

import org.mmbase.util.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Base implemenation and support class for all the email handlers
 * normally you extend this class for implementing a new mailtype
 *
 * @author Daniel Ockeloen
 * @version $Id: EmailHandler.java,v 1.3 2003-07-08 17:27:47 michiel Exp $
 */
public class EmailHandler {

    private static Logger log = Logging.getLoggerInstance(EmailHandler.class);

    /**
     * @javadoc
     *
     * Send the email node.
     * firsts finds all out all the related users and groups (or not)
     * then parse the content for subject and body
     * lastly mail it using the sendmail module
     */
    public static MMObjectNode sendMailNode(MMObjectNode node) {
        // get the sendmail module
        SendMailInterface sendmail = (SendMailInterface)Module.getModule("sendmail");
        if (sendmail == null) {
            log.error("sendmail module not active, cannot send email");
            node.commit();
            return node; // STATE_FAILED ?
        }

        // get subject of the mail (including url based)
        String subject = node.getStringValue("subject");
        String osubject = subject;

        // get To of the mail (to field + related users + related users in groups
        List toUsers = getAttachedUsers(node);
        if (log.isDebugEnabled()) {
            log.debug("USERS=" + toUsers);
        }

        // get From of the mail
        String from = node.getStringValue("from");

        // get ReplyTo of the mail
        String replyto = node.getStringValue("replyto");

        // get Body of the mail (including url based)
        String body = node.getStringValue("body");
        String obody = body;

        // loop all the users we need to mail
        Iterator i = toUsers.iterator();
        while (i.hasNext()) {

            // get the next user we need to email
            String to_one = (String)i.next();

            // mmbase number of the user
            String to_number = null;

            // parse the to field ( format mmbasenumber,emailadr )
            int pos = to_one.indexOf(",");
            if (pos != -1) {
                to_number = to_one.substring(0, pos);
                to_one = to_one.substring(pos + 1);
            }

            // if the body starts with a url call that url
            if (obody.indexOf("http://") == 0) {
                body = getUrlExtern(obody, "", to_number);

                // convert html to plain text unless a the html tag is found	
                if (body.indexOf("<html>") == -1 && body.indexOf("<HTML>") == -1) {
                    body = html2plain(body);
                }
            }

            // if the subject starts with a url call that url
            if (osubject.indexOf("http://") == 0) {
                subject = getUrlExtern(osubject, "", to_number);
                subject = stripToOneLine(subject);
            }

            // create new (sendmail) object
            Mail mail = new Mail(to_one, from);

            // set the subject
            mail.setSubject(subject);

            // set default date
            mail.setDate();

            // set the reply header if defined
            if (replyto != null && !replyto.equals(""))
                mail.setReplyTo(replyto);

            // fill the body
            mail.setText(body);

            // little trick if it seems valid html page set
            // the headers for html mail
            if (body.indexOf("<HTML>") != -1 && body.indexOf("</HTML>") != -1) {
                mail.setHeader("Mime-Version", "1.0");
                mail.setHeader("Content-Type", "text/html; charset=\"ISO-8859-1\"");
            }

            // is the don't mail tag set ? this allows
            // a generated body to signal it doesn't
            // want to be mailed since for some reason
            // invalid (for example there is no news for
            // you
            if (body.indexOf("<DONTMAIL>") == -1) {
                // if the subject contains 'fakemail'
                // perform all actions butt don't really
                // mail. This is done for testing
                if (subject != null && subject.indexOf("fakemail") != -1) {
                    // add one to the sendmail counter
                    // refix numberofmailsend++;
                    log.debug("Email -> fake send");
                    node.setValue("mailstatus", EmailBuilder.STATE_DELIVERED);
                } else {
                    if (to_one == null || sendmail.sendMail(mail) == false) {
                        log.debug("Email -> mail failed");
                        node.setValue("mailstatus", EmailBuilder.STATE_FAILED);
                        // add one to the sendmail counter
                        // refix numberofmailsend++;
                    } else {
                        // add one to the sendmail counter
                        // refix numberofmailsend++;
                        log.debug("Email -> mail send");
                        node.setValue("mailstatus", EmailBuilder.STATE_DELIVERED);
                    }
                }
            } else {
                log.debug("Don't mail tag found");
            }
        }
        // set the new mailedtime, that can be used by admins
        // to see when it was mailed vs the requested mail
        // time
        node.setValue("mailedtime", (int) (System.currentTimeMillis() / 1000));

        // commit the changes to the cloud
        node.commit();
        return node;
    }

    /**
     * get the To header if its not set directly
     * try to obtain it from related objects.
     */
    private static List getTo(MMObjectNode node) {
        List toUsers = new ArrayList();
        String to = node.getStringValue("to");
        if (to != null) {
            toUsers.add(to);
        }
        return toUsers;
    }

    /**
     * get the email addresses of related people
     */
    private static List getAttachedUsers(MMObjectNode node) {
        List toList = getTo(node);
        toList = getAttachedUsers(node, toList);

        Vector rels = node.getRelatedNodes("groups");
        if (rels != null) {
            Enumeration enum = rels.elements();
            while (enum.hasMoreElements()) {
                MMObjectNode pnode = (MMObjectNode)enum.nextElement();
                toList = getAttachedUsers(pnode, toList);
            }
        }

        if (toList.size() > 0) {
            return toList;
        } else {
            return null;
        }
    }

    /**
     * get the email addresses of related people
     * @todo can this not return a List?
     */
    private static List getAttachedUsers(MMObjectNode node, List toList) {
        // try and find related users
        Vector rels = node.getRelatedNodes("users");
        if (rels != null) {
            Enumeration enum = rels.elements();
            while (enum.hasMoreElements()) {
                MMObjectNode pnode = (MMObjectNode)enum.nextElement();
                String to = "" + pnode.getNumber() + "," + pnode.getStringValue("email");
                if (!toList.contains(to)) {
                    toList.add(to);
                }
            }
        }
        return toList;
    }

    private static String getUrlExtern(String absoluteUrl, String params, String usernumber) {
        try {
            if (usernumber != null)
                params += "&usernumber=" + usernumber;
            String prefix = "?";
            if (absoluteUrl.indexOf("?") != -1) {
                prefix = "&";
            }
            log.info("debug=" + absoluteUrl + prefix + params);
            URL includeURL = new URL(absoluteUrl + prefix + params);
            HttpURLConnection connection = (HttpURLConnection)includeURL.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            int buffersize = 10240;
            char[] buffer = new char[buffersize];
            StringBuffer string = new StringBuffer();
            int len;
            while ((len = in.read(buffer, 0, buffersize)) != -1) {
                string.append(buffer, 0, len);
            }
            String result = string.toString();
            return (result);

        } catch (Exception e) {
            // this is weird needs to be checked
            //e.printStackTrace();
        }
        return "";
    }

    private static String stripToOneLine(String input) {
        StringBuffer result = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(input, ",\n\r");
        while (tok.hasMoreTokens()) {
            result.append(tok.nextToken());
        }
        return result.toString();
    }

    /**
    * convert 'html' to 'plain' text
    * this removes the br and p tags and converts them
    * to returns and dubble returns for email use.
    */
    private static String html2plain(String input) {
        // define the result string
        StringBuffer result = new StringBuffer();

        // setup a tokenizer on all returns and linefeeds so
        // we can remove them
        StringTokenizer tok = new StringTokenizer(input, "\n\r");
        while (tok.hasMoreTokens()) {
            // add the content part stripped of its return/linefeed
            result.append(tok.nextToken());
        }

        // now use the html br and p tags to insert
        // the wanted returns 
        StringObject obj = new StringObject(result.toString());
        obj.replace("<br/>", "\n");
        obj.replace("<br />", "\n");
        obj.replace("<BR/>", "\n");
        obj.replace("<BR />", "\n");
        obj.replace("<br>", "\n");
        obj.replace("<BR>", "\n");
        obj.replace("<p>", "\n\n");
        obj.replace("<p/>", "\n\n");
        obj.replace("<p />", "\n\n");
        obj.replace("<P>", "\n\n");

        // return the coverted body
        return obj.toString();
    }

}
