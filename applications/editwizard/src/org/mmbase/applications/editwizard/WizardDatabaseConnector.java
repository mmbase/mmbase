/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.editwizard;

import org.mmbase.bridge.Cloud;
import java.util.*;
import java.io.*;
import org.mmbase.applications.dove.Dove;
import org.mmbase.util.logging.*;
import org.mmbase.bridge.LocalContext;
import org.w3c.dom.*;


/**
 * This class handles all communition with mmbase. It uses the MMBase-Dove code to do the transactions and get the information
 * needed for rendering the wizard screens.
 * The WizardDatabaseConnector can connect to MMBase and get data, relations, constraints, lists. It can also
 * store changes, create new objects and relations.
 *
 * The connector can be instantiated without the wizard class, but will usually be called from the Wizard class itself.
 *
 * EditWizard
 * @javadoc
 * @author Kars Veling
 * @author Michiel Meeuwissen
 * @author Pierre van Rooden
 * @since MMBase-1.6
 * @version $Id: WizardDatabaseConnector.java,v 1.20 2002-08-26 14:38:50 michiel Exp $
 *
 */
public class WizardDatabaseConnector {

    // logging
    private static Logger log = Logging.getLoggerInstance(WizardDatabaseConnector.class.getName());

    int didcounter=1;
    private Cloud userCloud = null;

    /**
     * Constructor: Creates the connector. Call #init also.
     */
    public WizardDatabaseConnector(){
    }

    /**
     * Sets the right cloud for the user info.
     *
     * @param       cloud   The cloud from which the userinfo should be set.
     */
    public void setUserInfo(Cloud cloud) {
        userCloud=cloud;
    }

    /**
     * This method tags the datanodes in the given document. The wizards use the tagged datanodes so that each datanode can be identified.
     *
     * @param       data    The data document which should be tagged.
     */
    public void tagDataNodes(Document data) {
        didcounter = 1;
        tagDataNode(data.getDocumentElement());
    }

    /**
     * This method tags datanodes starting from the current node.
     * A internal counter is used to make sure identifiers are still unique.
     */
    public void tagDataNode(Node node) {
        NodeList nodes = Utils.selectNodeList(node, ".|.//*");
        didcounter = Utils.tagNodeList(nodes, "did", "d", didcounter);
    }

    /**
     * This method loads relations from MMBase and stores the result in the given object node.
     *
     * @param       object          The objectNode where the results should be appended to.
     * @param       objectnumber    The objectnumber of the parentobject from where the relations should originate.
     * @param       loadaction      The node with loadaction data. Has inforation about what relations should be loaded and what fields should be retrieved.
     */
    private void loadRelations(Node object, String objectnumber, Node loadaction) throws Exception {
        // loads relations using the loadaction rules
        if ((org.mmbase.Version.getMajor()==1) && (org.mmbase.Version.getMinor()==5)) {
            // backward compatibilty: The Dove included with MMBase 1.5 does not
            // auto-load objects in relations.
            // This code is included only for the separate wizard release and is deprecated
            loadRelations15(object,objectnumber,loadaction);
            return;
        }
        // MMBase 1.6 code and beyond
        NodeList reldefs = Utils.selectNodeList(loadaction, "relation");
        // complete reldefs: add empty <object> tag where there is none.
        for (int i=0; i<reldefs.getLength(); i++) {
            Node rel = reldefs.item(i);
            // if there is not yet an object attached, load it now
            NodeList objs = Utils.selectNodeList(rel, "object");
            if (objs.getLength()==0) {
                Element obj = rel.getOwnerDocument().createElement("object");
                rel.appendChild(obj);
            }
        }
        // load relations (automatically loads related objects and 'deep' relations)
        if (reldefs.getLength()>0) getRelations(object, objectnumber, reldefs);
    }

    /**
     * This method loads relations from MMBase and stores the result in the given object node.
     * @deprecated This code is for backward compatibilty: The Dove included with MMBase 1.5 does not
     *              auto-load objects in relations. This code is included only for the separate wizard release.
     *
     * @param       object          The objectNode where the results should be appended to.
     * @param       objectnumber    The objectnumber of the parentobject from where the relations should originate.
     * @param       loadaction      The node with loadaction data. Has inforation about what relations should be loaded and what fields should be retrieved.
     */
    private void loadRelations15(Node object, String objectnumber, Node loadaction) throws Exception {
        NodeList reldefs = Utils.selectNodeList(loadaction, "relation");
        if (reldefs.getLength()>0) getRelations(object, objectnumber, reldefs);

        // load objects to which relations are pointing
        // uses a bit odd syntax (relation directly under relation).
        NodeList loadactionrestrictionfields = Utils.selectNodeList(loadaction, "field");
        NodeList rels = Utils.selectNodeList(object, "relation");
        for (int i=0; i<rels.getLength(); i++) {
            Node rel = rels.item(i);
            String relatedobject = Utils.getAttribute(rel, "destination", "");
            Node loadedobj = getData(rel, relatedobject, loadactionrestrictionfields);

            // object loaded. Check to see if we need to follow more relations...
            // find corresponding loadaction settings...
            String reldestination=Utils.getAttribute(loadedobj, "type");
            Node deeprels = Utils.selectSingleNode(loadaction, "relation[@destination='"+reldestination+"']/object[relation/@destination]");
            if (deeprels!=null) {
                // yep. we should carry on loading! (Recurse!)
                loadRelations(loadedobj, Utils.getAttribute(loadedobj,"number"), deeprels);
            }
        }
    }

    /**
     * This method loads an object and the necessary relations and fields, according to the given schema.
     *
     * @param schema The schema carrying all the information needed for loading the proper object and related fields and objects.
     * @param objectnumber The objectnumber of the object to start with.
     * @return The resulting data document.
     */
    public Document load(Node schema, String objectnumber) throws Exception {
        // intialize data xml
        Document data = Utils.parseXML("<data />");

        try {
            // load initial object using object number
            log.debug("Loading: " + objectnumber);

            // restrict fields to load
            NodeList fieldstoload = Utils.selectNodeList(schema, "action[@type='load']/field");
            Node object=null;
            if (fieldstoload==null || fieldstoload.getLength()==0) {
                object = getData(data.getDocumentElement(), objectnumber);
            } else {
                object = getData(data.getDocumentElement(), objectnumber, fieldstoload);
            }

            // load relations, if present
            Node loadaction = Utils.selectSingleNode(schema, "action[@type='load']");
            if (loadaction!=null) {
                loadRelations(object, objectnumber, loadaction);
            }
        } catch (Exception e) {
            log.error("Could not load object ["+objectnumber+"]. MMBase returned some errors.\n"+e.getMessage());
            throw e;
        }
        tagDataNodes(data);
        return data;
    }

    /**
     * This method gets constraint information from mmbase about a specific objecttype.
     *
     * @param     objecttype      the objecttype where you want constraint information from.
     * @return   the constraintsnode as received from mmbase (Dove)
     */
    public Node getConstraints(String objecttype) throws Exception {
        // fires getData command and places result in targetNode
        ConnectorCommand cmd = new ConnectorCommandGetConstraints(objecttype);
        fireCommand(cmd);

        if (!cmd.hasError()) {
            // place object in targetNode
            Node result = cmd.getResponseXML().getFirstChild().cloneNode(true);
            return result;
        } else {
            throw new Exception("getConstraints command not succesful, for objecttype " + objecttype);
        }
    }

    /**
     * This method retrieves a list from mmbase. It uses a query which is sent to mmbase.
     */
    public Node getList(Node query) throws Exception {
        // fires getData command and places result in targetNode
        ConnectorCommand cmd = new ConnectorCommandGetList(query);
        fireCommand(cmd);

        if (!cmd.hasError()) {
           // place object in targetNode
            if (log.isDebugEnabled()) log.debug(Utils.getSerializedXML(cmd.getResponseXML()));
            Node result = cmd.getResponseXML().cloneNode(true);
            return result;
        } else {
            throw new Exception("getList command not succesful");
        }
    }

    /**
     * This method retrieves data (objectdata) from mmbase.
     *
     * @param     targetNode      Results are appended to this targetNode.
     * @param     objectnumber    The number of the object to load.
     * @return   The resulting node with the objectdata.
     */
    public Node getData(Node targetNode, String objectnumber) throws Exception {
        return getData(targetNode, objectnumber, null);
    }

    /**
     * This method retrieves data (objectdata) from mmbase.
     *
     * @param     targetNode      Results are appended to this targetNode.
     * @param     objectnumber    The number of the object to load.
     * @param     restrictions    These restrictions will restrict the load action. So that not too large or too many fields are retrieved.
     * @return   The resulting node with the objectdata.
     */
    public Node getData(Node targetNode, String objectnumber, NodeList restrictions) throws Exception {
        // fires getData command and places result in targetNode
        Node objectNode=getDataRaw(objectnumber, restrictions);
        // place object in targetNode
        objectNode = targetNode.getOwnerDocument().importNode(objectNode.cloneNode(true),true);
        tagDataNode(objectNode);
        targetNode.appendChild(objectNode);
        return objectNode;
    }

    /**
     * This method retrieves data (objectdata) from mmbase.
     *
     * @param     objectnumber    The number of the object to load.
     * @param     restrictions    These restrictions will restrict the load action. So that not too large or too many fields are retrieved.
     * @return   The resulting node with the objectdata.
     */
    public Node getDataRaw(String objectnumber, NodeList restrictions) throws Exception {

        // fires getData command and places result in targetNode
        ConnectorCommandGetData cmd = new ConnectorCommandGetData(objectnumber, restrictions);
        fireCommand(cmd);

        if (!cmd.hasError()) {
            // place object in targetNode
            return Utils.selectSingleNode(cmd.getResponseXML(), "/*/object[@number='" + objectnumber + "']");
        } else {
            throw new Exception("Could not fire getData command for object " + objectnumber);
        }
    }

    public void getRelations(Node targetNode, String objectnumber) throws Exception {
        getRelations(targetNode, objectnumber, null);
    }

    /**
     * This method gets relation information from mmbase.
     *
     * @param       targetNode      The targetnode where the results should be appended.
     * @param       objectnumber    The objectnumber of the parent object from where the relations originate.
     * @param       loadaction      The loadaction data as defined in the schema. These are used as 'restrictions'.
     *
     */
    public void getRelations(Node targetNode, String objectnumber, NodeList queryrelations) throws Exception {
        // fires getRelations command and places results targetNode
        ConnectorCommandGetRelations cmd = new ConnectorCommandGetRelations(objectnumber, queryrelations);
        fireCommand(cmd);
        if (!cmd.hasError()) {
            NodeList relations = Utils.selectNodeList(cmd.getResponseXML(), "/*/object/relation");

            for (int i=0; i<relations.getLength(); i++) {
                tagDataNode(relations.item(i));
            }
            Utils.appendNodeList(relations, targetNode);
        } else {
            throw new Exception("Could NOT fire getRelations command for object " + objectnumber);
        }
    }

    /**
     * This method gets a new temporarily object of the given type.
     *
     * @param       targetNode      The place where the results should be appended.
     * @param       objecttype      The objecttype which should be created.
     * @return     The resulting object node.
     */
    public Node getNew(Node targetNode, String objecttype) throws Exception {
        // fires getNew command and places result in targetNode
        ConnectorCommandGetNew cmd = new ConnectorCommandGetNew(objecttype);
        fireCommand(cmd);

        if (!cmd.hasError()) {
            Node objectNode = targetNode.getOwnerDocument().importNode(Utils.selectSingleNode(cmd.getResponseXML(), "/*/object[@type='"+objecttype+"']").cloneNode(true), true);
            tagDataNode(objectNode);
            targetNode.appendChild(objectNode);
            return objectNode;
        } else {
            throw new Exception("getNew command returned an error. Objecttype="+objecttype);
        }
    }

    /**
     * This method creates a new temporarily relation.
     *
     * @param     targetNode      The place where the results should be appended.
     * @param     role            The name of the role the new relation should have.
     * @param     sourceobjectnumber      the number of the sourceobject
     * @param     destinationobjectnumber the number of the destination object
     * @return   The resulting relation node.
     */
    public Node getNewRelation(Node targetNode, String role, String sourceobjectnumber, String destinationobjectnumber) throws Exception {
        // fires getNewRelation command and places result in targetNode
        ConnectorCommandGetNewRelation cmd = new ConnectorCommandGetNewRelation(role, sourceobjectnumber, destinationobjectnumber);
        fireCommand(cmd);

        if (!cmd.hasError()) {
            Node objectNode = targetNode.getOwnerDocument().importNode(Utils.selectSingleNode(cmd.getResponseXML(), "/*/relation").cloneNode(true), true);
            tagDataNode(objectNode);
            targetNode.appendChild(objectNode);
            return objectNode;
        } else {
            throw new Exception("getNewRelation command returned an error. role="+role + ", source="+sourceobjectnumber+", dest="+destinationobjectnumber);
        }
    }

    /**
     * Adds or replaces values specified for fields in the wizard to a recently created node.
     * @param data the document conatining all (current) object data
     * @param targetParentNode The place where the results should be appended.
     * @param objectDef The objectdefinition.
     * @param objectNode The new object
     * @param params The parameters to use when creating the objects and relations.
     * @param createorder ordernr under which this object is added (i.e. when added to a list)
     *                     The first ordernr in a list is 1
     */
    private void fillObjectFields(Document data, Node targetParentNode, Node objectDef,
                                  Node objectNode, Map params, int createorder) {
        // fill-in (or place) defined fields and their values.
        NodeList fields = Utils.selectNodeList(objectDef, "field");
        for (int i=0; i<fields.getLength(); i++) {
            Node field = fields.item(i);
            String fieldname = Utils.getAttribute(field, "name");
            // does this field already exist?
            Node datafield = Utils.selectSingleNode(objectNode, "field[@name='"+fieldname+"']");
            String value=Utils.getText(field);
            // if you add a list of items, the order of the list may be of import.
            // the variable $pos is used to make that distinction
            params.put("pos",createorder+"");
            Node parent=data.getDocumentElement();
            if (log.isDebugEnabled()) log.debug("parent="+parent.toString());
            value=Utils.transformAttribute(parent,value,false,params);
            params.remove("pos");
            if (value==null) value="";
            if (datafield!=null) {
                // yep. fill-in
                Utils.storeText(datafield, value, params); // place param values inside if needed
            } else {
                // nope. create. (Or, actually, clone and import node from def and place it in data
                Node newfield = targetParentNode.getOwnerDocument().importNode(field.cloneNode(true), true);
                objectNode.appendChild(newfield);
                Utils.storeText(newfield, value, params); // process innerText: check for params
            }
        }
    }

    /**
     * This method can create a object (or a tree of objects and relations)
     *
     * @param targetParentNode The place where the results should be appended.
     * @param objectDef The objectdefinition.
     * @param params The params to use when creating the objects and relations.
     * @return The resulting object(tree) node.
     */
    public Node createObject(Document data, Node targetParentNode, Node objectDef, Map params) throws WizardException {
        return createObject(data, targetParentNode, objectDef, params, 1);
    }

    /**
     * This method can create a object (or a tree of objects and relations)
     *
     * this method should be called from the wizard if it needs to create a new
     * combination of fields, objects and relations.
     * See further documentation or the samples for the definition of the action
     *
     * in global: the Action node looks like this: (Note: this function expects the $lt;object/> node,
     * not the action node.
     * <pre>
     * $lt;action type="create"$gt;
     *   $lt;object type="authors"$gt;
     *     $lt;field name="name"$gt;Enter name here$lt;/field$gt;
     *     $lt;relation role="related" [destination="234" *]$gt;
     *       $lt;object type="locations"$gt;
     *         $lt;field name="street"$gt;Enter street$lt;/field$gt;
     *       $lt;/object$gt;
     *     $lt;/relation$gt;
     *   $lt;/object$gt;
     * $lt;/action$gt;
     * </pre>
     * *) if dnumber is supplied, no new object is created (shouldn't be included in the relation node either),
     *  but the relation will be created and directly linked to an object with number "dnumber".
     *
     * @param targetParentNode The place where the results should be appended.
     * @param objectDef The objectdefinition.
     * @param params The params to use when creating the objects and relations.
     * @param createorder ordernr under which this object is added (i.e. when added to a list)
     *                     The first ordernr in a list is 1
     * @return The resulting object(tree) node.
     */
    public Node createObject(Document data, Node targetParentNode, Node objectDef, Map params, int createorder) throws WizardException {

        String objecttype = Utils.getAttribute(objectDef, "type");

        if (log.isDebugEnabled()) log.debug("Create object of type " + objecttype);
        String nodename = objectDef.getNodeName();

        // no relations to add here..
        NodeList relations = null;
        Node objectNode = null;

        // check if we maybe should create multiple objects or relations
        if (nodename.equals("action")) {
            NodeList objectdefs = Utils.selectNodeList(objectDef, "object|relation");
            Node firstobject=null;
            for (int i=0; i<objectdefs.getLength(); i++) {
                firstobject=createObject(data,targetParentNode, objectdefs.item(i), params);
            }
            return firstobject;
        }

        if (nodename.equals("relation")) {
            // objectNode equals targetParentNode
            objectNode = targetParentNode;
            relations = Utils.selectNodeList(objectDef, ".");
        }

        if (nodename.equals("object")) {
            try {
                // create a new object of the given type
                objectNode = getNew(targetParentNode, objecttype);
            } catch (Exception e) {
                log.error("Could NOT createObject with type:"+objecttype+". Message: "+ e.getMessage());
                throw new WizardException("Could NOT createObject with type:"+objecttype+". Message: "+ e.getMessage());
            }
            fillObjectFields(data,targetParentNode,objectDef,objectNode,params,createorder);
            relations = Utils.selectNodeList(objectDef, "relation");
        }

        // Let's see if we need to create new relations (maybe even with new objects inside...
        Node lastCreatedRelation = null;

        for (int i=0; i<relations.getLength(); i++) {
            Node relation = relations.item(i);
            String dnumber = Utils.getAttribute(relation, "destination", null);
            dnumber=Utils.transformAttribute(data.getDocumentElement(), dnumber, false, params);
            String role="related";
            String snumber="";
            Node relationNode = null;
            Node inside_object = null;
            try {
                // create the relation now we can get all needed params
                role = Utils.getAttribute(relation, "role", "related");
                snumber = Utils.getAttribute(objectNode, "number");
                // dnumber can be null
                relationNode = getNewRelation(objectNode, role, snumber, dnumber);
                fillObjectFields(data,targetParentNode,relation,relationNode,params,createorder);

                tagDataNode(relationNode);
                lastCreatedRelation = relationNode;
            } catch (Exception e) {
                log.error("Could NOT create relation in createObject. Role="+role+", snumber="+snumber+", destination="+dnumber);
                return null;
            }

            if (dnumber==null) {
                // no dnumber given! create the object
                Node inside_objectdef = Utils.selectSingleNode(relation, "object");
                if (inside_objectdef==null) {
                    // no destination is given AND no object to-be-created-new is placed.
                    // so, no destination should be added...
//                    tempobjectholder = Utils.parseXML("<object number=\"\" type=\"" + Utils.getAttribute(relation, "destinationtype", "") + "\" disposable=\"true\"/>");
//                    inside_object = tempobjectholder.getDocumentElement();
                    inside_object=data.createElement("object");
                    ((Element)inside_object).setAttribute("number","");
                    ((Element)inside_object).setAttribute("type",Utils.getAttribute(relation, "destinationtype", ""));
                    ((Element)inside_object).setAttribute("disposable","true");
                } else {
//                    tempobjectholder = Utils.parseXML("<tmpdata/>");
                    inside_object = createObject(data,relationNode, inside_objectdef, params);
                    dnumber = Utils.getAttribute(inside_object, "number");
                    ((Element)relationNode).setAttribute("destination",dnumber);
                }
                relationNode.appendChild(inside_object);
            }

            try {
                // now check if we need to load the inside object...
                if (inside_object==null) {
                    // yep. we don't have it yet. Let's load it
                    inside_object = getData(relationNode, dnumber);
                    // but annotate that thisone is loaded from mmbase. Not a new one
                    Utils.setAttribute(inside_object, "already-exists", "true");
                } else {
                    // we already have it. Let's copy/clone and place it.
//                    inside_object = relationNode.getOwnerDocument().importNode(inside_object.cloneNode(true), true);
//                    relationNode.appendChild(inside_object);
                }
            } catch (Exception e) {
                log.error("Could NOT place inside object in createObject. Message: "+e.getMessage());
                return null;
            }
        }
        if (nodename.equals("relation")) {
            return lastCreatedRelation;
        } else {
            return objectNode;
        }
    }

    /**
     * Sends an Element containing the xml-representation of a command to a Dove
     * class, and retuirns the result as an Element.
     * @param cmd the command Element to execute
     * @param binaries a HashMap containing files (binaries) uploaded in the wizard
     */
    private Element sendCommand(Element cmd, Map binaries) throws WizardException {
        Element results=null;
        try {
            Document tmp = Utils.emptyDocument();
            Dove dove = new Dove(tmp);
            results = dove.executeRequest(cmd,userCloud,binaries);
        } catch (Exception e) {
            log.error("Error while communicating with Dove servlet."+e.getMessage());
            throw new WizardException("Error while communicating with Dove servlet. Message:"+e.getMessage());
        }
        return results;
    }

    /**
     * This is an internal method which is used to fire a command to connect to mmbase via Dove.
     */
    private Document fireCommand(ConnectorCommand command) throws SecurityException,WizardException {
        List tmp = new Vector();
        tmp.add(command);
        return fireCommandList(tmp);
    }

    /**
     * This is an internal method which is used to fire commands to connect to mmbase via Dove.
     */
    private Document fireCommandList(List commands) throws SecurityException,WizardException {
        // send commands away from here... away!
        // first create request xml
        Document req = Utils.parseXML("<request/>");
        Element docel = req.getDocumentElement();

        Iterator i  = commands.iterator();
        while (i.hasNext()) {
            ConnectorCommand cmd = (ConnectorCommand) i.next();
            docel.appendChild(req.importNode(cmd.getCommandXML().getDocumentElement().cloneNode(true), true));
        }

        String res="";

        Element results=sendCommand(docel,null);

        Document response = Utils.emptyDocument();
        response.appendChild(response.importNode(results,true));

        // map response back to each command.
        i = commands.iterator();
        while (i.hasNext()) {
            ConnectorCommand cmd = (ConnectorCommand) i.next();

            // find response for this command
            Node resp = Utils.selectSingleNode(response, "/*/"+cmd.getName() +"[@id]");
            if (resp!=null) {
                // yes we found a response
                cmd.setResponse(resp);
            } else {
                log.error("Could NOT store response "+cmd.getId()+" in a ConnectorCommand");
                log.error(cmd.getResponseXML());
            }
        }
        return response;
    }

    /**
     * This method can fire a Put command to Dove. It uses #getPutData to construct the transaction.
     *
     * @param     originalData            The original data object tree.
     * @param     newData                 The new and manipulated data. According to differences between the original and the new data, the transaction is constructed.
     * @param     binaries                 A hashmap with the uploaded binaries.
     * @return   The element containing the results of the put transaction.
     */
    public Element put(Document originalData, Document newData, Map binaries) throws WizardException {
        Node putcmd =getPutData(originalData, newData);
        return sendCommand(putcmd.getOwnerDocument().getDocumentElement(), binaries);
    }

    /**
     * This method constructs a update transaction ready for mmbase.
     * The differences between the original and the new data define the transaction.
     *
     * @param     originalData    The original data.
     * @param     newData         The new data.
     */
    public Node getPutData(Document originalData, Document newData) throws WizardException {
        Document workDoc = Utils.emptyDocument();
        workDoc.appendChild(workDoc.importNode(newData.getDocumentElement().cloneNode(true), true));

        Node workRoot = workDoc.getDocumentElement();

        // initialize request xml
        Document req = Utils.parseXML("<request><put id=\"put\"><original/><new/></put></request>");

        Node reqorig = Utils.selectSingleNode(req, "/request/put/original");
        Node reqnew = Utils.selectSingleNode(req, "/request/put/new");

        // Remove disposable objects (disposable=true)
        // Remove all relations which have NO destination chosen (destination="-");
        NodeList disposables = Utils.selectNodeList(workRoot, ".//*[@disposable or @destination='-']");
        for (int i=0; i<disposables.getLength(); i++) {
            Node disp = disposables.item(i);
            disp.getParentNode().removeChild(disp);
        }

        // serialize original data. Place objects first, relations second
        makeFlat(originalData, reqorig, ".//object", "field");
        makeFlat(originalData, reqorig, ".//relation", "field");

        // serialize new data. Place objects first, relations second
        makeFlat(workRoot, reqnew, ".//object", "field");
        makeFlat(workRoot, reqnew, ".//relation", "field");

        // find all changed or new relations and objects
        NodeList nodes = Utils.selectNodeList(reqnew, ".//relation|.//object[not(@disposable)]");
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String nodename = node.getNodeName();

            String nodenumber = Utils.getAttribute(node, "number", "");
            Node orignode = Utils.selectSingleNode(reqorig, ".//*[@number='"+nodenumber+"' and not(@already-exists)]");
            if (orignode!=null) {
                // we found the original relation. Check to see if destination has changed.
                if (nodename.equals("relation")) {
                    String destination = Utils.getAttribute(node,"destination", "");
                    String olddestination = Utils.getAttribute(orignode,"destination", "");
                    if (!destination.equals(olddestination) && !destination.equals("")) {
                        // ok. it's different
                        Utils.setAttribute(node, "status", "changed");
                        // store original destination also. easier to process later on
                        Utils.setAttribute(node, "olddestination", olddestination);
                    } else {
                        // it's the same (or at least: the destination is the same)
                        // now se check if some inside-fields are changed.
                        boolean valueschanged = checkRelationFieldsChanged(orignode, node);

                        if (valueschanged) {
                            // values in the fields are changed, destination/source are still te same.
                            // let's store that knowledge.
                            Utils.setAttribute(node,"status", "fieldschangedonly");
                        } else {
                            // really nothing changed.
                                // remove relation from both orig as new
                            node.getParentNode().removeChild(node);
                                orignode.getParentNode().removeChild(orignode);
                        }
                    }
                }
                if (nodename.equals("object")) {
                    // object
                    // check if it is changed
                    boolean different = isDifferent(node, orignode);
                    if (!different) {
                        // remove both objects
                        node.getParentNode().removeChild(node);
                        orignode.getParentNode().removeChild(orignode);
                    } else {
                        // check if fields are different?
                        NodeList fields=Utils.selectNodeList(node,"field");
                        for (int j=0; j<fields.getLength(); j++) {
                            Node origfield = Utils.selectSingleNode(orignode, "field[@name='"+Utils.getAttribute(fields.item(j), "name")+"']");
                            if (origfield!=null) {
                                if (!isDifferent(fields.item(j), origfield)) {
                                    // the same. let's remove this field also
                                    fields.item(j).getParentNode().removeChild(fields.item(j));
                                    origfield.getParentNode().removeChild(origfield);
                                }
                            }
                        }
                    }
                }
            } else {
                // this is a new relation or object. Remember that
                // but, check first if the may-be-new object has a  "already-exists" attribute. If so,
                // we don't have a new object, no no, this is a later-loaded object which is not added to the
                // original datanode (should be better in later versions, eg. by using a repository).
                String already_exists = Utils.getAttribute(node, "already-exists", "false");
                if (!already_exists.equals("true")) {
                    // go ahead. this seems to be a really new one...
                    Utils.setAttribute(node, "status", "new");
                } else {
                    // remove it from the list.
                    node.getParentNode().removeChild(node);
                }
            }
        }

        // find all deleted relations and objects
        NodeList orignodes = Utils.selectNodeList(reqorig, ".//relation|.//object");
        for (int i=0; i<orignodes.getLength(); i++) {
            Node orignode = orignodes.item(i);

            String nodenumber = Utils.getAttribute(orignode, "number", "");
            Node node = Utils.selectSingleNode(reqnew, ".//*[@number='"+nodenumber+"']");
            if (node==null) {
                // item is apparently deleted.
                // place relation node anyway but say that it should be deleted (and make it so more explicit)
                Node newnode = req.createElement(orignode.getNodeName());
                Utils.copyAllAttributes(orignode, newnode);
                Utils.setAttribute(newnode, "status", "delete");
                reqnew.appendChild(newnode);
            }
        }

        // now, do our final calculations:
        //
        //
        // 2. change "changed" relations into a delete + a create command.
        //    and, make sure the create command is in the right format.
        NodeList rels = Utils.selectNodeList(req, "//new/relation[@status='changed']");
        for (int i=0; i<rels.getLength(); i++) {
            Node rel = rels.item(i);
            Node newrel = rel.cloneNode(true);

            // say that old relation should be deleted
            Utils.setAttribute(rel, "destination", Utils.getAttribute(rel, "olddestination", ""));
            rel.getAttributes().removeNamedItem("olddestination");
            Utils.setAttribute(rel, "status", "delete");

            // say that a new relation should be formed
            newrel.getAttributes().removeNamedItem("number");
            newrel.getAttributes().removeNamedItem("olddestination");
            Utils.setAttribute(newrel, "status", "new");
            String role = Utils.getAttribute(newrel, "role", "related");
            Utils.setAttribute(newrel, "role", role);

            // copy inside fields also (except dnumber, snumber and rnumber fields)
            NodeList flds = Utils.selectNodeList(rel, "field");
            Utils.appendNodeList(flds,rel);

            // store the new rel in the list also
            rel.getParentNode().appendChild(newrel);
        }

        //
        // 3. search for 'fieldschangedonly' fields of the relations. If so, we should make a special command for the Dove
        // servlet.
        //
        rels = Utils.selectNodeList(req, "//new/relation[@status='fieldschangedonly']");
        for (int i=0; i<rels.getLength(); i++) {
            Node rel = rels.item(i);
            String number = Utils.getAttribute(rel,"number","");
            Node origrel = Utils.selectSingleNode(req, "//original/relation[@number='"+number+"']");
            if (!number.equals("") && origrel!=null) {
                // we found the original relation also. Now, we can process these nodes.
                convertRelationIntoObject(origrel);
                convertRelationIntoObject(rel);
            }
        }
        return req.getDocumentElement();
    }

    /**
     * This method makes the object data tree flat, so that Dove can construct a transaction from it.
     *
     * @param     sourcenode              The sourcenode from which should be flattened.
     * @param     targetParentNode        The targetParentNode where the results should be appended.
     * @param     allowChildrenXpath      This xpath defines what children may be copied in te proces and should NOT be flattened.
     */
    public void makeFlat(Node sourcenode, Node targetParentNode, String xpath, String allowedChildrenXpath) {
        NodeList list = Utils.selectNodeList(sourcenode, xpath);
        for (int i=0; i<list.getLength(); i++) {
            Node item = list.item(i);
            Node newnode = targetParentNode.getOwnerDocument().importNode(item, false);
            targetParentNode.appendChild(newnode);
            cloneOneDeep(item, newnode, allowedChildrenXpath);
        }
    }

    /**
     * This method can clone an object node one deep. So, only the first level children will be copied.
     *
     * @param     sourcenode              The sourcenode from where the flattening should start.
     * @param     targetParentNode        The parent on which the results should be appended.
     * @param     allowedChildrenXpath    The xpath describing what children may be copied.
     */
    public void cloneOneDeep(Node sourcenode, Node targetParentNode, String allowedChildrenXpath) {
        NodeList list = Utils.selectNodeList(sourcenode, allowedChildrenXpath);
        Utils.appendNodeList(list, targetParentNode);
    }

    /**
     * This method compares two xml nodes and checks them for identity.
     *
     * @param     node1   The first node to check
     * @param     node2   Compare with thisone.
     * @return   True if they are different, False if they are similar.
     */
    public boolean isDifferent(Node node1, Node node2) {
        // only checks textnodes and childnumbers
        boolean res = false;
        if (node1.getChildNodes().getLength()!=node2.getChildNodes().getLength()) {
            // ander aantal kindjes!
            return true;
        }
        // andere getnodetype?
        if (node1.getNodeType()!=node2.getNodeType()) {
            return true;
        }
        if (node1.getNodeType()==Node.TEXT_NODE) {
            String s1 = node1.getNodeValue();
            String s2 = node2.getNodeValue();
            if (!s1.equals(s2)) return true;
        }
        // check kids
        NodeList kids = node1.getChildNodes();
        NodeList kids2 = node2.getChildNodes();

        for (int i=0; i<kids.getLength(); i++) {
            if (isDifferent(kids.item(i), kids2.item(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method checks if relationfields are changed. It does NOT check the source-destination numbers, only the other fields,
     * like the pos field in a posrel relation.
     *
     * @param       origrel         The original relation
     * @param       rel             The new relation
     * @return     True if the relations are different, false if they are the same.
     */
    private boolean checkRelationFieldsChanged(Node origrel, Node rel) throws WizardException{
        NodeList origflds = Utils.selectNodeList(origrel, "field");
        NodeList newflds = Utils.selectNodeList(rel, "field");
        Document tmp = Utils.parseXML("<tmp><n1><r/></n1><n2><r/></n2></tmp>");

        Node n1 = Utils.selectSingleNode(tmp, "/tmp/n1/r");
        Node n2 = Utils.selectSingleNode(tmp, "/tmp/n2/r");

        Utils.appendNodeList(origflds,n1);
        Utils.appendNodeList(newflds,n2);

        return isDifferent(n1,n2);
    }

    /**
     * this method converts a <relation /> node into an <object /> node.
     * source/destination attributes are removed, and rnumber, snumber, dnumber fields also.
     * It is mainly used by the putData commands to store extra relation fields values.
     * @param     rel     The relation which should be converted.
     */
    private void convertRelationIntoObject(Node rel) {

        Node obj = rel.getOwnerDocument().createElement("object");

        // copy attributes, except...
        List except = new Vector();
        except.add("destination");
        except.add("source");
        except.add("role");
        except.add("status"); // we don't need status anymore also!
        Utils.copyAllAttributes(rel, obj, except);

        // copy fields, except... (uses RELATIONFIELDS_XPATH)
        NodeList flds = Utils.selectNodeList(rel, "field");

        Utils.appendNodeList(flds, obj);

        // remove rel, place obj instead
        rel.getParentNode().replaceChild(obj, rel);
    }
}
