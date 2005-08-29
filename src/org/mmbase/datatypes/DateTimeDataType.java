/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

import java.util.Calendar;
import java.util.Date;
import org.mmbase.bridge.*;
import org.mmbase.storage.search.FieldValueDateConstraint;
import org.mmbase.util.Casting;

/**
 * @javadoc
 *
 * @author Pierre van Rooden
 * @version $Id: DateTimeDataType.java,v 1.9 2005-08-29 13:16:31 michiel Exp $
 * @since MMBase-1.8
 */
public class DateTimeDataType extends DataType {

    public static final String PROPERTY_MIN = "min";
    public static final Date PROPERTY_MIN_DEFAULT = null;
    public static final String ERROR_MIN_INCLUSIVE = "minInclusive";
    public static final String ERROR_MIN_EXCLUSIVE = "minExclusive";

    public static final String PROPERTY_MAX = "max";
    public static final Date PROPERTY_MAX_DEFAULT = null;
    public static final String ERROR_MAX_INCLUSIVE = "maxInclusive";
    public static final String ERROR_MAX_EXCLUSIVE = "maxExclusive";

    protected DataType.Property minProperty;
    protected int minPrecision;
    protected boolean minInclusive;

    protected DataType.Property maxProperty;
    protected int maxPrecision;
    protected boolean maxInclusive;

    // keys for use with error messages to retrive from the bundle
    private String minInclusiveErrorKey;
    private String minExclusiveErrorKey;
    private String maxInclusiveErrorKey;
    private String maxExclusiveErrorKey;

    /**
     * Constructor for DateTime field.
     */
    public DateTimeDataType(String name) {
        super(name, Date.class);
    }

    // javadoc inherited
    public void erase() {
        super.erase();
        // Determine the key to retrieve an error message from a property's bundle
        minInclusiveErrorKey = getBaseTypeIdentifier() + ".minInclusive.error";
        minExclusiveErrorKey = getBaseTypeIdentifier() + ".minExclusive.error";
        maxInclusiveErrorKey = getBaseTypeIdentifier() + ".maxInclusive.error";
        maxExclusiveErrorKey = getBaseTypeIdentifier() + ".maxExclusive.error";

        minProperty = null;
        minInclusive = true;
        minPrecision = Calendar.MILLISECOND;
        maxProperty = null;
        minInclusive = true;
        maxPrecision = Calendar.MILLISECOND;
    }

    public void inherit(DataType origin) {
        super.inherit(origin);
        if (origin instanceof DateTimeDataType) {
            DateTimeDataType dataType = (DateTimeDataType)origin;
            minProperty = inheritProperty(dataType.minProperty);
            minInclusive = dataType.isMinInclusive();
            minPrecision = dataType.getMinPrecision();
            maxProperty = inheritProperty(dataType.maxProperty);
            maxInclusive = dataType.isMaxInclusive();
            maxPrecision = dataType.getMaxPrecision();
        }
    }

    /**
     * Returns the minimum value for this data type.
     * @return the property defining the minimum value
     */
    public Date getMin() {
        if (minProperty == null) {
            return PROPERTY_MIN_DEFAULT;
        } else {
            return (Date)minProperty.getValue();
        }
    }

    /**
     * Returns the minimum value for this data type.
     * @return the minimum value as an <code>Number</code>, or <code>null</code> if there is no minimum.
     */
    public DataType.Property getMinProperty() {
        if (minProperty == null) minProperty = createProperty(PROPERTY_MIN, PROPERTY_MIN_DEFAULT);
        // change the key for the property error description to match the inclusive status
        if (minInclusive) {
            minProperty.getLocalizedErrorDescription().setKey(ERROR_MIN_INCLUSIVE);
        } else {
            minProperty.getLocalizedErrorDescription().setKey(ERROR_MIN_EXCLUSIVE);
        }
        return minProperty;
    }

    /**
     * Returns the precision for comparing the minimum value for this data type.
     * @return the precision value, a constant from {@link org.mmbase.storage.search.FieldValueDateConstraint}
     */
    public int getMinPrecision() {
        return minPrecision;
    }

    /**
     * Returns whether the minimum value for this data type is inclusive or not.
     * @return <code>true</code> if the minimum value if inclusive, <code>false</code> if it is not, or if there is no minimum.
     */
    public boolean isMinInclusive() {
        return minInclusive;
    }

    /**
     * Returns the maximum value for this data type.
     * @return the maximum value as an <code>Date</code>, or <code>null</code> if there is no maximum.
     */
    public Date getMax() {
        if (maxProperty == null) {
            return PROPERTY_MAX_DEFAULT;
        } else {
            return (Date)maxProperty.getValue();
        }
    }

    /**
     * Returns the maximum value for this data type.
     * @return the property defining the maximum value
     */
    public DataType.Property getMaxProperty() {
        if (maxProperty == null) maxProperty = createProperty(PROPERTY_MAX, PROPERTY_MAX_DEFAULT);
        // change the key for the property error description to match the inclusive status
        if (maxInclusive) {
            maxProperty.getLocalizedErrorDescription().setKey(ERROR_MAX_INCLUSIVE);
        } else {
            maxProperty.getLocalizedErrorDescription().setKey(ERROR_MAX_EXCLUSIVE);
        }
        return maxProperty;
    }

    /**
     * Returns the precision for comparing the maximum value for this data type.
     * @return the precision value, a constant from {@link org.mmbase.storage.search.FieldValueDateConstraint}
     */
    public int getMaxPrecision() {
        return maxPrecision;
    }

    /**
     * Returns whether the maximum value for this data type is inclusive or not.
     * @return <code>true</code> if the maximum value if inclusive, <code>false</code> if it is not, or if there is no minimum.
     */
    public boolean isMaxInclusive() {
        return maxInclusive;
    }

    /**
     * Sets the minimum Date value for this data type.
     * @param length the minimum as an <code>Date</code>, or <code>null</code> if there is no minimum.
     * @throws Class Identifier: java.lang.UnsupportedOperationException if this data type is read-only (i.e. defined by MBase)
     */
    public DataType.Property setMin(Date value) {
        return setProperty(getMinProperty(), value);
    }

    /**
     * Sets the precision for comparing the minimum value for this data type.
     * @param precision the precision value, a constant from {@link org.mmbase.storage.search.FieldValueDateConstraint}
     */
    public void setMinPrecision(int precision) {
        minPrecision = precision;
    }

    /**
     * Sets whether the maximum value is inclusive.
     * @param inclusive <code>true</code> if the value is inclusive
     */
    public void setMinInclusive(boolean inclusive) {
        minInclusive = inclusive;
    }

    /**
     * Sets the minimum Date value for this data type.
     * @param length the minimum as an <code>Date</code>, or <code>null</code> if there is no minimum.
     * @param precision precision, a constant from {@link org.mmbase.storage.search.FieldValueDateConstraint}
     * @param inclusive whether the minimum value is inclusive or not
     * @throws Class Identifier: java.lang.UnsupportedOperationException if this data type is read-only (i.e. defined by MBase)
     */
    public DataType.Property setMin(Date value, int precision, boolean inclusive) {
        edit();
        setMinPrecision(precision);
        setMinInclusive(inclusive);
        return setMin(value);
    }

    /**
     * Sets the maximum Date value for this data type.
     * @param length the maximum as an <code>Date</code>, or <code>null</code> if there is no maximum.
     * @throws Class Identifier: java.lang.UnsupportedOperationException if this data type is read-only (i.e. defined by MBase)
     */
    public DataType.Property setMax(Date value) {
        return setProperty(getMaxProperty(), value);
    }

    /**
     * Sets the precision for comparing the maximum value for this data type.
     * @param precision the precision value, a constant from {@link org.mmbase.storage.search.FieldValueDateConstraint}
     */
    public void setMaxPrecision(int precision) {
        maxPrecision = precision;
    }

    /**
     * Sets whether the maximum value is inclusive.
     * @param inclusive <code>true</code> if the value is inclusive
     */
    public void setMaxInclusive(boolean inclusive) {
        maxInclusive = inclusive;
    }

    /**
     * Sets the maximum Date value for this data type.
     * @param length the maximum as an <code>Date</code>, or <code>null</code> if there is no maximum.
     * @param precision precision, a constant from {@link org.mmbase.storage.search.FieldValueDateConstraint}
     * @param inclusive whether the maximum value is inclusive or not
     * @throws Class Identifier: java.lang.UnsupportedOperationException if this data type is read-only (i.e. defined by MBase)
     */
    public DataType.Property setMax(Date value, int precision, boolean inclusive) {
        edit();
        setMaxPrecision(precision);
        setMaxInclusive(inclusive);
        return setMax(value);
    }

    /**
     * Returns a long value representing the date in milliseconds since 1/1/1970,
     * adjusted for the precision given.
     * @param date the date to obtain the value from
     * @param precisison the precision, similar to the <code>java.util.Calendar</code> constants, or the
     *        constants from {@link org.mmbase.storage.search.FieldValueDateConstraint}
     * @returns the date as a <code>long</code>
     */
    protected long getDateLongValue(Date date, int precision) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        switch (precision) {
        // 'CENTURY' does not exist in Calendar, but does in FieldValueDateConstraint
        case FieldValueDateConstraint.CENTURY : {
            int year = calendar.get(Calendar.YEAR);
            year %= 100;
            calendar.set(Calendar.YEAR, year);
        }
        case FieldValueDateConstraint.YEAR: calendar.clear(Calendar.MONTH);
            // 'Quarter' does not exist in Calendar, but does in FieldValueDateConstraint
        case FieldValueDateConstraint.QUARTER : {
            int month = calendar.get(Calendar.MONTH);
            month %= 4;
            calendar.set(Calendar.MONTH, month);
        }
        case FieldValueDateConstraint.MONTH : ;
        case FieldValueDateConstraint.WEEK : calendar.clear(Calendar.DAY_OF_MONTH);
        case FieldValueDateConstraint.DAY_OF_MONTH : ;
        case FieldValueDateConstraint.DAY_OF_YEAR : ;
        case FieldValueDateConstraint.DAY_OF_WEEK : calendar.clear(Calendar.HOUR);
        case FieldValueDateConstraint.HOUR : calendar.clear(Calendar.MINUTE);
        case FieldValueDateConstraint.MINUTE : calendar.clear(Calendar.SECOND);
        case FieldValueDateConstraint.SECOND : calendar.clear(Calendar.MILLISECOND);
        }
        return calendar.getTimeInMillis();
    }

    public void validate(Object value, Node node, Field field, Cloud cloud) {
        super.validate(value, node, field, cloud);
        if (value != null) {
            Date date = Casting.toDate(value);
            Date minimum = getMin();
            if (minimum != null) {
                long minValue = getDateLongValue(minimum, minPrecision);
                long dateValue = getDateLongValue(date, minPrecision);
                if (minValue > dateValue || (!minInclusive && minValue == dateValue)) {
                    failOnValidate(getMinProperty(), value, cloud);
                }
            }
            Date maximum = getMax();
            if (maximum != null) {
                long maxValue = getDateLongValue(maximum, maxPrecision);
                long dateValue = getDateLongValue(date, maxPrecision);
                if (maxValue < dateValue || (!maxInclusive && maxValue == dateValue)) {
                    failOnValidate(getMaxProperty(), value, cloud);
                }
            }
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        if (getMin() != null) {
            buf.append("min:" + getMin() + " " + getMinPrecision()).append(isMinInclusive() ? " inclusive" : " exclusive").append("\n");
        }
        if (getMax() != null) {
            buf.append("max:" + getMax() + " " + getMaxPrecision()).append(isMaxInclusive() ? " inclusive" : " exclusive").append("\n");
        }
        return buf.toString();
    }

}
