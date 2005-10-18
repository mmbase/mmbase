/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.util.fields;

import org.mmbase.bridge.*;
import java.util.Date;

/**
 * This processor can be used as a 'set' processor on a (datetime) field. The field will then be set
 * to the current time when the node is committed. If the field is set in another way, an exception is
 * thrown (in other words, the field is read only).
 *
 * @author Michiel Meeuwissen
 * @version $Id: LastModified.java,v 1.5 2005-10-18 11:34:59 michiel Exp $
 * @since MMBase-1.8
 * @see   LastModifier
 */

public class LastModified implements CommitProcessor, Processor {

    private static final int serialVersionUID = 1;
    /**
     * You can plug this in on every set-action besides 'object' which will make this
     * field unmodifiable, except for set(Object) itself (which is never used from editors).
     */
    public Object process(Node node, Field field, Object value) {
        throw new BridgeException("You cannot change the field " + field.getName());
    }

    public void commit(Node node, Field field) {
        node.setValueWithoutProcess(field.getName(), new Date());
    }

    public String toString() {
        return "lastmodified";
    }
}
