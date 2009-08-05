/*
 * 
 * This software is OSI Certified Open Source Software. OSI Certified is a certification mark of the Open Source
 * Initiative.
 * 
 * The license (Mozilla version 1.0) can be read at the MMBase site. See http://www.MMBase.org/license
 */
package com.finalist.cmsc.workflow.forms;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.BasicStep;

import com.finalist.cmsc.services.workflow.*;
import com.finalist.cmsc.struts.MMBaseFormlessAction;
import com.finalist.cmsc.workflow.RepositoryWorkflow;
import com.finalist.cmsc.workflow.WorkflowManager;

public abstract class WorkflowAction extends MMBaseFormlessAction {

   protected static final String REMARK_UNCHANGED = "[unchanged-item]";

   public ActionForward execute(ActionMapping mapping, HttpServletRequest request, Cloud cloud) throws Exception {
      String actionValueStr = request.getParameter("actionvalue");

      String remark = request.getParameter("remark");
      if (remark != null && remark.equals(REMARK_UNCHANGED)) {
         remark = null;
      }
      if (StringUtils.isNotEmpty(actionValueStr)) {
         List<Node> nodes = new ArrayList<Node>();
         Enumeration<String> parameters = request.getParameterNames();
         while (parameters.hasMoreElements()) {
            String name = parameters.nextElement();
            if (name.startsWith("check_")) {
               int lastUScore = name.lastIndexOf("_");
               int number = Integer.parseInt(name.substring(lastUScore + 1));
               if (cloud.hasNode(number)) {
                  Node n = cloud.getNode(number);
                  if(n != null) {
                     nodes.add(n);
                  }
               }
            }
         }
         List<Node> workflowErrors = WorkflowUtil.performWorkflowAction(actionValueStr, nodes, remark);

         if (workflowErrors != null && workflowErrors.size() > 0) {
            String url = mapping.getPath() + "?status=" + request.getParameter("status");
            request.setAttribute("errors", workflowErrors);
            request.setAttribute("returnUrl", url);
            request.setAttribute("contentNodeList", nodes);
            // return new ActionForward(url);
         }
      }

      String orderby = request.getParameter("orderby");
      if (StringUtils.isEmpty(orderby)) {
         orderby = "contenttype";
      }
      request.setAttribute("orderby", orderby);

      String laststatus = request.getParameter("laststatus");
      if (StringUtils.isEmpty(laststatus) || laststatus == null) {
         request.setAttribute("laststatus", true);
      } else {
         request.setAttribute("laststatus", laststatus.equals("true") ? false : true);
      }

      String status = Workflow.STATUS_DRAFT;
      String statusStr = request.getParameter("status");
      if (StringUtils.isNotEmpty(statusStr)) {
         status = statusStr;
      }

      WorkflowStatusInfo statusInfo = Workflow.getStatusInfo(cloud);
      request.setAttribute("statusInfo", statusInfo);
      String type = getWorkflowType();
      addToRequest(request, "workflowType", type);
      String nodetype = null;
      String nodetypeGUI = null;
      String nodetypeStr = request.getParameter("workflowNodetype");
      String fromIndex = request.getParameter("fromIndex");
      if (fromIndex == null) {
         fromIndex = "no";
      }
      if (StringUtils.isEmpty(nodetypeStr) && fromIndex.equalsIgnoreCase("yes")) {
         nodetypeStr = (String) request.getSession().getAttribute("workflowNodetype");
      }
      if (StringUtils.isNotEmpty(nodetypeStr)) {
         nodetype = nodetypeStr;
         nodetypeGUI = cloud.getNodeManager(nodetype).getGUIName();
         addToRequest(request, "workflowNodetype", nodetype);
         addToRequest(request, "workflowNodetypeGUI", nodetypeGUI);
      }

      if (!RepositoryWorkflow.TYPE_ALLCONTENT.equals(type)) {
         NodeQuery listQuery = WorkflowManager.createListQuery(cloud);
         Queries.addConstraint(listQuery, WorkflowManager.getStatusConstraint(listQuery, status));
         if (!Workflow.isAcceptedStepEnabled() && Workflow.STATUS_FINISHED.equals(status)) {
            SearchUtil.addConstraint(listQuery, WorkflowManager
                  .getStatusConstraint(listQuery, Workflow.STATUS_APPROVED), CompositeConstraint.LOGICAL_OR);
         }
         Queries.addConstraint(listQuery, WorkflowManager.getTypeConstraint(listQuery, type));
         if (!StringUtils.isBlank(nodetype)) {
            WorkflowManager.addNodetypeConstraint(cloud, listQuery, nodetype);
         }
         NodeQuery wfQuery = createDetailQuery(cloud, orderby, (laststatus == null) ? false : (laststatus
               .equals("true")));
         addWorkflowListToRequest(request, cloud, wfQuery, listQuery, "results");
      } else {
         addAllcontentListToRequest(request, cloud, orderby,status, laststatus);
      }

      request.setAttribute("acceptedEnabled", Workflow.isAcceptedStepEnabled());
      HttpSession session = request.getSession();
      session.setAttribute("workflowType", type);
      if (StringUtils.isNotEmpty(nodetype)) {
         session.setAttribute("workflowNodetype", nodetype);
         session.setAttribute("workflowNodetypeGUI", nodetypeGUI);
      } else {
         session.removeAttribute("workflowNodetype");
         session.removeAttribute("workflowNodetypeGUI");
      }
      session.setAttribute("workflow.status", status);
      Map<String, Integer> treeStatus = (Map<String, Integer>) session.getAttribute("workflowTreeStatus");
      if (treeStatus == null) {
         treeStatus = new HashMap<String, Integer>();
         treeStatus.put("allcontent", 1);
         treeStatus.put("content", 1);
         treeStatus.put("asset", 1);
         session.setAttribute("workflowTreeStatus", treeStatus);
      }
      return mapping.findForward(SUCCESS);
   }

   protected abstract void addAllcontentListToRequest(HttpServletRequest request, Cloud cloud,String orderby, String status,
         String laststatus);

   protected abstract String getWorkflowType();

   protected abstract NodeQuery createDetailQuery(Cloud cloud, String orderby, boolean AorD);


   private void addWorkflowListToRequest(HttpServletRequest request, Cloud cloud, NodeQuery detailQuery,
         NodeQuery listQuery, String attributeName) {
      NodeList ceWorkflowNumbers = cloud.getList(listQuery);
      if (!ceWorkflowNumbers.isEmpty()) {
         NodeQuery numbersQuery = createDetailsWithNumbersQuery(detailQuery, ceWorkflowNumbers);
         NodeList dataList = cloud.getList(numbersQuery);
         request.setAttribute(attributeName, dataList);
      }
   }

   protected NodeQuery createDetailsWithNumbersQuery(NodeQuery wfQuery, NodeList workflowNumbers) {
      NodeQuery detailQuery = (NodeQuery) wfQuery.clone();

      BasicStep wfStep = (BasicStep) detailQuery.getStep(WorkflowManager.WORKFLOW_MANAGER_NAME);
      // Retrieve the data of the content element workflow items.
      Iterator<Node> wfIterator = workflowNumbers.iterator();
      while (wfIterator.hasNext()) {
         Node node = wfIterator.next();
         int workflowNumber = node.getIntValue(WorkflowManager.WORKFLOW_MANAGER_NAME + ".number");
         wfStep.addNode(workflowNumber);
      }
      return detailQuery;
   }

   protected void addOrderBy(NodeManager manager, NodeQuery query, String fieldname, boolean aord) {
      Step step = query.getStep(manager.getName());
      StepField sf = query.createStepField(step, manager.getField(fieldname));
      if (aord) {
         /*
          * System.out.println("WorkflowAction : orderby-- " + fieldname + " descending");
          */
         query.addSortOrder(sf, SortOrder.ORDER_DESCENDING);
      } else {
         /*
          * System.out.println("WorkflowAction : orderby-- " + fieldname + " ascending");
          */
         query.addSortOrder(sf, SortOrder.ORDER_ASCENDING);
      }
   }

}
