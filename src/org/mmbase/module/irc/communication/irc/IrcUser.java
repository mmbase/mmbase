/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.module.irc.communication.irc;

import	org.mmbase.module.irc.communication.*;
import	org.mmbase.module.irc.communication.irc.*;

/**
 * Class IrcUser
 *
 * @obsolete
 * @deprecated use IrcConnection instead
 * @author vpro
 */

public class IrcUser extends IrcConnection implements CommunicationInterface {
    /**
     * @javadoc
     */
    public IrcUser( CommunicationUserInterface com )
    {
        super( com );
    }
}
