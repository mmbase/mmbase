package org.mmbase.security.basic;

import org.mmbase.security.UserContext;
import org.mmbase.security.Operation;

import org.mmbase.module.core.MMObjectNode;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


public class Authorization extends org.mmbase.security.Authorization {
    private static Logger log=Logging.getLoggerInstance(Authorization.class.getName());

    private static org.mmbase.module.core.MMObjectBuilder builder = null;

    private MMObjectNode getMMNode(int n) {
        if(builder == null) {
            org.mmbase.module.core.MMBase mmb = (org.mmbase.module.core.MMBase)org.mmbase.module.Module.getModule("MMBASEROOT");
            builder =  mmb.getMMObject("typedef");
            if(builder == null) throw new org.mmbase.security.SecurityException("builder not found");
        }
        MMObjectNode node = builder.getNode(n);
        if(node == null) throw new org.mmbase.security.SecurityException("node not found");
        return node;
    }

    protected void load() {
    }

    public void create(UserContext user, int nodeNumber) {
        if(manager.getActive()) {
            MMObjectNode node = getMMNode(nodeNumber);
            node.setValue("owner", user.get("username"));
            node.commit();
        }
    }

    public void update(UserContext user, int nodeNumber) {
        if(manager.getActive()) {
            MMObjectNode node = getMMNode(nodeNumber);
            node.setValue("owner", user.get("username"));
            node.commit();
        }
    }

    public void remove(UserContext user, int node) {
    }

    public boolean check(UserContext user, int nodeNumber, Operation operation) {
	log.debug("checking user: " + user.get("username") + " operation: " + operation.getInt() + " node: " + nodeNumber);
        boolean permitted = true;
        if(manager.getActive()) {
            switch(operation.getInt()) {
                case Operation.CREATE_INT:
                    // say we may always create, if we are authenticated.
                    permitted = true;
		    if (permitted) log.debug("user was logged in thus could create new node");
		    else log.debug("user was not logged in thus could NOT create new node");
                    break;
                case Operation.LINK_INT:
                    // nah, we always except links from other nodes.....
                    permitted = true;
                    break;
                case Operation.READ_INT:
                    // nah, we may always view other nodes.,....
                    permitted = true;
                    break;
                case Operation.REMOVE_INT:
                    // same rights as writing, no break
                case Operation.WRITE_INT:
                    // dont think so when we are anonymous...
                    // we are logged in, check if we may edit this node,....
                    MMObjectNode node = getMMNode(nodeNumber);
                    String ownerName = node.getStringValue("owner");
                    if(ownerName.equals("bridge")) {
                        // was created by the bridge, we can take this one....
                        permitted = true;
                    }
                    else {
                        log.debug("owner of checking field is:'"+ownerName+"' and user is '"+user.get("username")+"'");
                        permitted = (node.getStringValue("owner").compareTo(user.get("username")) == 0);
                    }
                    
                    break;
                default:
                    throw new org.mmbase.security.SecurityException("Operation was NOT permitted...");
            }
        }
        if(permitted) log.debug("operation was permitted");
        else log.info(" user: " + user.get("username") + " operation: " + operation.getInt() + " node: " + nodeNumber  + "   operation was NOT permitted");
        return permitted;
    }

    public void assert(UserContext user, int node, Operation operation) throws org.mmbase.security.SecurityException
    {
        // hmm, we can use check :)
        if(manager.getActive()){
            if (!check(user,node,operation)) throw new org.mmbase.security.SecurityException("Operation was NOT permitted...");
        }
    }
}
