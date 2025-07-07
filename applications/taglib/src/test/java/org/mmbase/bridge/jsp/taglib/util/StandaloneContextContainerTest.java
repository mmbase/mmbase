/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.jsp.taglib.util;

import javax.servlet.jsp.PageContext;
import static org.junit.Assert.*;
import org.junit.Test;
import org.mmbase.bridge.mock.jsp.MockPageContext;



/**
 * @version $Id$
 */

public  class StandaloneContextContainerTest {


    @Test
    public void basic() throws Exception {

        PageContext pageContext = new MockPageContext();

        PageContextContainer parent = new PageContextContainer(pageContext);

        StandaloneContextContainer container = new StandaloneContextContainer(pageContext, "test", parent);

        container.register("a", "A");
        assertEquals("A", container.get("a"));
        assertEquals("A", pageContext.getAttribute("a"));
        try {
            container.register("a", "AA");
            fail("Should throw exception");
        } catch (Exception e) {
            // ok
        }

        assertTrue(container.isRegistered("a"));

        container.reregister("a", "AA");

        assertEquals("AA", container.get("a"));
        assertEquals("AA", pageContext.getAttribute("a"));

        container.release(pageContext, null);

        assertEquals("AA", container.get("a"));
        assertEquals(null, pageContext.getAttribute("a"));

    }

}
