/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.text.SimpleDateFormat;
import java.util.*;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;

/**
 * @javadoc
 * @application Tools
 * @author David van Zeventer
 * @version $Id$
 */
public class AnnotRel extends InsRel {

    // Defining possible annotation types
    public final static int HOURS = 0;
    public final static int MINUTES = 1;
    public final static int SECONDS = 2;
    public final static int MILLIS = 3;
    /*
        public final static int LINES   = 4;
        public final static int WORDS   = 5;
        public final static int CHARS   = 6;
        public final static int PIXELS  = 7;
        public final static int ROWS    = 8;
        public final static int COLS    = 9;
    */

    private static final Logger log = Logging.getLoggerInstance(AnnotRel.class);

    /**
     * Sets defaults for a node.
     * Initializes all numeric fields to 0, and sets the annotation type to {@link #MILLIS}.
     * @param node The node to set the defaults of.
     */
    @Override
    public void setDefaults(MMObjectNode node) {
        super.setDefaults(node);
        // Set the default value for pos and length to 0 (0:0:0.0)
        node.setValue("pos", 0);
        node.setValue("end", 0);
        node.setValue("length", 0);
        // All time values are default stored in milliseconds.
        node.setValue("type", MILLIS);
    }

    /**
     * What should a GUI display for this node/field combo.
     * Displays the pos, end, and length fields as time-values,
     * and the annotation type field as a descriptive string.
     * @param node The node to display
     * @param field the name field of the field to display
     * @return the display of the node's field as a <code>String</code>, null if not specified
     */
    @Override
    public String getGUIIndicator(String field, MMObjectNode node) {
        if (field.equals("pos")) {
            int time = node.getIntValue("pos");
            return RelativeTime.convertIntToTime(time);
        } else if (field.equals("end")) {
            int time = node.getIntValue("end");
            return RelativeTime.convertIntToTime(time);
        } else if (field.equals("length")) {
            int time = node.getIntValue("length");
            return RelativeTime.convertIntToTime(time);
        } else if (field.equals("type")) {
            int val = node.getIntValue("type");
            if (val == HOURS) {
                return "Hours";
            } else if (val == MINUTES) {
                return "Minuten"; // return "Minutes";
            } else if (val == SECONDS) {
                return "Seconden"; // return "Seconds";
            } else if (val == MILLIS) {
                return "Milliseconden"; // return "Milliseconds";
            }

            /*
              else if (val==LINES) {
                return "Regels";
            } else if (val==WORDS) {
                return "Woorden";
            } else if (val==CHARS) {
                return "Karakters";
            } else if (val==PIXELS) {
                return "Pixels";
            } else if (val==ROWS) {
                return "Rijen";
            } else if (val==COLS) {
                return "Kolommen";
            }
            */
        }
        return null;
    }

    /**
     * The hook that passes all form related pages to the correct handler.
     * This method is not supported.
     * @param sp The PageInfo
     * @param cmds the commands (PRC-CMD) to process
     * @param vars variables (PRC-VAR) to use
     * @return the result value as a <code>String</code>
     */
    public boolean process(PageInfo sp, Hashtable cmds, Hashtable vars) {
        log.debug("process: This method isn't implemented yet.");
        return false;
    }

    /**
     * Obtains a string value by performing the provided command.
     * This method is not supported.
     * @param sp The PageInfo
     * @param command the command to execute
     * @return the result value as a <code>String</code>
     */
    @Override
    public String replace(PageInfo sp, StringTokenizer command) {
        log.debug("replace: This method isn't implemented yet.");
        return "";
    }

    /**
     * Provides additional functionality when setting field values.
     * This method makes sure that the pos, end, and length values have the
     * correct value.
     * @param node the node whose fields are changed
     * @param field the fieldname that is changed
     * @return <code>true</code> if the call was handled.
     */
    public boolean setValue(MMObjectNode node, String field) {
        if (field.equals("end")) {
            int pos = node.getIntValue("pos");
            int end = node.getIntValue("end");
            if (end != -1)
                node.setValue("length", (end - pos));
        } else if (field.equals("pos")) {
            int pos = node.getIntValue("pos");
            int end = node.getIntValue("end");
            if (end != -1)
                node.setValue("length", (end - pos));
        } else if (field.equals("length")) {
            // extra check needed to make sure we don't create a loop !
            // XXX: ???
            int pos = node.getIntValue("pos");
            int end = node.getIntValue("end");
            int len = node.getIntValue("length");
        }
        return true;
    }

    @Override
    public Object getValue(MMObjectNode node, String field) {
        if (field.equals("ms_pos")) {
            int pos = node.getIntValue("pos");
            //format to pos is in ms
            return new SimpleDateFormat("hh:mm.0").format(new Date(pos));
        } else if (field.equals("ms_length")) {
            int len = node.getIntValue("length");
            return new SimpleDateFormat("hh:mm").format(new Date(len));
        } else if (field.equals("end")) {
            int pos = node.getIntValue("pos");
            int len = node.getIntValue("length");
            int end = pos + len;
            return ("" + end);
        }
        return (null);
    }
}
