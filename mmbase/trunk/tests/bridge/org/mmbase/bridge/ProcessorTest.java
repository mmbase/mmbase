/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;

import org.mmbase.tests.*;
import org.mmbase.datatypes.processors.*;
import junit.framework.*;

import org.mmbase.util.DynamicDate;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 *
 * @author Michiel Meeuwissen
 * @version $Id: ProcessorTest.java,v 1.4 2008-12-02 08:06:28 michiel Exp $
 * @since MMBase-1.9.1
  */
public class ProcessorTest extends BridgeTest {
    private static final Logger log = Logging.getLoggerInstance(TransactionTest.class);

    public ProcessorTest(String name) {
        super(name);
    }

    protected Node testCommitProcessorIsChanged1(Cloud c) {
        NodeManager nm = c.getNodeManager("mustbechanged");
        Node n = nm.createNode();
        n.setStringValue("string", "bla");
        n.commit();
        return n;
    }

    protected void testCommitProcessorIsChanged2(Cloud c, int nn) {
        Node n = c.getNode(nn);
        n.setStringValue("string", "blie");
        n.commit();
    }
    protected void testCommitProcessorIsChanged3(Cloud c, int nn) {
        Node n = c.getNode(nn);
        try {
            n.commit();
            throw new AssertionFailedError("Should have thrown exception");
        } catch(RuntimeException ru) {
            // ok
        }
    }


    public void testCommitProcessorIsChanged() {
        Cloud c = getCloud();
        int nn  = testCommitProcessorIsChanged1(c).getNumber();
        testCommitProcessorIsChanged2(c, nn);
        testCommitProcessorIsChanged3(c, nn);
    }
    public void testCommitProcessorIsChangedTransaction() {
        Cloud c = getCloud();
        Transaction t = c.getTransaction("aa");
        Node n  = testCommitProcessorIsChanged1(t);
        t.commit();
        int nn = n.getNumber();
        t = c.getTransaction("bb");
        testCommitProcessorIsChanged2(t, nn);
        t.commit();
        t = c.getTransaction("cc");
        testCommitProcessorIsChanged3(t, nn);
        t.commit();

    }

    protected Node testAge(Cloud c) {
        NodeManager nm = c.getNodeManager("datatypes");
        Node n = nm.createNode();
        n.setDateValue("birthdate", DynamicDate.eval("2008-01-01"));
        n.commit();
        n = c.getNode(n.getNumber());
        assertEquals(DynamicDate.eval("2008-01-01"), n.getDateValue("birthdate"));
        n.setIntValue("age", 10);
        assertEquals(10, n.getIntValue("age"));
        n.commit();
        assertEquals(10, n.getIntValue("age"));
        return n;
    }

    public void testAge() {
        org.mmbase.cache.CacheManager.getInstance().disable(".*");
        testAge(getCloud());
        org.mmbase.cache.CacheManager.getInstance().readConfiguration();
    }

    public void testAgeTransaction() {
        org.mmbase.cache.CacheManager.getInstance().disable(".*");
        Transaction t = getCloud().getTransaction("bla");
        Node n = testAge(t);
        t.commit();
        assertEquals(10, getCloud().getNode(n.getNumber()).getIntValue("age"));
        org.mmbase.cache.CacheManager.getInstance().readConfiguration();
    }


    protected int testCommitCount(Cloud c) {
        NodeManager nm = c.getNodeManager("datatypes");
        int ccbefore = CountCommitProcessor.count;
        Node n = nm.createNode();
        n.commit();
        if (c.getCloudContext().getUri().equals(ContextProvider.DEFAULT_CLOUD_CONTEXT_NAME)) {
            assertEquals(ccbefore + 1, CountCommitProcessor.count);
        }
        return n.getNumber();
    }

    public void testCommitCount() {
        testCommitCount(getCloud());
    }

    public void testCommitCountTransaction() {
        int ccbefore = CountCommitProcessor.count;
        Transaction t = getCloud().getTransaction("commitcount");
        testCommitCount(t);
        t.commit();
        if (getCloudContext().getUri().equals(ContextProvider.DEFAULT_CLOUD_CONTEXT_NAME)) {
            // there is no point in calling a commit processor twice
            assertEquals(ccbefore + 1, CountCommitProcessor.count);
        }
    }
    static int nn = 0;
    public void testCommitCountTransaction2() {
        Transaction t = getCloud().getTransaction("commitcount2");
        int ccbefore = CountCommitProcessor.count;
        NodeManager nm = t.getNodeManager("datatypes");
        Node n = nm.createNode();
        // not committing node
        t.commit(); // but only the transaction.

        nn = n.getNumber();
        if (getCloudContext().getUri().equals(ContextProvider.DEFAULT_CLOUD_CONTEXT_NAME)) {
            // commit processor must have been called.
            assertEquals(ccbefore + 1, CountCommitProcessor.count);
        }
    }

    protected void testCommitCountOnChange(Cloud c, int nn) {
        int ccbefore = CountCommitProcessor.count;
        int changedbefore = CountCommitProcessor.changed;
        Node n = c.getNode(nn);
        n.commit();
        if (getCloudContext().getUri().equals(ContextProvider.DEFAULT_CLOUD_CONTEXT_NAME)) {
            assertEquals(ccbefore + 1, CountCommitProcessor.count);
            assertEquals(changedbefore, CountCommitProcessor.changed);
        }
    }
    protected void testCommitCountOnChange2(Cloud c, int nn) {
        int ccbefore = CountCommitProcessor.count;
        int changedbefore = CountCommitProcessor.changed;
        Node n = c.getNode(nn);
        n.setStringValue("string", "foobar");
        n.commit();
        if (getCloudContext().getUri().equals(ContextProvider.DEFAULT_CLOUD_CONTEXT_NAME)) {
            assertEquals(ccbefore + 1, CountCommitProcessor.count);
            assertEquals(changedbefore + 1, CountCommitProcessor.changed);
        }
    }

    public void testCommitCountOnChange() {
        testCommitCountOnChange(getCloud(), nn);
        testCommitCountOnChange2(getCloud(), nn);
    }

    public void testCommitCountOnChangeTransaction() {
        testCommitCountOnChange(getCloud().getTransaction("commitcount3"), nn);
        testCommitCountOnChange2(getCloud().getTransaction("commitcount4"), nn);
    }




}
