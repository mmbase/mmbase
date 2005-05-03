/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

/**
 * Collects MMBase specific 'cast' information, as static
 * to... functions. This is used (and used to be implemented) in
 * MMObjectNode. But this functionality is more generic to MMbase.
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 * @version $Id: Casting.java,v 1.44 2005-05-03 19:24:07 michiel Exp $
 */

import java.util.*;
import java.text.*;
import java.io.Writer;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.ContextProvider;
import org.mmbase.bridge.util.NodeWrapper;
import org.mmbase.bridge.util.MapNode;
import org.mmbase.module.core.*;
import org.mmbase.util.transformers.CharTransformer;
import org.mmbase.util.transformers.XmlField;
import org.mmbase.util.logging.*;

import org.w3c.dom.*;

public class Casting {

    /**
     * A Date formatter that creates a date based on a ISO 8601 date and a ISO 8601 time.
     * I.e. 2004-12-01 14:30:00.
     * It is NOT 100% ISO 8601, as opposed to {@link #ISO_8601_UTC}, as the standard actually requires
     * a 'T' to be placed between the date and the time.
     * The date given is the date for the local (server) time. Use this formatter if you want to display
     * user-friendly dates in local time.
     */
    public final static DateFormat ISO_8601_LOOSE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * A Date formatter that creates a ISO 8601 datetime according to UTC/GMT.
     * I.e. 2004-12-01T14:30:00Z.
     * This is 100% ISO 8601, as opposed to {@link #ISO_8601_LOOSE}.
     * Use this formatter if you want to export dates.
     */
    public final static DateFormat ISO_8601_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    {
        ISO_8601_UTC.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }

    private static final Logger log = Logging.getLoggerInstance(Casting.class);

    /**
     * Returns whether the passed object is of the given class.
     * Unlike Class instanceof this also includes Object Types that
     * are representative for primitive types (i.e. Integer for int).
     * @param type the type (class) to check
     * @param value the value whose type to check
     * @return <code>true</code> if compatible
     */
    public static boolean isType(Class type, Object value) {
        if (type.isPrimitive()) {
            return (type.equals(Boolean.TYPE) && value instanceof Boolean) ||
                   (type.equals(Byte.TYPE) && value instanceof Byte) ||
                   (type.equals(Character.TYPE) && value instanceof Character) ||
                   (type.equals(Short.TYPE) && value instanceof Short) ||
                   (type.equals(Integer.TYPE) && value instanceof Integer) ||
                   (type.equals(Long.TYPE) && value instanceof Long) ||
                   (type.equals(Float.TYPE) && value instanceof Float) ||
                   (type.equals(Double.TYPE) && value instanceof Double);
        } else {
            return value == null || type.isInstance(value);
        }
    }

    /**
     * Tries to 'cast' an object for use with the provided class. E.g. if value is a String, but the
     * type passed is Integer, then the string is act to an Integer.
     * If the type passed is a primitive type, the object is cast to an Object Types that is representative
     * for that type (i.e. Integer for int).
     * @param value The value to be converted
     * @param type the type (class)
     * @return value the converted value
     */
    public static Object toType(Class type, Object value) {
        if (isType(type, value))  {
            return value;
        } else {
            if (type.equals(Boolean.TYPE) || type.equals(Boolean.class)) {
                return Boolean.valueOf(toBoolean(value));
            } else if (type.equals(Byte.TYPE) || type.equals(Byte.class)) {
                return new Byte(toInteger(value).byteValue());
            } else if (type.equals(Character.TYPE) || type.equals(Character.class)) {
                String chars = toString(value);
                if (chars.length() > 0) {
                    return new Character(chars.charAt(0));
                } else {
                    return new Character(Character.MIN_VALUE);
                }
            } else if (type.equals(Short.TYPE) || type.equals(Short.class)) {
                return new Short(toInteger(value).shortValue());
            } else if (type.equals(Integer.TYPE) || type.equals(Integer.class)) {
                return toInteger(value);
            } else if (type.equals(Long.TYPE) || type.equals(Long.class)) {
                return new Long(toLong(value));
            } else if (type.equals(Float.TYPE) || type.equals(Float.class)) {
                return new Float(toFloat(value));
            } else if (type.equals(Double.TYPE) || type.equals(Double.class)) {
                return new Double(toDouble(value));
            } else if (type.equals(Number.class)) {
                Number res;
                try {
                    res = new Long("" + value);
                } catch (NumberFormatException nfe) {
                    try {
                        res = new Double("" + value);
                    } catch (NumberFormatException nfe1) {
                        res = new Integer(-1);
                    }
                }
                return res;
            } else if (type.equals(byte[].class)) {
                return toByte(value);
            } else if (type.equals(String.class)) {
                return toString(value);
            } else if (type.equals(Date.class)) {
                return toDate(value);
            } else if (type.equals(Node.class)) {
                return toNode(value, ContextProvider.getDefaultCloudContext().getCloud("mmbase"));
            } else if (type.equals(MMObjectNode.class)) {
                return toNode(value, MMBase.getMMBase().getTypeDef());
            } else if (type.equals(Document.class)) {
                return toXML(value);
            } else if (type.equals(List.class)) {
                return toList(value);
            } else {
                // don't know
                return value;
            }
        }
    }

    /**
     * Convert an object to a String.
     * 'null' is converted to an empty string.
     * @param o the object to convert
     * @return the converted value as a <code>String</code>
     */
    public static String toString(Object o) {
        if (o instanceof String) {
            return (String)o;
        }
        if (o == null || o == MMObjectNode.VALUE_NULL) {
            return "";
        }
        return toStringBuffer(new StringBuffer(), o).toString();
    }

    /**
     * Convert an object to a string, using a StringBuffer.
     * @param buffer The StringBuffer with which to create the string
     * @param o the object to convert
     * @return the StringBuffer used for conversion (same as the buffer parameter)
     * @since MMBase-1.7
     */
    public static StringBuffer toStringBuffer(StringBuffer buffer, Object o) {
        if (o == null || o == MMObjectNode.VALUE_NULL) {
            return buffer;
        }
        try {
            toWriter(new StringBufferWriter(buffer), o);
        } catch (java.io.IOException e) {}
        return buffer;
    }

    /**
     * Convert an object to a string, using a Writer.
     * @param writer The Writer with which to create (write) the string
     * @param o the object to convert
     * @return the Writer used for conversion (same as the writer parameter)
     * @since MMBase-1.7
     */
    public static Writer toWriter(Writer writer, Object o) throws java.io.IOException {
        if (o instanceof Writer) {
            return writer;
        }
        Object s = wrap(o, null);
        writer.write(s.toString());
        return writer;
    }

    /**
     * Wraps it in an object with a toString as we desire. Casting can now be done with
     * toString() on the resulting object.
     *
     * This is used to make JSTL en EL behave similarly as mmbase taglib when writing objects to the
     * page (taglib calls Casting, but they of course don't).
     *
     * @todo  Not everything is wrapped (and can be unwrapped) already.
     * @param o        The object to be wrapped
     * @param escaper  <code>null</code> or a CharTransformer to pipe the strings through
     * @since MMBase-1.8
     */

    public static Object wrap(final Object o, final CharTransformer escaper) {
        if (o == null || o == MMObjectNode.VALUE_NULL) {
            return escape(escaper, "");
        } else if (o instanceof Node) {
            return new MapNode((Node)o) {
                    public Object getValue(String fieldName) {
                        return escape(escaper, super.getStringValue(fieldName));
                    }
                    public String toString() {
                        return escape(escaper, "" + node.getNumber());
                    }
                };
        } else if (o instanceof MMObjectNode) {
            // don't know how to wrap
            return escape(escaper, "" + ((MMObjectNode)o).getNumber());
        } else if (o instanceof Date) {
            return new java.util.Date(((Date)o).getTime()) {
                    private static final int serialVersionUID = 1; // increase this if object chages.
                    public String toString() {
                        String r;
                        if (getTime()  != -1) { // datetime not set
                            r = ISO_8601_UTC.format((Date)o);
                        } else {
                            r = "";
                        }
                        return escape(escaper, r);
                    }
                };
        } else if (o instanceof Document) {
            // don't know how to wrap
            return escape(escaper, convertXmlToString(null, (Document)o));
        } else if (o instanceof List) {
            return new ListWrapper((List) o, escaper);
        } else if (o instanceof byte[]) {
            try {
                return escape(escaper, new String((byte[])o, MMBase.getMMBase().getEncoding()));
            } catch (java.io.UnsupportedEncodingException uee) {
                // should never happen
                log.error(uee);
                return o;
            }
        } else {
            if (o instanceof String && escaper != null) {
                return new StringWrapper((String) o, escaper);
            } else {
                return o;
            }
        }

    }

    private static String escape(CharTransformer escaper, String string) {
        if (escaper != null) {
            return escaper.transform(string);
        } else {
            return string;
        }
    }
    /**
     * When you want to undo the wrapping, this method can be used.
     * @since MMBase-1.8
     */
    public static Object unWrap(final Object o) {
        if (o instanceof NodeWrapper) {
            return ((NodeWrapper)o).getNode();
        } else if (o instanceof ListWrapper) {
            return ((ListWrapper)o).getList();
        } else if (o instanceof StringWrapper) {
            return ((StringWrapper)o).getString();
        } else {
            return o;
        }
    }

    /**
     * Convert an object to a List.
     * A String is split up (as if it was a comma-separated String).
     * Individual objects are wrapped and returned as Lists with one item.
     * <code>null</code> and the empty string are  returned as an empty list.
     * @param o the object to convert
     * @return the converted value as a <code>List</code>
     * @since MMBase-1.7
     */
    public static List toList(Object o) {
        if (o instanceof List) {
            return (List)o;
        } else if (o instanceof Collection) {
            return new ArrayList((Collection) o);
        } else if (o instanceof String) {            
            return StringSplitter.split((String)o);
        } else if (o instanceof Map) {            
            return new ArrayList(((Map)o).entrySet());
        } else {
            List l = new ArrayList();
            if (o != null && o != MMObjectNode.VALUE_NULL) {
                l.add(o);
            }
            return l;
        }
    }

    /**
     * @since MMBase-1.8
     */
    public static Collection toCollection(Object o) {
        if (o instanceof Collection) {
            return (Collection)o;
        } else if (o instanceof Map) {
            return ((Map)o).entrySet();
        } else if (o instanceof String) {            
            return StringSplitter.split((String)o);      
        } else {
            List l = new ArrayList();
            if (o != null && o != MMObjectNode.VALUE_NULL) {
                l.add(o);
            }
            return l;
        }
    }

    /**
     * Convert the value to a <code>Document</code> object.
     * If the value is not itself a Document, the method attempts to
     * attempts to convert the String value into an XML.
     * A <code>null</code> value is returned as <code>null</code>.
     * If the value cannot be converted, this method throws an IllegalArgumentException.
     * @param o the object to be converted to an XML document
     * @param documentType the xml document type
     * @param conversion encoder conversion type
     * @return  the value as a DOM Element or <code>null</code>
     * @throws  IllegalArgumentException if the value could not be converted
     * @since MMBase-1.6
     */
    static public Document toXML(Object o) {
        if (o == null || o == MMObjectNode.VALUE_NULL) return null;
        if (!(o instanceof Document)) {
            //do conversion from String to Document...
            // This is a laborous action, so we log it on service.
            // It will happen often if the nodes are not cached and so on.
            String xmltext = toString(o);
            if (log.isServiceEnabled()) {
                String msg = xmltext;
                if (msg.length() > 20)
                    msg = msg.substring(0, 20);
                log.service("Object " + msg + "... is not a Document, but a " + o.getClass().getName());
            }
            return convertStringToXML(xmltext);
        }
        return (Document)o;
    }

    /**
     * Convert an object to a byte array.
     * @param obj The object to be converted
     * @return the value as an <code>byte[]</code> (binary/blob field)
     */
    static public byte[] toByte(Object obj) {
        if (obj instanceof byte[]) {
            // was allready unmapped so return the value
            return (byte[])obj;
        } else if (obj == null || obj == MMObjectNode.VALUE_NULL) {
            return new byte[] {};
        } else {
            return toString(obj).getBytes();
        }
    }

    static public InputStream toInputStream(Object obj) {
        if (obj instanceof InputStream) {
            return (InputStream) obj;
        } else {
            return new ByteArrayInputStream(toByte(obj));                    
        }
    }

    /**
     * Convert an object to an MMObjectNode.
     * If the value is Numeric, the method
     * tries to obtrain the object with that number.
     * If it is a String, the method tries to obtain the object with
     * that alias. If it is a Node, the equivalent MMObjecTNode is loaded.
     * All remaining situations return <code>null</code>.
     * @param i the object to convert
     * @param parent the MMObjectBuilder to use for loading a node
     * @return the value as a <code>MMObjectNode</code>
     */
    public static MMObjectNode toNode(Object i, MMObjectBuilder parent) {
        MMObjectNode res = null;
        if (i instanceof MMObjectNode) {
            res = (MMObjectNode)i;
        } else if (i instanceof Node) {
            res = parent.getNode(((Node)i).getNumber());
        } else if (i instanceof Number) {
            int nodenumber = ((Number)i).intValue();
            if (nodenumber != -1) {
                res = parent.getNode(nodenumber);
            }
        } else if (i != null && i != MMObjectNode.VALUE_NULL && !i.equals("")) {
            res = parent.getNode(i.toString());
        }
        return res;
    }

    /**
     * Convert an object to an Node.
     * If the value is Numeric, the method
     * tries to obtrain the object with that number.
     * If it is a String, the method tries to obtain the object with
     * that alias. If it is a MMObjectNode, the equivalent bridge Node is loaded.
     * All remaining situations return <code>null</code>.
     * @param i the object to convert
     * @param cloud the Cloud to use for loading a node
     * @return the value as a <code>Node</code>
     * @since MMBase-1.7
     */
    public static Node toNode(Object i, Cloud cloud) {
        Node res = null;
        if (i instanceof Node) {
            res = (Node)i;
        } else if (i instanceof MMObjectNode) {
            org.mmbase.bridge.NodeList list = new org.mmbase.bridge.implementation.BasicNodeList(Collections.singletonList(i),cloud);
            res = list.getNode(0);
        } else if (i instanceof Number) {
            int nodenumber = ((Number)i).intValue();
            if (nodenumber != -1) {
                res = cloud.getNode(nodenumber);
            }
        } else if (i != null &&  i != MMObjectNode.VALUE_NULL && !i.equals("")) {
            res = cloud.getNode(i.toString());
        }
        return res;
    }

    /**
     * Convert an object to an <code>int</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * If a value is an MMObjectNode, it's number field is returned.
     * All remaining values return the provided default value.
     * @param i the object to convert
     * @param def the default value if conversion is impossible
     * @return the converted value as an <code>int</code>
     * @since MMBase-1.7
     */
    static public int toInt(Object i, int def) {
        int res = def;
        if (i instanceof MMObjectNode) {
            res = ((MMObjectNode)i).getNumber();
        } else if (i instanceof Node) {
            res = ((Node)i).getNumber();
        } else if (i instanceof Boolean) {
            res = ((Boolean)i).booleanValue() ? 1 : 0;
        } else if (i instanceof Date) {
            long timeValue = ((Date)i).getTime();
            if (timeValue!=-1) timeValue = timeValue / 1000;
            res = (int) timeValue;
        } else if (i instanceof Number) {
            res = ((Number)i).intValue();
        } else if (i != null && i != MMObjectNode.VALUE_NULL) {
            try {
                res = Integer.parseInt("" + i);
            } catch (NumberFormatException e) {
                // not an integer? perhaps it is a fload or double represented as String.
                try {
                    res = Double.valueOf("" + i).intValue();
                } catch (NumberFormatException ex) {
                    // give up, fall back to default.
                }
            }
        }
        return res;
    }

    /**
     * Convert an object to an <code>int</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * If a value is an MMObjectNode, it's number field is returned.
     * All remaining values return -1.
     * @param i the object to convert
     * @return the converted value as an <code>int</code>
     */
    static public int toInt(Object i) {
        return toInt(i, -1);
    }

    /**
     * Convert an object to a <code>boolean</code>.
     * If the value is numeric, this call returns <code>true</code>
     * if the value is a positive, non-zero, value. In other words, values '0'
     * and '-1' are concidered <code>false</code>.
     * If the value is a string, this call returns <code>true</code> if
     * the value is "true" or "yes" (case-insensitive).
     * In all other cases (including calling byte fields), <code>false</code>
     * is returned.
     * @param i the object to convert
     * @return the converted value as a <code>boolean</code>
     */
    static public boolean toBoolean(Object b) {
        if (b instanceof Boolean) {
            return ((Boolean)b).booleanValue();
        } else if (b instanceof Number) {
            return ((Number)b).doubleValue() > 0;
        } else if (b instanceof Node || b instanceof MMObjectNode) {
            return true; // return true if a NODE is filled
        } else if (b instanceof Date) {
            return ((Date)b).getTime() != -1;
        } else if (b instanceof Document) {
            return false; // undefined
        } else if (b instanceof String) {
            // note: we don't use Boolean.valueOf() because that only captures
            // the value "true"
            String s = ((String)b).toLowerCase();
            return s.equals("true") || s.equals("yes") || s.equals("1");
        } else {
            return false;
        }
    }

    /**
     * Convert an object to an Integer.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * All remaining values return -1.
     * @param i the object to convert
     * @return the converted value as a <code>Integer</code>
     */
    static public Integer toInteger(Object i) {
        if (i instanceof Integer) {
            return (Integer)i;
        } else {
            return new Integer(toInt(i));
        }
    }

    /**
     * Convert an object to a <code>long</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * All remaining values return the provided default value.
     * @param i the object to convert
     * @param def the default value if conversion is impossible
     * @return the converted value as a <code>long</code>
     * @since MMBase-1.7
     */
    static public long toLong(Object i, long def) {
        long res = def;
        if (i instanceof Boolean) {
            res = ((Boolean)i).booleanValue() ? 1 : 0;
        } else if (i instanceof Number) {
            res = ((Number)i).longValue();
        } else if (i instanceof Date) {
            res = ((Date)i).getTime();
            if (res!=-1) res /= 1000;
        } else if (i instanceof MMObjectNode) {
            res = ((MMObjectNode)i).getNumber();
        } else if (i instanceof Node) {
            res = ((Node)i).getNumber();
        } else if (i != null && i != MMObjectNode.VALUE_NULL) {
            try {
                res = Long.parseLong("" + i);
            } catch (NumberFormatException e) {
                // not an integer? perhaps it is a float or double represented as String.
                try {
                    res = Double.valueOf("" + i).longValue();
                } catch (NumberFormatException ex) {
                    // give up, fall back to default.
                }
            }
        }
        return res;
    }

    /**
     * Convert an object to a <code>long</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * All remaining values return -1.
     * @param i the object to convert
     * @return the converted value as a <code>long</code>
     * @since MMBase-1.7
     */
    static public long toLong(Object i) {
        return toLong(i, -1);
    }

    /**
     * Convert an object to an <code>float</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * All remaining values return the default value.
     * @param i the object to convert
     * @param def the default value if conversion is impossible
     * @return the converted value as a <code>float</code>
     */
    static public float toFloat(Object i, float def) {
        float res = def;
        if (i instanceof Boolean) {
            res = ((Boolean)i).booleanValue() ? 1 : 0;
        } else if (i instanceof Number) {
            res = ((Number)i).floatValue();
        } else if (i instanceof Date) {
            res = ((Date)i).getTime();
            if (res!=-1) res = res / 1000;
        } else if (i instanceof MMObjectNode) {
            res = ((MMObjectNode)i).getNumber();
        } else if (i instanceof Node) {
            res = ((Node)i).getNumber();
        } else if (i != null && i != MMObjectNode.VALUE_NULL) {
            try {
                res = Float.parseFloat("" + i);
            } catch (NumberFormatException e) {}
        }
        return res;
    }

    /**
     * Convert an object to an <code>float</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * All remaining values return -1.
     * @param i the object to convert
     * @param def the default value if conversion is impossible
     * @return the converted value as a <code>float</code>
     */
    static public float toFloat(Object i) {
        return toFloat(i, -1);
    }

    /**
     * Convert an object to an <code>double</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * All remaining values return the default value.
     * @param i the object to convert
     * @param def the default value if conversion is impossible
     * @return the converted value as a <code>double</code>
     */
    static public double toDouble(Object i, double def) {
        double res = def;
        if (i instanceof Boolean) {
            res = ((Boolean)i).booleanValue() ? 1 : 0;
        } else if (i instanceof Number) {
            res = ((Number)i).doubleValue();
        } else if (i instanceof Date) {
            res = ((Date)i).getTime();
            if (res!=-1) res = res / 1000;
        } else if (i instanceof MMObjectNode) {
            res = ((MMObjectNode)i).getNumber();
        } else if (i instanceof Node) {
            res = ((Node)i).getNumber();
        } else if (i != null && i != MMObjectNode.VALUE_NULL) {
            try {
                res = Double.parseDouble("" + i);
            } catch (NumberFormatException e) {}
        }
        return res;
    }

    /**
     * Convert an object to an <code>double</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * All remaining values return -1.
     * @param i the object to convert
     * @param def the default value if conversion is impossible
     * @return the converted value as a <code>double</code>
     */
    static public double toDouble(Object i) {
        return toDouble(i, -1);
    }

    /**
     * Convert an object to a <code>Date</code>.
     * String values are parsed to a date, if possible.
     * Numeric values are assumed to represent number of seconds since 1970.
     * All remaining values return 1969-12-31 23:59 GMT.
     * @param i the object to convert
     * @param def the default value if conversion is impossible
     * @return the converted value as a <code>Date</code>
     * @since MMBase-1.7
     */
    static public java.util.Date toDate(Object d) {
        java.util.Date date = null;
        if (d instanceof java.util.Date) {
            date = (java.util.Date) d;
        } else {
            try {
                long dateInSeconds = -1;
                if (d instanceof Number) {
                    dateInSeconds = ((Number)d).longValue();
                } else if (d != null && d != MMObjectNode.VALUE_NULL) {
                        dateInSeconds = Long.parseLong("" + d);
                }
                if (dateInSeconds == -1) {
                    date = new java.util.Date(-1);
                } else {
                    date = new java.util.Date(dateInSeconds * 1000);
                }
            } catch (NumberFormatException e) {
                // not a number. hence it is likely in string format
                try {
                    date = ISO_8601_UTC.parse(""+d);
                } catch (ParseException pe) {
                    date = new java.util.Date(-1);
                }
            }
        }
        return date;
    }

    /**
     * Convert a String value to a Document
     * @param value The current value (can be null)
     * @return  the value as a DOM Element or <code>null</code>
     * @throws  IllegalArgumentException if the value could not be converted
     */
    static private Document convertStringToXML(String value) {
        if (value == null || value == MMObjectNode.VALUE_NULL || value.equals("")) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.trace("using xml string:\n" + value);
        }

        try {
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            dfactory.setValidating(false);
            DocumentBuilder documentBuilder = dfactory.newDocumentBuilder();
            // dont log errors, and try to process as much as possible...
            XMLErrorHandler errorHandler = new XMLErrorHandler(false, org.mmbase.util.XMLErrorHandler.NEVER);
            documentBuilder.setErrorHandler(errorHandler);
            documentBuilder.setEntityResolver(new XMLEntityResolver());
            // ByteArrayInputStream?
            // Yes, in contradiction to what one would think, XML are bytes, rather then characters.
            Document doc = documentBuilder.parse(new java.io.ByteArrayInputStream(value.getBytes("UTF-8")));
            if (log.isDebugEnabled()) {
                log.trace("parsed: " + convertXmlToString(null, doc));
            }
            if (!errorHandler.foundNothing()) {
                throw new IllegalArgumentException("xml invalid:\n" + errorHandler.getMessageBuffer() + "for xml:\n" + value);
            }
            return doc;
        } catch (ParserConfigurationException pce) {
            throw new IllegalArgumentException("[sax parser] not well formed xml: " + pce.toString() + "\n" + Logging.stackTrace(pce));
        } catch (org.xml.sax.SAXException se) {
            log.debug("[sax] not well formed xml: " + se.toString() + "(" + se.getMessage() + ")\n" + Logging.stackTrace(se));
            return convertStringToXML("<p>" + Encode.encode("ESCAPE_XML", value) + "</p>"); // Should _always_ be sax-compliant.
        } catch (java.io.IOException ioe) {
            String msg = "[io] not well formed xml: " + ioe.toString() + "\n" + Logging.stackTrace(ioe);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Convert a xml document to a String
     * if tehd oceument is <code>null</code>, an empty string is returned.
     * @param docType the xml document type
     * @param xml the Document
     * @return  the value as a string
     * @throws  IllegalArgumentException if the value could not be converted
     */
    static private String convertXmlToString(String doctype, Document xml) {
        log.debug("converting from xml to string");

        // check for null values
        if (xml == null) {
            log.debug("field was empty");
            // string with null isnt allowed in mmbase...
            return "";
        }
        // check if we are using the right DOC-type for this field....
        //String doctype = parent.getField(fieldName).getDBDocType();
        if (doctype != null) {
            // we have a doctype... the doctype of the document has to mach the doctype of the doctype which is needed..
            org.w3c.dom.DocumentType type = xml.getDoctype();
            String publicId = type.getPublicId();
            if (doctype.indexOf(publicId) == -1) {
                //throw new RuntimeException("doctype('"+doctype+"') required by field '"+fieldName+"' and public id was NOT in it : '"+publicId+"'");
            }
            log.warn("doctype check can not completely be trusted");
        }

        try {
            //make a string from the XML
            TransformerFactory tfactory = org.mmbase.cache.xslt.FactoryCache.getCache().getDefaultFactory();
            Transformer serializer = tfactory.newTransformer();

            // for now, we save everything in ident form, this since it makes debugging a little bit more handy
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");

            // store as less as possible, otherthings should be resolved from gui-type
            serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            java.io.StringWriter str = new java.io.StringWriter();
            serializer.transform(new javax.xml.transform.dom.DOMSource(xml), new javax.xml.transform.stream.StreamResult(str));
            if (log.isDebugEnabled()) {
                log.debug("xml -> string:\n" + str.toString());
            }
            return str.toString();
        } catch (TransformerConfigurationException tce) {
            String message = tce.toString() + " " + Logging.stackTrace(tce);
            log.error(message);
            throw new IllegalArgumentException(message);
        } catch (TransformerException te) {
            String message = te.toString() + " " + Logging.stackTrace(te);
            log.error(message);
            throw new IllegalArgumentException(message);
        }
    }

    /*
     * Wraps a List with an 'Escaper'.
     * @since MMBase-1.8
     */
    public static class ListWrapper extends AbstractList{
        private final List list;
        private final CharTransformer escaper;
        ListWrapper (List l, CharTransformer e) {
            list = l;
            escaper = e;
        }
        public Object get(int index) { return escape(escaper, "" + list.get(index)); }
        public int size() { return list.size(); }
        public Object set(int index, Object value) { return list.set(index, value); }
        public void add(int index, Object value) { list.add(index, value); }
        public Object remove(int index) { return list.remove(index); }
        public boolean isEmpty() 	    {return list.isEmpty();}
        public boolean contains(Object o)   {return list.contains(o);}
        public Object[] toArray() 	    {return list.toArray();}
        public Object[] toArray(Object[] a) {return list.toArray(a);}
        public Iterator iterator() { return list.iterator(); }
        public ListIterator listIterator() { return list.listIterator(); }
        public String toString() {
            StringBuffer buf = new StringBuffer();
            Iterator i = list.iterator();
            boolean hasNext = i.hasNext();
            while (hasNext) {
                Casting.toStringBuffer(buf, i.next());
                hasNext = i.hasNext();
                if (hasNext) {
                    buf.append(',');
                }
            }
            return buf.toString();
        }
        public List getList() {
            return list;
        }
    }

    /**
     * Wraps a String with an 'Escaper'.
     * @since MMBase-1.8
     */
    public static class StringWrapper implements CharSequence {
        private final CharTransformer escaper;
        private final String string;
        private  String escaped = null;
        StringWrapper(String s, CharTransformer e) {
            escaper = e;
            string  = s;
            
        }
       
        public char charAt(int index) {
            toString();
            return escaped.charAt(index);
        }
        public int length() {
            toString();
            return escaped.length();
        }
        
        public CharSequence subSequence(int start, int end) {
            toString();
            return escaped.subSequence(start, end);
        }

        public String toString() {
            if (escaped == null) escaped = escape(escaper, string);
            return escaped;
        }
        public String getString() {
            return string;
        }
    }

}


