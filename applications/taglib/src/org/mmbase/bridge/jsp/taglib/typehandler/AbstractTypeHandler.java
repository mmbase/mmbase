/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib.typehandler;

import javax.servlet.jsp.JspTagException;
import java.util.*;

import org.mmbase.bridge.jsp.taglib.edit.FormTag;
import org.mmbase.util.Entry;
import org.mmbase.util.LocalizedString;
import org.mmbase.util.transformers.Xml;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.bridge.jsp.taglib.*;
import org.mmbase.storage.search.*;
import org.mmbase.datatypes.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * @javadoc
 *
 * @author Gerard van de Looi
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 * @version $Id: AbstractTypeHandler.java,v 1.40 2006-03-29 01:22:15 michiel Exp $
 */

public abstract class AbstractTypeHandler implements TypeHandler {
    private static final Logger log = Logging.getLoggerInstance(AbstractTypeHandler.class);

    protected FieldInfoTag tag;
    protected EnumHandler eh;
    protected boolean gotEnumHandler = false;

    /**
     * Constructor for AbstractTypeHandler.
     */
    public AbstractTypeHandler(FieldInfoTag tag) {
        super();
        this.tag = tag;

    }
    public void init() {
        eh = null;
        gotEnumHandler = false;
    }


    protected EnumHandler getEnumHandler(Node node, Field field) throws JspTagException {
        if (gotEnumHandler) return eh;
        gotEnumHandler = true;
        DataType dt = field.getDataType();

        if (dt.getEnumerationValues(tag.getLocale(), tag.getCloudVar(), node, field) != null) {
            return new EnumHandler(tag, node, field);
        }

        // XXX: todo the following stuff may peraps be somehow wrapped to IntegerDataType itself;
        // but what to do with 200L??
        if (dt instanceof IntegerDataType) {
            IntegerDataType idt = (IntegerDataType) dt;
            final int min = idt.getMin() + (idt.isMinInclusive() ? 0 : 1);
            final int max = idt.getMax() - (idt.isMaxInclusive() ? 0 : 1);
            if ((long) max - min < 200L) {
                return new EnumHandler(tag, node, field) {
                        int i = min;
                        protected Iterator getIterator(Node node, Field field) {
                            return new Iterator() {
                                    public boolean hasNext() {
                                        return i <= max;
                                    }
                                    public Object next() {
                                        Integer value = new Integer(i++);
                                        return new Entry(value, value);
                                    }
                                    public void remove() {
                                        throw new UnsupportedOperationException();
                                    }
                                };
                        }
                    };
            }
        }
        if (dt instanceof LongDataType) {
            LongDataType ldt = (LongDataType) dt;
            final long min = ldt.getMin() + (ldt.isMinInclusive() ? 0 : 1);
            final long max = ldt.getMax() - (ldt.isMaxInclusive() ? 0 : 1);
            if ((double) max - min < 200.0) {
                return new EnumHandler(tag, node, field) {
                        long i = min;
                        protected Iterator getIterator(Node node, Field field) {
                            return new Iterator() {
                                    public boolean hasNext() {
                                        return i <= max;
                                    }
                                    public Object next() {
                                        Long value = new Long(i++);
                                        return new Entry(value, value);
                                    }
                                    public void remove() {
                                        throw new UnsupportedOperationException();
                                    }
                                };
                        }
                    };
            }
        }

        return null;
    }

    protected StringBuffer addExtraAttributes(StringBuffer buf) throws JspTagException {
        String options = tag.getOptions();
        if (options != null) {
            int i = options.indexOf("extra:");
            if (i > -1) {
                buf.append(" " + options.substring(i + 6) + " ");
            }
        }
        return buf;
    }

    /**
     * @see TypeHandler#htmlInput(Node, Field, boolean)
     */
    public String htmlInput(Node node, Field field, boolean search) throws JspTagException {
        eh = getEnumHandler(node, field);
        if (eh != null) {
            return eh.htmlInput(node, field, search);
        }
        // default implementation.
        StringBuffer show =  new StringBuffer("<input type=\"text\" class=\"small\" size=\"80\" ");
        addExtraAttributes(show);
        show.append("name=\"").append(prefix(field.getName())).append("\" ");
        show.append("id=\"").append(prefixID(field.getName())).append("\" ");
        show.append("value=\"");
        if (node != null) {
            show.append(node.getStringValue(field.getName()));
        } else {
            Object searchParam = getFieldValue(node, field, ! search);
            show.append((searchParam == null ? "" : searchParam));
        }
        show.append("\" />");
        return show.toString();
    }

    protected Object getFieldValue(String fieldName) throws JspTagException {
        Object found = tag.getContextProvider().getContextContainer().find(tag.getPageContext(), prefix(fieldName));
        if ("".equals(found)) found = null;
        return found;
    }
    protected Object cast(Object value, Node node, Field field) {
        return field.getDataType().cast(value, node, field);
    }
    protected Object getFieldValue(Node node, Field field, boolean useDefault) throws JspTagException {
        String fieldName = field.getName();
        Object value = getFieldValue(fieldName);
        if (value == null) {
            if (node != null) {
                value = node.getValue(fieldName);
            } else if (useDefault) {
                value = field.getDataType().getDefaultValue();
            }
        } else {
            value = cast(value, node, field);
        }
        return value;
    }

    public String checkHtmlInput(Node node, Field field, boolean errors) throws JspTagException {
        eh = getEnumHandler(node, field);
        if (eh != null) {
            return eh.checkHtmlInput(node, field, errors);
        }
        String fieldName = field.getName();
        Object fieldValue = getFieldValue(node, field, false);
        DataType dt = field.getDataType();
        Collection col = dt.validate(fieldValue, node, field);
        if (col.size() == 0) {
            // do actually set the field, because some need cross-field checking
            if (fieldValue != null && node != null && ! fieldValue.equals(node.getValue(fieldName))) {
                node.setValue(fieldName,  fieldValue);
            }
            return "";
        } else {
            FormTag form =  (FormTag) tag.findParentTag(FormTag.class, null, false);
            if (form != null) {
                form.setValid(false);
            }
            if (errors) {
                StringBuffer show = new StringBuffer("<div class=\"check-error\">");
                Locale locale =  tag.getLocale();
                Iterator i = col.iterator();
                while (i.hasNext()) {
                    LocalizedString error = (LocalizedString) i.next();
                    show.append("<span>");
                    Xml.XMLEscape(error.get(locale), show);
                    show.append("</span>");
                }
                show.append("</div>");
                return show.toString();
            } else {
                return "";
            }
        }
    }

    /**
     * @see TypeHandler#useHtmlInput(Node, Field)
     */
    public boolean useHtmlInput(Node node, Field field) throws JspTagException {
        String fieldName = field.getName();
        Object fieldValue = getFieldValue(node, field, false);
        if (fieldValue == null) {

        } else {
            if (! fieldValue.equals(node.getValue(fieldName))) {
                //log.info("Field " + field + " " + node.getValue(fieldName) + " --> " + fieldValue);
                node.setValue(fieldName,  fieldValue);
                return true;
            }
        }
        return false;
    }



    /**
     * @see TypeHandler#whereHtmlInput(Field)
     */
    public String whereHtmlInput(Field field) throws JspTagException {
        eh = getEnumHandler(null, field);
        if (eh != null) {
            return eh.whereHtmlInput(field);
        }
        String string = findString(field);
        if (string == null) return null;
        return "( [" + field.getName() + "] =" + getSearchValue(string) + ")";
    }

    /**
     * The operator to be used by whereHtmlInput(field, query)
     * @since MMBase-1.7
     */
    protected int getOperator() {
        return FieldCompareConstraint.EQUAL;
    }
    /**
     * Converts the value to the actual value to be searched. (mainly targeted at StringHandler).
     * @since MMBase-1.7
     */
    protected String getSearchValue(String string) {
        return string;
    }

    /**
     * @since MMBase-1.7
     */
    final protected String findString(Field field) throws JspTagException {
        String fieldName = field.getName();
        String search = (String) tag.getContextProvider().getContextContainer().find(tag.getPageContext(), prefix(fieldName));
        if (search == null || "".equals(search)) {
            return null;
        }
        return search;
    }


    public void paramHtmlInput(ParamHandler handler, Field field) throws JspTagException  {
        eh = getEnumHandler(null, field);
        if (eh != null) {
            eh.paramHtmlInput(handler, field);
            return;
        }
        handler.addParameter(prefix(field.getName()), findString(field));
    }


    /**
     * Adds search constraint to Query object.
     * @return null if nothing to be searched, the constraint if constraint added
     */

    public Constraint whereHtmlInput(Field field, Query query) throws JspTagException {
        eh = getEnumHandler(null, field);
        if (eh != null) {
            return eh.whereHtmlInput(field, query);
        }
        String value = findString(field);
        if (value != null) {
            String fieldName = field.getName();
            if (query.getSteps().size() > 1) {
                fieldName = field.getNodeManager().getName()+"."+fieldName;
            }
            Constraint con = Queries.createConstraint(query, fieldName, getOperator(), getSearchValue(value));
            Queries.addConstraint(query, con);
            return con;
        } else {
            return null;
        }
    }

    /**
     * Puts a prefix before a name. This is used in htmlInput and
     * useHtmlInput, they need it to get a reasonably unique value for
     * the name attribute of form elements.
     *
     */
    protected String prefix(String s) throws JspTagException {
        String id = tag.findFieldProvider().getId();
        if (id == null) id = "";
        return id + "_" + s;
    }

    /**
     * Puts a prefix 'mm_' before an id in form fields. To be used in ccs etc..
     *
     * @param   s   Fieldname
     * @return  String with the id, like f.e. 'mm_title'
     */
    protected String prefixID(String s) throws JspTagException {
        String id = tag.findFieldProvider().getId();
        id = (id == null) ? "" : id + "_";
        return "mm_" + id + s;
    }



}
