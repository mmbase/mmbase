/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;

import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.JspTagException;

import org.mmbase.bridge.Node;
import org.mmbase.bridge.Field;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * The FieldTag can be used as a child of a 'NodeProvider' tag.   
 * 
 * @author Michiel Meeuwissen
 */
public class FieldTag extends FieldReferrerTag implements FieldProvider, Writer {
    
    private static Logger log = Logging.getLoggerInstance(FieldTag.class.getName()); 

    // Writer implementation:
    protected WriterHelper helper = new WriterHelper();
    public void setType(String t) throws JspTagException { 
        helper.setType(t);
    }
    public void setJspvar(String j) {
        helper.setJspvar(j);
    }
    public void setSession(String s) throws JspTagException {
        helper.setSession(getAttributeValue(s));
    }
    public void setWrite(String w) throws JspTagException {
        helper.setWrite(getAttributeBoolean(w));
    }
    public Object getValue() {
        return helper.getValue();
    }
    
    protected Node   node;
    protected NodeProvider nodeProvider;
    protected Field  field;
    protected String fieldName;
    private   String name;   
       
    public void setName(String n) throws JspTagException {
        name = getAttributeValue(n);
    }

    // NodeProvider Implementation
    /**
     * A fieldprovider also provides a node.
     */

    public Node getNodeVar() throws JspTagException {
        if (node == null) {
            nodeProvider = findNodeProvider();
            node = nodeProvider.getNodeVar();
        }
        if (node == null) {
            throw new JspTagException ("Did not find node in the parent node provider");
        }
        return node;
    }
    public void setModified() {
        nodeProvider.setModified();
    }


    
    public Field getFieldVar() {
        return field;
    }

    private void setFieldVar(String n) throws JspTagException {
        if (n != null) {             
            field = getNodeVar().getNodeManager().getField(n);
            fieldName = n;
            if (getReferid() != null) {
                throw new JspTagException ("Could not indicate both  'referid' and 'name' attribute");  
            }
        } else { 
            if (getReferid() == null) {
                field = getField(); // get from parent.
                fieldName = field.getName();
            }
        }       
    }
    protected void setFieldVar() throws JspTagException {
        setFieldVar(name);
    }

    /**
     * Does something with the generated output. This default
     * implementation does nothing, but extending classes could
     * override this function.
     * 
     **/
    protected String convert (String s) throws JspTagException { // virtual
        return s;
    }

    public int doStartTag() throws JspTagException {
        node= null;
        fieldName = name;
        setFieldVar(fieldName);                       
        // found the node now. Now we can decide what must be shown:
        Object value;
        // now also 'node' is availabe;
        if (field == null) { // some function, or 'referid' was used.
            if (getReferid() != null) { // referid
                value = getString(getReferid());
            } else {                    // function
                value = getNodeVar().getStringValue(fieldName);
            }
        } else { // a field was found!
            if (field.getType() == Field.TYPE_BYTE) {        
                value = node.getByteValue(name);
            } else {    
                value = convert(getNodeVar().getStringValue(fieldName));
            }
        }
        helper.setValue(value);
        helper.setJspVar(pageContext);        
        if (getId() != null) {
            getContextTag().register(getId(), helper.getValue());
        }
        
        return EVAL_BODY_TAG;
    }

    /**
     * write the value of the field.
     **/
    public int doAfterBody() throws JspTagException { 
        helper.setBodyContent(bodyContent);
        if ((! "".equals(bodyContent.getString()) && getReferid() != null)) {
            throw new JspTagException("Cannot use body in reused field (only the value of the field was stored, because a real 'field' object does not exist in MMBase)");
        }        
        return helper.doAfterBody();
    }
}
