/*
 * ClassName: SimilarObjectFinder.java
 *
 * Date: dec. 1st. 2001
 *
 * Copyright notice: 
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative.
 *
 * The license (Mozilla version 1.0) can be read at the MMBase site.
 * See http://www.MMBase.org/license
 */

package org.mmbase.applications.xmlimporter;

import java.util.*;
import org.mmbase.applications.xmlimporter.*;

/**
 * This interface provides methods to customize the way similar
 * objects are searched in both the persistent and temporary cloud.
 *
 * @author Rob van Maris: Finalist IT Group
 *
 * @version 1.0
 */
public interface SimilarObjectFinder {

    /**
     * Initialize this instance (called once per transaction).
     * @param params The initialization parameters, provided as 
     *  name/value pairs (both String).
     * @throws TransactionHandlerException if a failure occurred.
     */
    public void init(HashMap params) throws TransactionHandlerException;
    
    /**
     * Searches for similar object. Objects found in the
     * persistent cloud will be accessed in the transaction.
     * @return List of the similar objects found.
     * @param transaction The transaction where the tmpObj belongs to.
     * @param tmpObj The object to search for.
     * @throws TransactionHandlerException If a failure occurred.
     */
    public List findSimilarObject(Transaction transaction, TmpObject tmpObj)
    throws TransactionHandlerException;
    
}

