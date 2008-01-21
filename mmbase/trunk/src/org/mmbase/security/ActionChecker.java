/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security;

import org.mmbase.bridge.Node;
import org.mmbase.util.functions.Parameters;
/**
 * A piece of 'action check' functionality. Provided by actions themselves, but security
 * implementations can perhaps also use this interface to administer their implementation of {@link
 * Authorization#check(UserContext, Action)}.
 *
 * @author Michiel Meeuwissen
 * @version $Id: ActionChecker.java,v 1.4 2008-01-21 17:28:15 michiel Exp $
 * @since MMBase-1.9
 */
public interface ActionChecker extends java.io.Serializable {

    boolean check(UserContext user, Action ac, Parameters parameters);

    /**
     * This basic implementation of ActionChecker checks the action only based on rank. A minimal
     * rank is to be supplied in the constructor.
     */

    public static class Rank implements  ActionChecker {
        final org.mmbase.security.Rank rank;
        public Rank(org.mmbase.security.Rank r) {
            rank = r;
        }
        public boolean check(UserContext user, Action ac, Parameters parameters) {
            return user.getRank().getInt() >= rank.getInt();
        }
        public String toString() {
            return "at least " + rank;
        }
    }
}
