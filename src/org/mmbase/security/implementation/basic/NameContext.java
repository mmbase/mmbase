/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.security.implementation.basic;

import org.mmbase.security.Rank;
import org.mmbase.security.UserContext;
import org.mmbase.security.SecurityException;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A UserContext implementation based only on user name, which serves as the identifier for the
 * user.
 * 
 * @author Eduard Witteveen
 * @version $Id: NameContext.java,v 1.4 2004-04-19 17:23:47 michiel Exp $
 */
public class NameContext extends UserContext {
    private static Logger log = Logging.getLoggerInstance(NameContext.class);

    private String identifier = null;
    private Rank   rank       = null;

    public NameContext(Rank rank) {
        this.rank = rank;
    }

    public String getIdentifier() {
        if(identifier == null) {
            throw new SecurityException("No user name was set by the security implementation. This is required.");
        }
        return identifier;
    }

    public Rank getRank() {
        if(rank == null) {
            throw new SecurityException("No rank was provider by the security implementation. This is required.");
        }
        return rank;
    }

    /**
     * @since MMBase-1.8
     */
    void setRank(Rank r) {
        rank = r;
    }

    void setIdentifier(String ident) {
        this.identifier = ident;
    }
}
