/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.util.*;
import org.mmbase.module.corebuilders.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.security.*;

/**
 * @javadoc
 * @author Rico Jansen
 * @version $Id: TransactionManager.java,v 1.27 2004-06-23 12:30:32 pierre Exp $
 */
public class TransactionManager implements TransactionManagerInterface {

    private static final Logger log = Logging.getLoggerInstance(TransactionManager.class);

    public static final String EXISTS_NO = "no";
    public static final int I_EXISTS_NO = 0;
    public static final String EXISTS_YES = "yes";
    public static final int I_EXISTS_YES = 1;
    public static final String EXISTS_NOLONGER = "nolonger";
    public static final int I_EXISTS_NOLONGER = 2;

    private TemporaryNodeManagerInterface tmpNodeManager;
    private MMBaseCop mmbaseCop = null;
    private MMBase mmbase;
    protected Map transactions = new HashMap();
    protected TransactionResolver transactionResolver;

    public TransactionManager(MMBase mmbase,TemporaryNodeManagerInterface tmpn) {
        this.mmbase = mmbase;
        this.tmpNodeManager = tmpn;
        transactionResolver = new TransactionResolver(mmbase);
        mmbaseCop = mmbase.getMMBaseCop();
    }

    synchronized protected Collection getTransaction(String transactionName) throws TransactionManagerException {
        Collection transaction = (Collection)transactions.get(transactionName);
        if (transaction == null) {
            throw new TransactionManagerException("Transaction "+transactionName+" does not exist");
        } else {
            return transaction;
        }
    }

    synchronized protected Collection createTransaction(String transactionName) throws TransactionManagerException {
        if (!transactions.containsKey(transactionName)) {
            Collection transactionNodes = new Vector();
            transactions.put(transactionName,transactionNodes);
            return transactionNodes;
        } else {
            throw new TransactionManagerException("Transaction "+transactionName+" already exists");
        }
    }

    synchronized protected Collection deleteTransaction(String transactionName) {
        return (Collection) transactions.remove(transactionName);
    }

    /**
     * @deprecated use get()
     */
    public Vector getNodes(Object user, String transactionName) {
        try {
            return (Vector)getTransaction(transactionName);
        } catch (TransactionManagerException tme) {
            return null;
        }
    }

    public String create(Object user, String transactionName) throws TransactionManagerException {
        createTransaction(transactionName);
        if (log.isDebugEnabled()) {
            log.debug("Create transaction for " + transactionName);
        }
        return transactionName;
    }

    public Collection get(Object user, String transactionName) throws TransactionManagerException {
        return getTransaction(transactionName);
    }

    public String addNode(String transactionName, String owner, String tmpnumber)
        throws TransactionManagerException {
        Collection transaction = getTransaction(transactionName);
        MMObjectNode node = tmpNodeManager.getNode(owner,tmpnumber);
        if (node != null) {
            if (!transaction.contains(node)) {
                transaction.add(node);
//            } else {
//                throw new TransactionManagerException(
//                    "Node " + tmpnumber + " not added as it was already in transaction " + transactionName);
            }
        } else {
            throw new TransactionManagerException("Node " + tmpnumber + " doesn't exist.");
        }
        return tmpnumber;
    }

    public String removeNode(String transactionName, String owner, String tmpnumber)
        throws TransactionManagerException {
        Collection transaction = getTransaction(transactionName);
        MMObjectNode node = tmpNodeManager.getNode(owner, tmpnumber);
        if (node!=null) {
            if (transaction.contains(node)) {
                transaction.remove(node);
//            } else {
//                throw new TransactionManagerException("Node " + tmpnumber + " is not in transaction " + transactionName);
            }
        } else {
            throw new TransactionManagerException("Node " + tmpnumber + " doesn't exist.");
        }
        return tmpnumber;
    }

    public String deleteObject(String transactionName, String owner, String tmpnumber)
        throws TransactionManagerException    {
        Collection transaction = getTransaction(transactionName);
        MMObjectNode node = tmpNodeManager.getNode(owner, tmpnumber);
        if (node!=null) {
            if (transaction.contains(node)) {
                // Mark it as deleted
                node.setValue("_exists",EXISTS_NOLONGER);
             } else {
                throw new TransactionManagerException("Node " + tmpnumber + " is not in transaction " + transactionName);
            }
        } else {
            throw new TransactionManagerException("Node " + tmpnumber + " doesn't exist.");
        }
        return tmpnumber;
    }

    public String cancel(Object user, String transactionName)
            throws TransactionManagerException {
        Collection transaction = getTransaction(transactionName);
        // remove nodes from the temporary node cache
        MMObjectBuilder builder = mmbase.getTypeDef();
        for (Iterator i = transaction.iterator(); i.hasNext(); ) {
            MMObjectNode node=(MMObjectNode)i.next();
            builder.removeTmpNode(node.getStringValue("_number"));
        }
        deleteTransaction(transactionName);
        if (log.isDebugEnabled()) {
            log.debug("Removed transaction " + transactionName + "\n" + transaction);
        }
        return transactionName;
    }

    public String commit(Object user, String transactionName)
        throws TransactionManagerException {
        Collection transaction = getTransaction(transactionName);
        try {
            boolean resolved = transactionResolver.resolve(transaction);
            if (!resolved) {
                log.error("Can't resolve transaction " + transactionName);
                log.error("Nodes \n" + transaction);
                throw new TransactionManagerException("Can't resolve transaction " + transactionName);
            } else {
                resolved = performCommits(user, transaction);
                if (!resolved) {
                    log.error("Can't commit transaction " + transactionName);
                    log.error("Nodes \n" + transaction);
                    throw new TransactionManagerException("Can't commit transaction " + transactionName);
                }
            }
        } finally {
            // remove nodes from the temporary node cache
            MMObjectBuilder builder = mmbase.getTypeDef();
            for (Iterator i = transaction.iterator(); i.hasNext(); ) {
                MMObjectNode node=(MMObjectNode)i.next();
                builder.removeTmpNode(node.getStringValue("_number"));
            }
            deleteTransaction(transactionName);
        }
        return transactionName;
    }

    private final static int UNCOMMITED = 0;
    private final static int COMMITED = 1;
    private final static int FAILED = 2;
    private final static int NODE = 3;
    private final static int RELATION = 4;

    boolean performCommits(Object user, Collection nodes) {
        if (nodes == null || nodes.size() == 0) {
            log.warn("Empty list of nodes");
            return true;
        }

        MMObjectBuilder bul = mmbase.getMMObject("typedef");
        boolean okay = false;
        int[] nodestate = new int[nodes.size()];
        int[] nodeexist = new int[nodes.size()];
        String username = findUserName(user),exists;

        log.debug("Checking types and existence");

        int i = 0;
        for (Iterator nodeIterator = nodes.iterator(); nodeIterator.hasNext(); i++) {
            MMObjectNode node = (MMObjectNode)nodeIterator.next();
            // Nodes are uncommited by default
            nodestate[i] = UNCOMMITED;
            exists = node.getStringValue("_exists");
            if (exists == null) {
                throw new IllegalStateException("The _exists field does not exist on node "+node);
            } else if (exists.equals(EXISTS_NO)) {
                nodeexist[i]=I_EXISTS_NO;
            } else if (exists.equals(EXISTS_YES)) {
                nodeexist[i]=I_EXISTS_YES;
            } else if (exists.equals(EXISTS_NOLONGER)) {
                nodeexist[i]=I_EXISTS_NOLONGER;
            } else {
                throw new IllegalStateException("Invalid value for _exists on node "+node);
            }
        }

        log.debug("Commiting nodes");

        // First commit all the NODES
        i = 0;
        for (Iterator nodeIterator = nodes.iterator(); nodeIterator.hasNext(); i++) {
            MMObjectNode node = (MMObjectNode)nodeIterator.next();
            if (!(node.getBuilder() instanceof InsRel)) {
                if (nodeexist[i] == I_EXISTS_YES ) {
                    // use safe commit, which locks the node cache
                    boolean commitOK;
                    if (user instanceof UserContext) {
                        commitOK = node.commit((UserContext)user);
                    } else {
                        commitOK = node.parent.safeCommit(node);
                    }
                    if (commitOK) {
                        nodestate[i]=COMMITED;
                    } else {
                        nodestate[i]=FAILED;
                    }
                } else if (nodeexist[i] == I_EXISTS_NO ) {
                    int insertOK;
                    if (user instanceof UserContext) {
                        insertOK = node.insert((UserContext)user);
                    } else {
                        insertOK = node.parent.safeInsert(node, username);
                    }
                    if (insertOK > 0) {
                        nodestate[i] = COMMITED;
                    } else {
                        nodestate[i] = FAILED;
                        String message = "When this failed, it is possible that the creation of an insrel went right, which leads to a database inconsistency..  stop now.. (transaction 2.0: [rollback?])";
                        log.error(message);
                        throw new RuntimeException(message);
                    }
                }
            }
        }

        log.debug("Commiting relations");

        // Then commit all the RELATIONS
        i = 0;
        for (Iterator nodeIterator = nodes.iterator(); nodeIterator.hasNext(); i++) {
            MMObjectNode node = (MMObjectNode)nodeIterator.next();
            if (node.getBuilder() instanceof InsRel) {
                if (nodeexist[i] == I_EXISTS_YES ) {
                    boolean commitOK;
                    if (user instanceof UserContext) {
                        commitOK = node.commit((UserContext)user);
                    } else {
                        commitOK = node.parent.safeCommit(node);
                    }
                    if (commitOK) {
                        nodestate[i]=COMMITED;
                    } else {
                        nodestate[i]=FAILED;
                    }
                } else if (nodeexist[i] == I_EXISTS_NO ) {
                    int insertOK;
                    if (user instanceof UserContext) {
                        insertOK = node.insert((UserContext)user);
                    } else {
                        insertOK = node.parent.safeInsert(node, username);
                    }
                    if (insertOK > 0) {
                        nodestate[i] = COMMITED;
                    } else {
                        nodestate[i] = FAILED;
                        String message = "relation failed(transaction 2.0: [rollback?])";
                        log.error(message);
                    }
                }
            }
        }

        log.debug("Deleting relations");

        // Then commit all the RELATIONS that must be deleted
        i = 0;
        for (Iterator nodeIterator = nodes.iterator(); nodeIterator.hasNext(); i++) {
            MMObjectNode node = (MMObjectNode)nodeIterator.next();
            if (node.getBuilder() instanceof InsRel && nodeexist[i] == I_EXISTS_NOLONGER) {
                // no return information
                if (user instanceof UserContext) {
                    node.remove((UserContext)user);
                } else {
                    node.parent.removeNode(node);
                }
                nodestate[i]=COMMITED;
            }
        }

        log.debug("Deleting nodes");
        // Then commit all the NODES that must be deleted
        i = 0;
        for (Iterator nodeIterator = nodes.iterator(); nodeIterator.hasNext(); i++) {
            MMObjectNode node = (MMObjectNode)nodeIterator.next();
            if (!(node.getBuilder() instanceof InsRel) && (nodeexist[i] == I_EXISTS_NOLONGER)) {
                // no return information
                if (user instanceof UserContext) {
                    node.remove((UserContext)user);
                } else {
                    node.parent.removeNode(node);
                }
                nodestate[i]=COMMITED;
            }
        }

        // check for failures
        okay=true;
        i = 0;
        for (Iterator nodeIterator = nodes.iterator(); nodeIterator.hasNext(); i++) {
            MMObjectNode node = (MMObjectNode)nodeIterator.next();
            if (nodestate[i] == FAILED) {
                okay=false;
                log.error("Failed node "+node.toString());
            }
        }
        return okay;
    }

    public String findUserName(Object user) {
        if (user instanceof UserContext) {
            return ((UserContext)user).getIdentifier();
        } else {
            return "";
        }
    }

}
