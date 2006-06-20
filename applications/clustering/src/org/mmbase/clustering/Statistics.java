/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.clustering;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A structute for basic statistical information about receiving an sending messages in the cluster.
 * @version $Id: Statistics.java,v 1.1 2006-06-20 17:30:45 michiel Exp $
 */
public class Statistics {

    public long count         = 0;
    public long cost          = 0; // ms
    public long parseCost     = 0; //ms
    public long bytes         = 0;

    // get methods to make them available to EL. (java is a bit silly..)
    public long getCount() {
        return count;
    }
    public long getCost() {
        return cost;
    }
    public long getParseCost() {
        return parseCost;
    }
    public long getBytes() {
        return bytes;
    }
}

