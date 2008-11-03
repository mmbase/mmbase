/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.context;

import org.mmbase.security.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This UserContext class provides a storage for the authentication
 * and authorization, so that information can be shared.
 * This class is NOT a container class for client related stuff, altrough
 * this is possible.
 *
 * @author Eduard Witteveen
 * @version $Id: ContextUserContext.java,v 1.12 2008-11-03 17:35:00 michiel Exp $
 */
public class ContextUserContext extends BasicUser implements java.io.Serializable {
    private static final Logger   log = Logging.getLoggerInstance(ContextUserContext.class);
    private static final long serialVersionUID = 2L;

    private String  username;
    private Rank    rank;
    private long    key;
    /** The SecurityManager, who (eventually) created this instance */
    protected transient MMBaseCop manager;

    public ContextUserContext(String username, Rank rank, long key, MMBaseCop manager, String app) {
        super(app);
        this.rank = rank;
        this.username = username;
        this.key = key;
        this.manager = manager;
    }

    public String getIdentifier() {
        return username;
    }

    public String getOwnerField() {
        if (manager == null) {
            manager = org.mmbase.module.core.MMBase.getMMBase().getMMBaseCop();
        }
        Authorization auth = manager.getAuthorization();
        if (auth instanceof ContextAuthorization) {
            return ((ContextAuthorization)auth).getDefaultContext(this);
        } else {
            log.error("Authorization is not ContextAuxthorization but " + auth.getClass());
            return getIdentifier();
        }
    }

    public Rank getRank() {
        return rank;
    }

    long getKey() {
        return key;
    }


    public boolean equals(Object o) {
        if (o instanceof ContextUserContext) {
            ContextUserContext ou = (ContextUserContext) o;
            return super.equals(o) && key == ou.key;
        } else {
            return false;
        }
    }


}
