/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.functions;

import java.util.*;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.bridge.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * The gui function of MMObjectBuilder
 *
 * @author Michiel Meeuwissen
 * @version $Id: GuiFunction.java,v 1.2 2006-09-15 14:56:28 michiel Exp $
 * @since MMBase-1.9
 */
public class GuiFunction extends NodeFunction {

    private static final Logger log = Logging.getLoggerInstance(GuiFunction.class);
    public static final Parameter[] PARAMETERS = {
        Parameter.FIELD,
        Parameter.LANGUAGE,
        new Parameter("session", String.class),
        Parameter.RESPONSE,
        Parameter.REQUEST,
        Parameter.LOCALE,
        new Parameter("stringvalue", String.class)
        //new Parameter("length", Integer.class),
        //       field, language, session, response, request) Returns a (XHTML) gui representation of the node (if field is '') or of a certain field. It can take into consideration a http session variable name with loging information and a language");

    };

    public GuiFunction() {
        super("gui", PARAMETERS);
    }

    protected String getFunctionValue(Node node, Parameters parameters) {
        if (log.isDebugEnabled()) {
            log.debug("GUI of builder with " + parameters);
        }
        String fieldName = (String) parameters.get(Parameter.FIELD);
        if (fieldName != null && (! fieldName.equals("")) && parameters.get("stringvalue") == null) {
            if (node.getSize(fieldName) < 2000) {
                parameters.set("stringvalue", node.getStringValue(fieldName));
            }
        }
        MMObjectNode n = (MMObjectNode) parameters.get(Parameter.CORENODE);
        return n.getBuilder().getGUIIndicator(n, parameters);
    }

}
