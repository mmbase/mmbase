/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.xmlimporter;

/**
 * Dummy User object, this object needs to be replace by
 * the real User object (when that is finished).
 *
 * @author Rob van Maris: Finalist IT Group
 * @since MMBase-1.5
 * @version $Id: User.java,v 1.3 2003-03-07 08:50:03 pierre Exp $
 */
public class User {

    /** User name. */
    private String name;

    /**
     * Creates new User object.
     * @param name The user name.
     */
    public User(String name) {
        this.name= name;
    }

    /**
     * Provides name to be used for transactions.
     * @return Name, based on last 8 characters of original name.
     */
    String getName() {
        int length = name.length();
        String tempname = "TR"+ name.substring(length-8,length);
        return tempname;
    }
}