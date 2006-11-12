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
 * The MMBase transaction manager manages a group of changes.
 * @javadoc
 *
 * @author Rico Jansen
 * @version $Id: TransactionManager.java,v 1.38 2006-11-12 09:31:46 michiel Exp $
 */
public class TransactionManager {

    private static final Logger log = Logging.getLoggerInstance(TransactionManager.class);

    public static final String EXISTS_NO = "no";
    public static final int I_EXISTS_NO = 0;
    public static final String EXISTS_YES = "yes";
    public static final int I_EXISTS_YES = 1;
    public static final String EXISTS_NOLONGER = "nolonger";
    public static final int I_EXISTS_NOLONGER = 2;

    private TemporaryNodeManager tmpNodeManager;
    private TransactionResolver transactionResolver;

    protected Map<String, Collection<MMObjectNode>> transactions = new HashMap<String, Collection<MMObjectNode>>(); 

    public static TransactionManager instance;

    /**
     * @since MMBase-1.9
     */
    public static TransactionManager getInstance() {
        if (instance == null) {
            instance = new TransactionManager();
        }
        return instance;
    }


    private TransactionManager() {
    }

    public TemporaryNodeManager getTemporaryNodeManager() {
        if (tmpNodeManager == null) {
            tmpNodeManager = new TemporaryNodeManager(MMBase.getMMBase());
        }
        return tmpNodeManager;

    }
    private TransactionResolver getTransactionResolver() {
        if (transactionResolver == null) {
            transactionResolver = new TransactionResolver(MMBase.getMMBase());
        }
        return transactionResolver;
    }

    /**
     * Returns transaction with given name.
     * @param transactionName The name of the transaction to return
     * @exception TransactionManagerExcpeption if the transaction with given name does not exist
     * @return Collection containing the nodes in this transaction (as {@link org.mmbase.module.core.MMObjectNode}s).
     */
    synchronized public  Collection<MMObjectNode> getTransaction(String transactionName) throws TransactionManagerException {
        Collection<MMObjectNode> transaction = transactions.get(transactionName);
        if (transaction == null) {
            throw new TransactionManagerException("Transaction " + transactionName + " does not exist (existing are " + transactions.keySet() + ")");
        } else {
            return transaction;
        }
    }

    /**
     * Return a Set of the names of all created transactions.
     *
     * @since MMBase-1.9
     */
    public Set<String> getTransactions() {
        return Collections.unmodifiableSet(transactions.keySet());
    }

    /**
     * Creates transaction with given name.
     * @param transactionName The name of the transaction to return
     * @exception TransactionManagerExcpeption if the transaction with given name existed already
     * @return Collection containing the nodes in this transaction (so, this is an empty collection)
     */
    synchronized public Collection<MMObjectNode> createTransaction(String transactionName) throws TransactionManagerException {
        if (!transactions.containsKey(transactionName)) {
            List<MMObjectNode> transactionNodes = new ArrayList();
            transactions.put(transactionName, transactionNodes);
            return transactionNodes;
        } else {
            throw new TransactionManagerException("Transaction " + transactionName + " already exists");
        }
    }

    /**
     * Removes the transaction with given name
     * @return the collection with nodes from the removed transaction or <code>null</code> if no transaction with this name existed
     */
    synchronized protected Collection<MMObjectNode> deleteTransaction(String transactionName) {
        return transactions.remove(transactionName);
    }


    public String addNode(String transactionName, String owner, String tmpnumber) throws TransactionManagerException {
        Collection<MMObjectNode> transaction = getTransaction(transactionName);
        MMObjectNode node = getTemporaryNodeManager().getNode(owner, tmpnumber);
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

    public String removeNode(String transactionName, String owner, String tmpnumber) throws TransactionManagerException {
        Collection<MMObjectNode> transaction = getTransaction(transactionName);
        MMObjectNode node = getTemporaryNodeManager().getNode(owner, tmpnumber);
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

    public String deleteObject(String transactionName, String owner, String tmpnumber) throws TransactionManagerException    {
        Collection<MMObjectNode> transaction = getTransaction(transactionName);
        MMObjectNode node = getTemporaryNodeManager().getNode(owner, tmpnumber);
        if (node != null) {
            if (transaction.contains(node)) {
                // Mark it as deleted
                node.storeValue(MMObjectBuilder.TMP_FIELD_EXISTS, EXISTS_NOLONGER);
             } else {
                throw new TransactionManagerException("Node " + tmpnumber + " is not in transaction " + transactionName);
            }
        } else {
            throw new TransactionManagerException("Node " + tmpnumber + " doesn't exist.");
        }
        return tmpnumber;
    }

    public String cancel(Object user, String transactionName) throws TransactionManagerException {
        Collection<MMObjectNode> transaction = getTransaction(transactionName);
        // remove nodes from the temporary node cache
        MMObjectBuilder builder = MMBase.getMMBase().getTypeDef();
        for (MMObjectNode node : transaction) {
            builder.removeTmpNode(node.getStringValue(MMObjectBuilder.TMP_FIELD_NUMBER));
        }
        deleteTransaction(transactionName);
        if (log.isDebugEnabled()) {
            log.debug("Removed transaction (after cancel) " + transactionName + "\n" + transaction);
        }
        return transactionName;
    }

    public String commit(Object user, String transactionName) throws TransactionManagerException {
        Collection<MMObjectNode> transaction = getTransaction(transactionName);
        try {
            boolean resolved = getTransactionResolver().resolve(transaction);
            if (!resolved) {
                log.error("Can't resolve transaction " + transactionName);
                log.error("Nodes \n" + transaction);
                throw new TransactionManagerException("Can't resolve transaction " + transactionName + "" + transaction);
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
            MMObjectBuilder builder = MMBase.getMMBase().getTypeDef();
            for (MMObjectNode node : transaction) {
                builder.removeTmpNode(node.getStringValue(MMObjectBuilder.TMP_FIELD_NUMBER));
            }
            deleteTransaction(transactionName);
            if (log.isDebugEnabled()) {
                log.debug("Removed transaction (after commit) " + transactionName + "\n" + transaction);
            }
        }
        return transactionName;
    }

    private final static int UNCOMMITED = 0;
    private final static int COMMITED = 1;
    private final static int FAILED = 2;
    private final static int NODE = 3;
    private final static int RELATION = 4;

    boolean performCommits(Object user, Collection<MMObjectNode> nodes) {
        if (nodes == null || nodes.size() == 0) {
            log.warn("Empty list of nodes");
            return true;
        }

        int[] nodestate = new int[nodes.size()];
        int[] nodeexist = new int[nodes.size()];
        String username = findUserName(user);

        log.debug("Checking types and existance");

        int i = -1;
        for (MMObjectNode node : nodes) {
            i++;
            // Nodes are uncommited by default
            nodestate[i] = UNCOMMITED;
            String exists = node.getStringValue(MMObjectBuilder.TMP_FIELD_EXISTS);
            if (exists == null) {
                throw new IllegalStateException("The _exists field does not exist on node "+node);
            } else if (exists.equals(EXISTS_NO)) {
                nodeexist[i] = I_EXISTS_NO;
            } else if (exists.equals(EXISTS_YES)) {
                nodeexist[i] = I_EXISTS_YES;
            } else if (exists.equals(EXISTS_NOLONGER)) {
                nodeexist[i] = I_EXISTS_NOLONGER;
            } else {
                throw new IllegalStateException("Invalid value for _exists on node "+node);
            }
        }

        log.debug("Commiting nodes");

        i = -1;
        // First commit all the NODES
        for (MMObjectNode node : nodes) {
            i++;
            if (!(node.getBuilder() instanceof InsRel)) {
                if (nodeexist[i] == I_EXISTS_YES ) {
                    if (! node.isChanged()) continue;
                    // use safe commit, which locks the node cache
                    boolean commitOK;
                    if (user instanceof UserContext) {
                        commitOK = node.commit((UserContext)user);
                    } else {
                        commitOK = node.parent.safeCommit(node);
                    }
                    if (commitOK) {
                        nodestate[i] = COMMITED;
                    } else {
                        nodestate[i] = FAILED;
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
                        throw new RuntimeException(message);
                    }
                }
            }
        }

        log.debug("Commiting relations");

        // Then commit all the RELATIONS
        i = -1;
        for (MMObjectNode node : nodes) {
            i++;
            if (node.getBuilder() instanceof InsRel) {
                // excactly the same code as 10 lines ago. Should be dispatched to some method..
                if (nodeexist[i] == I_EXISTS_YES ) {
                    if (! node.isChanged()) continue;
                    boolean commitOK;
                    if (user instanceof UserContext) {
                        commitOK = node.commit((UserContext)user);
                    } else {
                        commitOK = node.parent.safeCommit(node);
                    }
                    if (commitOK) {
                        nodestate[i] = COMMITED;
                    } else {
                        nodestate[i] = FAILED;
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
        i = -1;
        for (MMObjectNode node : nodes) {
            i++;
            if (node.getBuilder() instanceof InsRel && nodeexist[i] == I_EXISTS_NOLONGER) {
                // no return information
                if (user instanceof UserContext) {
                    node.remove((UserContext)user);
                } else {
                    node.parent.removeNode(node);
                }
                nodestate[i] = COMMITED;
            }
        }

        log.debug("Deleting nodes");
        // Then commit all the NODES that must be deleted
        i = -1;
        for (MMObjectNode node : nodes) {
            i++;
            if (!(node.getBuilder() instanceof InsRel) && (nodeexist[i] == I_EXISTS_NOLONGER)) {
                // no return information
                if (user instanceof UserContext) {
                    node.remove((UserContext)user);
                } else {
                    node.parent.removeNode(node);
                }
                nodestate[i] = COMMITED;
            }
        }

        // check for failures
        boolean okay=true;
        i = -1;
        for (MMObjectNode node : nodes) {
            i++;
            if (nodestate[i] == FAILED) {
                okay = false;
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
