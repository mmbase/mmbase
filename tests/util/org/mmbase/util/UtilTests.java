/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

import junit.framework.*;

/**
 * TestSuite that runs all the util junit tests.
 *
 * @author Jaco de Groot
 */
public class UtilTests {

    public static void main(String[] args) {
        try { 
            junit.textui.TestRunner.run(suite());
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static Test suite() throws Exception {
        // Create the test suite
        TestSuite suite= new TestSuite("Util Tests");
        suite.addTestSuite(ResourceLoaderTest.class);
        return suite;
    }
}
