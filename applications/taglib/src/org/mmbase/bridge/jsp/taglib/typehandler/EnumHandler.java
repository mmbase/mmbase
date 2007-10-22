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

import java.util.*;

import org.mmbase.util.SortedBundle;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This handler can be used to create option list by use of a resource.
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 * @version $Id: EnumHandler.java,v 1.38.2.1 2007-10-22 14:10:01 michiel Exp $
 */

public class EnumHandler extends AbstractTypeHandler implements TypeHandler {

    private static final Logger log = Logging.getLoggerInstance(EnumHandler.class);
    private Iterator iterator;
    private boolean available;

    /**
     * @since MMBase-1.8
     */
    public EnumHandler(FieldInfoTag tag) throws JspTagException {
        super(tag);
        available = true;
    }

    /**
     * @since MMBase-1.8
     */
    protected Iterator getIterator(Node node, Field field) throws JspTagException  {
        DataType dataType = field.getDataType();
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
                    Class type;
                    switch(field.getType()) {
                    case Field.TYPE_STRING:  type = String.class; break;
                    case Field.TYPE_INTEGER: type = Integer.class; break;
                    case Field.TYPE_LONG:    type = Long.class; break;

                        // I wonder if enums for the following types could make any sense, but well:
                    case Field.TYPE_FLOAT:   type = Float.class; break;
                    case Field.TYPE_DOUBLE:  type = Double.class; break;
                    case Field.TYPE_BINARY:    type = byte[].class; break;
                    case Field.TYPE_XML:     type = String.class; break; // Document.class ?
                    case Field.TYPE_NODE:    type = Node.class; break;
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


    protected EnumHandler getEnumHandler(Node node, Field field) throws JspTagException {
        return null;
    }
    public boolean isAvailable() {
        return available;
    }

    protected Object cast(Object value, Node node, Field field) {
        if ("".equals(value)) return null;
        return field.getDataType().cast(value, node, field);
    }

    protected Object getFieldValue(Node node, Field field, boolean useDefault) throws JspTagException {
        Object value = super.getFieldValue(node, field, useDefault);
        // if an enum is required ('not null'), and no default value was specified, then we simply default to the first
        // entry, as HTML rendering would do any way.
        if (value == null && field.getDataType().isRequired()) {
            Iterator i = getIterator(node, field);
            if (i!= null && i.hasNext()) {
                value = ((Map.Entry) i.next()).getKey();
            }
        }
        return value;
    }

    public String htmlInput(Node node, Field field, boolean search) throws JspTagException {
        StringBuffer buffer = new StringBuffer();
        String fieldName = field.getName();
        buffer.append("<select class=\"" + getClasses(node, field) + "\" name=\"").append(prefix(fieldName)).append("\" ");
        buffer.append("id=\"").append(prefixID(fieldName)).append("\" ");
        addExtraAttributes(buffer);
        buffer.append(">");
        Object value  = cast(getFieldValue(node, field, true), node, field);
        if (log.isDebugEnabled()) {
            log.debug("using value " + (value == null ? "NULL" : value.getClass().getName() + " " + value));
        }
        if (! field.getDataType().isRequired()) {
            buffer.append("<option value=\"\" ");
            if (value == null) buffer.append("selected=\"selected\" ");
            buffer.append(">--</option>");
        }
        if (iterator == null) {
            iterator = getIterator(node, field);
        }

        String valueString = Casting.toString(value);
        while(iterator != null && iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object key = entry.getKey();
            if (key == null) {
                log.warn("Found null as enumeration key for " + field.getDataType());
                continue;
            }
            String keyString = Casting.toString(key);
            buffer.append("<option value=\"");
            buffer.append(keyString);
            buffer.append("\"");
            if (keyString.equals(valueString)) {
                buffer.append(" selected=\"selected\"");
            } else if (search) {
                String searchs = (String) tag.getContextProvider().getContextContainer().find(tag.getPageContext(), prefix(field.getName()));
                if (keyString.equals(searchs)) {
                    buffer.append(" selected=\"selected\"");
                }
            }
            buffer.append(">");
            buffer.append(entry.getValue());
            buffer.append("</option>");
        }
        buffer.append("</select>");
        if (search) {
            String name = prefix(field.getName()) + "_search";
            String fieldid = prefixID(field.getName() + "_search");
            String searchi =  (String) tag.getContextProvider().getContextContainer().find(tag.getPageContext(), name);
            buffer.append("<input type=\"checkbox\" name=\"").append(name).append("\" ");
            buffer.append("id=\"").append(fieldid).append("\" ");
            if (searchi != null) {
                buffer.append(" checked=\"checked\"");
            }
            buffer.append(" />");
        }
        return buffer.toString();
    }


    public void paramHtmlInput(ParamHandler handler, Field field) throws JspTagException  {
        String name = prefix(field.getName() + "_search");
        String searchi =  (String) tag.getContextProvider().getContextContainer().find(tag.getPageContext(), name);
        if (searchi != null) {
            handler.addParameter(name, "yes");
        }
        super.paramHtmlInput(handler, field);
    }


    /**
     * @see TypeHandler#whereHtmlInput(Field)
     */
    public String whereHtmlInput(Field field) throws JspTagException {
        String fieldName = field.getName();
        String id = prefix(fieldName + "_search");
        if ( (String) tag.getContextProvider().getContextContainer().find(tag.getPageContext(), id) == null) {
            return "";
        } else {
            return super.whereHtmlInput(field);
        }
    }


    public Constraint whereHtmlInput(Field field, Query query) throws JspTagException {
        String fieldName = field.getName();
        String id = prefix(fieldName + "_search");
        if ( (String) tag.getContextProvider().getContextContainer().find(tag.getPageContext(), id) == null) {
            return null;
        } else {
            return super.whereHtmlInput(field, query);
        }
    }



}
