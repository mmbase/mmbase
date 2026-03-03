/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

import java.text.*;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.mmbase.bridge.*;
import org.mmbase.util.Casting;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A DataType representing some kind of numeric value, like a floating point number or an integer number.
 *
 * @author Pierre van Rooden
 * @version $Id$
 * @since MMBase-1.8
 */
abstract public class NumberDataType<E extends Number & Comparable<E>> extends ComparableDataType<E> {
    private static final Logger log = Logging.getLoggerInstance(NumberDataType.class);

    private static final long serialVersionUID = 1L;

    // Cache NumberFormat instances by Locale for performance
    private static final Map<Locale, NumberFormat> NUMBER_FORMAT_CACHE = new ConcurrentHashMap<>();

    boolean allowSpecialNumbers = false;
    /**
     * Constructor for Number field.
     */
    public NumberDataType(String name, Class<E> classType) {
        super(name, classType);
    }

    /**
     * @since MMBase-1.9.2
     */
    public void setAllowSpecialNumbers(boolean sn) {
        allowSpecialNumbers = sn;
    }

    /**
     * @since MMBase-1.9.2
     */
    protected Number toNumber(String s) throws CastException {
        double d = Casting.toDouble(s);

        if (! allowSpecialNumbers) {
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new CastException("Special numbers not allowed: '" + d + "'");
            }
        }
        return new java.math.BigDecimal(d).stripTrailingZeros();
    }

    protected Number castString(final Object preCast, final Cloud cloud) throws CastException {
        // Handle null and empty string
        if (preCast == null) {
            return null;
        }

        // Handle String arrays
        if (preCast instanceof String[]) {
            String[] sa = (String[]) preCast;
            if (sa.length == 1) {
                return castString(sa[0], cloud);
            }
        }

        // Handle CharSequence (String, etc.)
        if (preCast instanceof CharSequence) {
            String s = (preCast instanceof String) ? (String) preCast : preCast.toString();

            // Return early for empty string
            if (s.isEmpty()) {
                return null;
            }

            Locale l = cloud != null ? cloud.getLocale() : Locale.getDefault();

            // Use cached NumberFormat for better performance
            NumberFormat nf = NUMBER_FORMAT_CACHE.computeIfAbsent(l, locale -> {
                NumberFormat fmt = NumberFormat.getNumberInstance(locale);
                fmt.setGroupingUsed(false); // we never want to parse e.g. "1.2" to "12"
                if (fmt instanceof DecimalFormat) {
                    ((DecimalFormat) fmt).setParseBigDecimal(true);
                } else {
                    log.warn("Not a DecimalFormat for locale: " + locale);
                }
                return fmt;
            });

            try {
                ParsePosition p = new ParsePosition(0);
                Number number = nf.parse(s, p);

                // Check if entire string was parsed
                if (p.getIndex() == s.length() && p.getErrorIndex() < 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Parsed " + s + " to " + number + " (" + p + " " + l);
                    }
                    return number;
                }

                // Fallback: try to parse as double pattern
                if (log.isDebugEnabled()) {
                    log.debug("Not correct, falling back to toDouble for: " + s);
                }
                if (!StringDataType.DOUBLE_PATTERN.matcher(s).matches()) {
                    throw new CastException("Not a number: '" + s + "'");
                }
                return toNumber(s);

            } catch (NumberFormatException nfe) {
                if (log.isDebugEnabled()) {
                    log.debug("NumberFormatException for " + nf + ": " + nfe.getMessage());
                }
            }
            return Casting.toDecimal(s);
        } else if (preCast instanceof Float) {
            float f = (Float) preCast;
            if (!Float.isInfinite(f)) {
                return Casting.toDecimal(preCast);
            }
            // not supported by decimal, return as-is
            return (Float) preCast;
        } else if (preCast instanceof Double) {
            double d = (Double) preCast;
            if (!Double.isInfinite(d)) {
                return Casting.toDecimal(preCast);
            }
            // not supported by decimal, return as-is
            return (Double) preCast;
        }

        // Default fallback for other Number types
        return Casting.toDecimal(preCast);
    }



    /**
     * @since MMBase-1.9
     */
    @Override
    protected Object castToValidate(Object value, Node node, Field field) throws CastException {
        if (value == null) return null;
        Object preCast = preCast(value, node, field); // resolves enumerations

        Object cs = castString(preCast, getCloud(getCloud(node, field)));
        if (log.isDebugEnabled()) {
            log.debug("" + this + " precast " + value + " -> " + preCast + " -> " + cs);
        }
        return cs;
    }

    @Override
    protected E cast(Object value, Cloud cloud, Node node, Field field) throws CastException {
        Object preValue = preCast(value, cloud, node, field);
        if (log.isDebugEnabled()) {
            log.debug("Precast " + value + " to " + preValue);
        }
        Number preCast = castString(preValue, cloud);
        if (preCast == null) return null;
        E cast = Casting.toType(getTypeAsClass(), cloud, preCast);
        return cast;
    }
}
