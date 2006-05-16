/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

import java.util.*;

import java.util.regex.Pattern;
import org.mmbase.bridge.*;
import org.mmbase.util.Casting;
import org.mmbase.util.logging.*;

/**
 * The datatype for String fields. Strings can be constrained by a regular expression, and have a
 * property 'password' which indicates that the contents should not be shown.
 *
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: StringDataType.java,v 1.32 2006-05-16 21:11:05 michiel Exp $
 * @since MMBase-1.8
 */
public class StringDataType extends ComparableDataType implements LengthDataType {
    private static final Logger log = Logging.getLoggerInstance(StringDataType.class);

    private static final long serialVersionUID = 1L; // increase this if object serialization changes (which we shouldn't do!)

    protected PatternRestriction patternRestriction = new PatternRestriction(Pattern.compile("(?s)\\A.*\\z"));
    private boolean isPassword = false;
    protected AbstractLengthDataType.MinRestriction minLengthRestriction = new AbstractLengthDataType.MinRestriction(this/* I hate java, no m.i. */, 0);
    protected AbstractLengthDataType.MaxRestriction maxLengthRestriction = new AbstractLengthDataType.MaxRestriction(this, Long.MAX_VALUE);

    /**
     * Constructor for string data type.
     * @param name the name of the data type
     */
    public StringDataType(String name) {
        super(name, String.class);
    }

    protected void inheritProperties(BasicDataType origin) {
        super.inheritProperties(origin);
        if (origin instanceof StringDataType) {
            StringDataType dataType = (StringDataType)origin;
            isPassword = dataType.isPassword();
        }
    }

    public static final Pattern DOUBLE_PATTERN;
    static {
        // copied from javadoc of Double: http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Double.html#valueOf(java.lang.String)
        final String Digits     = "(\\p{Digit}+)";
        final String HexDigits  = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp        = "[eE][+-]?"+Digits;
        final String fpRegex    =
            ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
             "[+-]?(" + // Optional sign character
             "NaN|" +           // "NaN" string
             "Infinity|" +      // "Infinity" string

             // A decimal floating-point string representing a finite positive
             // number without a leading sign has at most five basic pieces:
             // Digits . Digits ExponentPart FloatTypeSuffix
             //
             // Since this method allows integer-only strings as input
             // in addition to strings of floating-point literals, the
             // two sub-patterns below are simplifications of the grammar
             // productions from the Java Language Specification, 2nd
             // edition, section 3.10.2.

             // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
             "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

             // . Digits ExponentPart_opt FloatTypeSuffix_opt
             "(\\.("+Digits+")("+Exp+")?)|"+

             // Hexadecimal strings
             "((" +
             // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
             "(0[xX]" + HexDigits + "(\\.)?)|" +

             // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
             "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

             ")[pP][+-]?" + Digits + "))" +
             "[fFdD]?))" +
             "[\\x00-\\x20]*");// Optional trailing "whitespace"

        DOUBLE_PATTERN = Pattern.compile(fpRegex);
    }
    public static final Pattern BOOLEAN_PATTERN = Pattern.compile("\\A(1|0|true|false)\\z");
    public static final Pattern INTEGER_PATTERN = Pattern.compile("\\A-?[0-9]+\\z");
    public static final Pattern NON_NEGATIVE_INTEGER_PATTERN = Pattern.compile("\\A[0-9]+\\z");
    public static final Pattern LONG_PATTERN    = INTEGER_PATTERN;

    protected void inheritRestrictions(BasicDataType origin) {
        super.inheritRestrictions(origin);
        if (origin instanceof StringDataType) {
            StringDataType dataType = (StringDataType)origin;
            patternRestriction.inherit(dataType.patternRestriction);
            minLengthRestriction.inherit(dataType.minLengthRestriction);
            maxLengthRestriction.inherit(dataType.maxLengthRestriction);
        } else if (origin instanceof BooleanDataType) {
            patternRestriction.setValue(BOOLEAN_PATTERN);
        } else if (origin instanceof IntegerDataType) {
            PatternRestriction parent = new PatternRestriction(INTEGER_PATTERN);
            parent.setEnforceStrength(ENFORCE_ABSOLUTE);
            patternRestriction = new PatternRestriction( parent);
        } else if (origin instanceof LongDataType) {
            PatternRestriction parent = new PatternRestriction(LONG_PATTERN);
            parent.setEnforceStrength(ENFORCE_ABSOLUTE);
            patternRestriction = new PatternRestriction( parent);
        } else if (origin instanceof FloatDataType) {
            PatternRestriction parent = new PatternRestriction(DOUBLE_PATTERN);
            parent.setEnforceStrength(ENFORCE_ABSOLUTE);
            patternRestriction = new PatternRestriction( parent);
        } else if (origin instanceof DoubleDataType) {
            PatternRestriction parent = new PatternRestriction(DOUBLE_PATTERN);
            parent.setEnforceStrength(ENFORCE_ABSOLUTE);
            patternRestriction = new PatternRestriction( parent);
        }
        if (origin instanceof NumberDataType) {
            // number datatypes intrinsicly have a minimal and a maximal value, so these would have been interhited.
            // but on a string they would never work (strings are compared alphabeticly), so remove those restrictions:
            setMin(null, true);
            setMax(null, true);
        }
    }

    protected void cloneRestrictions(BasicDataType origin) {
        super.cloneRestrictions(origin);
        if (origin instanceof StringDataType) {
            StringDataType dataType = (StringDataType)origin;
            patternRestriction = new PatternRestriction(dataType.patternRestriction);
            minLengthRestriction = new AbstractLengthDataType.MinRestriction(this, dataType.minLengthRestriction);
            maxLengthRestriction = new AbstractLengthDataType.MaxRestriction(this, dataType.maxLengthRestriction);
        }
    }

    public long getLength(Object value) {
        return ((String) value).length();
    }
    /**
     * {@inheritDoc}
     */
    public long getMinLength() {
        return Casting.toLong(minLengthRestriction.getValue());
    }

    /**
     * {@inheritDoc}
     */
    public DataType.Restriction getMinLengthRestriction() {
        return minLengthRestriction;
    }

    /**
     * {@inheritDoc}
     */
    public void setMinLength(long value) {
        getMinLengthRestriction().setValue(new Long(value));
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxLength() {
        return Casting.toLong(getMaxLengthRestriction().getValue());
    }

    /**
     * {@inheritDoc}
     */
    public DataType.Restriction getMaxLengthRestriction() {
        return maxLengthRestriction;
    }
    /**
     * {@inheritDoc}
     */
    public void setMaxLength(long value) {
        getMaxLengthRestriction().setValue(new Long(value));
    }



    /**
     * Returns the regular expression pattern used to validate values for this datatype.
     * @return the pattern.
     */
    public Pattern getPattern() {
        return patternRestriction.getPattern();
    }

    /**
     * Returns the 'pattern' restriction, containing the value, error messages, and fixed status of this attribute.
     * @return the restriction as a {@link DataType.Restriction}
     */
    public DataType.Restriction getPatternRestriction() {
        return patternRestriction;
    }

    /**
     * Sets the regular expression pattern used to validate values for this datatype.
     * @param value the pattern as a <code>Pattern</code>, or <code>null</code> if no pattern should be applied.
     * @throws java.lang.UnsupportedOperationException if this datatype is read-only (i.e. defined by MMBase)
     */
    public void setPattern(Pattern value) {
        getPatternRestriction().setValue(value);
    }

    
    /**
     * Whether or not the data represents sensitive information, in which case e.g. an input
     * interface may present asterisks in stead of letters.
     */
    public boolean isPassword() {
        return isPassword;
    }
    public void setPassword(boolean pw) {
        edit();
        isPassword = pw;
    }

    public void toXml(org.w3c.dom.Element parent) {
        super.toXml(parent);
        getElement(parent, "minLength",  "description,class,property,default,unique,required,(minInclusive|minExclusive),(maxIncluse|maxExclusive),minLength").setAttribute("value", Casting.toString(minLengthRestriction.getValue()));
        getElement(parent, "maxLength",  "description,class,property,default,unique,required,(minInclusive|minExclusive),(maxIncluse|maxExclusive),minLength,maxLength").setAttribute("value", Casting.toString(maxLengthRestriction.getValue()));
        getElement(parent, "pattern",  "description,class,property,default,unique,required,(minInclusive|minExclusive),(maxIncluse|maxExclusive),minLength,maxLength,length,pattern").setAttribute("value", Casting.toString(patternRestriction.getPattern().pattern()));

    }

    protected Collection validateCastedValue(Collection errors, Object castedValue, Object value, Node node, Field field) {
        errors = super.validateCastedValue(errors, castedValue, value,  node, field);
        errors = patternRestriction.validate(errors, castedValue, node, field);
        errors = minLengthRestriction.validate(errors, castedValue, node, field);
        errors = maxLengthRestriction.validate(errors, castedValue, node, field);
        return errors;
    }

    protected StringBuffer toStringBuffer() {
        StringBuffer buf = super.toStringBuffer();
        Pattern p = getPattern();
        if (p != null && ! (p.pattern().equals(".*"))) {
            buf.append(" pattern:").append(p.pattern());
        }
        if (isPassword()) {
            buf.append(" password");
        }
        return buf;
    }

    protected class PatternRestriction extends AbstractRestriction {
        PatternRestriction(PatternRestriction source) {
            super(source);
        }
        PatternRestriction(Pattern v) {
            super("pattern", v);
        }
        Pattern getPattern() {
            return (Pattern) value;
        }
        protected boolean simpleValid(Object v, Node node, Field field) {
            String s = Casting.toString(v);
            boolean res =  value == null ? true : getPattern().matcher(s).matches();
            //log.info("VALIDATING " + v + " with " + getPattern() + " -> " + res);
            return res;
        }
    }

    public static void main(String [] argv) {
        Pattern p = Pattern.compile(argv[0]);
        System.out.println(p.matcher(argv[1]).matches());
    }

}
