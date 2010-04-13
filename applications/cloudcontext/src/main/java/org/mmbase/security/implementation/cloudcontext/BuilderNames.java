/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext;

import org.mmbase.module.core.NodeSearchQuery;
import java.util.*;

/**
 * Wraps a collection of NodeSearchQuery's to be a collection of builder names.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * MMBase-1.9.1
 */
public class BuilderNames extends AbstractCollection<String> {

    private final Collection<NodeSearchQuery> backing;
    public BuilderNames(Collection<NodeSearchQuery> b) {
        backing = b;

    }
    public int size() {
        return backing.size();
    }
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            Iterator<NodeSearchQuery> i = BuilderNames.this.backing.iterator();
            public String next() {
                NodeSearchQuery q = i.next();
                return q == null ? null : q.getBuilder().getTableName();
            }
            public boolean hasNext() {
                return i.hasNext();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
