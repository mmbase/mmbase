/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext;

import java.util.Set;
import org.mmbase.security.implementation.cloudcontext.builders.*;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.security.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * @javadoc
 * @author Eduard Witteveen
 * @author Pierre van Rooden
 * @version $Id: Verify.java,v 1.4 2003-06-17 09:32:18 michiel Exp $
 */
public class Verify extends Authorization {
    private static Logger    log = Logging.getLoggerInstance(Verify.class.getName());

    // javadoc inherited
    protected void load() {
    }

    // javadoc inherited
    public void create(UserContext userContext, int nodeId) {
        User user = (User) userContext;
        // odd, getOwnerField is called in BasicNodeManager yet, so I wonder when this is called.
        Contexts.getBuilder().setContext((User) user, nodeId, user.getOwnerField());
    }

    // javadoc inherited
    public void update(UserContext userContext, int nodeId)  {
    }


    // javadoc inherited
    public void remove(UserContext userContext, int nodeId)  {
    }

    // javadoc inherited
    public boolean check(UserContext userContext, int nodeId, Operation operation)  {
        return Contexts.getBuilder().mayDo((User) userContext, nodeId, operation);
    }

    // javadoc inherited
    public boolean check(UserContext userContext, int nodeId, int sourceNodeId, int destinationNodeId, Operation operation) {
        //log.debug("check if operation: " + operation + " is valid for: " + usercontext + " for node with number # " + i + "(between 2 nodes..)");        
        return Contexts.getBuilder().mayDo((User) userContext, nodeId, sourceNodeId, destinationNodeId, operation);
    }

    // javadoc inherited
    public String getContext(UserContext userContext, int nodeId) throws org.mmbase.security.SecurityException {
        //log.debug("check if we may read the node with # " + i + " nodeid?");
        return Contexts.getBuilder().getContext((User) userContext, nodeId);
    }


    // javadoc inherited
    public void setContext(UserContext userContext, int nodeId, String context) throws org.mmbase.security.SecurityException {
        //log.debug("[node #" + i + "] changed to context: " + s + " by [" + usercontext.getIdentifier() + "]");
        Contexts.getBuilder().setContext((User) userContext, nodeId, context);
    }

    // javadoc inherited
    public Set getPossibleContexts(UserContext userContext, int nodeId)  throws org.mmbase.security.SecurityException {
        return Contexts.getBuilder().getPossibleContexts((User) userContext, nodeId);
    }
}
