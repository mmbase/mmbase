/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.jsp.taglib.typehandler;

import javax.servlet.jsp.JspTagException;
import org.mmbase.bridge.*;
import org.mmbase.datatypes.DataType;
import org.mmbase.storage.search.Constraint;
import org.mmbase.bridge.jsp.taglib.*;
import org.mmbase.util.Casting;
import org.mmbase.util.transformers.Xml;

import java.util.*;

import org.mmbase.util.SortedBundle;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This handler can be used to create option list by use of a resource.
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 * @version $Id$
 */

public class EnumHandler extends AbstractTypeHandler implements TypeHandler {

    private static final Logger log = Logging.getLoggerInstance(EnumHandler.class);
    private static final Xml XML = new Xml(Xml.ESCAPE);
    private Iterator iterator;
    private boolean available;

    private boolean multiple = false;

    /**
     * @since MMBase-1.8
     */
    public EnumHandler(FieldInfoTag tag) throws JspTagException {
        super(tag);
        available = true;
    }

    public void setMultiple(boolean m) {
        multiple = m;
    }

    /**
     * @since MMBase-1.8
     */
    protected Iterator getIterator(Node node, Field field) throws JspTagException  {
        DataType<Object> dataType = field.getDataType();
        Locale locale = tag.getLocale();
        return dataType.getEnumerationValues(locale, tag.getCloudVar(), node, field);
    }
    /**
     * @param tag
     * @since MMBase-1.8
     */
    public EnumHandler(FieldInfoTag tag, Node node,  Field field) throws JspTagException {
        super(tag);
        iterator = getIterator(node, field);
        if (iterator == null) {
            // backwards compatibility mode
            String enumType = field.getGUIType();
            try {
                Class.forName(enumType);
            } catch (Exception ee) {
                try {
                    String resource;
                    if (enumType.indexOf('.') == -1 ) {
                        resource = "org.mmbase.bridge.jsp.taglib.typehandler.resources." + enumType;
                    } else {
                        resource = enumType;

                    }
                    Class<?> type;
                    switch(field.getType()) {
                    case Field.TYPE_STRING:  type = String.class; break;
                    case Field.TYPE_INTEGER: type = Integer.class; break;
                    case Field.TYPE_LONG:    type = Long.class; break;
                    case Field.TYPE_NODE:    type = Node.class; break;

                        // I wonder if enums for the following types could make any sense, but well:
                    case Field.TYPE_FLOAT:   type = Float.class; break;
                    case Field.TYPE_DOUBLE:  type = Double.class; break;
                    case Field.TYPE_BINARY:    type = byte[].class; break;
                    case Field.TYPE_XML:     type = String.class; break; // Document.class ?

                        /*
                          case Field.TYPE_DATETIME:  type = Date.class; break;
                          case Field.TYPE_BOOLEAN:   type = Boolean.class; break;
                          case Field.TYPE_LIST:     //  type = Boolean.class; break;
                        */
                    default: type = String.class;
                    }


                    iterator    = SortedBundle.getResource(resource, tag.getLocale(), getClass().getClassLoader(),
                                                         SortedBundle.NO_CONSTANTSPROVIDER, type, SortedBundle.NO_COMPARATOR).entrySet().iterator();
                } catch (java.util.MissingResourceException e) {
                    log.warn(e.toString() + " for field " + field.getName() + " of builder " + field.getNodeManager().getName());
                }
            }
        }
        available = iterator != null;
    }


    @Override
    protected EnumHandler getEnumHandler(Node node, Field field) throws JspTagException {
        return null;
    }
    public boolean isAvailable() {
        return available;
    }

    @Override
    protected Object cast(Object value, Node node, Field field) {
        if ("".equals(value)) return null;
        return field.getDataType().cast(value, node, field);
    }


    @Override
    protected Object convertToValidate(final Object s, final Node node, final Field field) throws JspTagException {
        DataType dt = field.getDataType();
        if (s == null && ! dt.isRequired()) {
            return null;
        }
        String ss = Casting.toString(s);
        Iterator i = getIterator(node, field);
        Object firstValue = null;
        if (i!= null && i.hasNext()) {
            Object value = ((Map.Entry) i.next()).getKey();
            if (log.isDebugEnabled()) {
                log.debug("Comparing " + value + " to " + s);
            }
            if (Casting.toString(value).equals(ss)) {
                return value;
            }
            firstValue = value;
        }
        while (i!= null && i.hasNext()) {
            Object value = ((Map.Entry) i.next()).getKey();
            if (log.isDebugEnabled()) {
                log.debug("Comparing " + value + " to " + s);
            }
            if (Casting.toString(value).equals(ss)) {
                return value;
            }
        }
        log.debug("Returning firstValue");
        return firstValue;

    }


    @Override
    public String htmlInput(final Node node, final Field field, final boolean search) throws JspTagException {
        StringBuilder buffer = new StringBuilder();
        String fieldName = field.getName();
        buffer.append("<select class=\"").append(getClasses(node, field)).append("\" name=\"").append(prefix(fieldName)).append("\" ");
        buffer.append("id=\"").append(prefixID(fieldName)).append("\" ");
        if (multiple) {
            buffer.append("multiple=\"multiple\" ");
        }
        addExtraAttributes(buffer);
        buffer.append(">");
        Object uncastValue = getFieldValue(node, field, true);
        Object unvalidValue  = cast(uncastValue, node, field);
        Object value         = multiple ? unvalidValue : convertToValidate(unvalidValue, node, field);
        if (log.isDebugEnabled()) {
            log.debug("cast " + uncastValue + " -> " + unvalidValue + " -> " + value + " -> " + Casting.toString(value));
            //cast 191 ->          [Users (191)] ->     Administrators (189) -> 189
        }

        if (! field.getDataType().isRequired() && ! multiple) {
            buffer.append("<option value=\"\" ");
            if (value == null) buffer.append("selected=\"selected\" ");
            buffer.append(">--</option>");
        }
        if (iterator == null) {
            iterator = getIterator(node, field);
        }

        List<String> valueString = multiple ? new ArrayList<String>() : Collections.singletonList(Casting.toString(value));

        if (log.isDebugEnabled()) {
            log.debug("using value for " + field + " " + (value == null ? "NULL" : value.getClass().getName() + " " + value) + " asString: " + valueString);
        }
        if (multiple) {
            for (Object v : Casting.toList(value)) {
                valueString.add(Casting.toString(v));
            }
        }
        String maxLength =  tag.getPageContext().getServletContext().getInitParameter("mmbase.taglib.max_enumhandler_length");
        int max = maxLength != null && maxLength.length() > 0 ? Integer.parseInt(maxLength) : Integer.MAX_VALUE;
        int count = 0;
        while(iterator != null && iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            count++;
            if (count > max) {
                log.debug("Enumeration truncated, because longer than " + max);
                break;
            }
            Object key = entry.getKey();
            if (key == null) {
                log.warn("Found null as enumeration key for " + field.getDataType());
                continue;
            }
            String keyString = Casting.toString(key);
            buffer.append("<option value=\"");
            buffer.append(XML.transform(keyString));
            buffer.append("\"");
            if (valueString.contains(keyString)) {
                buffer.append(" selected=\"selected\"");
            } else if (search) {
                String searchs = Casting.toString(tag.getContextProvider().getContextContainer().find(tag.getPageContext(), prefix(field.getName())));
                if (keyString.equals(searchs)) {
                    buffer.append(" selected=\"selected\"");
                }
            }
            buffer.append(">");
            buffer.append(XML.transform(Casting.toString(entry.getValue())));
            // buffer.append(Casting.toString(entry.getValue()));
            buffer.append("</option>");
        }
        buffer.append("</select>");
        if (search) {
            String name = prefix(field.getName()) + "_search";
            String fieldid = prefixID(field.getName() + "_search");
            String searchi =  Casting.toString(tag.getContextProvider().getContextContainer().find(tag.getPageContext(), name));
            buffer.append("<input type=\"checkbox\" name=\"").append(name).append("\" ");
            buffer.append("id=\"").append(fieldid).append("\" ");
            if (! "".equals(searchi)) {
                buffer.append(" checked=\"checked\"");
            }
            buffer.append(" />");
        }
        return buffer.toString();
    }


    public void paramHtmlInput(ParamHandler handler, Field field) throws JspTagException  {
        String name = prefix(field.getName() + "_search");
        String searchi =  Casting.toString(tag.getContextProvider().getContextContainer().find(tag.getPageContext(), name));
        if (! "".equals(searchi)) {
            handler.addParameter(name, "on");
        }
        super.paramHtmlInput(handler, field);
    }


    /**
     * @see TypeHandler#whereHtmlInput(Field)
     */
    @Override public String whereHtmlInput(Field field) throws JspTagException {
        String fieldName = field.getName();
        String id = prefix(fieldName + "_search");
        if ( (String) tag.getContextProvider().getContextContainer().find(tag.getPageContext(), id) == null) {
            return "";
        } else {
            return super.whereHtmlInput(field);
        }
    }


    @Override public Constraint whereHtmlInput(Field field, Query query) throws JspTagException {
        String fieldName = field.getName();
        String id = prefix(fieldName + "_search");
        if (tag.getContextProvider().getContextContainer().find(tag.getPageContext(), id) == null) {
            return null;
        } else {
            return super.whereHtmlInput(field, query);
        }
    }



}
