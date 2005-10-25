/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

import java.util.Collection;
import org.mmbase.bridge.*;

/**
 * The Node data type describes a data type which is based on an MMBase 'node' field. So the value
 * is an MMBase node, which can normally be described by a foreign key.
 *
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: NodeDataType.java,v 1.16 2005-10-25 18:33:21 michiel Exp $
 * @since MMBase-1.8
 */
public class NodeDataType extends BasicDataType {

    protected MustExistRestriction mustExistRestriction = new MustExistRestriction();

    /**
     * Constructor for node field.
     */
    public NodeDataType(String name) {
        super(name, Node.class);
    }


    public Object cast(Object value, Node node, Field field) {
        Cloud cloud = null;
        if (node != null) cloud = node.getCloud();
        if (field != null && cloud == null) cloud = field.getNodeManager().getCloud();
        return org.mmbase.util.Casting.toNode(enumerationRestriction.cast(value), cloud);
    }

    public void inherit(BasicDataType origin) {
        super.inherit(origin);
        if (origin instanceof NodeDataType) {
            mustExistRestriction = new MustExistRestriction(((NodeDataType)origin).mustExistRestriction);
        }
    }

    /**
     * Whether the Node of the value must exist
     *
     * XXX MM: How can you have a non-existing node? I don't really get it. AFAIK all nodes exist.
     *              especially since a node field is essentially a foreign key.
     */
    public boolean mustExist() {
        return mustExistRestriction.getValue().equals(Boolean.TRUE);
    }

    public MustExistRestriction getMustExistRestriction() {
        mustExistRestriction.setFixed(true);
        return mustExistRestriction;
    }

    protected Collection validateCastedValue(Collection errors, Object castedValue, Node node, Field field) {
        errors = super.validateCastedValue(errors, castedValue, node, field);
        errors = mustExistRestriction.validate(errors, castedValue, node, field);
        return errors;
    }

    private class MustExistRestriction extends AbstractRestriction {
        MustExistRestriction(MustExistRestriction me) {
            super(me);
        }
        MustExistRestriction() {
            super("mustExist", Boolean.TRUE);
        }
        public boolean valid(Object value, Node node, Field field) {
            if (getValue().equals(Boolean.TRUE)) {
                if (value != null) {
                    if (value instanceof String) {
                        return node.getCloud().hasNode((String)value);
                    } else if (value instanceof Number) {
                        int num = ((Number)value).intValue();
                        if (num < 0) return true;
                        return node.getCloud().hasNode(num);
                    } else if (value instanceof Node) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }
    }

}
