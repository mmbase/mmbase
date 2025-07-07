/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.jsp.taglib;

import javax.servlet.jsp.PageContext;
import org.junit.Test;
import org.mmbase.bridge.mock.jsp.MockPageContext;

/**

 * @version $Id$
 */

public  class MockContextProviderTest {




    @Test
    public void basic() {
        PageContext pageContext = new MockPageContext();
        ContextProvider provider = new MockContextProvider(pageContext);


    }



}
