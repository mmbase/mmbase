/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;

/**
 * This User interface defines the storage for the authentication
 * and authorization, so that information can be shared.
 * This interface is NOT a container class for client related stuff, altrough
 * this is possible. Notice that after the login on the cloud it is not
 * certain that you will receive the same User interface back !
 *
 * @author Eduard Witteveen
 * @version $Id: User.java,v 1.7 2003-08-18 09:16:41 michiel Exp $
 */
public interface User {

    /**
     *  Get the unique identifier for this user. This should be unique
     *  for every different user inside a cloud.
     *	@return an unique id for the current user
     */
    public String getIdentifier();

    /**
     *  Get the rank of this user.
     *	@return the rank of this user
     */
    public String getRank();

    /**
     *  Is valid
     *	@return <code>true</code> if the user is still valid.
     *      	<code>false</code> if the user is expired..
     */
    public boolean isValid();


    /**
     * @since MMBase-1.7
     */
    public String getOwnerField();

}
