/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;

import junit.framework.*;
import junit.runner.BaseTestRunner;

import org.mmbase.module.core.*;
import org.mmbase.module.builders.*;


/**
 * TestSuite that runs all the bridge tests.
 *
 * @author Jaco de Groot
 */
public class AllTests {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        startMMBase();

        // Create the test suite
        TestSuite suite= new TestSuite("Bridge Tests");
        suite.addTestSuite(CloudContextTest.class);
        suite.addTestSuite(EmptyNodeTest.class);
        suite.addTestSuite(EmptyNodeTestTransaction.class);
        suite.addTestSuite(FilledNodeTest.class);
        suite.addTestSuite(FilledNodeTestTransaction.class);
        suite.addTestSuite(NodeManagerTest.class);
        suite.addTestSuite(CloudTest.class);
        return suite;
    }
	
}
