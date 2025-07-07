/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.jsp.taglib;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import static org.junit.Assert.*;
import org.junit.Test;
import org.mmbase.bridge.jsp.taglib.util.ContextContainer;
import org.mmbase.bridge.mock.jsp.MockPageContext;

/**
 * @version $Id$
 */

public  class ContextTagTest {


    @Test
    public void basic() throws Exception {
        PageContext pageContext = new MockPageContext();
        ContextTag tag = new ContextTag();
        tag.setPageContext(pageContext);
        tag.doStartTag();
        assertNotNull(tag.getContextProvider());

        tag.register("a", "A");
        try {
            tag.register("a", "B");
            fail("Should have thrown exception");
        } catch (JspTagException te) {
            // ok
        }
        assertEquals("A", pageContext.getAttribute("a"));
        assertEquals("A", tag.getObject("a"));

        System.out.println("container impl: " + tag.getContextContainer().getClass().getName());
        tag.doAfterBody();
        tag.doEndTag();

        assertEquals(null, pageContext.getAttribute("a"));
    }


    @Test
    public void scope() throws Exception {

        PageContext pageContext = new MockPageContext();
        ContextTag tag = new ContextTag();
        tag.setScope("session");
        tag.setId("foobar");
        tag.setPageContext(pageContext);
        tag.doStartTag();

        tag.register("a", "A");
        try {
            tag.register("a", "B");
            fail("Should have thrown exception");
        } catch (JspTagException te) {
            // ok
        }
        assertEquals("A", pageContext.getAttribute("a"));
        assertEquals("A", tag.getObject("a"));

        tag.doAfterBody();
        tag.doEndTag();

        assertEquals(null, pageContext.getAttribute("a"));

        HttpSession ses = pageContext.getSession();
        pageContext.release();

        PageContext pageContext2 = new MockPageContext(pageContext.getServletContext());
        HttpSession ses2 = pageContext2.getSession();
        ses2.setAttribute("foobar", ses.getAttribute("foobar"));


        ContextTag tag2 = new ContextTag();
        tag2.setScope("session");
        tag2.setId("foobar");
        tag2.setPageContext(pageContext2);
        tag2.doStartTag();
        try {
            tag2.register("a", "B");
            fail("Should have thrown exception");
        } catch (JspTagException te) {
            // ok
        }
        assertEquals("A", pageContext2.getAttribute("a"));
        assertEquals("A", tag2.getObject("a"));
        tag2.doAfterBody();
        tag2.doEndTag();

        assertEquals("A", ((ContextContainer) ses.getAttribute("foobar")).get("a"));
        assertEquals(null, ((ContextContainer) ses.getAttribute("foobar")).get("b"));
        assertEquals("A", ((ContextContainer) ses2.getAttribute("foobar")).get("a"));
        assertEquals(null, ((ContextContainer) ses2.getAttribute("foobar")).get("b"));



    }



}
