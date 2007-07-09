/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;

/**
 * A list of modules
 *
 * @author Pierre van Rooden
 * @version $Id: ModuleList.java,v 1.7 2006-09-25 10:17:36 pierre Exp $
 */
public interface ModuleList<E extends Module> extends BridgeList<E> {

    /**
     * Returns the Module at the indicated postion in the list
     * @param index the position of the Module to retrieve
     * @return Module at the indicated postion
     */
    public Module getModule(int index);

    /**
     * Returns an type-specific iterator for this list.
     * @return Module iterator
     */
    public ModuleIterator moduleIterator();

}
