/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Michiel Meeuwissen

 */
public class ReplacingLocalizedStringTest {


    @Test
    public void makeLiterator() {

        assertEquals(ReplacingLocalizedString.makeLiteral("foobar"), "foobar");

    }



}
