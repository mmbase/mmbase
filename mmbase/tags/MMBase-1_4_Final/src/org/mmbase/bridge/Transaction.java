/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;

/**
 * A transaction is an environment that allows for the editing of nodes
 * within a 'safe' context. Either all edits in a transaction are comitted, or
 * all fail. A transaction acts as a cloud. All that can be done in a cloud can
 * be done in a transaction.
 * For example a node retrieved using the transaction's getNode method resides
 * in the transaction, if you change or remove the node, you can later roll it
 * back by calling the transaction's cancel method.
 *
 * @author Pierre van Rooden
 */
public interface Transaction extends Cloud {

  	/**
     * Commits this transaction. This has no effect if the transaction itself
     * was 'nested' in another transaction. In that case, nothing happens until
     * the 'outer' transaction commits. This routine also removes the
     * transaction as an 'active' transaction (it cannot be opened again).
     * 
     * @return <code>true</code> if the commit succeeded, <code>false</code>
     *         otherwise
     */
    public boolean commit();

  	/**
     * Cancels this transaction. If the transaction itself was 'nested' in
     * another transaction, that 'outer' transaction is also canceled.
     * This routine also removes the transaction (and all outer transactions)
     * as an 'active' transaction (it cannot be opened again).
     */
    public void cancel();

}
