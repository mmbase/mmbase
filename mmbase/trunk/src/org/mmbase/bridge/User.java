/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;

/**
 *  This User interface defines the storage for the authentication
 *  and authorization, so that information can be shared.
 *  This interface is NOT a container class for client related stuff, altrough 
 *  this is possible. Notice that after the login on the cloud it is not 
 *  certain that you will receive the same User interface back !
 */
public interface User {

    /**
     *  Get the unique identifier for this user. This should be unique 
     *  for every different user inside a cloud.
     *	@return     a unique identifier for this user.
     */
    public String getIdentifier();
    
    /**
     *  Get the rank of this user. 
     */
    public org.mmbase.security.Rank getRank();
}
