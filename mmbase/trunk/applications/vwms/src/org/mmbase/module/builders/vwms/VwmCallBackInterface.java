/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders.vwms;

/**
 * Virtual Web Master Client interface.
 * This interface is emnt for classes that want to get notified when things change in a vwm.
 * The only method in this interface is a notification method used when a vwm 'unloads'.
 *
 * @author Pierre van Rooden (javadocs)
 * @version $Id: VwmCallBackInterface.java,v 1.5 2003-03-10 11:50:25 pierre Exp $
 */

public interface VwmCallBackInterface {

    /**
    * Callback routine, called when a vwm is unloaded (?).
    * @return <code>true</code> if successfully handled
    */
    public boolean vwmUnload();
}
