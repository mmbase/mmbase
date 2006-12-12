/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.workflow;

import java.util.Iterator;
import java.util.List;

import net.sf.mmapps.commons.bridge.RelationUtil;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.storage.search.RelationStep;
import org.mmbase.storage.search.Step;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import com.finalist.cmsc.navigation.*;
import com.finalist.cmsc.security.*;
import com.finalist.cmsc.services.publish.Publish;
import com.finalist.cmsc.services.workflow.Workflow;
import com.finalist.cmsc.services.workflow.WorkflowException;


public class PageWorkflow extends WorkflowManager {

    /** MMbase logging system */
    private static Logger log = Logging.getLoggerInstance(PageWorkflow.class.getName());

    public static final String TYPE_PAGE = "page";
    
    public PageWorkflow(Cloud cloud) {
        super(cloud);
    }

    protected Node getPageNode(Node wfItem) {
        NodeList list = wfItem.getRelatedNodes(PagesUtil.PAGE, WORKFLOWREL, DESTINATION);
        if (!list.isEmpty()) {
            return list.getNode(0);
        }
        return null;
    }

    
    public Node createFor(Node page, String remark) {
        Node wfItem = createFor(TYPE_PAGE, remark);
        RelationUtil.createRelation(wfItem, page, WORKFLOWREL);
        log.debug("Workflow " + wfItem.getNumber() + " created for page " + page.getNumber());
        return wfItem;
    }


    @Override
    public void finishWriting(Node node, String remark) {
        Node wfItem;
        Node page;
        if (PagesUtil.isPageType(node)) {
            wfItem = getWorkflowNode(node, TYPE_PAGE);
            page = node;
        }
        else {
            wfItem = node;
            page = getPageNode(node);
        }
        super.finishWriting(wfItem, page, remark);
    }
    
    @Override
    public void accept(Node node, String remark) {
        Node wfItem;
        Node page;
        if (PagesUtil.isPageType(node)) {
            wfItem = getWorkflowNode(node, TYPE_PAGE);
            page = node;
        }
        else {
            wfItem = node;
            page = getPageNode(node);
        }
        super.accept(wfItem, page, remark);
    }

    @Override
    public void reject(Node node, String remark) {
        if (PagesUtil.isPageType(node)) {
            if (hasWorkflow(node, TYPE_PAGE)) {
               Node wf = getWorkflowNode(node, TYPE_PAGE);
               if (isStatusPublished(wf)) {
                   Publish.remove(node);
               }
               rejectWorkflow(wf, remark);
            }
        }
        else {
            if (isStatusPublished(node)) {
                Node page = getPageNode(node);
                Publish.remove(page);
            }
            rejectWorkflow(node, remark); 
        }
    }

    @Override
    public void publish(Node node) throws WorkflowException {
        publish(node, null);
    }

    @Override
    public void publish(Node node, List<Integer> publishNumbers) throws WorkflowException {
        Node page;
        if (PagesUtil.isPageType(node)) {
            page = node;
        }
        else {
            page = getPageNode(node);
        }
        publish(page, false, TYPE_PAGE, publishNumbers);
    }

    
    public void complete(Node contentNode) {
        complete(contentNode, TYPE_PAGE);
    }

    @Override
    protected List getUsersWithRights(Node node, Role role) {
        return NavigationUtil.getUsersWithRights(node, role);
    }

    @Override
    public boolean isWorkflowElement(Node node) {
        return PagesUtil.isPageType(node);
    }

    @Override
    public boolean isAllowedToAccept(Node node) {
        Node user = SecurityUtil.getUserNode(cloud);
        return NavigationUtil.getRole(user, node).getRole().getId() >= Role.EDITOR.getId();
    }

    @Override
    public boolean isAllowedToPublish(Node node) {
        Node user = SecurityUtil.getUserNode(cloud);
        return NavigationUtil.getRole(user, node).getRole().getId() >= Role.CHIEFEDITOR.getId();
    }

    public boolean isWorkflowType(String type) {
        return PagesUtil.isPageType(cloud.getNodeManager(type));
    }

    public boolean hasWorkflow(Node node) {
        return hasWorkflow(node, TYPE_PAGE);
    }

    @Override
    protected void checkNode(Node node, List<Node> errors, List<Integer> publishNumbers) {
        List path = NavigationUtil.getPathToRoot(node);
        path.remove(path.size() - 1);
        for (Iterator iter = path.iterator(); iter.hasNext();) {
            Node pathElement = (Node) iter.next();
            if (!Publish.isPublished(pathElement)
                    && (publishNumbers == null || !publishNumbers.contains(pathElement.getNumber())) ) {
                errors.add(pathElement);
            }
        }
        
        Cloud cloud = node.getCloud();
        NodeManager parameterManager = cloud.getNodeManager(PortletUtil.NODEPARAMETER);
        NodeManager portletManager = cloud.getNodeManager(PortletUtil.PORTLET);
        NodeManager pageManager = cloud.getNodeManager(PagesUtil.PAGE);

        Query query = cloud.createQuery();
        Step pageStep = query.addStep(pageManager);
        RelationStep step2 = query.addRelationStep(portletManager, PortletUtil.PORTLETREL, DESTINATION);
        RelationStep step4 = query.addRelationStep(parameterManager, PortletUtil.PARAMETERREL, DESTINATION);
        Step parameterStep = step4.getNext();

        query.addField(parameterStep, parameterManager.getField(PortletUtil.VALUE_FIELD));
        SearchUtil.addEqualConstraint(query, pageManager.getField("number"), Integer.valueOf(node.getNumber()));
        
        NodeList nodes = cloud.getList(query);
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            Node queryNode = (Node) iter.next();
            int qNumber = queryNode.getIntValue(PortletUtil.NODEPARAMETER + "." + PortletUtil.VALUE_FIELD);
            if (cloud.hasNode(qNumber)) {
                Node qNode = cloud.getNode(qNumber);
                if (!PagesUtil.isPageType(qNode)) {
                    if (Workflow.hasWorkflow(qNode)) {
                        if (!Workflow.mayPublish(qNode)) {
                            errors.add(qNode);
                        }
                    }
                }
            }
        }
    }

    @Override
    public UserRole getUserRole(Node node) {
        return NavigationUtil.getRole(node.getCloud(), node, false);
    }

}
