/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.applications.dove;

import java.util.*;
import org.w3c.dom.*;
import org.mmbase.util.logging.*;
import org.mmbase.bridge.Cloud;


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
 * @version $Id$
 */

public abstract class AbstractDove {

    // dtds
    public static final String DOCTYPE_REQUEST  = "https://www.mmbase.org/dtd/doverequest_1_1.dtd";
    public static final String DOCTYPE_RESPONSE = "https://www.mmbase.org/dtd/doveresponse_1_2.dtd";

    // keywords used for tags
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";

    public static final String SECURITY = "security";
    public static final String GETDATA = "getdata";
    public static final String GETNEW = "getnew";
    public static final String GETNEWRELATION = "getnewrelation";
    public static final String GETRELATIONS = "getrelations";
    public static final String GETCONSTRAINTS = "getconstraints";
    public static final String GETLIST = "getlist";
    public static final String PUT = "put";

    public static final String SECURITY_NAME = "name";
    public static final String SECURITY_PASSWORD = "password";
    public static final String SECURITY_CLOUD = "cloud";
    public static final String SECURITY_METHOD = "method";

    public static final String OBJECT = "object"; // confusing?
    public static final String FIELD = "field";
    public static final String ORIGINAL = "original";
    public static final String NEW = "new";
    public static final String RELATIONS = "relations";
    public static final String RELATION = "relation";
    public static final String ERROR = "error";
    public static final String QUERY = "query";
    public static final String PARENT = "parent";
    public static final String DESCENDANTS = "descendants";
    public static final String DESCENDANT = "descendant";

    public static final String SINGULARNAME = "singularname";
    public static final String PLURALNAME = "pluralname";
    public static final String DESCRIPTION = "description";
    public static final String FIELDS = "fields";
    public static final String GUINAME = "guiname";
    public static final String GUITYPE = "guitype";
    public static final String MAXLENGTH = "maxlength";
    public static final String REQUIRED = "required";

    // keywords used for attributes
    public static final String ELM_ID = "id";
    public static final String ELM_NUMBER = "number";
    public static final String ELM_OLDNUMBER = "oldnumber";
    public static final String ELM_TYPE = "type";
    public static final String ELM_SEARCHTYPE = "searchtype";
    public static final String ELM_STATUS = "status";
    public static final String ELM_NAME = "name";
    public static final String ELM_ROLE = "role";
    public static final String ELM_DESTINATION = "destination";
    public static final String ELM_DESTINATIONTYPE = "destinationtype";
    public static final String ELM_SOURCETYPE = "sourcetype";
    public static final String ELM_SEARCHDIR = "searchdir";
    public static final String ELM_CREATEDIR = "createdir";
    public static final String ELM_LANG = "xml:lang";
    public static final String ELM_XPATH = "xpath";
    public static final String ELM_WHERE = "where";
    public static final String ELM_ORDERBY = "orderby";
    public static final String ELM_DIRECTIONS = "directions";
    public static final String ELM_SOURCE = "source";
    public static final String ELM_HREF = "href";
    public static final String ELM_MAYWRITE = "maywrite";
    public static final String ELM_MAYDELETE = "maydelete";
    public static final String ELM_CONTEXT = "context";
    public static final String ELM_SIZE = "size";
    public static final String ELM_ENCODING = "encoding";

    // values
    public static final String IS_PARSER = "parser";
    public static final String IS_SERVER = "server";
    public static final String IS_CLIENT = "client";

    public static final String IS_TRUE = "true";
    public static final String IS_FALSE = "false";

    protected final static String[] TYPE_DESCRIPTIONS = {
        "unknown", "string", "integer", "unknown", "byte", "float", "double", "long", "xml", "node", "datetime", "boolean", "list"
    };

    //logger
    private static final Logger log = Logging.getLoggerInstance(Dove.class);

    /**
     * The document that is being constructed. This should be used to
     * construct new DOM elements.
     */
    protected final Document doc;

    /**
     * Constructor
     * @param doc the Document that is constructed. This should only be used to
     *            construct new DOM elements. New (child) nodes should not be added
     *            to this document, but to the out element.
     */
    public AbstractDove(Document doc) {
        this.doc = doc;
    }

    protected String getTypeDescription(int type) {
       if (type >= 0 && type < TYPE_DESCRIPTIONS.length) {
            return TYPE_DESCRIPTIONS[type];
       } else {
            return TYPE_DESCRIPTIONS[0];
       }
    }

    /**
     * Creates a DOM element which contains a Text Node, and adds it to the
     * specified node as a child.
     * Used for creating field and error tags.
     * @param tagname name of the new element
     * @param content content of the new element as a string
     * @param out the element to which to add the new Element.
     * @return the newly created element
     */
    protected Element addContentElement(String tagname,String content, Element out) {
        Element el = doc.createElement(tagname);
        if (content == null) content = "";
        Text tel = doc.createTextNode(content);
        el.appendChild(tel);
        out.appendChild(el);
        return el;
    }

    /**
     * Retrieves the first child node of an element that is itself an element.
     * If none exist, it returns <code>null</code>.
     * @param item the element to find the first child element of
     * @return the first child element, or <code>null</code>.
     */
    protected Element getFirstElement(Node item) {
        if (item == null) return null;
        item = item.getFirstChild();
        if ((item != null) && !(item instanceof Element)) {
            item = getNextElement(item);
        };
        return (Element)item;
    }

    /**
     * Retrieves the first child node of an element that is itself an element, and has the specified tagname.
     * If none exist, it returns <code>null</code>.
     * @param item the element to find the first child element of
     * @param tagname the tagname of the element desired
     * @return the first child element, or <code>null</code>.
     */
    protected Element getFirstElement(Node item, String tagname) {
        Element elm = getFirstElement(item);
        if (elm != null && !elm.getTagName().equals(tagname)) {
            elm = getNextElement(elm,tagname);
        }
        return elm;
    }

    /**
     * Retrieves the next sibling of an element that is itself an element.
     * If none exist, it returns <code>null</code>.
     * @param item the element to find the sibling element of
     * @return the sibling element, or <code>null</code>.
     */
    protected Element getNextElement(Node item) {
        if (item == null) return null;
        do {
            item = item.getNextSibling();
        } while ((item != null) && !(item instanceof Element));
        return (Element)item;
    }

    /**
     * Retrieves the the next sibling of an element that is itself an element, and has the specified tagname.
     * If none exist, it returns <code>null</code>.
     * @param item the element to find the sibling element of
     * @param tagname the tagname of the element desired
     * @return the first child element, or <code>null</code>.
     */
    protected Element getNextElement(Node item, String tagname) {
        Element elm = getNextElement(item);
        while (elm != null && !elm.getTagName().equals(tagname)) {
            elm = getNextElement(elm);
        }
        return elm;
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
     * @param cloud the MMCI cloud to use, if applicable.
     * @param repository Repository that contains the blobs
     */
    public abstract  void doRequest(Element in, Element out, Cloud cloud, Map<String,byte[]> repository);

    /**
     * Handles a request running one or more RPCs.
     * @param in the element that described the request (or input).
     *           The childnodes should describe the calls to perform.
     * @param out the element that described the response (or return value).
     *           Results of calls should be added as childs to this element.
     */
    public void doRequest(Element in, Element out) {
        doRequest(in, out, null, null);
    }

    /**
     * This method executes the request from the editors, it can be invoked directly
     * @param in the element that described the request (or input).
     *           The childnodes should describe the calls to perform.
     * @param cloud the MMCI cloud to use, if applicable.
     * @param repository Repository that contains the blobs
     * @return the element that described the response (or return value).
     *           Results of calls should be added as childs to this element.
     */
    public Element executeRequest (Element in, Cloud cloud, Map<String,byte[]> repository) {
        Element out = doc.createElement(RESPONSE);
        doc.appendChild(out);
        if (in.getTagName().equals(REQUEST)) {
            doRequest(in, out, cloud, repository);
            return out;
        }
        log.error("Request doesn't contain a <REQUEST> tag.");
        Element err = addContentElement(ERROR, "Request doesn't contain a <REQUEST> tag.", out);
        err.setAttribute(ELM_TYPE, IS_CLIENT);
        return out;
    }


    /**
     * This method executes the request from the editors, it can be invoked directly
     * @param in the element that described the request (or input).
     *           The childnodes should describe the calls to perform.
     * @return the element that described the response (or return value).
     *           Results of calls should be added as childs to this element.
     */
    public Element executeRequest (Element in) {
        return executeRequest(in, null, null);
    }
}

