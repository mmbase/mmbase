/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.applications.dove;

import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.mmbase.util.logging.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.bridge.*;


/**
 * This class handles Remote Procedure Calls described using a DOM model.
 * The original data is received in xml, likely through a specialized servlet such
 * as the DoveServlet (but this is not required. it may, for instance, be
 * possible to use this class in conjunction with SOAP or even outside a servlet
 * context).
 * The call should result in a new DOM tree (see the EditWizard API for details).
 * <br />
 * XXX: Currently, this class performs some type of validation on the xml received.
 * If errors occur, they are included in the response, at the place where they
 * occur. This means that the &lt;error &gt; tag can occur anywhere in the
 * response, which is not very neat.
 * <br />
 * Errors have three types:<br />
 * parser : the xml given is invalid or does not follow the grammar.
 *          This likely means there is a bug in the client code.<br />
 * server : the code invoked is either not yet implemented or another,
 *          server-related, error occurred (such as no memory, bad configuration,
 *          etc.).
 *          Server errors entirely fail a request.<br />
 * client : The data requested could not be retrieved or values specified
 *          were invalid. I.e. a requested node does not exist (any more), or a
 *          <code>put</code> failed due to locking or invalid data.<br />
 * <br />
 * This is ONLY for debugging purposes!
 * XML validation should occur at the parser and be captured early.
 * If we reach consensus that we must always validate, the xml-checking can be left
 * out, as we can then assumes a correct model. This will optimize processing.
 *
 * @author Pierre van Rooden
 * @since MMBase-1.5
 * @version $Id: Dove.java,v 1.12 2002-05-30 12:56:07 pierre Exp $
 */

public class Dove extends AbstractDove {

    //logger
    private static Logger log = Logging.getLoggerInstance(Dove.class.getName());

    /**
     * Constructor
     * @param doc the Document that is constructed. This should only be used to
     *            construct new DOM elements. New (child) nodes should not be added
     *            to this document, but to the out element.
     */
    public Dove(Document doc) {
        super(doc);
    }

    /**
     * Utility function, determines whether a field is a data field.
     * Data fields are persistent (non-virtual) fields.
     * Fields number, owner, and ottype, and the relation fields snumber,dnumber, rnumber, and dir
     * are excluded; these fields should be handled through the attributes of Element.
     * @param node  the MMBase node that owns the field
     * @param f The field to check
     */
    private boolean isDataField(org.mmbase.bridge.Node node, Field f) {
        String fname = f.getName();
        return (f.getState()==Field.STATE_PERSISTENT) && // skip virtual fields
               (!"owner".equals(fname)) && // skip owner/otype/number fields!
               (!"otype".equals(fname)) &&
               (!"number".equals(fname)) &&
               (!(node instanceof Relation) ||
                ((!"snumber".equals(fname)) &&
                 (!"dnumber".equals(fname)) &&
                 (!"rnumber".equals(fname)) &&
                 (!"dir".equals(fname))
                )
               );
    }

    /**
     * Utility function, determines whether a field is a data field.
     * dat fields are persistent (non-virtual) fields.
     * Fields number, owner, and ottype, and the relation fields snumber,dnumber, and rnumber
     * are not excluded; these fields should be handled through the attributes of Element.
     * @param node  the MMBase node that owns the field
     * @param fname The name of the field to check
     */
    private boolean isDataField(org.mmbase.bridge.Node node, String fname) {
        Field f=node.getNodeManager().getField(fname);
        return (f!=null) && isDataField(node,f);
    }

    /**
     * Handles a node storing its content in a DOM element.
     * This method accepts a object to store, as well as a DOM element, which
     * may contain as it child nodes elements describing the fields to retrieve.
     * The tagname of these elements should be 'field'.
     * Each element should have a 'name' attribute that identifies the field.
     * The result of this call should be a list of DOM elements describing each field,
     * which are appended to the out element.
     * If the 'in' node has no field elements, all fields are returned, with the
     * exception of some system-specific fields.
     * <br />
     * @todo Currently, the Dove does not return values for binary (byte) fields - a binary field is
     * always returned empty. This is done for optimization of the editwizards, which would otherwise
     * eat a lot of memory, but that DO need a reference to a bytefield.
     * Future versions of Dove should handle a better mechanism for handling binary fields.
     *
     * @param in the element that described the <code>getdata</code> call.
     *           The childnodes should describe the nodes to retrieve.
     * @param out the element that described the <code>getdata</code> result.
     *           Retrieved nodes should be added as childs to this element.
     * @param nd The node to store in out.
     */
    public void getDataNode(Element in, Element out, org.mmbase.bridge.Node nd) {
        NodeManager nm=nd.getNodeManager();
        out.setAttribute(ELM_TYPE,nm.getName());

        Element field=getFirstElement(in,FIELD);
        if (field==null) {
            for (FieldIterator i=nm.getFields().fieldIterator(); i.hasNext(); ) {
                Field f=i.nextField();
                String fname=f.getName();
                if (isDataField(nd,f)) {
                    String val="";
                    if (nm.getField(fname).getType()!=Field.TYPE_BYTE) {
                        val=nd.getStringValue(fname);
                    }
                    Element fel=addContentElement(FIELD,val,out);
                    fel.setAttribute(ELM_NAME,fname);
                }
            }
        } else {
            while (field!=null) { // select all child tags, should be 'field'
                if (field.getTagName().equals(FIELD)) {
                    String fname=(String)field.getAttribute(ELM_NAME);
                    if ((fname==null) || (fname.equals(""))) {
                        Element err=addContentElement(ERROR,"name required for field",out);
                        err.setAttribute(ELM_TYPE,IS_PARSER);
                    } else if (isDataField(nd,fname)) {
                        String val="";
                        if (nm.getField(fname).getType()!=Field.TYPE_BYTE) {
                            val=nd.getStringValue(fname);
                        }
                        Element fel=addContentElement(FIELD,val,out);
                        fel.setAttribute(ELM_NAME,fname);
                    }
                }
                field=getNextElement(field,FIELD);
            }
        }
    }

    /**
     * Handles a node by retrieving its data and storing its content in a
     * DOM element.
     * This method accepts a DOM element (describing a node), which may contain as it
     * child nodes elements describing the fields to retrieve.
     * The tagname of these elements should be 'field'.
     * Each element should have a 'name' attribute that identifies the field.
     * The result of this call should be a list of DOM elements describing each field,
     * which are appended to the out element.
     * If the 'in' node has no field elements, all fields are returned, with the
     * exception of some system-specific fields.
     * <br />
     * Note also that if no name was given for a field, a NullPointer exception is thrown.
     *
     * @param in the element that described the <code>getdata</code> call.
     *           The childnodes should describe the nodes to retrieve.
     * @param out the element that described the <code>getdata</code> result.
     *           Retrieved nodes should be added as childs to this element.
     * @param cloud the cloud to work on
     */
    public void getDataNode(Element in, Element out, Cloud cloud) {
        String alias = in.getAttribute(ELM_NUMBER);
        try {
            org.mmbase.bridge.Node nd = cloud.getNode(alias);
            getDataNode(in,out,nd);
        } catch (RuntimeException e) {
            Element err=addContentElement(ERROR,"node not found",out);
            err.setAttribute(ELM_TYPE,IS_SERVER);
        }
    }

    /**
     * Handles a node by retrieving its relations and storing its content in a
     * DOM element.
     * This method accepts a DOM element (describing a node), which may contain as it
     * child nodes elements describing the relations to retrieve.
     * The tagname of these elements should be 'relation'.
     * If the 'in' node has no relation elements, all relations are returned.
     *
     * @param in the element that described the <code>getdata</code> call.
     *           The childnodes should describe the nodes to retrieve.
     * @param out the element that described the <code>getdata</code> result.
     *           Retrieved nodes should be added as childs to this element.
     * @param cloud the cloud to work on
     */
    public void getRelationsNodes(Element in, Element out, Cloud cloud) {
        String alias = in.getAttribute(ELM_NUMBER);
        try {
            org.mmbase.bridge.Node nd = cloud.getNode(alias);
            NodeManager nm=nd.getNodeManager();
            out.setAttribute(ELM_TYPE,nm.getName());

            Element relation=getFirstElement(in,RELATION);
            int thisNumber=nd.getNumber();
            if (relation==null) {
                for (RelationIterator i=nd.getRelations().relationIterator(); i.hasNext(); ) {
                    Relation nrel=i.nextRelation();
                    Element data=doc.createElement(RELATION);
                    data.setAttribute(ELM_NUMBER, ""+nrel.getNumber());
                    data.setAttribute(ELM_ROLE, nrel.getRelationManager().getForwardRole());
                    if (thisNumber==nrel.getIntValue("snumber")) {
                        data.setAttribute(ELM_SOURCE, ""+nrel.getValue("snumber"));
                        data.setAttribute(ELM_DESTINATION, ""+nrel.getValue("dnumber"));
                    } else {
                        data.setAttribute(ELM_SOURCE, ""+nrel.getValue("dnumber"));
                        data.setAttribute(ELM_DESTINATION, ""+nrel.getValue("snumber"));
                    }
                    out.appendChild(data);
                    getDataNode(null,data,nrel);
                }
            } else {
                while (relation!=null) { // select all child tags, should be 'relation'
                    if (relation.getTagName().equals(RELATION)) {
                        String role=(String)relation.getAttribute(ELM_ROLE);
                        if ("".equals(role)) role=null;
                        String destinationType=(String)relation.getAttribute(ELM_DESTINATIONTYPE);
                        if (("".equals(destinationType)) || (destinationType==null)) {
                            destinationType=(String)relation.getAttribute(ELM_DESTINATION);
                        }
                        if ("".equals(destinationType)) destinationType=null;

                        // determines whether to load the object (and possible restrictions)
                        Element objectDef=getFirstElement(relation,OBJECT);
                        try {
                            for (RelationIterator i=nd.getRelations(role,destinationType).relationIterator(); i.hasNext(); ) {
                                Relation nrel=i.nextRelation();
                                Element data=doc.createElement(RELATION);
                                if (role!=null) {
                                    data.setAttribute(ELM_ROLE, role);
                                } else {
                                    data.setAttribute(ELM_ROLE,nrel.getRelationManager().getForwardRole());
                                }
                                int othernumber=nrel.getIntValue("dnumber");
                                if (thisNumber==nrel.getIntValue("snumber")) {
                                    data.setAttribute(ELM_SOURCE, ""+nrel.getValue("snumber"));
                                    data.setAttribute(ELM_DESTINATION, ""+nrel.getValue("dnumber"));
                                } else {
                                    data.setAttribute(ELM_SOURCE, ""+nrel.getValue("dnumber"));
                                    data.setAttribute(ELM_DESTINATION, ""+nrel.getValue("snumber"));
                                    othernumber=nrel.getIntValue("snumber");
                                }
                                data.setAttribute(ELM_NUMBER, ""+nrel.getNumber());
                                out.appendChild(data);
                                getDataNode(relation,data,nrel);
                                if (objectDef!=null) {
                                    Element nodeData=doc.createElement(OBJECT);
                                    nodeData.setAttribute(ELM_NUMBER, ""+othernumber);
                                    data.appendChild(nodeData);
                                    getDataNode(objectDef,nodeData,cloud.getNode(othernumber));
                                }
                            }
                        } catch (RuntimeException e) {
                            Element err=addContentElement(ERROR,"role or nodetype for relation invalid ("+e.getMessage()+")",out);
                            err.setAttribute(ELM_TYPE,IS_CLIENT);
                        }
                    }
                    relation=getNextElement(relation,RELATION);
               }
            }
        } catch (RuntimeException e) {
            Element err=addContentElement(ERROR,"node not found",out);
            err.setAttribute(ELM_TYPE,IS_SERVER);
        }
    }

    /**
     * Handles a getdata call, by obtaining data for each node specified.
     * This method accepts a DOM element, which should contain as it
     * child nodes elements describing the nodes to retrieve.
     * The tagname of these elements should be 'object'.
     * Each element should have an 'number' attribute that described the number
     * or alias of the node to retrieve.
     * The result of this call should be a list of DOM elements describing each node,
     * which are appended to the out element.
     *
     * @param in the element that described the <code>getdata</code> call.
     *           The childnodes should describe the nodes to retrieve.
     * @param out the element that described the <code>getdata</code> result.
     *           Retrieved nodes should be added as childs to this element.
     * @param cloud the cloud to work on
     */
    public void getData(Element in, Element out, Cloud cloud) {
        Element node=getFirstElement(in);
        while (node!=null) { // select all child tags, should be 'object'
            if (node.getTagName().equals(OBJECT)) {
                String number = node.getAttribute(ELM_NUMBER); // check id;
                if (number.equals("")) {
                    Element err=addContentElement(ERROR,"number required for node",out);
                    err.setAttribute(ELM_TYPE,IS_PARSER);
                } else {
                    Element data=doc.createElement(OBJECT);
                    data.setAttribute(ELM_NUMBER, number);
                    out.appendChild(data);
                    getDataNode(node,data,cloud);
                }
            } else {
                Element err=addContentElement(ERROR,"Unknown subtag in getdata: "+node.getTagName(),out);
                err.setAttribute(ELM_TYPE,IS_PARSER);
            }
            node=getNextElement(node);
        }
    }

    /**
     * Handles a getnew call, by obtaining data for the new node of a specified type.
     * This method accepts a DOM element, which should contain as it's attribute
     * the type of the node to return.
     * The result of this call should be DOM element describing a new node,
     * which is appended to the out element.
     *
     * @param in the element that described the <code>getnew</code> call.
     * @param out the element that described the <code>getnew</code> result.
     *           The new node should be added as a child to this element.
     * @param cloud the cloud to work on
     */
    public void getNew(Element in, Element out, Cloud cloud) {
        String nodemanagername = in.getAttribute(ELM_TYPE); // check type;
        if (nodemanagername.equals("")) {
            Element err=addContentElement(ERROR,"type required for getnew",out);
            err.setAttribute(ELM_TYPE,IS_PARSER);
        } else {
            try {
                out.setAttribute(ELM_TYPE,nodemanagername);
                NodeManager nm =cloud.getNodeManager(nodemanagername);
                org.mmbase.bridge.Node n=nm.createNode();
                try {
                    Element data=doc.createElement(OBJECT);
                    int number=n.getNumber();
                    data.setAttribute(ELM_NUMBER, "n"+number);
                    out.appendChild(data);
                    getDataNode(null,data,n);
                } finally {
                    n.cancel();  // have to cancel node !
                }
            } catch (RuntimeException e) {
                Element err=addContentElement(ERROR,"node type does not exist",out);
                err.setAttribute(ELM_TYPE,IS_CLIENT);
            }
        }
    }

    /**
     * Handles a getnewrelation call, by obtaining data for the new node of a specified type.
     * This method accepts a DOM element, which should contain as it's attribute
     * the role of the relation to return.
     * The result of this call should be DOM element describing a new node,
     * which is appended to the out element.
     *
     * @param in the element that described the <code>getnew</code> call.
     * @param out the element that described the <code>getnew</code> result.
     *           The new node should be added as a child to this element.
     * @param cloud the cloud to work on
     */
    public void getNewRelation(Element in, Element out, Cloud cloud) {
        String rolename = in.getAttribute(ELM_ROLE); // check role;
        String destination = in.getAttribute(ELM_DESTINATION); // check destination;
        String source = in.getAttribute(ELM_SOURCE); // check source;
        if (rolename.equals("")) {
            Element err=addContentElement(ERROR,"role required for getrelations",out);
            err.setAttribute(ELM_TYPE,IS_PARSER);
        } else {
            try {
                out.setAttribute(ELM_ROLE,rolename);
                out.setAttribute(ELM_DESTINATION, destination);
                out.setAttribute(ELM_SOURCE, source);
                RelationManager nm =cloud.getRelationManager(rolename);
                org.mmbase.bridge.Node n=nm.createNode();
                try {
                    Element data=doc.createElement(RELATION);
                    int number=n.getNumber();
                    data.setAttribute(ELM_NUMBER, "n"+number);
                    data.setAttribute(ELM_DESTINATION, destination);
                    data.setAttribute(ELM_SOURCE, source);
                    data.setAttribute(ELM_ROLE,rolename);
                    out.appendChild(data);
                    getDataNode(null,data,n);
                } finally {
                    n.cancel();  // have to cancel node !
                }
            } catch (RuntimeException e) {
                Element err=addContentElement(ERROR,"role does not exist",out);
                err.setAttribute(ELM_TYPE,IS_CLIENT);
            }
        }
    }

    /**
     * Handles a getrelations call, by obtaining relations for each node specified.
     * This method accepts a DOM element, which should contain as it
     * child nodes elements describing the nodes to retrieve the relations from.
     * The tagname of these elements should be 'object'.
     * Each element should have an 'number' attribute that described the number
     * or alias of the node to retrieve.
     * The result of this call should be a list of DOM elements with a data element for
     * each node,which are appended to the out element. The node element's children
     * are the relations.
     *
     * @param in the element that described the <code>getrelations</code> call.
     *           The childnodes should describe the nodes to retrieve the relations from.
     * @param out the element that described the <code>getrelations</code> result.
     *           Retrieved nodes and their relations should be added as childs to this element.
     * @param cloud the cloud to work on
     */
    public void getRelations(Element in, Element out, Cloud cloud) {
        Element node=getFirstElement(in);
        while (node!=null) { // select all child tags, should be 'object'
            if (node.getTagName().equals(OBJECT)) {
                String number = node.getAttribute(ELM_NUMBER); // check id;
                if (number.equals("")) {
                    Element err=addContentElement(ERROR,"number required for node",out);
                    err.setAttribute(ELM_TYPE,IS_PARSER);
                } else {
                    Element data=doc.createElement(OBJECT);
                    data.setAttribute(ELM_NUMBER, number);
                    out.appendChild(data);
                    getRelationsNodes(node,data,cloud);
                }
            } else {
                Element err=addContentElement(ERROR,"Unknown subtag in getrelations: "+node.getTagName(),out);
                err.setAttribute(ELM_TYPE,IS_PARSER);
            }
            node=getNextElement(node);
        }
    }

    /**
     * Handles a getrelations call, by obtaining relations for each node specified.
     * This method accepts a DOM element, which should contain as it
     * child nodes elements describing the nodes to retrieve the relations from.
     * The tagname of these elements should be 'object'.
     * Each element should have an 'number' attribute that described the number
     * or alias of the node to retrieve.
     * The result of this call should be a list of DOM elements with a data element for
     * each node,which are appended to the out element. The node element's children
     * are the relations.
     *
     * @param in the element that described the <code>getconstraints</code> call.
     *           The childnodes should describe the node types to retrieve the constraints of.
     * @param out the element that described the <code>getconstraints</code> result.
     *           Retrieved constraints should be added as childs to this element.
     * @param cloud the cloud to work on
     */
    public void getConstraints(Element in, Element out, Cloud cloud) {
        String nodemanagername = in.getAttribute(ELM_TYPE); // check type;
        if (nodemanagername.equals("")) {
            Element err=addContentElement(ERROR,"type required for getconstraints",out);
            err.setAttribute(ELM_TYPE,IS_PARSER);
        } else {
            try {
                out.setAttribute(ELM_TYPE,nodemanagername);
                NodeManager nm =cloud.getNodeManager(nodemanagername);

                // XXX: language is currently ignored
                // (not supported by the MMCI)
                String lang= in.getAttribute(ELM_LANG);
                if ("".equals(lang)) {
                    lang=null;
                    // should be:
                    // lang = cloud.getLanguage();
                } else {
                    out.setAttribute(ELM_LANG,lang);
                }

                // singular name (XXX:language is ignored)
                // should be something akin to :
                // xxx=nm.getGUIName(SINGULAR_NAME,lang);
                //
                Element elm=addContentElement(SINGULARNAME,nm.getGUIName(),out);
                if (lang!=null) elm.setAttribute(ELM_LANG,lang);

                // plural name  (XXX:language is ignored)
                // XXX: returns same as singular as MMCI does not support this yet
                elm=addContentElement(PLURALNAME,nm.getGUIName(),out);
                if (lang!=null) elm.setAttribute(ELM_LANG,lang);

                // description  (XXX:language is ignored)
                elm=addContentElement(DESCRIPTION,nm.getDescription(),out);
                if (lang!=null) elm.setAttribute(ELM_LANG,lang);

                // fields
                Element fields=doc.createElement(FIELDS);
                out.appendChild(fields);
                for(FieldIterator i = nm.getFields().fieldIterator(); i.hasNext(); ) {
                    Field fielddef=i.nextField();
                    String fname=fielddef.getName();
                    // Filter out the owner/otype/number/CacheCount fields and
                    // the virtual fields.
                    if (isDataField(null,fielddef)) {
                        int dbtype = fielddef.getType();
                        Element field=doc.createElement(FIELD);
                        field.setAttribute(ELM_NAME,fname);
                        fields.appendChild(field);
                        // guiname (XXX:language is ignored)
                        elm=addContentElement(GUINAME,fielddef.getGUIName(),out);
                        if (lang!=null) elm.setAttribute(ELM_LANG,lang);
                        // guitype
                        String guitype=fielddef.getGUIType();
                        if (guitype.indexOf("/")==-1) {
                            if (guitype.equals("field")) {
                                guitype="string/text";
                            } else if (guitype.equals("string")) {
                                guitype="string/line";
                            } else if (guitype.equals("eventtime")) {
                                guitype="datetime/date";
                            } else {
                                String dttype="string";
                                int itype=fielddef.getType();
                                if (itype==Field.TYPE_NODE)
                                    dttype="int";
                                else if (itype==Field.TYPE_INTEGER)
                                    dttype="int";
                                else if (itype==Field.TYPE_LONG)
                                    dttype="long";
                                else if (itype==Field.TYPE_FLOAT)
                                    dttype="float";
                                else if (itype==Field.TYPE_DOUBLE)
                                    dttype="double";
                                else if (itype==Field.TYPE_BYTE)
                                    dttype="binary";
                                guitype=dttype+"/"+guitype;
                            }
                        }

                        addContentElement(GUITYPE,guitype,field);

                        int maxLength=fielddef.getMaxLength();
                        if (maxLength>0) {
                            addContentElement(MAXLENGTH,""+maxLength,field);
                        }

                        // required XXX: not supported by MMCI
                        // always returns false for the moment
                        /*
                        if (fielddef.getRequired()) {
                            addContentElement(REQUIRED,IS_TRUE,field);
                        } else {
                            addContentElement(REQUIRED,IS_FALSE,field);
                        }
                        */
                        addContentElement(REQUIRED,IS_FALSE,field);
                    }
                }
                // relations
                Element relations=doc.createElement(RELATIONS);
                out.appendChild(relations);
                // XXX: getAllowedRelations is not yet supported by the MMCI
                // This code commented out
            } catch (RuntimeException e) {
                Element err=addContentElement(ERROR,"node type does not exist",out);
                err.setAttribute(ELM_TYPE,IS_CLIENT);
            }
        }
    }

    /**
     * Handles a getlist call.
     * This method accepts a DOM element, which should contain as it
     * child nodes elements describing the queries to retrieve the nodes with.
     * The tagname of these elements should be 'query'.
     * Each element should have a 'xpath' attribute to indicate the nodemanager to run the list on,
     * and optionally a 'where' that contains the constraints.
     * The result of this call should be a list of DOM elements with a data element for
     * each query, which are appended to the out element. The node element's children
     * are the nodes from the list.
     * @param in the element that described the <code>getlist</code> call.
     *           The childnodes should describe the queries to run.
     * @param out the element that described the <code>getlist</code> result.
     *           Retrieved nodes should be added as childs to this element.
     * @param cloud the cloud to work on
     */
    public void getList(Element in, Element out, Cloud cloud) {
        Element query=getFirstElement(in);
        while (query!=null) { // select all child tags, should be 'query'
            if (query.getTagName().equals(QUERY)) {
                String xpath = query.getAttribute(ELM_XPATH); // check id;
                String where = query.getAttribute(ELM_WHERE); // check id;
                if (xpath.equals("")) {
                    Element err=addContentElement(ERROR,"xpath required for query",out);
                    err.setAttribute(ELM_TYPE,IS_PARSER);
                } else {
                    Element querydata=doc.createElement(QUERY);
                    querydata.setAttribute(ELM_XPATH, xpath);
                    if (!where .equals("")) {
                        querydata.setAttribute(ELM_WHERE, where);
                    }
                    out.appendChild(querydata);
                    // get node template
                    Element node=getFirstElement(query);

                    if (xpath.indexOf("/*@")!=0) {
                        Element err=addContentElement(ERROR,"invalid xpath",out);
                        err.setAttribute(ELM_TYPE,IS_CLIENT);
                    } else {
                        //get node data, bit stupid
                        try {
                            NodeManager nm = cloud.getNodeManager(xpath.substring(3));
                            for(NodeIterator i =nm.getList(where,null,null).nodeIterator(); i.hasNext(); ) {
                                org.mmbase.bridge.Node n=i.nextNode();
                                Element data=doc.createElement(OBJECT);
                                data.setAttribute(ELM_NUMBER, ""+n.getNumber());
                                querydata.appendChild(data);
                                getDataNode(node,data,n);
                            }
                        } catch(RuntimeException e) {
                            Element err=addContentElement(ERROR,"node type does not exist",out);
                            err.setAttribute(ELM_TYPE,IS_CLIENT);
                        }
                    }
                }
            } else {
                Element err=addContentElement(ERROR,"Unknown subtag in getlist: "+query.getTagName(),out);
                err.setAttribute(ELM_TYPE,IS_PARSER);
            }
            query=getNextElement(query);
        }
    }

    /**
     * Handles a put call.
     * This method accepts a DOM element, which should contain as it
     * child nodes elements describing the original and the new cloud.
     * @param in the element that described the <code>put</code> call.
     *           The childnodes should describe the old and the new cloud.
     * @param out the element that described the <code>put</code> result.
     *           The result of the put (an error or the resulting cloud) should be added
     *           as childs to this element.
     * @param cloud the cloud to work on
     * @param repository Repository that contains the blobs
     */
    public void put(Element in, Element out, Cloud cloud, Map repository) {
        // first colelct all new and original nodes
        Map orgnodes=new HashMap();
        Map newnodes=new HashMap();
        Map orgrelations=new HashMap();
        Map newrelations=new HashMap();

        // get all the needed info from the xml stream
        Element query=getFirstElement(in);
        while (query!=null) { // select child tags, should be 'original' or 'new'
            if (query.getTagName().equals(ORIGINAL) || query.getTagName().equals(NEW)) {
                boolean isorg=query.getTagName().equals(ORIGINAL);
                Element node=getFirstElement(query);

                while (node!=null) { // select all child tags, should be 'object'
                    if (node.getTagName().equals(OBJECT) || node.getTagName().equals(RELATION)) {
                        boolean isrelation=node.getTagName().equals(RELATION);

                        // store the values of one node
                        Map values=new HashMap();
                        if (isrelation)
                            if (isorg)
                                orgrelations.put((String)node.getAttribute(ELM_NUMBER),values);
                            else
                                newrelations.put((String)node.getAttribute(ELM_NUMBER),values);
                        else
                            if (isorg)
                                orgnodes.put((String)node.getAttribute(ELM_NUMBER),values);
                            else
                                newnodes.put((String)node.getAttribute(ELM_NUMBER),values);
                        if (!isorg)
                            values.put("_status",(String)node.getAttribute(ELM_STATUS));
                        if (isrelation) {
                            String role=node.getAttribute(ELM_ROLE);
                            if (role!=null) values.put("_role",role);

                            String source=node.getAttribute(ELM_SOURCE);
                            if (source!=null) values.put("_source",source);

                            String destination=node.getAttribute(ELM_DESTINATION);
                            if (destination!=null) values.put("_destination",destination);
                        } else {
                            String type=node.getAttribute(ELM_TYPE);
                            if (type!=null) values.put("_otype",type);
                        }
                        Element field=getFirstElement(node);
                        while (field!=null) { // select all child tags, should be 'fields'
                            if (field.getTagName().equals(FIELD)) {
                                String fieldname=field.getAttribute(ELM_NAME);
                                String href=field.getAttribute(ELM_HREF);
                                if (!href.equals("")) {
                                    Object repval=repository.get(href);
                                    if (repval!=null) {
                                        values.put(fieldname,repval);
                                    }
                                } else {
                                    if(field.getFirstChild()==null) {
                                        values.put(fieldname,"");
                                    } else {
                                        values.put(fieldname,field.getFirstChild().getNodeValue());
                                    }
                                }
                            }
                            field=getNextElement(field);
                        }
                    }
                    node=getNextElement(node);
                }
            }
            query=getNextElement(query);
        }
        // are there new nodes to handle ?
        if (newnodes.size()>0 || newrelations.size()>0) {
            Transaction trans = cloud.createTransaction();
            Map nodesadded = new HashMap ();
            Map relationsadded = new HashMap();
            if (mergeClouds(orgnodes,newnodes,orgrelations,newrelations,nodesadded,relationsadded,out,trans) ) {
                try {
                    trans.commit();
                    // retrieve all numbers and reset them to the right value
                    // This is possible, as the nodes themselves contain this info after the
                    // transaction
                    //
                    for (Iterator i = nodesadded.entrySet().iterator(); i.hasNext(); ) {
                        Map.Entry me=(Map.Entry)i.next();
                        org.mmbase.bridge.Node n = (org.mmbase.bridge.Node)me.getKey();
                        Element oe = (Element)me.getValue();
                        oe.setAttribute(ELM_NUMBER,n.getStringValue("number"));
                    }
                    // retrieve all numbers, snumbers, dnumbers and reset them to the right value
                    for (Iterator i = relationsadded.entrySet().iterator(); i.hasNext(); ) {
                        Map.Entry me=(Map.Entry)i.next();
                        org.mmbase.bridge.Node n = (org.mmbase.bridge.Node)me.getKey();
                        Element re = (Element)me.getValue();
                        re.setAttribute(ELM_NUMBER,n.getStringValue("number"));
                        re.setAttribute(ELM_SOURCE,n.getStringValue("snumber"));
                        re.setAttribute(ELM_DESTINATION,n.getStringValue("dnumber"));
                    }
                } catch (RuntimeException e) {
                    Element err=addContentElement(ERROR,"Transaction failed : "+e.getMessage(),out);
                    log.error(Logging.stackTrace(e));
                    err.setAttribute(ELM_TYPE,IS_SERVER);
                }
            } else {
                trans.cancel();
            }
        }
    }


    /**
     * Converts the value of a field to a node reference (either a temporary id or an object alias or number).
     * Used for determing values of the snumber and dnumber relation fields.
     * @param name the name of the field
     * @param values a Map with field values
     * @param aliases a Map with mappings from XML aliases to node reference values
     * @return the node reference value
     */
    protected String getNodeReferenceFromValue(String name, Map values, Map aliases) {
        String result=null;
        String value=(String)values.get(name);
        Object tmp=aliases.get(value);
        if (tmp==null) {
            // oke its not a temporary alias, return the original value
            result=value;
        } else {
            result=""+tmp;
        }
        return result;
    }

    /**
     * Fills the fields of the specified node with the supplied values.
     * The node's XML element is upadeytd with the affected field elements.
     * This method does not validate whether a node's values have changed since the
     * transaction started.
     * @param alias the node alias in the put tree
     * @param node the node to fill
     * @param objectelement the XML element to fill with the changed data for feedback
     * @param values a Map with new node values
     * @return true if succesful, false if an error ocurred
     */
    protected boolean fillFields(String alias, org.mmbase.bridge.Node node, Element objectelement, Map values) {
        return fillFields(alias, node, objectelement, values, null);
    }

    /**
     * Fills the fields of the specified node with the supplied values.
     * The node's XML element is updated with the affected field elements.
     * This method validates whether a node's values have changed since the
     * transaction started, and fails if this was teh acse.
     * @param alias the node alias in the put tree
     * @param node the node to fill
     * @param out the XML element to fill with the changed data for feedback
     * @param values a Map with new node values
     * @param orgvalues a Map with the original values of the node, needed for checking.
     *        if <code>null</code>, no checking takes place
     * @return true if succesful, false if an error ocurred
     */
    protected boolean fillFields(String alias, org.mmbase.bridge.Node node, Element out, Map values, Map orgvalues) {
        NodeManager nm = node.getNodeManager();
        for (Iterator i = values.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry me=(Map.Entry)i.next();
            String key=(String)me.getKey();
            if (isDataField(node,key)) {
                Object value=me.getValue();
                if ((orgvalues!=null) &&
                    (!(value instanceof byte[]))) {    // XXX: currently, we do not validate on byte fields
                    String orgvalue=(String)orgvalues.get(key);
                    String mmbasevalue=node.getStringValue(key);
                    if (!orgvalue.equals(mmbasevalue)) {
                        // give error node was changed in cloud
                        Element err=addContentElement(ERROR,"Node was changed in the cloud, node number : "+alias+" field name "+key,out);
                        err.setAttribute(ELM_TYPE,IS_SERVER);
                        return false;
                    }
                }
                node.setValue(key,value);
                Element fieldelement=doc.createElement(FIELD);
                fieldelement.setAttribute(ELM_NAME,key);
                if (!(value instanceof byte[])) {
                    Text tel=doc.createTextNode(value.toString());
                    fieldelement.appendChild(tel);
                }
                out.appendChild(fieldelement);
            }
        }
        return true;
    }

    /**
     * Creates a new node.
     * @param alias the node alias in the put tree
     * @param values a Map with new node values
     * @param aliases a Map with mappings from XML aliases to node reference values
     * @param nodesadded a Map used to keep associations between xml elements and newly created nodes,
     *                   needed for resolving new node numbers
     * @param out the element that describes 'new' cloud.
     *           The result of the put (an error or the resulting cloud) should be added
     *           as childs to this element.
     * @param cloud the cloud to work on
     * @return true if succesful, false if an error ocurred
     */
    protected boolean putNewNode(String alias, Map values, Map aliases, Map nodesadded, Element out, Cloud cloud) {
        String type=(String)values.get("_otype");
        try {
            NodeManager nm=cloud.getNodeManager(type);
            org.mmbase.bridge.Node newnode=nm.createNode();

            Element objectelement=doc.createElement(OBJECT);
            objectelement.setAttribute(ELM_TYPE,type);
            fillFields(alias,newnode,objectelement,values);
            try {
                newnode.commit();
                int number=newnode.getNumber();
                aliases.put(alias,new Integer(number));
                objectelement.setAttribute(ELM_NUMBER,""+number);
                objectelement.setAttribute(ELM_OLDNUMBER, alias);
                // keep in transaction for later update...
                nodesadded.put(newnode,objectelement);
                // add node to response
                out.appendChild(objectelement);
                return true;
            } catch (RuntimeException e) {
                // give error
                Element err=addContentElement(ERROR,"Cloud can not insert this object, alias number : "+alias + "(" + e.toString() + ")",out);
                err.setAttribute(ELM_TYPE,IS_SERVER);
            }
        } catch (RuntimeException e) {
            // give error this cloud doesn't support this type
            Element err=addContentElement(ERROR,"Cloud does not support type : " + type + "(" + e.toString() + ")",out);
            err.setAttribute(ELM_TYPE,IS_SERVER);
        }
        return false;
    }

    /**
     * Creates a new node.
     * @param alias the node alias in the put tree
     * @param values a Map with new node values
     * @param aliases a Map with mappings from XML aliases to node reference values
     * @param relationsadded a Map used to keep associations between xml elements and newly created relations,
     *                   needed for resolving new node numbers and references
     * @param out the element that describes 'new' cloud.
     *           The result of the put (an error or the resulting cloud) should be added
     *           as childs to this element.
     * @param cloud the cloud to work on
     * @return true if succesful, false if an error ocurred
     */
    protected boolean putNewRelation(String alias, Map values, Map aliases, Map relationsadded, Element out, Cloud cloud) {
        String role=(String)values.get("_role");
        try {
            RelationManager relman=cloud.getRelationManager(role);
            String sourcenumber=getNodeReferenceFromValue("_source",values, aliases);
            String destinationnumber=getNodeReferenceFromValue("_destination",values, aliases);
            Relation newnode=relman.createRelation(cloud.getNode(sourcenumber),cloud.getNode(destinationnumber));
            Element relationelement=doc.createElement(RELATION);
            // note that source and destination may be switched (internally) when you
            // commit a bi-directional relation, if the order of the two differs in typerel
            relationelement.setAttribute(ELM_SOURCE,""+sourcenumber);
            relationelement.setAttribute(ELM_DESTINATION,""+destinationnumber);
            relationelement.setAttribute(ELM_ROLE,role);
            fillFields(alias,newnode,relationelement,values);
            try {
                newnode.commit();
                int number=newnode.getNumber();
                aliases.put(alias,new Integer(number));
                // keep in transaction for later update...
                relationsadded.put(newnode,relationelement);
                // add node to response
                relationelement.setAttribute(ELM_NUMBER,""+number); // result in transaction ???
                out.appendChild(relationelement);
                return true;
            } catch (RuntimeException e) {
                // give error
                Element err=addContentElement(ERROR,"Cloud can not insert this object, alias number : " + alias + "(" + e.toString() + ")",out);
                err.setAttribute(ELM_TYPE,IS_SERVER);
            }
        } catch (RuntimeException e) {
            // give error can't find builder of that type
            Element err=addContentElement(ERROR,"Cloud does not support role : " + role + ":" + e.getMessage(),out);
            err.setAttribute(ELM_TYPE,IS_CLIENT);
        }
        return false;
    }


    /**
     * Creates a new node.
     * @param alias the node alias in the put tree
     * @param orgvalues a Map with the original values of the node
     * @param out the element that describes 'new' cloud.
     *           The result of the put (an error or the resulting cloud) should be added
     *           as childs to this element.
     * @param cloud the cloud to work on
     * @return true if succesful, false if an error ocurred
     */
    protected boolean putDeleteNode(String alias, Map orgvalues, Element out, Cloud cloud) {
        // check if this org node is also found in
        // mmbase cloud and if its still the same
        // also check if its the same type
        String type=(String)orgvalues.get("_otype");
        try {
            NodeManager nm=cloud.getNodeManager(type);
            try {
                org.mmbase.bridge.Node mmbasenode=cloud.getNode(alias);
                if (mmbasenode.getNodeManager().getName().equals(nm.getName()) ) {
                    mmbasenode.delete(true);
                    return true;
                } else {
                    // give error its the wrong type
                    Element err=addContentElement(ERROR,"Node not same type as in the cloud, node number : "+alias+", cloud type="+mmbasenode.getNodeManager().getName()+", expected="+nm.getName()+")",out);
                    err.setAttribute(ELM_TYPE,IS_SERVER);
                }
            } catch(RuntimeException e) {
                // give error node not found
                Element err=addContentElement(ERROR,"Node not in the cloud (anymore?), node number : "+alias + "(" + e.toString() + ")",out);
                err.setAttribute(ELM_TYPE,IS_SERVER);
            }
        } catch(RuntimeException e) {
            // give error can't find builder of that type
            Element err=addContentElement(ERROR,"Cloud does not support type : "+type + "(" + e.toString() + ")",out);
            err.setAttribute(ELM_TYPE,IS_CLIENT);
        }
        return false;
     }

    /**
     * Creates a new node.
     * @param alias the node alias in the put tree
     * @param orgvalues a Map with the original values of the relation
     * @param out the element that describes 'new' cloud.
     *           The result of the put (an error or the resulting cloud) should be added
     *           as childs to this element.
     * @param cloud the cloud to work on
     * @return true if succesful, false if an error ocurred
     */
    protected boolean putDeleteRelation(String alias, Map orgvalues, Element out, Cloud cloud) {
        String role=(String)orgvalues.get("_role");
        try {
            RelationManager relman=cloud.getRelationManager(role);
            try {
                org.mmbase.bridge.Node mmbasenode=cloud.getNode(alias);
                // check if they are the same type
                if (mmbasenode.getNodeManager().getName().equals(relman.getName()) ) {
                    mmbasenode.delete(true);
                    return true;
                } else {
                    Element err=addContentElement(ERROR,"Node not same type as in the cloud, node number : "+alias+", cloud type="+mmbasenode.getNodeManager().getName()+", expected="+relman.getName()+")",out);
                    err.setAttribute(ELM_TYPE,IS_SERVER);
                }
            } catch (RuntimeException e) {
                Element err=addContentElement(ERROR,"Relation not in the cloud (anymore?), relation number : "+alias,out);
                err.setAttribute(ELM_TYPE,IS_SERVER);
            }
        } catch (RuntimeException e) {
            // give error can't find builder of that type
            Element err=addContentElement(ERROR,"Cloud does not support role : "+role,out);
            err.setAttribute(ELM_TYPE,IS_SERVER);
        }
        return false;
     }

    /**
     * Creates a new node.
     * @param alias the node alias in the put tree
     * @param values a Map with new node values
     * @param orgvalues a Map with original node values, used for validation
     * @param aliases a Map with mappings from XML aliases to node reference values
     * @param out the element that describes 'new' cloud.
     *           The result of the put (an error or the resulting cloud) should be added
     *           as childs to this element.
     * @param cloud the cloud to work on
     * @return true if succesful, false if an error ocurred
     */
    protected boolean putChangeNode(String alias, Map values, Map orgvalues, Map aliases, Element out, Cloud cloud) {
        // now check if this org node is also found in
        // mmbase cloud and if its still the same
        // also check if its the same type
        String type=(String)values.get("_otype");
        try {
            NodeManager nm=cloud.getNodeManager(type);
            try {
                org.mmbase.bridge.Node mmbasenode=cloud.getNode(alias);
                // check if they are the same type
                if (mmbasenode.getNodeManager().getName().equals(nm.getName()) ) {
                    // create new node for response
                    Element objectelement=doc.createElement(OBJECT);
                    objectelement.setAttribute(ELM_TYPE,type);
                    objectelement.setAttribute(ELM_NUMBER,alias);
                    if (!fillFields(alias,mmbasenode,objectelement,values,orgvalues)) {
                        out.appendChild(objectelement);
                        return false;
                    }
                    mmbasenode.commit();
                    // add node to response
                    out.appendChild(objectelement);
                    return true;
                } else {
                    // give error its the wrong type
                    Element err=addContentElement(ERROR,"Node not same type as in the cloud, node number : "+alias+", cloud type="+mmbasenode.getNodeManager().getName()+", expected="+nm.getName()+")",out);
                    err.setAttribute(ELM_TYPE,IS_SERVER);
                }

            } catch(RuntimeException e) {
                // give error node not found
                Element err=addContentElement(ERROR,"Node not in the cloud (any more), node number : "+alias + "(" + e.toString() + ")",out);
                err.setAttribute(ELM_TYPE,IS_SERVER);
            }
        } catch(RuntimeException e) {
            // give error can't find builder of that type
            Element err=addContentElement(ERROR,"Cloud does not support type : "+type + "(" + e.toString() + ")",out);
            err.setAttribute(ELM_TYPE,IS_CLIENT);
        }
        return false;
    }

    /**
     * Performs the put within a transaction.
     * @param orgnodes the nodes in the original cloud, used in validation
     * @param newnodes the nodes in the new cloud
     * @param orgrelations the relations in the original cloud, used in validation
     * @param newrelations the relations in the new cloud
     * @param nodesadded a Map used to keep associations between xml elements and newly created nodes,
     *                   needed for resolving new node numbers and references
     * @param relationsadded a Map used to keep associations between xml elements and newly created relations,
     *                   needed for resolving new node numbers and references
     * @param out the element that described the <code>put</code> result.
     *           The result of the put (an error or the resulting cloud) should be added
     *           as childs to this element.
     * @param cloud the cloud to work on
     */
    protected boolean mergeClouds(Map orgnodes, Map newnodes, Map orgrelations, Map newrelations,
                                  Map nodesadded, Map relationsadded, Element out, Cloud cloud) {
        Map aliases=new HashMap(); // hash from alias names to real names

        // create new tag and add it to response
        Element newelement=doc.createElement(NEW);
        out.appendChild(newelement);

        // lets enum all the newnodes
        for (Iterator i = newnodes.entrySet().iterator(); i.hasNext(); ) {
            // handle the several cases we have when we have
            // a new node (merge, really new)
            Map.Entry me=(Map.Entry)i.next();
            String alias=(String)me.getKey();
            Map values=(Map)me.getValue();
            String status=(String)values.get("_status");

            // is it a new node if so create one and remember its alias
            if (status!=null && status.equals("new")) {
                if (!putNewNode(alias,values,aliases, nodesadded, newelement, cloud)) return false;
            } else if (status!=null && status.equals("delete")) {
                // to check if they send a original
                // XXX: no original is not an error ???
                Map orgvalues=(Map)orgnodes.get(alias);
                if (orgvalues!=null) {
                    if(!putDeleteNode(alias,orgvalues,newelement, cloud)) return false;;
                }
            } else if (status==null || status.equals("") || status.equals("change")) {
                // check if they send a original
                Map orgvalues=(Map )orgnodes.get(alias);
                if (orgvalues!=null) {
                    if(!putChangeNode(alias,values,orgvalues,aliases, newelement, cloud)) return false;
                } else {
                    // give error not a org. node
                    Element err=addContentElement(ERROR,"Node not defined in original tag, node number : "+alias,out);
                    err.setAttribute(ELM_TYPE,IS_SERVER);
                    return false;
                }
            } else {
                // give error not a org. node
                Element err=addContentElement(ERROR,"Invalid status "+status+" for node : "+alias,out);
                err.setAttribute(ELM_TYPE,IS_SERVER);
                return false;
            }
        }

        // now handle all the relations
        for (Iterator i = newrelations.entrySet().iterator(); i.hasNext(); ) {
            // handle the several cases we have when we have
            // a new node (merge, really new)
            Map.Entry me=(Map.Entry)i.next();
            String alias=(String)me.getKey();
            Map values=(Map)me.getValue();
            String status=(String)values.get("_status");

            // is it a new node if so create one and remember its alias
            if (status!=null && status.equals("new")) {
                if (!putNewRelation(alias,values,aliases, relationsadded, newelement, cloud)) return false;
            } else if (status!=null && status.equals("delete")) {
                Map orgvalues=(Map)orgrelations.get(alias);
                if (orgvalues!=null) {
                    if(!putDeleteRelation(alias,orgvalues,newelement, cloud)) return false;;
                } // no error ???
            // add code for change relation ?
            } else {
                // give error not a org. node
                Element err=addContentElement(ERROR,"Invalid status "+status+" for node : "+alias,out);
                err.setAttribute(ELM_TYPE,IS_SERVER);
                return false;
            }
        }
        return true;
    }

    /**
     * Handles a request running one or more RPCs.
     * This method accepts a root DOM element, which should contain as it
     * child nodes elements describing the call(s) to perform.
     * Each element is an xml element with as the tagname the name of the
     * call to run. Valid calls are getdata, getrelations, getlist, getconstraints,
     * getnew, and put.
     * The calls are redirected to the appropriate encapsulating method in
     * this class. The end result of each call should be a DOM tree, built
     * using the given parameters, which is then appended to the out element.
     * <br />
     * XXX:This method runs all commands in order of entrance. It does
     * not currently check whether all child nodes refer to the same call.
     * This means that it is possible to mix 'getdata' and 'getrelations' calls
     * in one request. Ina dditon, it ignores calls it does not know how to handle.
     * as described above, this should be handled by the xml parser.
     * Note also that if no id was given for a call, a NullPointer exception is thrown.
     *
     * @param in the element that described the request (or input).
     *           The childnodes should describe the calls to perform.
     * @param out the element that described the response (or return value).
     *           Results of calls should be added as childs to this element.
     * @param cloud the cloud to work on - if null, a cloud will be created by the Dove class
     * @param repository Repository that contains the blobs
     */
    public void doRequest(Element in, Element out, Cloud cloud, Map repository) {
        // set repository for blobs
        if (repository==null) repository= new HashMap();

        Element command=getFirstElement(in);
        if ((cloud==null)) {
            // make new cloud if not specified,
            cloud=checkSecurity(command,out);
            if(cloud==null)
                return; // Athentication failed, so stop
            command=getNextElement(command);
        }
        // skip security if cloud was given - or throw exception???
        if (SECURITY.equals(command.getTagName())) {
            command=getNextElement(command);
        }

        while (command!=null) {
            String cmd=command.getTagName();
            Element data=doc.createElement(cmd);
            if (command.hasAttribute(ELM_ID)) {
                data.setAttribute(ELM_ID, command.getAttribute(ELM_ID));
            }
            out.appendChild(data);
            if (cmd.equals(GETDATA)) {
                getData(command,data,cloud);
            } else if (cmd.equals(GETNEW)) {
                getNew(command,data,cloud);
            } else if (cmd.equals(GETNEWRELATION)) {
                getNewRelation(command,data,cloud);
            } else if (cmd.equals(GETRELATIONS)) {
                getRelations(command,data,cloud);
            } else if (cmd.equals(GETCONSTRAINTS)) {
                getConstraints(command,data,cloud);
            } else if (cmd.equals(GETLIST)) {
                getList(command,data,cloud);
            } else if (cmd.equals(PUT)) {
                put(command,data,cloud,repository);
            } else {
                Element err=addContentElement(ERROR,"Unknown command: "+cmd,data);
                err.setAttribute(ELM_TYPE,IS_PARSER);
            }
            command=getNextElement(command);
        }
    }

    /**
     * checkSecurity tries to authenticate the user that performs the request
     * @return true if the user is authenticated, false otherwise
     */
    private Cloud checkSecurity(Element command, Element out) {
        Element data=doc.createElement(SECURITY);
        out.appendChild(data);

        if (command!=null && command.getTagName().equals(SECURITY)) {
            String username = command.getAttribute(SECURITY_NAME); // retrieve name
            try {
                String password = command.getAttribute(SECURITY_PASSWORD); // retrieve password
                String methodname = command.getAttribute(SECURITY_METHOD ); // retrieve method name
                if ((methodname==null) || (methodname.equals(""))) methodname="name/password";
                String cloudname = command.getAttribute(SECURITY_CLOUD); // retrieve cloud name
                if ((cloudname==null) || (cloudname.equals(""))) cloudname="mmbase";
                HashMap user = new HashMap();
                if ((username!=null) && (!username.equals(""))) {
                    user.put("username", username);
                    user.put("password", password);
                }
                Cloud cloud = LocalContext.getCloudContext().getCloud(cloudname,methodname,user);
                return cloud;
            } catch (RuntimeException e) {
                // most likely security failed...
                log.warn("Authentication error : "+e.getMessage());
                Element err=addContentElement(ERROR,"Authentication error : "+e.getMessage(),data);
                err.setAttribute(ELM_TYPE,IS_CLIENT);
            }
        } else {
            log.warn("Authentication error  (security info missing)");
            Element err=addContentElement(ERROR,"Authentication error (security info missing).",data);
            err.setAttribute(ELM_TYPE,IS_CLIENT);
        }
        return null;
    }
}

