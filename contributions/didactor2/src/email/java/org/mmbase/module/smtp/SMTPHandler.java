package org.mmbase.module.smtp;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;
import org.mmbase.bridge.*;
import java.util.*;
import java.io.*;
import javax.mail.*;
import javax.mail.internet.*;
import nl.didactor.mail.*;

/**
 * <p>
 * Listener thread, that accepts connection on port 25 (default) and
 * delegates all work to its worker threads. It is a minimum implementation,
 * it only implements commands listed in section 4.5.1 of RFC 2821.
 *</p>
 * <h1>How are multiparts dispatched to mmbase objects?</h2>
 * <p> 
 * If the mimetype of the message itself is not multipart, then the message can simply be stored in
 * a object of the type 'emails'. The mime-type of the mail can be sotred in the mime-type field of
 * the message.
 * </p>
 * <p>
 * If the mimetype of the orignal message is multipart/alternative, then only the 'best' part is
 * stored in the mmbase email node. Currently only text/plain and text/html alternatives are
 * recognized. text/html is supposed to be better.
 * </p>
 * <p>
 * If the mimetype of the orignal message is multipart/mixed, then the INLINE part can be stored
 * in the object. If no disposition given on a part, then it is considered INLINE if text/*. If the
 * disposition if ATTACHMENT, then those are stored as related attachment-objects.
 * </p>
 * TODO: What happens which attached mail-messages? Will those not cause a big mess?
 *
 * @author Johannes Verelst &lt;johannes.verelst@eo.nl&gt;
 * @version $Id: SMTPHandler.java,v 1.22 2007-05-16 11:33:18 michiel Exp $
 */
public class SMTPHandler implements Runnable {
    private static final Logger log = Logging.getLoggerInstance(SMTPHandler.class);

    private boolean running = true;
    private final java.net.Socket socket;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;

    private final Cloud cloud;
    private final Map<String, String> properties;

    /** State indicating we sent our '220' initial session instantiation message, and are now waiting for a HELO */
    private final int STATE_HELO = 1;

    /** State indicating we received a HELO and we are now waiting for a 'MAIL FROM:' */
    private final int STATE_MAILFROM = 2;

    /** State indicating we received a MAIL FROM and we are now waiting for a 'RCPT TO:' */
    private final int STATE_RCPTTO = 3;

    /** State indicating we received a DATA and we are now processing the data */
    private final int STATE_DATA = 4;

    /** State indicating we received a QUIT, and that we may close the connection */
    private final int STATE_FINISHED = 5;

    /** The current state of this handler */
    private int state = 0;


    private class MailBox {
        public final Node box;  // mailbox object (mails are related to this node) Can be the user
                                // node itself.
        public final Node user; 
        MailBox(Node b, Node u) {
            box = b; user = u;
        }
    }

    /** 
     * List containing Node objects for all mailboxes of the receipients 
     */
    private final List<MailBox> mailboxes = new ArrayList<MailBox>(); 

    /**
     * Public constructor. Set all data that is needed for this thread to run.
     */
    public SMTPHandler(java.net.Socket socket, Map<String, String> properties, Cloud cloud) {
        this.socket = socket;
        this.properties = properties;
        this.cloud = cloud;
    }

    /**
     * The main run method of this thread. It will read data from the given
     * socket line by line, and it will call the parser for this data.
     */
    public void run() {
        // talk to the other party
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(is));  // which encoding?
            writer = new BufferedWriter(new OutputStreamWriter(os)); // which encoding?
        } catch (IOException e) {
            log.error("Exception while initializing inputstream to incoming SMTP connection: " + e.getMessage(), e);
            return;
        }

        try {
            writer.write("220 " + properties.get("hostname") + " Service ready\r\n"); // SMTP uses Windows-like newline conventions
            writer.flush();

            while (state < STATE_FINISHED) {
                String line = reader.readLine();
                parseLine(line);
            }
        } catch (IOException e) {
            log.warn("Caught IOException: " + e);
        }

        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            log.warn("Cannot cleanup my reader, writer or socket: " + e);
        }
    }

    /**
     * Parse input received from the client. This method has the following side-effects:
     * <ul>
     *  <li> It can alter the 'state' variable
     *  <li> It can read extra data from the 'reader'
     *  <li> It can write data to the 'writer'
     *  <li> It can add Nodes to the 'mailboxes' vector
     * </ul>
     */
    private void parseLine(String line) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("SMTP INCOMING: " + line);
        }

        String uLine = line.toUpperCase();

        if (uLine.startsWith("QUIT")) {
            state = STATE_FINISHED;
            writer.write("221 Goodbye.\r\n");
            writer.flush();
            return;
        }

        if (uLine.startsWith("RSET")) {
            state = STATE_MAILFROM;
            mailboxes.clear();
            writer.write("250 Spontanious amnesia has struck me, I forgot everything!\r\n");
            writer.flush();
            return;
        }

        if (uLine.startsWith("HELO")) {
            if (state > STATE_HELO) {
                writer.write("503 5.0.0 " + properties.get("hostname") + "Duplicate HELO/EHLO\r\n");
                writer.flush();
            } else {
                writer.write("250 " + properties.get("hostname") + " Good day [" + socket.getInetAddress().getHostAddress() + "], how are you today?\r\n");
                writer.flush();
                state = STATE_MAILFROM;
            }
            return;
        }

        if (uLine.startsWith("MAIL FROM:")){
            if (state < STATE_MAILFROM) {
                writer.write("503 That's not nice! Polite people say HELO first\r\n");
                writer.flush();
            } else if (state > STATE_MAILFROM) {
                writer.write("503 You cannot specify MAIL FROM after a RCPT TO\r\n");
                writer.flush();
            } else {
                String address = line.substring(9, line.length());
                String sender[] = parseAddress(address);

                writer.write("250 That address looks okay, I'll allow you to send mail.\r\n");
                writer.flush();
                state = STATE_RCPTTO;
            }
            return;
        }

        if (uLine.startsWith("RCPT TO:")) {
            log.debug(line);
            if (state < STATE_RCPTTO) {
                writer.write("503 You should say MAIL FROM first\r\n");
                writer.flush();
            } else if (state >= STATE_DATA) {
                writer.write("503 You cannot use RCPT TO: at this state\r\n");
                writer.flush();
            } else {
                String address = line.substring(7, line.length());
                String recepient[] = parseAddress(address);
                if (recepient.length != 2) {
                    log.service("Can't parse address " + address);
                    writer.write("553 This user format is unknown here\r\n");// 
                    writer.flush();
                    return;
                }
                String username = recepient[0];
                String domain = recepient[1];
                String domains = properties.get("domains");
                log.service("Incoming mail for " + username + " @ "+domain);
                for (StringTokenizer st = new StringTokenizer(domains, ","); st.hasMoreTokens();) {
                    if (domain.toLowerCase().endsWith(st.nextToken().toLowerCase())) {
                        if (! addMailbox(username)) {
                            log.service("Mail for " + username + " rejected: no mailbox");
                            writer.write("550 User not found: " + username + "\r\n");
                            writer.flush();
                            return;
                        }
                        log.service("Mail for " + username + " accepted");
                        writer.write("250 Yeah, that user lives here. Bring on the data!\r\n");
                        writer.flush();
                        return;
                    } 
                }
                log.service("Mail for domain " + domain + " not accepted, not one of " + domains);
                writer.write("553 We do not accept mail for domain '" + domain + "'\r\n");
                writer.flush();
            }
            return;
        }

        if (uLine.startsWith("DATA")) {
            if (state < STATE_RCPTTO) {
                writer.write("503 You should issue an RCPT TO first\r\n");
                writer.flush();
            } else if (state != STATE_RCPTTO) {
                writer.write("503 Command not possible at this state\r\n");
                writer.flush();
            } else if (mailboxes.size() == 0) {
                writer.write("503 You should issue an RCPT TO first\r\n");
                writer.flush();
            } else {
                // start reading all the data, until the '.'
                writer.write("354 Enter mail, end with CRLF.CRLF\r\n");
                writer.flush();
                char[] endchars = {'\r', '\n', '.', '\r', '\n'};
                char[] last5chars = new char[endchars.length];
                int currentpos = 0;
                int c;
                StringBuilder data = new StringBuilder();
                boolean isreading = true;
                while (isreading) {
                    while ((c = reader.read()) == -1) {
                        try {
                            // why's this?
                            Thread.currentThread().sleep(50);
                        } catch (InterruptedException e) {}
                    }
                    data.append((char)c);

                    for (int i = 0; i < last5chars.length - 1; i++) {
                        last5chars[i] = last5chars[i + 1];
                    }
                    last5chars[last5chars.length - 1] = (char)c;

                    isreading = false;
                    for (int i = 0; i < last5chars.length; i++) {
                        if (last5chars[i] != endchars[i]) {
                            isreading = true;
                            break;
                        }
                    }
                }

                // Copy everything but the '.\r\n' to the result
                String result = data.substring(0, data.length() - 3);
                if (handleData(result)) {
                    writer.write("250 Rejoice! We will deliver this email to the user.\r\n");
                    writer.flush();
                    state = STATE_MAILFROM;
                } else  {
                    writer.write("550 Message not accepted.\r\n");
                    writer.flush();
                }
            }
            return;
        }

        writer.write("503 Sorry, but I have no idea what you mean.\r\n");
        writer.flush();
    }

    /**
     * Interrupt method, is called only during shutdown
     */
    public void interrupt() {
        log.info("Interrupt() called");
    }

    /**
     * Parse a string of addresses, which are given in an RCPT TO: or MAIL FROM:
     * line by the client. This is a strict RFC implementation.
     * @return an array of strings, the first element contains the username, the second element is the domain
     */
    private String[] parseAddress(String address) {
        if (address == null)
            return new String[0];

        if (address.equals("<>"))
            return new String[0];

        int leftbracket = address.indexOf("<");
        int rightbracket = address.indexOf(">");
        int colon = address.indexOf(":");

        // If we have source routing, we must ignore everything before the colon
        if (colon > 0) {
            leftbracket = colon;
        }

        // if the left or right brackets are not supplied, we MAY bounce the message. We
        // however try to parse the address still

        if (leftbracket < 0) {
            leftbracket = 0;
        }
        if (rightbracket < 0) {
            rightbracket = address.length();
        }

        // Trim off any whitespace that may be left
        String finaladdress = address.substring(leftbracket + 1, rightbracket).trim();
        int atsign = finaladdress.indexOf("@");
        if (atsign < 0)
            return new String[0];

        String[] retval = new String[2];
        retval[0] = finaladdress.substring(0, atsign);
        retval[1] = finaladdress.substring(atsign + 1, finaladdress.length());
        return retval;
    }

    protected void nodeSetHeader(Node node, String fieldName, Address[] values) {
        Field field = node.getNodeManager().getField(fieldName);
        long maxLength = ((org.mmbase.datatypes.StringDataType) field.getDataType()).getMaxLength();
        StringBuilder buf = new StringBuilder();
        if (values != null) {
            log.debug("Using " + values.length + " values");
            for (int i = 0 ; i < values.length; i++) {
                Address value = values[i];
                if (buf.length() + value.toString().length() + 2 < maxLength) {
                    buf.append(value.toString());
                    if (i < values.length) {
                        buf.append(", ");
                    }
                } else {
                    log.warn("Could not store " + value + " for field '" + fieldName + "' of email node because field can maximully contain " + maxLength + " chars");
                }
            }
        }
        nodeSetHeader(node, fieldName, buf.toString());
    }
    protected void nodeSetHeader(Node node, String fieldName, String value) {
        Field field = node.getNodeManager().getField(fieldName);
        int maxLength = field.getMaxLength();
        log.trace("max length for " + fieldName + " is " + maxLength);
        if (value.length() >= maxLength) {
            log.warn("Truncating field " + fieldName + " for node " + node + " (" + value.length() + " > " + maxLength + ")");
            value = value.substring(0, maxLength - 1);
        } else {
            log.trace(value.length() + " < " + maxLength);
        }
        node.setStringValue(fieldName, value);
    }
    protected String getMimeType(String contentType) {
        if (contentType == null) return null;
        int pos = contentType.indexOf(';');
        String mimeType;
        if (pos > 0) {
            mimeType = contentType.substring(0, pos);
        } else {
            mimeType = contentType;
        }
        return mimeType;
    }


    /**
     * @return 1 if mt1 bettern then mt2, -1 if mt2 is better, 0 if they are equal. -1 if unknown.
     */

    protected int compareMimeTypes(String mt1, String mt2) {
        if (mt1.equals(mt2)) return 0;
        if (mt1.equals("text/html") && mt2.equals("text/plain")) {
            return 1;
        }
        if (mt2.equals("text/html") && mt1.equals("text/plain")) {
            return -1;
        }
        return -1;
    }

    /**
     * Handle the data from the DATA command. This method does all the work: it creates
     * objects in mailboxes.
     */
    private boolean handleData(String data) {
        if (log.isTraceEnabled()) {
            log.trace("Data: [" + data + "]");
        }
        NodeManager emailbuilder = cloud.getNodeManager((String)properties.get("emailbuilder"));
        MimeMessage message = null;
        try {
            message = new MimeMessage(null, new ByteArrayInputStream(data.getBytes())); 
            /// which encoding?!
        } catch (MessagingException e) {
            log.error("Cannot parse message data: [" + data + "]");
            return false;
        }
        for (MailBox mailbox : mailboxes) {
            log.debug("Delivering to mailbox node " + mailbox.box.getNumber());
            Node email = emailbuilder.createNode();
            if (properties.containsKey("emailbuilder.typefield")) {
                 email.setIntValue((String)properties.get("emailbuilder.typefield"), 2); // new unread mail
            }
            if (properties.containsKey("emailbuilder.headersfield")) {
                StringBuilder headers = new StringBuilder();
                try {
                    Enumeration e = message.getAllHeaderLines();
                    while (e.hasMoreElements()) {
                        String header = (String) e.nextElement();
                        headers.append(header).append("\r\n");
                    }
                    log.debug("Using headers " + headers);
                    nodeSetHeader(email, properties.get("emailbuilder.headersfield"), headers.toString());
                } catch (MessagingException me) {
                    log.warn(me);
                    nodeSetHeader(email, properties.get("emailbuilder.headersfield"), headers.toString());
                }
            }
            if (properties.containsKey("emailbuilder.tofield")) {
                try {
                    Address[] value = message.getRecipients(Message.RecipientType.TO);
                    nodeSetHeader(email, properties.get("emailbuilder.tofield"), value);
                } catch (MessagingException e) {
                    log.service(e);
                }
            }
            if (properties.containsKey("emailbuilder.ccfield")) {
                try {
                    Address[] value = message.getRecipients(Message.RecipientType.CC);
                    nodeSetHeader(email, properties.get("emailbuilder.ccfield"), value);
                } catch (MessagingException e) {
                    log.service(e);
                }
            }
            if (properties.containsKey("emailbuilder.bccfield")) {
                try {
                    Address[] value = message.getRecipients(Message.RecipientType.BCC);
                    nodeSetHeader(email, properties.get("emailbuilder.bccfield"), value);
                } catch (MessagingException e) {
                    log.service(e);
                }
            }
            if (properties.containsKey("emailbuilder.fromfield")) {
                try {
                    String value = message.getHeader("From", ", ");
                    if (value == null) value = "";
                    nodeSetHeader(email, properties.get("emailbuilder.fromfield"), value);
                } catch (MessagingException e) {
                    log.service(e);
                }
            }
            if (properties.containsKey("emailbuilder.subjectfield")) {
                try {
                    String value = message.getSubject();
                    if (value == null) value = "(empty)";
                    nodeSetHeader(email, properties.get("emailbuilder.subjectfield"), value);
                } catch (MessagingException e) {
                    log.service(e);
                }
            }
            if (properties.containsKey("emailbuilder.datefield")) {
                try {
                    Date d = message.getSentDate();
                    if (d == null) {
                        d = new Date();
                    }
                    email.setIntValue(properties.get("emailbuilder.datefield"), (int)(d.getTime() / 1000));
                } catch (MessagingException e) {
                    log.service(e);
                }
            }
            if (email.getNodeManager().hasField("mimetype")) {
                try {
                    String contentType = message.getContentType();
                    if (contentType != null) {
                        nodeSetHeader(email, "mimetype", getMimeType(contentType));
                    }
                } catch (MessagingException me) {
                    log.warn(me);
                }
            }
            try {
                if (! message.isMimeType("multipart/*")) {
                    if (log.isDebugEnabled()) {
                        log.debug("Non multipart mail, simply filling the body of the mail node with " + message);
                    }
                    if (message.getContent() != null) {
                        nodeSetHeader(email, properties.get("emailbuilder.bodyfield"), "" + message.getContent());
                    }
                    try {
                        email.commit(); 
                    } catch (Exception e) {
                        log.error(e);
                    }
                } else {
                    // now parse the attachments
                    try {
                        log.debug("Extracting parts for message with mimetype " + message.getContentType());
                        List<Node> attachmentsVector = extractPart(message, new ArrayList<Node>(), email);
                        email.commit();
                        for (Node attachment : attachmentsVector) {
                            Relation rel = email.createRelation(attachment, cloud.getRelationManager("related"));
                            rel.commit();
                        }
                    } catch (Exception e) {
                        log.error("Exception while parsing attachments: " + e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
                try {
                    nodeSetHeader(email, properties.get("emailbuilder.bodyfield"), "" + data);
                    email.commit();
                } catch (Exception ee) {
                    log.error(ee);
                }
            }
            Relation rel = mailbox.box.createRelation(email, cloud.getRelationManager("related"));
            rel.commit();

            //TODO: send to this user if he wants to
            Node user = mailbox.user;
            if (user.getBooleanValue("email-mayforward")) {
                try {
                    String mailadres = user.getStringValue("email");
                    log.service("Forwarding " + email + " to " + mailadres);
                    ExtendedJMSendMail sendmail = (ExtendedJMSendMail)org.mmbase.module.Module.getModule("sendmail");
                    sendmail.startModule();
                    sendmail.sendMail(mailadres, email);
                } catch (Throwable e) {
                    log.warn("Exception in forward " + e.getMessage(), e);
                    return false;
                }
            }
        }
        return true;
    }

    protected String getContent(Part p) throws Exception {
        Object content = null;
        try {
            content = p.getContent();
        } catch (UnsupportedEncodingException e) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                p.writeTo(bos);
                content = bos.toString("ISO-8859-1");
            } catch (IOException e2) {}
        }
        if (content instanceof String) {
            return (String) content;
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            p.writeTo(bos);
            return  bos.toString("ISO-8859-1");
        }
    }

    /**
     * Extract all attachments from a Part of a MultiPart message.
     * @author Gerard van Enk
     * @param p Part object that is being dissected
     * @param attachments List of parts that already extracted
     * @param mail Mail Node that describes the mail that is being dissected
     * @return The given attachments List, but wihth the currently extracted ones added.
     **/
    private List<Node> extractPart(final Part p, final List<Node> attachments, final Node mail) throws Exception {

        String disposition = p.getDisposition();
        if (disposition == null) {
            // according to RFC 2183
            // Content-Disposition is an optional header field. In its absence, the
            // MUA may use whatever presentation method it deems suitable.

            // this, I deem suitable:

            disposition = p.isMimeType("text/*") || p.isMimeType("message/*")  ? Part.INLINE : Part.ATTACHMENT;
        }
        if (log.isDebugEnabled()) {
            log.debug("Extracting attachments from " + p + " (" + p.getContentType() + ", disposition " + p.getDisposition() + ") for node " + mail);
        }

        if (Part.INLINE.equals(disposition)) {
            // only adding text/plain - text/html will be stored as attachment!
            // should used somehow multipart/alternative

            // MM I think this goes wrong if the original mimeType was multipart/alternative
            // when forwarding:
            // 2006-12-28 10:32:38,758 ERROR nl.didactor.mail.ExtendedJMSendMail sendRemoteMail.373  - JMSendMail failure: MIME part of type "multipart/alternative" contains object of type java.lang.String instead of MimeMultipart
            // and in the web-interface it still shows 2 attachments, while it should show none.
            String mimeType = getMimeType(p.getContentType());
            String mailMimeType = mail.getStringValue("mimetype");



            log.debug("Found attachments with type: text/plain");
            String content = getContent(p);

            String bodyField = properties.get("emailbuilder.bodyfield");
            if (content != null) {
                int compareMimeType = compareMimeTypes(mimeType, mailMimeType);
                String currentBody = mail.getStringValue(bodyField);
                if (currentBody == null || "".equals(currentBody) || compareMimeType > 0) {
                    mail.setStringValue(bodyField, content);
                    mail.setStringValue("mimetype", mimeType);
                } else if (compareMimeType == 0) {
                    mail.setStringValue(bodyField, currentBody +"\r\n\r\n" + content);
                } else {
                    if (p.isMimeType("text/*")) {
                        log.debug("Ignoring part with mimeType " + mimeType + " (not better than already stored part with mimeType " + mailMimeType);
                    } else {
                        log.debug("Found a non-text alternative inline part, cannot handle that, treat it as an ordinary attachment");
                        Node tempAttachment = storeAttachment(p);
                        if (tempAttachment != null) {
                            attachments.add(tempAttachment);
                        }
                    }
                }
            } else {
                log.debug("Content of part is null, ignored");
            }
        } else if (p.isMimeType("multipart/*")) {

            Multipart mp = (Multipart)p.getContent();
            int count = mp.getCount();
            log.debug("Found attachment with type: " + p.getContentType() + " it has " + count + " parts, will add those recursively");
            for (int i = 0; i < count; i++) {
                extractPart(mp.getBodyPart(i), attachments, mail);
            }
        } else if (p.isMimeType("message/*")) {
            log.debug("Found attachment with type: " + p.getContentType());
            extractPart((Part) p.getContent(), attachments, mail);
        } else {
            log.debug("Found attachment with type: " + p.getContentType());
            Node tempAttachment = storeAttachment(p);
            if (tempAttachment != null) {
                attachments.add(tempAttachment);
            }
        }
        return attachments;
    }

    /**
     * Store an attachment (contained in a Part) in the MMBase object cloud.
     * @param p
     * @return Node in the MMBase object cloud
     */
    private Node storeAttachment(Part p) throws MessagingException {
        String fileName = p.getFileName();
        NodeManager attachmentManager = cloud.getNodeManager("attachments");

        if (attachmentManager == null) {
            log.error("Attachments builder not activated");
            return null;
        }

        Node attachmentNode = attachmentManager.createNode();


        if (p instanceof MimeBodyPart) {
            MimeBodyPart mbp = (MimeBodyPart) p;
            String contentId = mbp.getContentID();
            if (contentId != null) {
                // a bit of misuse, of course.
                // targeting at working of multipart/related messages.
                attachmentNode.setStringValue("description", contentId);
            }
        }
        String mimeType = getMimeType(p.getContentType());

        String title = fileName;

        if (title == null || "".equals(title)) {
            title = "attachment " + mimeType;
        }
        attachmentNode.setStringValue("title", title);

        attachmentNode.setStringValue("mimetype", mimeType);
        attachmentNode.setStringValue("filename", fileName);
        attachmentNode.setIntValue("size", p.getSize());

        try {
            attachmentNode.setInputStreamValue("handle", p.getInputStream(), p.getSize());
        } catch (Exception ex) {
            log.error("Caught exception while trying to read attachment data: " + ex);
        }

        attachmentNode.commit();
        log.debug("committed attachment to MMBase");

        return attachmentNode;
    }




    /**
     * This method returns a Node to which the email should be related.
     * This node can be the user object represented by the given string parameter,
     * or it can be another object that is related to this user. This behaviour
     * is defined in the config file for this module.
     * @return whether or not this succeeded
     */
    private boolean addMailbox(String user) {

        String usersbuilder = properties.get("usersbuilder");
        NodeManager manager = cloud.getNodeManager(usersbuilder);
        NodeList nodelist = manager.getList(properties.get("usersbuilder.accountfield") + " = '" + user + "'", null, null);
        if (nodelist.size() != 1) {
            return false;
        }
        Node usernode = nodelist.getNode(0);
        if (properties.containsKey("mailboxbuilder")) {
            String where = null;
            String mailboxbuilder = properties.get("mailboxbuilder");
            if (properties.containsKey("mailboxbuilder.where")) {
                where = properties.get("mailboxbuilder.where");
            }
            nodelist = cloud.getList(
                "" + usernode.getNumber(),              //startnodes
                usersbuilder + "," + mailboxbuilder,    //path
                mailboxbuilder + ".number",             //fields
                where,                                  //constraints
                null,                                   //orderby
                null,                                   //directions
                null,                                   //searchdir
                true                                    //distinct
            );
            if (nodelist.size() == 1) {
                String number = nodelist.getNode(0).getStringValue(mailboxbuilder + ".number");
                mailboxes.add(new MailBox(cloud.getNode(number), usernode));
                return true;
            } else if (nodelist.size() == 0) {
                String notfoundaction = "bounce";
                if (properties.containsKey("mailboxbuilder.notfound")) {
                    notfoundaction = properties.get("mailboxbuilder.notfound");
                }
                if ("bounce".equals(notfoundaction)) {
                    return false;
                }
                /* this needs to be implemented
                if ("create".equals(notfoundaction)) {

                }
                */
            } else {
                log.error("Too many mailboxes for user '" + user + "'");
                return false;
            }
        } else {
            mailboxes.add(new MailBox(usernode, usernode));
        }
        return false;
    }
}
