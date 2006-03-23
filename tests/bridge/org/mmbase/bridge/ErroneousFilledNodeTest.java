/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;
import org.w3c.dom.Document;
import org.mmbase.util.Casting;
import java.util.*;

/**
 * Like FilledNodeTest but the used builder is oddly configured.
 *
 * @author Michiel Meeuwissen
 * @since MMBaes-1.8
 */
public class ErroneousFilledNodeTest extends FilledNodeTest {


    protected String getNodeManager() {
        return "aaerrors";
    }
    public ErroneousFilledNodeTest(String name) {
        super(name);
    }


}
