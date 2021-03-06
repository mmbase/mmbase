/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security;

import org.mmbase.util.functions.Parameters;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.logging.*;
/**
 * A piece of 'action check' functionality. Provided by actions themselves, but security
 * implementations can perhaps also use this interface to administer their implementation of {@link
 * Authorization#check(UserContext, Action, Parameters)}.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.9
 */
public interface ActionChecker extends java.io.Serializable {

    boolean check(UserContext user, Action ac, Parameters parameters);

    Parameter[] getParameterDefinition();


    /**
     * The ActionChecker that always allows every action to to everybody.
     * @since MMBase-1.9.2
     */
    public static final ActionChecker ALLOWS = new ActionChecker() {
            private static final long serialVersionUID = 1L;
            public boolean check(UserContext user, Action ac, Parameters parameters) {
                return true;
            }
            public Parameter[] getParameterDefinition() {
                return Parameter.EMPTY;
            }
            @Override
            public String toString() {
                return "allows";
            }
        };

    /**
     * This basic implementation of ActionChecker checks the action only based on rank. A minimal
     * rank is to be supplied in the constructor.
     */

    public static class Rank implements  ActionChecker {
        private static final long serialVersionUID = 7047822780810829661L;
        private static final Logger log = Logging.getLoggerInstance(Rank.class);
        final String rank;
        public Rank(org.mmbase.security.Rank r) {
            rank = r.toString();
        }
        /**
         * @since MMBase-1.9.6
         */
        public Rank(String r) {
            rank = r;
        }
        protected org.mmbase.security.Rank getRank() {
            org.mmbase.security.Rank r = org.mmbase.security.Rank.getRank(rank);
            if (r == null) {
                log.error("No such rank " + rank + " returning " + org.mmbase.security.Rank.ADMIN);
                return org.mmbase.security.Rank.ADMIN;
            }
            return r;
        }
        public boolean check(UserContext user, Action ac, Parameters parameters) {
            return user.getRank().getInt() >= getRank().getInt();
        }
        public Parameter[] getParameterDefinition() {
            return Parameter.EMPTY;
        }
        @Override
        public String toString() {
            return "at least " + rank + (org.mmbase.security.Rank.getRank(rank) == null ? "(rank doesn't exist)" : "");
        }
    }
}
