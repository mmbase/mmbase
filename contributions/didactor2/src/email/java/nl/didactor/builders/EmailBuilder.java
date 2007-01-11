package nl.didactor.builders;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.module.core.*;
import org.mmbase.module.*;
import org.mmbase.util.*;
import org.mmbase.util.functions.*;
import nl.didactor.mail.ExtendedJMSendMail;
import java.util.*;

/**
 * This class handles objects of type 'emails'. When new emails are created,
 * it checks whether this email should be sent using the 'sendmail' module.
 *
 * @author Johannes Verelst &lt;johannes.verelst@eo.nl&gt;
 */
public class EmailBuilder extends MMObjectBuilder {
    private static final Logger log=Logging.getLoggerInstance(EmailBuilder.class);
    private ExtendedJMSendMail sendmail;

    /**
     * Initialize the builder
     */
    public boolean init() {
        sendmail = (ExtendedJMSendMail)Module.getModule("sendmail");
        sendmail.startModule();
        return super.init();
    }

    /**
     * Test whether or not this email object should be sent using the
     * sendmail module. If so, make sure that the 'type' value is set
     * back to '0'. 
     * This method will only email nodes that are already committed to
     * the database. Otherwise it's a pain to find out which objects
     * are related to it (attachments that need to be sent with it)
     */
    public MMObjectNode preCommit(MMObjectNode node) {
        if (log.isDebugEnabled())
            log.debug("preCommit(" + node + ")");

        int nodeNumber = node.getNumber();
        if (nodeNumber == -1)
            return node;

        if (node.getIntValue("type") == 1) {
            // This goes wrong if the node is new, because that it cannot be gotten with
            // cloud.getNode yet....

            log.debug("Trying to send mail ...");
            org.mmbase.bridge.Cloud cloud = org.mmbase.bridge.LocalContext.getCloudContext().getCloud("mmbase");
            org.mmbase.bridge.Node n;
            if (nodeNumber > 0) {
                n = cloud.getNode(nodeNumber);
            } else {
                // mapnodes do not have relations.
                n = new org.mmbase.bridge.util.MapNode(node.getValues(), 
                                                       cloud.getNodeManager(getTableName())); 
            }

            if (sendmail.sendMail(n)) {
                log.debug("Succeeded!");
                node.setValue("type", 0);
            } else {
                log.error("Cannot send mail '" + node + "'");
                // TODO: we have to notify the user that something went wrong. Ideally, a bounce message
            }
        }
        return node;
    }

    /**
     * Insert a new object into the cloud. This will trigger
     * a mail-send if the 'type' field of the node is 1.
     */
    public int insert(String owner, MMObjectNode node) {
        int nr = super.insert(owner, node);
        log.debug("Inserted " + nr);
        int type = node.getIntValue("type");
        if (type == 1) {
            log.debug("Calling precommit because type is 1");
            preCommit(node);
        } else {
            log.debug("Type " + type);
        }

        return nr;
    }


    /**
     * I guess that following stuff
     *  - converts 'pseudo' HTML to better HTML
     * - removes <script tags and the like
     */
    protected String html(String body) {
        body = body.replaceAll("\n","<br/>\n");
        body = body.replaceAll("(?i)<[\\s]*/?script.*?>|<[\\s]*/?embed.*?>|<[\\s]*/?object.*?>|<[\\s]*a[\\s]*href[^>]*javascript[\\s]*:[^(^)^>]*[(][^)]*[)][^>]*>[^<]*(<[\\s]*/[\\s]*a[^>]*>)*", "");
        return body;
    }

    public String getGUIIndicator(MMObjectNode node, Parameters pars) {
        String field = pars.getString("field");
        if ("body".equals(field)) {
            String mimeType = hasField("mimetype") ? node.getStringValue("mimetype") : null;
            if (mimeType == null) mimeType = "text/html";
            String body = node.getStringValue("body");
            if (mimeType.startsWith("text/plain")) {
                return org.mmbase.util.transformers.XmlField.richToHTMLBlock(body);
            } else if (mimeType.startsWith("multipart/")) {
                if ("".equals(body)) {
                    log.debug("Multipare message with no explicit body, mime-type compliance.");
                    String b = null;
                    String lastMime = "text/plain";
                    List attachments = node.getRelatedNodes("attachments");
                    Iterator i = attachments.iterator();
                    while (i.hasNext()) {
                        MMObjectNode attachment = (MMObjectNode) i.next();
                        String attachmentMime = attachment.getStringValue("mimetype");
                        log.debug("Considering attachment " + attachment.getNumber() + " " + attachmentMime);
                        if (attachmentMime.startsWith("text") && (b == null || ! lastMime.equals("text/html"))) {
                            try {
                                b = new String(attachment.getByteValue("handle"), "UTF-8");
                            } catch (java.io.UnsupportedEncodingException use) {
                                // should  not happen
                                log.warn(use);
                                b = attachment.getStringValue("handle");
                            }
                            lastMime = attachmentMime;
                        }
                    }
                    if (b == null) b = "----";
                    return html(b);
                } else {
                    return html(body);
                }
            } else {
                //if (mimeType.startsWith("text/html")) { 
                return html(body);
            }
        } else if ("headers".equals(field)) {
            StringBuffer buf = new StringBuffer();
            String[] headers = node.getStringValue("headers").split("[\n\r]+");
            for (int i = 0 ; i < headers.length; i++) {
                String header = headers[i];

                // these are stored in separate fields
                if (header.startsWith("Subject:")) {
                    continue;
                }
                if (header.startsWith("Cc:")) {
                    continue;
                }
                if (header.startsWith("Bcc:")) {
                    continue;
                }
                if (header.startsWith("From:")) {
                    continue;
                }
                buf.append(org.mmbase.util.transformers.Xml.XMLEscape(header)).append("<br />");
            }
            return buf.toString();
        } else {
            return super.getGUIIndicator(node, pars);
        }
    }

    public String toString(MMObjectNode n) {
        return n.getNumber() + " " + n.getStringValue("from") + "->" + n.getStringValue("to") + " '" + n.getStringValue("subject") + "' " + n.getDateValue("date");
    }

}

