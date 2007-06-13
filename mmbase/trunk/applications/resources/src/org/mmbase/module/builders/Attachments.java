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
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.magicfile.MagicFile;

/**
 * This builder can be used for 'attachments' builders. That is
 * builders which have a 'handle' field and are associated with the
 * 'attachments servlet.
 *
 * @author cjr@dds.nl
 * @author Michiel Meeuwissen
 * @version $Id: Attachments.java,v 1.2 2007-06-13 19:40:56 nklasens Exp $
 */
public class Attachments extends AbstractServletBuilder {
    private static final Logger log = Logging.getLoggerInstance(Attachments.class);

    public static final String FIELD_SIZE       = "size";

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

            String fileName = getFileName(node, new StringBuffer()).toString();
            String title;

            if (fileName == null || fileName.equals("")) {
                title = "[*]";
            } else {
                title = "[" + fileName + "]";
            }

            if (/*size == -1  || */ num == -1) { // check on size seems sensible, but size was often not filled
                return title;
            } else {
                String ses = getSession(a, node.getNumber());
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

    protected final Set<String> ATTACHMENTS_HANDLE_FIELDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] {FIELD_MIMETYPE, FIELD_SIZE})));
    // javadoc inherited
    protected Set<String> getHandleFields() {
        return ATTACHMENTS_HANDLE_FIELDS;
    }

    /**
     * If mimetype is not filled on storage in the database, then we
     * can try to do smart things.
     */
    protected void checkHandle(MMObjectNode node) {
        super.checkHandle(node);
        if (getField(FIELD_SIZE) != null) {
            if (node.getIntValue(FIELD_SIZE) == -1) {
                node.setValue(FIELD_SIZE, node.getByteValue(FIELD_HANDLE).length);
            }
        }
    }

    /**
     * Implements 'mimetype' function (Very simply for attachments, because they have the field).
     *
     * @since MMBase-1.6.1
     */
    protected Object executeFunction(MMObjectNode node, String function, List<?> args) {
        log.debug("executeFunction of attachments builder");
        if ("mimetype".equals(function)) {
            return node.getStringValue(FIELD_MIMETYPE);
        } else if (function.equals("format")) {
            String mimeType = node.getStringValue(FIELD_MIMETYPE);
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
