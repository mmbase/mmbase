/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.implementation;

import java.util.Collection;
import org.mmbase.bridge.*;
import org.mmbase.util.logging.*;

/**
 * A list of Modules
 *
 * @author Pierre van Rooden
 * @version $Id: BasicModuleList.java,v 1.10 2003-03-21 17:45:06 michiel Exp $
 */
public class BasicModuleList extends BasicList implements ModuleList {
    private static Logger log = Logging.getLoggerInstance(BasicModuleList.class.getName());

    BasicModuleList() {
        super();
    }

    /**
     * ...
     */
    BasicModuleList(Collection c) {
        super(c);
    }

    protected Object validate(Object o) throws ClassCastException {
        return (Module)o;
    }

    /**
     *
     */
    public Module getModule(int index) {
        return (Module)get(index);
    }

    /**
     *
     */
    public ModuleIterator moduleIterator() {
        return new BasicModuleIterator();
    };

    protected class BasicModuleIterator extends BasicIterator implements ModuleIterator {

        public Module nextModule() {
            return (Module)next();
        }

        public Module previousModule() {
            return (Module)previous();
        }

    }

}
