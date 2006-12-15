/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.util.Casting;
import org.mmbase.util.logging.*;
import org.w3c.dom.Element;

/**
 * A LengthDataType is a datatype that defines a length for its values ({@link #getLength(Object)}) ,
 * and restrictions on that (minimal an maximal length).
 *
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: AbstractLengthDataType.java,v 1.17 2006-12-15 13:38:01 michiel Exp $
 * @since MMBase-1.8
 */
abstract public class AbstractLengthDataType<E> extends BasicDataType<E> implements LengthDataType<E> {
    private static final Logger log = Logging.getLoggerInstance(LengthDataType.class);

    protected MinRestriction minLengthRestriction = new MinRestriction(this, 0);
    protected MaxRestriction maxLengthRestriction = new MaxRestriction(this, Long.MAX_VALUE);

    /**
     * Constructor for big data field.
     * @param name the name of the data type
     * @param classType the class of the data type's possible value
     */
    public AbstractLengthDataType(String name, Class classType) {
        super(name, classType);
        minLengthRestriction.setEnforceStrength(ENFORCE_ABSOLUTE);
    }


    protected void cloneRestrictions(BasicDataType origin) {
        super.cloneRestrictions(origin);
        if (origin instanceof AbstractLengthDataType) {
            AbstractLengthDataType dataType = (AbstractLengthDataType)origin;
            // make new instances because of this can be called from a clone .. We hate java.
            minLengthRestriction = new MinRestriction(this, dataType.minLengthRestriction);
            maxLengthRestriction = new MaxRestriction(this, dataType.maxLengthRestriction);
        }
    }

    protected void inheritRestrictions(BasicDataType origin) {
        super.inheritRestrictions(origin);
        if (origin instanceof AbstractLengthDataType) {
            AbstractLengthDataType dataType = (AbstractLengthDataType)origin;
            // make new instances because of this can be called from a clone .. We hate java.
            minLengthRestriction.inherit(dataType.minLengthRestriction);
            maxLengthRestriction.inherit(dataType.maxLengthRestriction);
        }
    }

    /**
     * {@inheritDoc}
     */
    public abstract long getLength(E value);

    /**
     * {@inheritDoc}
     */
    public long getMinLength() {
        return minLengthRestriction.getValue();
    }

    /**
     * {@inheritDoc}
     */
    public DataType.Restriction<Long> getMinLengthRestriction() {
        return minLengthRestriction;
    }

    /**
     * {@inheritDoc}
     */
    public void setMinLength(long value) {
        getMinLengthRestriction().setValue(Long.valueOf(value));
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxLength() {
        return getMaxLengthRestriction().getValue();
    }

    /**
     * {@inheritDoc}
     */
    public DataType.Restriction<Long> getMaxLengthRestriction() {
        return maxLengthRestriction;
    }

    /**
     * Sets the maximum length of binary values for this datatype.
     * @param value the maximum length as an <code>int</code>, or -1 if there is no maximum length.
     * @throws Class Identifier: java.lang.UnsupportedOperationException if this datatype is finished
     */
    public void setMaxLength(long value) {
        getMaxLengthRestriction().setValue(Long.valueOf(value));
    }

    protected Collection validateCastValue(Collection errors, Object castValue, Object value,  Node node, Field field) {
        errors = super.validateCastValue(errors, castValue, value,  node, field);
        errors = minLengthRestriction.validate(errors, castValue, node, field);
        errors = maxLengthRestriction.validate(errors, castValue, node, field);
        return errors;
    }

    public void toXml(Element parent) {
        super.toXml(parent);
        getElement(parent, "minLength",  "description,class,property,default,unique,required,(minInclusive|minExclusive),(maxIncluse|maxExclusive),minLength").setAttribute("value", Casting.toString(minLengthRestriction.getValue()));
        getElement(parent, "maxLength",  "description,class,property,default,unique,required,(minInclusive|minExclusive),(maxIncluse|maxExclusive),minLength,maxLength").setAttribute("value", Casting.toString(maxLengthRestriction.getValue()));

    }

    protected StringBuffer toStringBuffer() {
        StringBuffer buf = super.toStringBuffer();
        if (getMinLength() > 0) {
            buf.append("minLength:" + getMinLength() + " ");
        }
        if (getMaxLength() < Long.MAX_VALUE) {
            buf.append("maxLength:" + getMaxLength() + " ");
        }
        return buf;
    }

    static class MinRestriction extends StaticAbstractRestriction<Long> {
        MinRestriction(BasicDataType dt, MinRestriction source) {
            super(dt, source);
        }

        MinRestriction(BasicDataType dt, long min) {
            super(dt, "minLength", Long.valueOf(min));
        }

        protected boolean simpleValid(Object v, Node node, Field field) {
            if (v == null) return true; // depends on 'required'
            long min = Casting.toLong(getValue());
            return ((LengthDataType) parent).getLength(v) >= min;
        }
    }

    static class MaxRestriction extends StaticAbstractRestriction<Long> {
        MaxRestriction(BasicDataType dt, MaxRestriction source) {
            super(dt, source);
        }

        MaxRestriction(BasicDataType dt, long max) {
            super(dt, "maxLength", Long.valueOf(max));
        }

        protected boolean simpleValid(Object v, Node node, Field field) {
            if (v == null) return true; // depends on 'required'
            long max = Casting.toLong(getValue());
            long length = ((LengthDataType) parent).getLength(v);
            return length <= max;
        }
    }

}
