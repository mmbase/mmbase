/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.jsp.taglib.util;

import javax.servlet.jsp.PageContext;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.mmbase.bridge.mock.jsp.MockPageContext;



/**
 * @version $Id$
 */

public  class BasicBackingTest {



    @Test
    public void mirrorPut() {
        PageContext pageContext = new MockPageContext();
        pageContext.setAttribute("a", "X");
        pageContext.setAttribute("b", "Y");

        BasicBacking backing = new BasicBacking(pageContext, false);

        backing.mirrorPut("a", "A", false);
        backing.mirrorPut("b", "B", true);
        backing.mirrorPut("c", "C", false);

        assertEquals("A", pageContext.getAttribute("a"));
        assertEquals("B", pageContext.getAttribute("b"));
        assertEquals("C", pageContext.getAttribute("c"));

        assertEquals("X",  backing.originalPageContextValues.get("a"));
        assertEquals("Y",  backing.originalPageContextValues.get("b"));
        assertEquals(null,  backing.originalPageContextValues.get("c"));

        backing.release();

        assertEquals("X", pageContext.getAttribute("a"));
        assertEquals("Y", pageContext.getAttribute("b"));
        assertEquals(null, pageContext.getAttribute("c"));


    }

    @Test
    public void basic() {
        // put something on the pageContext
        // use a backing
        // check the pageContext

        PageContext pageContext = new MockPageContext();
        pageContext.setAttribute("a", "X");


        BasicBacking backing = new BasicBacking(pageContext, false);
        // - put something in.   (a -> A)
        // - put something else in (b-> B)
        // (the point is that one of those was already in the pageContext(.

        backing.put("a", "A");
        assertEquals("A", backing.get("a"));
        if (pageContext != null) {
            assertEquals("A", pageContext.getAttribute("a"));
        }


        backing.put("b", "B");

        assertEquals("B", backing.get("b"));
        if (pageContext != null) {
            assertEquals("B", pageContext.getAttribute("b"));
        }


        backing.release();

        assertEquals(null, pageContext.getAttribute("b"));
        assertEquals("X", pageContext.getAttribute("a"));   // should have the original value

    }

    @Test
    public void reset() throws Exception {
        // put something on the pageContext
        // use a backing, in which we _reset_ that value
        // check the pageContext

        PageContext pageContext = new MockPageContext();
        pageContext.setAttribute("a", "X");

        BasicBacking backing = new BasicBacking(pageContext, false);

        pageContext.setAttribute("a", "X");

        backing.put("a", "A", true);
        assertEquals("A", backing.get("a"));
        assertEquals("A", pageContext.getAttribute("a"));

        backing.put("a", "AA", true);
        assertEquals("AA", backing.get("a"));
        assertEquals("AA", pageContext.getAttribute("a"));

        backing.release();

        assertEquals("X", pageContext.getAttribute("a"));

    }


}
