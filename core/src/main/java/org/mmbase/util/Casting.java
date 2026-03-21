/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

import java.io.*;
import java.math.BigDecimal;
import java.text.*;
import java.time.Duration;

import java.time.format.DateTimeParseException;

import java.util.*;
import java.util.function.Supplier;

import javax.xml.parsers.*;

import org.apache.commons.fileupload.FileItem;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.*;
import org.mmbase.datatypes.DataType;
import org.mmbase.datatypes.DataTypes;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.transformers.CharTransformer;
import org.mmbase.util.xml.XMLWriter;
import org.w3c.dom.Document;


/**
 * Collects MMBase specific 'cast' information, as static to... functions. This is used (and used to
 * be implemented) in MMObjectNode. But this functionality is more generic to MMbase.
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 * @version $Id$
 */


public class Casting {

    private static final Logger log = Logging.getLoggerInstance(Casting.class);


    /**
     * A Date formatter that creates a date based on a ISO 8601 date and a ISO 8601 time.
     * I.e. 2004-12-01 14:30:00.
     * It is NOT 100% ISO 8601, as opposed to {@link #ISO_8601_UTC}, as the standard actually requires
     * a 'T' to be placed between the date and the time.
     * The date given is the date for the local (server) time. Use this formatter if you want to display
     * user-friendly dates in local time.

     * XXX: According to http://en.wikipedia.org/wiki/ISO_8601, the standard allows ' ' in stead of
     * 'T' if no misunderstanding arises, which is the case here. So I don't think this is 'loose'.
     */
    public final static ThreadLocal<DateFormat> ISO_8601_LOOSE =
        new ThreadLocal<DateFormat>() {
        @Override
        protected synchronized DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            }
    };

    /**
     * A Date formatter that creates a ISO 8601 datetime according to UTC/GMT.
     * I.e. 2004-12-01T14:30:00Z.
     * This is 100% ISO 8601, as opposed to {@link #ISO_8601_LOOSE}.
     * Use this formatter if you want to export dates.
     *
     * XXX: Hmm, we parse with UTC now, while we don't store them as such.
     */
    public final static ThreadLocal<DateFormat> ISO_8601_UTC =
        new ThreadLocal<DateFormat>() {
        @Override
        protected synchronized DateFormat initialValue() {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                try {
                    df.setTimeZone(TimeZone.getTimeZone("UTC"));
                } catch (Throwable t) {
                    log.warn(t.getMessage(), t);
                }
                return df;
            }
    };

    public final static ThreadLocal<DateFormat> ISO_8601_DATE =
        new ThreadLocal<DateFormat>() {
        @Override
        protected synchronized DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            }
    };
    public final static ThreadLocal<DateFormat> ISO_8601_TIME =
        new ThreadLocal<DateFormat>() {
        @Override
        protected synchronized DateFormat initialValue() {
                return new SimpleDateFormat("HH:mm:ss", Locale.US);
            }
    };



    /**
     * Returns whether the passed object is of the given class.
     * Unlike Class instanceof this also includes Object Types that
     * are representative for primitive types (i.e. Integer for int).
     * @param type the type (class) to check
     * @param value the value whose type to check
     * @return <code>true</code> if compatible
     * @since MMBase-1.8
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
     * @param type the type (class)
     * @param value The value to be converted
     * @return value the converted value
     * @since MMBase-1.8
     */
    public static <C> C toType(Class<C> type, Object value) {
        return toType(type, null, value);
    }

    private static Cloud anonymousCloud = null;

    /**
     * Tries to 'cast' an object for use with the provided class. E.g. if value is a String, but the
     * type passed is Integer, then the string is act to an Integer.
     * If the type passed is a primitive type, the object is cast to an Object Types that is representative
     * for that type (i.e. Integer for int).
     * @param type the type (class)
     * @param cloud When casting to Node, a cloud may be needed. May be <code>null</code>, for an anonymous cloud to be tried.
     * @param value The value to be converted
     * @return value the converted value
     * @since MMBase-1.8
     */
    @SuppressWarnings("unchecked")
    public static <C> C toType(Class<C> type, Cloud cloud, Object value) {
        if (value != null && isType(type, value))  {
            return (C) value;
        } else {
            if (type.equals(Boolean.TYPE) || type.equals(Boolean.class)) {
                return (C) Boolean.valueOf(toBoolean(value));
            } else if (type.equals(Byte.TYPE) || type.equals(Byte.class)) {
                return (C) Byte.valueOf(toInteger(value).byteValue());
            } else if (type.equals(Character.TYPE) || type.equals(Character.class)) {
                String chars = toString(value);
                if (chars.length() > 0) {
                    return (C) Character.valueOf(chars.charAt(0));
                } else {
                    return (C) Character.valueOf(Character.MIN_VALUE);
                }
            } else if (type.equals(Short.TYPE) || type.equals(Short.class)) {
                return (C) Short.valueOf(toInteger(value).shortValue());
            } else if (type.equals(Integer.TYPE) || type.equals(Integer.class)) {
                return (C) toInteger(value);
            } else if (type.equals(Long.TYPE) || type.equals(Long.class)) {
                return (C) Long.valueOf(toLong(value));
            } else if (type.equals(Float.TYPE) || type.equals(Float.class)) {
                return (C) Float.valueOf(toFloat(value));
            } else if (type.equals(Double.TYPE) || type.equals(Double.class)) {
                return (C) Double.valueOf(toDouble(value));
            } else if (type.equals(Number.class)) {
                Number res;
                try {
                    res = Long.valueOf("" + value);
                } catch (NumberFormatException nfe) {
                    try {
                        res = Double.valueOf("" + value);
                    } catch (NumberFormatException nfe1) {
                        res = Integer.valueOf(-1);
                    }
                }
                return (C) res;
            } else if (type.equals(byte[].class)) {
                return (C) toByte(value);
            } else if (type.equals(SerializableInputStream.class)) {
                return (C) toSerializableInputStream(value);
            } else if (type.equals(String.class)) {
                return (C) toString(value);
            } else if (type.equals(CharSequence.class)) {
                return (C) toString(value);
            } else if (type.equals(Date.class)) {
                return (C) toDate(value);
            } else if (type.equals(Node.class)) {
                try {
                    if (cloud == null) {
                        cloud = CloudThreadLocal.currentCloud();
                        if (cloud == null) {
                            if (anonymousCloud == null || !anonymousCloud.getUser().isValid()) {
                                anonymousCloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase");
                            }
                            cloud = anonymousCloud;
                        }
                    }
                    return (C) toNode(value, cloud);
                } catch (Exception e) {
                    // suppose that that was because mmbase not running
                    return (C) (value instanceof Node ? value : null);
                }
            } else if (type.equals(Document.class)) {
                return (C) toXML(value);
            } else if (type.equals(List.class)) {
                return (C) toList(value);
            } else if (type.equals(Map.class)) {
                return (C) toMap(value);
            } else if (type.equals(Collection.class)) {
                return (C) toCollection(value);
            } else if (type.equals(BigDecimal.class)) {
                return (C) toDecimal(value);
            } else if (type.equals(java.util.regex.Pattern.class)) {
                if (java.util.regex.Pattern.class.isInstance(value)) {
                    return (C) value;
                }
                return (C) java.util.regex.Pattern.compile(toString(value));
            } else if (type.equals(Class.class)) {
                if (Class.class.isInstance(value)) {
                    return (C) value;
                }
                try {
                    return (C) Class.forName(toString(value));
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
           } else if (type.equals(org.mmbase.datatypes.DataType.class)) {
                return (C) toDataType(value);
           } else if (type.equals(org.mmbase.security.Operation.class)) {
                return (C) org.mmbase.security.Operation.getOperation(toString(value));
           } else if (type.equals(Locale.class)) {
                if (value instanceof Locale) {
                    return (C) value;
                } else {
                    return (C) new Locale(toString(value));
                }
           } else if (type.equals(TimeZone.class)) {
                if (value == null || "".equals(value)) return null;
                if (value instanceof TimeZone) {
                    return (C) value;
                } else {
                    return (C) TimeZone.getTimeZone(toString(value));
                }
            } else if (type.equals(Collator.class)) {
                if (value instanceof Collator) {
                    return (C) value;
                } else {
                    return (C) LocaleCollator.getInstance(toString(value));
                }
            } else {
                log.error("Don't know how to convert to " + type, new Exception());
                if (value == null || "".equals(value)) {
                    // just to avoid the error
                    return null;
                }

                // don't know
                return (C) value;
            }
        }
    }

    /**
     * This method should report whether {@link #toType} is implemented for given 2 classes.
     *
     * It is not yet fully implemented.
     *
     * @since MMBase-1.9.2
     */
    public static boolean canCast(Class<? extends Object> from, Class<? extends Object> to) {
        return isStringRepresentable(from) && isStringRepresentable(to);
    }

    /**
     * Whether or not Casting can more or less reliably cast a certain type to String and back.
     * For collection types also the entries of the collection must be string representable.
     * @since MMBase-1.8
     */
    public static boolean isStringRepresentable(Class<?> type) {
        return
            CharSequence.class.isAssignableFrom(type) ||
            Number.class.isAssignableFrom(type) ||
            Boolean.TYPE.isAssignableFrom(type) ||
            Boolean.class.isAssignableFrom(type) ||
            Character.class.isAssignableFrom(type) ||
            Node.class.isAssignableFrom(type) ||
            Document.class.isAssignableFrom(type) ||
            Collection.class.isAssignableFrom(type) ||
            Date.class.isAssignableFrom(type) ||
            Map.class.isAssignableFrom(type) ||
                Duration.class.isAssignableFrom(type);
    }



    /**
     * Convert an object to a String.
     * <code>null</code> is converted to an empty string.
     * @param o the object to convert
     * @return the converted value as a <code>String</code>
     */
    public static String toString(Object o) {
        if (o instanceof String) {
            return (String)o;
        }
        if (o == null || "".equals(o)) {
            return "";
        }

        return toStringBuilder(new StringBuilder(), o).toString();
    }

    /**
     * Convert an object to a string, using a StringBuffer.
     * @param buffer The StringBuffer with which to create the string
     * @param o the object to convert
     * @return the StringBuffer used for conversion (same as the buffer parameter)
     * @since MMBase-1.7
     */
    public static StringBuffer toStringBuffer(StringBuffer buffer, Object o) {
        if (o == null) {
            return buffer;
        }
        try {
            toWriter(new StringBufferWriter(buffer), o);
        } catch (IOException e) {}
        return buffer;
    }

    /**
     * Convert an object to a string, using a StringBuilder
     * @param buffer The StringBuilder with which to create the string
     * @param o the object to convert
     * @return the StringBuilder used for conversion (same as the buffer parameter)
     * @since MMBase-1.9
     */
    public static StringBuilder toStringBuilder(StringBuilder buffer, Object o) {
        if (o == null) {
            return buffer;
        }
        try {
            toWriter(new StringBuilderWriter(buffer), o);
        } catch (IOException e) {}
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
        } else if (o instanceof SortedBundle.ValueWrapper) {
            o = ((SortedBundle.ValueWrapper) o).getKey();
        }
        Object s = wrap(o, null);
        if (s instanceof org.mmbase.bridge.Query) {
            s = ((org.mmbase.bridge.Query) s).toSql();
        }
        writer.write(s.toString());
        return writer;
    }

    /**
     * Wraps an object in another object with a toString as we desire. Casting can now be done with
     * toString() on the resulting object.
     *
     * This is used to make JSTL en EL behave similarly as mmbase taglib when writing objects to the
     * page (taglib calls Casting, but JSTL of course doesn't).
     *
     * @todo  Not everything is wrapped (and can be unwrapped) already.
     * @param o        The object to be wrapped
     * @param escaper  <code>null</code> or a CharTransformer to pipe the strings through
     * @since MMBase-1.8
     */

    public static Object wrap(final Object o, final CharTransformer escaper) {
        if (o == null) {
            return escape(escaper, "");
        } else if (o instanceof Unwrappable) {
            return o;
        } else if (o instanceof Node) {
            return new NodeMap((Node)o) {

                @Override
                public Object getValue(String fieldName) {
                    NodeManager nm = getNodeManager();
                    if (nm.hasField(fieldName)) {
                        switch (nm.getField(fieldName).getType()) {
                        case org.mmbase.bridge.Field.TYPE_NODE:
                            // I don't understand why, but the 'number' field is of type NODE,
                            // which makes no sense whatsoever.
                            if (!"number".equals(fieldName)) {
                                return wrap(getNodeValue(fieldName), escaper);
                            } else {
                                return super.getStringValue(fieldName);
                            }
                        case org.mmbase.bridge.Field.TYPE_DATETIME:
                            return wrap(getDateValue(fieldName), escaper);
                        case org.mmbase.bridge.Field.TYPE_XML:
                            return wrap(getXMLValue(fieldName), escaper);
                        case org.mmbase.bridge.Field.TYPE_UNKNOWN:
                            log.debug("NodeManager " + nm + " has field " + fieldName + " but it is of unknown type.");
                            return wrap(super.getValueWithoutProcess(fieldName), escaper);
                        default:
                            return escape(escaper, Casting.toString(super.getValue(fieldName)));
                        }
                    } else {
                        return escape(escaper, Casting.toString(super.getValue(fieldName)));
                    }
                }
                @Override
                public String toString() {
                    return escape(escaper, "" + node.getNumber());
                }
            };
        } else if (o instanceof Date) {
            return new java.util.Date(((Date)o).getTime()) {
                private static final long serialVersionUID = 1L; // increase this if object chages.

                @Override
                public String toString() {
                    long time = getTime();
                    return time == -1 ? "-1" : ("" + time / 1000);
                }
            };
        } else if (o instanceof org.w3c.dom.Node) {
            // don't know how to wrap
            return escape(escaper, XMLWriter.write((org.w3c.dom.Node) o, false, true));
        } else if (o instanceof org.mmbase.bridge.NodeList) {
            return new NodeListWrapper((org.mmbase.bridge.NodeList) o, escaper);
        } else if (o instanceof List) {
            return new ListWrapper((List) o, escaper);
        } else if (o instanceof byte[]) {
            return escape(escaper, new String((byte[])o));
        } else if (o instanceof Object[]) {
            return new ListWrapper(Arrays.asList((Object[])o), escaper);
        } else if (o instanceof String) { // It is important that a String remains a String, because all kind of tools
                                          // want actual Strings.
            return escape(escaper, (String) o);
        } else if (o instanceof CharSequence) {
            return new StringWrapper((CharSequence) o, escaper);
        } else if (o instanceof InputStream) {
            try {
                return new StringSerializableInputStream(toSerializableInputStream(o), escaper);
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        } else {
            return o;
        }


    }

    private static String escape(CharTransformer escaper, CharSequence string) {
        if (escaper != null) {
            return escaper.transform(string == null ? "" : string.toString());
        } else {
            return string == null ? "" : string.toString();
        }
    }
    /**
     * When you want to undo the wrapping, this method can be used.
     * @since MMBase-1.8
     */
    public static Object unWrap(final Object o) {
        if (o instanceof NodeWrapper) {
            return ((NodeWrapper)o).getNode();
        } else if (o instanceof NodeListWrapper) {
            return ((NodeListWrapper)o).getCollection();
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
        return toList(o, ",");
    }


    /**
     * As {@link #toList(Object)} but with one extra argument.
     *
     * @param delimiter Regexp to use when splitting up the string if the object is a String. <code>null</code> or the empty string mean the default, which is a comma.
     * @since MMBase-1.8
     */
    public static List toList(Object o, String delimiter) {
        if (o instanceof List) {
            return (List)o;
        } else if (o instanceof Collection) {
            return new ArrayList((Collection) o);
        } else if (o instanceof String) {
            if ("".equals(delimiter) || delimiter == null) delimiter = ",";
            return StringSplitter.split((String)o, delimiter);
        } else if (o instanceof Map) {
            return new ArrayList(((Map)o).entrySet());
        } else {
            if (o == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(o);
        }
    }


    /**
     * @since MMBase-1.8
     */
    public static Map toMap(Object o) {
        if (o == null) {
            return new HashMap();
        } else if (o instanceof Map) {
            return (Map) o;
        } else if (o instanceof org.mmbase.util.functions.Parameters) {
            return ((org.mmbase.util.functions.Parameters) o).toMap();
        } else if (o instanceof Collection) {
            Map result = new HashMap();
            Iterator i = ((Collection)o).iterator();
            while (i.hasNext()) {
                Object n = i.next();
                if (n instanceof Map.Entry) {
                    Map.Entry entry = (Map.Entry) n;
                    result.put(entry.getKey(), entry.getValue());
                } else {
                    result.put(n, n);
                }
            }
            return result;
        } else if (o instanceof Node) {
            return new NodeMap((Node)o);
        } else {
            Map m = new HashMap();
            m.put(o, o);
            return m;
        }
    }

    /**
     * Transforms an object to a collection. If the object is a collection already, then nothing
     * happens. If it is a Map, then the 'entry set' is returned. A string is interpreted as a
     * comma-separated list of strings. Other objects are wrapped in an ArrayList with one element.
     *
     * @since MMBase-1.8.5
     */
    public static Collection toCollection(Object o, String delimiter) {
        if (o instanceof Collection) {
            return (Collection)o;
        } else if (o instanceof Map) {
            return ((Map)o).entrySet();
        } else if (o instanceof String) {
            if ("".equals(delimiter) || delimiter == null) delimiter = ",";
            return StringSplitter.split((String)o, delimiter);
        } else if (o instanceof Object[]) {
            return Arrays.asList((Object[]) o);
        } else {
            if (o == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(o);
        }
    }
    /**
     * @since MMBase-1.8
     */
    public static Collection toCollection(Object o) {
        return toCollection(o, ",");
    }


    /**
     * Convert the value to a <code>Document</code> object.
     * If the value is not itself a Document, the method attempts to
     * attempts to convert the String value into an XML.
     * A <code>null</code> value is returned as <code>null</code>.
     * If the value cannot be converted, this method throws an IllegalArgumentException.
     * @param o the object to be converted to an XML document
     * @return  the value as a DOM Element or <code>null</code>
     * @throws  IllegalArgumentException if the value could not be converted
     * @since MMBase-1.6
     */
    static public Document toXML(Object o) {
        if (o == null) return null;
        if (!(o instanceof Document)) {
            //do conversion from String to Document...
            // This is a laborous action, so we log it on debug.
            // It will happen often if the nodes are not cached and so on.
            String xmltext = toString(o);
            if (log.isDebugEnabled()) {
                String msg = xmltext;
                if (msg.length() > 84) {
                    msg = msg.substring(0, 80) + "...";
                }
                log.debug("Object '" + msg + "' is not a Document, but a " + o.getClass().getName() + "");
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
        if (obj == null) {
            log.debug("Converted null to empty byte array", new Exception());
            return new byte[] {};
        } else if (obj instanceof byte[]) {
            if (log.isDebugEnabled()) {
                log.debug("Already byte array " + obj + " l:" + ((byte[]) obj).length, new Exception());
            }
            // was allready unmapped so return the value
            return (byte[])obj;
        } else if (obj instanceof FileItem) {
            return ((FileItem) obj).get();
        } else if (obj instanceof SerializableInputStream) {
            try {
                SerializableInputStream is = (SerializableInputStream) obj;
                return is.get();
            } catch (IOException ioe) {
                log.error(ioe);
                return new byte[0];
            }
        } else if (obj instanceof InputStream) {
            log.debug("IS " + obj);
            InputStream in = (InputStream) obj;
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            try {
                IOUtil.copy(in, out);
            } catch (IOException ioe) {
                log.error(ioe);
            } finally {
                try { in.close(); } catch (IOException ioe) {}
            }
            return out.toByteArray();
        } else {
            log.debug("S " + obj.getClass() + " " + obj, new Exception());
            return toString(obj).getBytes();
        }
    }

    static public InputStream toInputStream(Object obj) {
        if (obj instanceof InputStream) {
            return (InputStream) obj;
        } else {
            return toSerializableInputStream(obj);
        }
    }
    static public SerializableInputStream toSerializableInputStream(Object obj) {
        if (obj instanceof SerializableInputStream) {
            return (SerializableInputStream) obj;
        } else if (obj instanceof byte[]) {
            return new SerializableInputStream((byte[]) obj);
        } else if (obj instanceof FileItem) {
            try {
                return new SerializableInputStream((FileItem) obj);
            } catch (IOException ioe) {
                log.error(ioe.getMessage(), ioe);
                return new SerializableInputStream(new byte[0]);
            }
        } else  {
            return new SerializableInputStream(toByte(obj));
        }
    }


    /**
     * Convert an object to an Node.
     * If the value is Numeric, the method
     * tries to obtain the mmbase object with that number.
     * A <code>Map</code> returns a virtual <code>Node</code> representing the map, (a
     * {@link MapNode}).
     * All remaining situations return the node with the alias <code>i.toString()</code>, which can
     * be <code>null</code> if no node which such an alias.
     * @param i the object to convert
     * @param cloud the Cloud to use for loading a node
     * @return the value as a <code>Node</code>
     * @since MMBase-1.7
     */
    public static Node toNode(Object i, Cloud cloud) {
        Node res = null;
        if (i instanceof Node) {
            res = (Node)i;
        } else if (i instanceof Number) {
            int nodenumber = ((Number)i).intValue();
            if (nodenumber != -1 && cloud.hasNode(nodenumber)) {
                res = cloud.getNode(nodenumber);
            }
        } else if (i instanceof Map<?, ?>) {
            res = new MapNode((Map)i, cloud);
        } else if (i != null && !i.equals("")) {
            res = cloud.getNode(toString(i));
        }
        return res;
    }

    /**
     * Convert an object to an <code>int</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * If a value is an Node, it's number field is returned.
     * All remaining values return the provided default value.
     * @param i the object to convert
     * @param def the default value if conversion is impossible
     * @return the converted value as an <code>int</code>
     * @since MMBase-1.7
     */
    static public int toInt(Object i, int def) {
        int res = def;
        if (i == null) {
            return def;
        } else if (i instanceof Number) {
            long l = ((Number)i).longValue();
            if (l > Integer.MAX_VALUE) {
                res = Integer.MAX_VALUE;
            } else if (l < Integer.MIN_VALUE) {
                res = Integer.MIN_VALUE;
            } else {
                res = (int) l;
            }
        } else if (i instanceof Node) {
            res = ((Node)i).getNumber();
        } else if (i instanceof Boolean) {
            res = ((Boolean)i).booleanValue() ? 1 : 0;
        } else if (i instanceof Date) {
            long timeValue = ((Date)i).getTime();

            if (timeValue != -1) timeValue /= 1000;

            if (timeValue > Integer.MAX_VALUE) {
                res = Integer.MAX_VALUE;
            } else if (timeValue < Integer.MIN_VALUE) {
                res = Integer.MIN_VALUE;
            } else {
                res = (int) timeValue;
            }
        } else if (i instanceof Object[]) {
            Object[] array = (Object[]) i;
            if (array.length == 0) return 0;
            if (array.length >= 1) return toInt(array[0], def);
        } else if (i != null) {
            try {
                res = Integer.parseInt("" + i);
            } catch (NumberFormatException e) {
                // not an integer? perhaps it is a float or double represented as String.
                try {
                    res = toInt(Double.valueOf("" + i), def); // recursive to hit the check on MAX_VALUE/MIN_VALUE also here.
                } catch (NumberFormatException ex) {
                    // try if the value is a string representing a boolean.
                    if(i instanceof String){
                        String s = ((String)i).toLowerCase();
                        if (s.equals("true") || s.equals("yes")) {
                            res = 1;
                        } else if(s.equals("false") || s.equals("no")) {
                            res = 0;
                        }
                    }
                }
            }
        }
        return res;
    }

    /**
     * Convert an object to an <code>int</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * If a value is a Node, it's number field is returned.
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
     * and '-1' are considered <code>false</code>.
     * If the value is a string, this call returns <code>true</code> if
     * the value is "true" or "yes" (case-insensitive).
     * In all other cases (including calling byte fields), <code>false</code>
     * is returned.
     * @param b the object to convert
     * @return the converted value as a <code>boolean</code>
     */
    static public boolean toBoolean(Object b) {
        if (b == null) {
            return false;
        } else if (b instanceof Boolean) {
            return ((Boolean)b).booleanValue();
        } else if (b instanceof Number) {
            return ((Number)b).doubleValue() > 0;
        } else if (b instanceof Node) {
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
            return Integer.valueOf(toInt(i));
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
            if (res !=- 1) res /= 1000;
        } else if (i instanceof Node) {
            res = ((Node)i).getNumber();
        } else if (i instanceof Object[]) {
            Object[] array = (Object[]) i;
            if (array.length == 0) return 0;
            if (array.length >= 1) return toLong(array[0], def);
        } else if (i != null) {
            if(i instanceof String){
                String s = ((String)i).toLowerCase();
                if (s.equals("true") || s.equals("yes")) {
                    return 1;
                } else if(s.equals("false") || s.equals("no")) {
                    return 0;
                }
            }
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
        } else if (i instanceof Node) {
            res = ((Node)i).getNumber();
        } else if (i != null) {
            if(i instanceof String){
                String s = ((String)i).toLowerCase();
                if (s.equals("true") || s.equals("yes")) {
                    res = 1;
                } else if(s.equals("false") || s.equals("no")) {
                    res = 0;
                }
            }
            try {
                res = Float.parseFloat("" + i);
            } catch (NumberFormatException e) {
                // use default
            }
        }
        return res;
    }

    /**
     * Convert an object to an <code>float</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * All remaining values return -1.
     * @param i the object to convert
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
            if (res != -1) res = res / 1000;
        } else if (i instanceof Node) {
            res = ((Node)i).getNumber();
        } else if (i instanceof Object[]) {
            Object[] array = (Object[]) i;
            if (array.length == 0) return 0;
            if (array.length >= 1) return toDouble(array[0], def);
        } else if (i != null) {
            try {
                res = Double.parseDouble("" + i);
            } catch (NumberFormatException e) {
//              try if the value is a string representing a boolean.
                if(i instanceof String){
                    String s = ((String)i).toLowerCase();
                    if (s.equals("true") || s.equals("yes")) {
                        res = 1;
                    } else if(s.equals("false") || s.equals("no")) {
                        res = 0;
                    }
                }
            }
        }
        return res;
    }

    /**
     * Convert an object to an <code>double</code>.
     * Boolean values return 0 for false, 1 for true.
     * String values are parsed to a number, if possible.
     * All remaining values return -1.
     * @param i the object to convert
     * @return the converted value as a <code>double</code>
     */
    static public double toDouble(Object i) {
        return toDouble(i, -1);
    }


    /**
     * @since MMBase-1.9.1
     */
    static public BigDecimal toDecimal(Object i) {
        if (i instanceof BigDecimal) {
            return (BigDecimal) i;
        } else if (i instanceof CharSequence) {
            try {
                return new BigDecimal("" + i).stripTrailingZeros();
            } catch (NumberFormatException nfe) {
                if(i instanceof String){
                    String s = ((String)i).toLowerCase();
                    if (s.equals("true") || s.equals("yes")) {
                        return BigDecimal.ONE;
                    } else if(s.equals("false") || s.equals("no")) {
                        return BigDecimal.ZERO;
                    }
                }
                return BigDecimal.ONE.negate();
            }
        } else if (i instanceof Long) {
            return new BigDecimal((Long) i);
        } else if (i instanceof Integer) {
            return new BigDecimal((Integer) i);
        } else if (i instanceof Double) {
            return new BigDecimal((Double) i);
        } else if (i instanceof Float) {
            return new BigDecimal((Float) i);
        } else {
            return new BigDecimal(toDouble(i)).stripTrailingZeros();
        }
    }

    /**
     * @since MMBase-1.9.1
     */
    static public DataType<?> toDataType(Object o) {
        if (o instanceof DataType<?>) {
            return (DataType<?>) o;
        } else {
            return DataTypes.getDataType(toString(o));
        }

    }




    /**
     * Convert an object to a <code>Date</code>.
     * String values are parsed to a date, if possible.
     * Numeric values are assumed to represent number of seconds since 1970.
     * All remaining values return 1969-12-31 23:59 GMT.
     * @param d the object to convert
     * @return the converted value as a <code>Date</code>, never <code>null</code>
     * @since MMBase-1.7
     */
    static public Date toDate(Object d) {
        if (d == null) return new Date(-1);
        Date date = null;

        if (d instanceof Date) {
            date = (Date) d;
        } else {
            try {
                long dateInSeconds = -1;
                if (d instanceof Number) {
                    dateInSeconds = ((Number)d).longValue();
                } else if (d instanceof Document) {
                    // impossible
                    dateInSeconds = -1;
                } else if (d instanceof Boolean) {
                    dateInSeconds = -1;
                } else if (d instanceof Collection<?>) {
                    // impossible
                    dateInSeconds = -1;
                } else if (d instanceof Node) {
                    // impossible
                    dateInSeconds = -1;
                } else if (d != null) {
                    d = toString(d);
                    if (d.equals("")) {
                        return new Date(-1);
                    }
                    dateInSeconds = Long.parseLong((String) d);
                } else {
                    dateInSeconds = -1;
                }
                if (dateInSeconds == -1) {
                    date = new Date(-1);
                } else if (dateInSeconds > Long.MAX_VALUE / 1000) {
                    date = new Date(Long.MAX_VALUE); // or should this throw an exception?
                } else if (dateInSeconds < Long.MIN_VALUE / 1000) {
                    date = new Date(Long.MIN_VALUE); // or should this throw an exception?
                } else {
                    date = new Date(dateInSeconds * 1000);
                }
            } catch (NumberFormatException e) {
                try {
                    date =  DynamicDate.getInstance((String) d);
                } catch (org.mmbase.util.dateparser.ParseException pe) {
                    log.error("Parser exception in " + d, pe);
                    return new Date(-1);
                } catch (Error per) {
                    throw new Error("Parser error in " + d, per);
                }
            }
        }
        return date;
    }
    static public Duration toDuration(Object i, Supplier<Duration> def) {
        if (i instanceof Duration) {
            return (Duration) i;
        } else if (i instanceof Number) {
            return Duration.ofMillis((long) (((Number) i).doubleValue() * 1000L));
        } else if (i instanceof CharSequence) {
            String s = i.toString();
            if (s.startsWith("T")) {
                s = "P" + s;
            }
            try {
                return Duration.parse(s);
            } catch (DateTimeParseException e) {
                return Duration.ofMillis((long) (Double.parseDouble(s) * 1000L));
            }
        }
        return def.get();
    }
    static public Duration toDuration(Object i) {
        return toDuration(i, () -> {
            throw new IllegalArgumentException();
        });
    }


        static DocumentBuilder DOCUMENTBUILDER;
    static {
        try {
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            dfactory.setValidating(false);
            dfactory.setNamespaceAware(true);
            DOCUMENTBUILDER = dfactory.newDocumentBuilder();
            DOCUMENTBUILDER.setEntityResolver(new org.mmbase.util.xml.EntityResolver(false));
        } catch (ParserConfigurationException pce) {
            log.error("[sax parser]: " + pce.toString(), pce);
        } catch (Exception e) {
            log.error(e);
        }
        assert DOCUMENTBUILDER != null;
    }
    /**
     * Convert a String value to a Document
     * @param value The current value (can be null)
     * @return  the value as a DOM Element or <code>null</code>
     * @throws  IllegalArgumentException if the value could not be converted
     */
    static private Document convertStringToXML(String value) {
        if (value == null) {
            return null;
        }
        if (log.isTraceEnabled()) {
            log.trace("using xml string:\n" + value);
        }
        try {
            Document doc;
            final org.mmbase.util.xml.ErrorHandler errorHandler = new org.mmbase.util.xml.ErrorHandler(false, org.mmbase.util.xml.ErrorHandler.NEVER);
            assert DOCUMENTBUILDER != null;
            synchronized(DOCUMENTBUILDER) {
                // dont log errors, and try to process as much as possible...
                DOCUMENTBUILDER.setErrorHandler(errorHandler);
                // ByteArrayInputStream?
                // Yes, in contradiction to what one would think, XML are bytes, rather then characters.
                doc = DOCUMENTBUILDER.parse(new ByteArrayInputStream(value.getBytes("UTF-8")));
            }
            if (log.isTraceEnabled()) {
                log.trace("parsed: " + XMLWriter.write(doc, false, true));
            }
            if (!errorHandler.foundNothing()) {
                throw new IllegalArgumentException("xml invalid:\n" + errorHandler.getMessageBuffer() + "for xml:\n" + value);
            }
            return doc;
        } catch (org.xml.sax.SAXException se) {
            if (log.isDebugEnabled()) {
                log.debug("[sax] not well formed xml: " + se.toString() + "(" + se.getMessage() + ")\n" + Logging.stackTrace(se));
            }
            return convertStringToXML("<p>" + Encode.encode("ESCAPE_XML", value) + "</p>"); // Should _always_ be sax-compliant.
        } catch (IOException ioe) {
            String msg = "[io] not well formed xml: " + ioe.toString() + "\n" + Logging.stackTrace(ioe);
            throw new IllegalArgumentException(msg);
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
        public Object get(int index) { return Casting.wrap(list.get(index), escaper); }
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
            StringBuilder buf = new StringBuilder();
            Iterator i = list.iterator();
            boolean hasNext = i.hasNext();
            while (hasNext) {
                Casting.toStringBuilder(buf, i.next());
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
     * @since MMBase-1.9
     */
    public static class NodeListWrapper extends org.mmbase.bridge.util.CollectionNodeList implements Unwrappable {
        private final CharTransformer escaper;
        NodeListWrapper(org.mmbase.bridge.NodeList list, CharTransformer e) {
            super(list);
            for (Map.Entry<Object, Object> entry : list.getProperties().entrySet()) {
                setProperty(entry.getKey(), entry.getValue());
            }
            escaper = e;
        }
        public Node get(int index) {
            return (Node) Casting.wrap(super.get(index), escaper);
        }
        public String toString() {
            StringBuilder buf = new StringBuilder();
            Iterator<Node> i = iterator();
            boolean hasNext = i.hasNext();
            while (hasNext) {
                Casting.toStringBuilder(buf, i.next());
                hasNext = i.hasNext();
                if (hasNext) {
                    buf.append(',');
                }
            }
            return buf.toString();
        }

    }

    /**
     * Wraps a String with an 'Escaper'.
     * @since MMBase-1.8
     */
    public static class StringWrapper implements CharSequence {
        private final CharTransformer escaper;
        private final CharSequence string;
        private  String escaped = null;
        StringWrapper(CharSequence s, CharTransformer e) {
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

        @Override
        public String toString() {
            if (escaped == null) escaped = escape(escaper, string);
            return escaped;
        }
        public CharSequence getString() {
            return string;
        }
    }

    /**
     * Clases implementing this will not be wrapped by {@link #wrap}, even if the e.g. are CharSequence.
     * @since MMBase-1.9
     */
    public static interface Unwrappable {
    }

    /**
     * @since MMBase-1.9
     */
    public static boolean equals(Object o1, Object o2) {
        if (o1 == null) return o2 == null;

        if (o1 instanceof org.w3c.dom.Node) {
            return (o2 instanceof org.w3c.dom.Node && ((org.w3c.dom.Node) o1).isEqualNode((org.w3c.dom.Node) o2));
        } else {
            return o1.equals(o2);
        }
    }

    /**
     * A SerializableInputStream where the toString represents the (escaped) contents of the stream itself.
     * @since MMBase-1.9.2
     */
    static class StringSerializableInputStream extends SerializableInputStream implements Unwrappable {
        private static final long serialVersionUID = 2L;

        CharTransformer escaper;
        StringSerializableInputStream(SerializableInputStream is, CharTransformer e) throws IOException {
            super(is);
            escaper = e;
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            _writeObject(out);
            out.writeObject(escaper);
        }
        private void readObject(java.io.ObjectInputStream oin) throws IOException, ClassNotFoundException {
            _readObject(oin);
            escaper = (CharTransformer) oin.readObject();
        }


        @Override
        public String toString() {
            try {
                return Casting.escape(escaper, new String(get()));
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    private Casting() {
    }

}


