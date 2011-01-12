/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.bridge.util.NodeURLStreamHandlerFactory;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;

/**
 * The resources builder can be used by {@link org.mmbase.util.ResourceLoader} to load resources from
 * (configuration files, classes, resourcebundles).
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since   MMBase-1.8
 */
public class Resources extends Attachments {
    private static final Logger log = Logging.getLoggerInstance(Resources.class);


    /**
     * Implements virtual filename field.
     * {@inheritDoc}
     */
    @Override
    public Object getValue(MMObjectNode node, String field) {
        if (field.equals(NodeURLStreamHandlerFactory.FILENAME_FIELD)) {
            String s = node.getStringValue(NodeURLStreamHandlerFactory.RESOURCENAME_FIELD);
            int i = s.lastIndexOf("/");
            if (i > 0) {
                return s.substring(i + 1);
            } else {
                return s;
            }
        } else {
            return super.getValue(node, field);
        }
    }

}
