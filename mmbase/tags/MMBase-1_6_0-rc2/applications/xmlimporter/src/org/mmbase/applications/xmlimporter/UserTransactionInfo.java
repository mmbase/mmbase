/*
 * ClassName: UserTransactionInfo.java
 *
 * Date: dec. 1st. 2001
 *
 * Copyright notice:
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative.
 *
 * The license (Mozilla version 1.0) can be read at the MMBase site.
 * See http://www.MMBase.org/license
 */

package org.mmbase.applications.xmlimporter;

import java.util.Hashtable;

/**
 * Container class which contains a collection with all transactions for a user.
 *
 * @author Rob van Maris: Finalist IT Group
 * @since MMBase-1.5
 * @version $Id: UserTransactionInfo.java,v 1.2 2002-02-27 16:54:28 pierre Exp $
 */
public class UserTransactionInfo {

    /** Creates new UserTransactionInfo object.
     */
    public UserTransactionInfo() {
    }

    /** All known transactions of a user, mapped by the user-provided id. */
    public Hashtable knownTransactionContexts = new Hashtable();

    /** The user. */
    public User user = null;
}