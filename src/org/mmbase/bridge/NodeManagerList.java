/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;

import java.util.List;

/**
 * A list of node managers
 *
 * @author Pierre van Rooden
 * @version $Id: NodeManagerList.java,v 1.4 2002-09-23 14:31:02 pierre Exp $
 */
public interface NodeManagerList extends BridgeList {

    /**
     * Returns the NodeManager at the indicated postion in the list
     * @param index the position of the NodeManager to retrieve
     */
    public NodeManager getNodeManager(int index);

    /**
     * Returns an type-specific iterator for this list.
     */
    public NodeManagerIterator nodeManagerIterator();

}
