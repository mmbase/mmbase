/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.mmbase.module.core.*;
import org.mmbase.util.*;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.*;

/**
 * This builder can be used for 'attachments' builders. That is
 * builders which have a 'handle' field and are associated with the
 * 'attachments servlet.
 *
 * @author cjr@dds.nl
 * @author Michiel Meeuwissen
 * @version $Id: Attachments.java,v 1.34 2004-10-09 13:54:11 pierre Exp $
 */
public class Attachments extends AbstractServletBuilder {
    private static final Logger log = Logging.getLoggerInstance(Attachments.class);


    protected String getAssociation() {
        return "attachments";
    }
    protected String getDefaultPath() {
        return "/attachment.db";
    }

    protected String getSGUIIndicator(MMObjectNode node, Parameters a) {
        String field = a.getString("field");
        if (field.equals("handle") || field.equals("")) {
            int num  = node.getIntValue("number");
            //int size = node.getIntValue("size");

            String fileName = node.getStringValue("filename");
            String title;

            if (fileName == null || fileName.equals("")) {
                title = "[*]";
            } else {
                title = "[" + fileName + "]";
            }

            if (/*size == -1  || */ num == -1) { // check on size seems sensible, but size was often not filled
                return title;               
            } else {
                String ses = (String) a.get("session");
                if (log.isDebugEnabled()) {
                    log.debug("bridge: " + usesBridgeServlet + " ses: " + ses);
                }
                StringBuffer servlet = new StringBuffer();
                HttpServletRequest req = (HttpServletRequest) a.get("request");
                if (req != null) {            
                    servlet.append(getServletPath(UriParser.makeRelative(new java.io.File(req.getServletPath()).getParent(), "/")));
                } else {
                    servlet.append(getServletPath());
                }
                boolean addFileName =   ! (servlet.charAt(servlet.length() - 1) == '?' ||  "".equals(fileName));
                servlet.append(usesBridgeServlet && ses != null ? "session=" + ses + "+" : "").append(num);

                if (addFileName) {
                    servlet.append('/').append(fileName);
                }

                HttpServletResponse res = (HttpServletResponse) a.get("response");
                String url;
                if (res != null) {
                    url = res.encodeURL(servlet.toString());
                } else {
                    url = servlet.toString();
                }
                return "<a href=\"" + url + "\" target=\"extern\">" + title + "</a>";
            }
        }
        return super.getSuperGUIIndicator(field, node);
    }

    /**
     * If mimetype is not filled on storage in the database, then we
     * can try to do smart things.
     */
    protected void checkHandle(MMObjectNode node) {
        String mimetype = node.getStringValue("mimetype");
        if (mimetype == null || mimetype.equals("")) {
            log.service("Mimetype of attachment '" + node.getStringValue("title") + "' was not set. Using magic to determine it automaticly.");
            Object h = node.getValue("handle");
            if (h != null && (h instanceof byte[])) { // if unfilled h can be $SHORTED, sigh, sigh (at least when using editwizard)

                // if (! (h instanceof byte[])) throw new RuntimeException("Handle field was not a byte[] but a '" + h.getClass().getName() + "'" + " with value " + h);
                byte[] handle = (byte[]) h;
                node.setValue("size", handle.length); // also the size, why not.
                log.debug("Attachment size of file = " + handle.length);
            
                String filename = node.getStringValue("filename");
                String extension = null;
                int dotIndex = filename.lastIndexOf("."); 
                if (dotIndex > -1) {
                    extension = filename.substring(dotIndex + 1);
                }

                MagicFile magic = MagicFile.getInstance();
                try {
                    String mime = null;
                    if (extension == null) {
                        mime = magic.getMimeType(handle);
                    } else {
                        mime = magic.getMimeType(handle, extension);
                    }
                    log.service("Found mime-type: " + mime);
                    node.setValue("mimetype", mime);
                } catch (Throwable e) {
                    log.warn("Exception in MagicFile  for " + node);
                    node.setValue("mimetype", "application/octet-stream");                    
                }            
            }
        }
    }

    public int insert(String owner, MMObjectNode node) {
        checkHandle(node);
        return super.insert(owner, node);
    }
    public boolean commit(MMObjectNode node) {
        Collection changed = node.getChanged();
        if (changed.contains("handle")) {
            node.setValue("mimetype", "");
            checkHandle(node);
        }
        if (changed.contains("mimetype") && "".equals(node.getStringValue("mimetype"))) {
            checkHandle(node);
        }
        return super.commit(node);
    }


    /**
     * Implements 'mimetype' function (Very simply for attachments, because they have the field).
     *
     * @since MMBase-1.6.1
     */
    protected Object executeFunction(MMObjectNode node, String function, List args) {
        log.debug("executeFunction of attachments builder");
        if ("mimetype".equals(function)) {
            return node.getStringValue("mimetype");
        } else if (function.equals("format")) {
            String mimeType = node.getStringValue("mimetype");
            if (mimeType.length() > 0) {
                MagicFile mf = MagicFile.getInstance();
                String ext = mf.mimeTypeToExtension(mimeType);
                if (! "".equals(ext)) {
                    return ext;
                }
            }
        }
        return super.executeFunction(node, function, args);
    }

}
