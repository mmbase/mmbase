package com.finalist.portlets.guestbook.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.mmapps.modules.cloudprovider.CloudProvider;
import net.sf.mmapps.modules.cloudprovider.CloudProviderFactory;

import org.mmbase.remotepublishing.CloudManager;
import org.mmbase.storage.search.CompositeConstraint;
import org.mmbase.storage.search.Constraint;
import org.mmbase.storage.search.FieldValueConstraint;
import org.mmbase.storage.search.SortOrder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Field;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeIterator;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.NodeManager;
import org.mmbase.bridge.NodeQuery;
import org.mmbase.bridge.util.Queries;
import com.finalist.cmsc.mmbase.PropertiesUtil;
import com.finalist.cmsc.resources.forms.QueryStringComposer;
import com.finalist.cmsc.struts.MMBaseAction;

/**
 * 
 * 
 */
public class SearchGuestBookAction extends MMBaseAction {

   private static Log log = LogFactory.getLog(SearchGuestBookAction.class);

   private static final String REPOSITORY_SEARCH_RESULTS_PER_PAGE = "repository.search.results.per.page";


   /**
    * Puts the results in a list of maps, so the searching is done inside the
    * action because most of the mm-tags do not work with a remote cloud. The
    * result are put on the request and displayed by the JSP view. The searching
    * might be a bit complicated but I don't have a clue how mmbase works.
    */
   @Override
   public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
         HttpServletResponse response, Cloud cld) throws Exception {

      List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
      QueryStringComposer queryStringComposer = new QueryStringComposer();
      GuestBookForm guestBookForm = (GuestBookForm) form;
      Cloud cloud = getCloud(guestBookForm.isRemote());
      int count = 0;
      int offset = 0;
      if (guestBookForm.getOffset() != null && guestBookForm.getOffset().matches("\\d+")) {
         offset = Integer.parseInt(guestBookForm.getOffset());
      }
      String resultsPerPage = PropertiesUtil.getProperty(REPOSITORY_SEARCH_RESULTS_PER_PAGE);
      int maxNumber = 50;
      if (resultsPerPage != null && resultsPerPage.matches("\\d+")) {
         maxNumber = Integer.parseInt(resultsPerPage);
      }
      int queryOffset = maxNumber * offset;
      String direction = getParameterAsString(request, "direction", "up");

      String action = request.getParameter("action");
      if ("guestmessages".equals(action)) {
         String name = request.getParameter("name");
         String email = request.getParameter("email");
         String title = request.getParameter("title");
         String body = request.getParameter("body");
         NodeQuery query = createMessagesQuery(cloud, direction, queryOffset, maxNumber, name, email, title, body);
         NodeList nodes = query.getNodeManager().getList(query);
         count = Queries.count(query);
         rows = populateMessageRows(nodes);
      }
      else {
         log.error("Unknown action: " + action);
         return mapping.findForward("error");
      }

      queryStringComposer.addParameter("action", action);
      if (guestBookForm.isRemote()) {
         queryStringComposer.addParameter("remote", "on");
      }
      queryStringComposer.addParameter("offset", String.valueOf(offset));

      request.setAttribute("offset", new Integer(offset));
      request.setAttribute("rows", rows);
      request.setAttribute("resultCount", count);
      request.setAttribute("geturl", queryStringComposer.getQueryString());

      return mapping.findForward(action);
   }


   @Override
   public String getRequiredRankStr() {
      return null;
   }


   private List<Map<String, Object>> populateMessageRows(NodeList nodes) {
      List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
      for (NodeIterator iter = nodes.nodeIterator(); iter.hasNext();) {
         Node node = iter.nextNode();
         Map<String, Object> columns = new HashMap<String, Object>();
         rows.add(columns);
         columns.put("number", node.getNumber());
         columns.put("name", node.getStringValue("name"));
         columns.put("email", node.getStringValue("email"));
         columns.put("title", node.getStringValue("title"));
         columns.put("body", node.getStringValue("body"));
      }
      return rows;
   }


   private NodeQuery createMessagesQuery(Cloud cloud, String direction, int offset, int maxNumber, String name,
         String email, String title, String body) {
      NodeManager manager = cloud.getNodeManager("guestmessage");
      NodeQuery query = manager.createQuery();
      if (!StringUtils.isBlank(name)) {
         addConstraint(query, manager, "name", name);
      }
      if (!StringUtils.isBlank(email)) {
         addConstraint(query, manager, "email", email);
      }
      if (!StringUtils.isBlank(title)) {
         addConstraint(query, manager, "title", title);
      }
      if (!StringUtils.isBlank(body)) {
         addConstraint(query, manager, "body", body);
      }
      query.addSortOrder(query.getStepField(manager.getField("creationdate")), SortOrder.ORDER_DESCENDING);
      query.setOffset(offset);
      query.setMaxNumber(maxNumber);
      log.debug("query: " + query.toSql());
      return query;
   }


   private Cloud getCloud(boolean isRemote) {
      CloudProvider cloudProvider = CloudProviderFactory.getCloudProvider();
      Cloud cloud = cloudProvider.getCloud();
      log.debug("Using remote cloud?: " + isRemote);
      if (isRemote) {
         return CloudManager.getCloud(cloud, "live.server");
      }
      return cloud;
   }


   private String getParameterAsString(HttpServletRequest request, String name, String defaultValue) {
      String value = request.getParameter(name);
      if (StringUtils.isBlank(value)) {
         return defaultValue;
      }
      return value;
   }


   private void addConstraint(NodeQuery query, NodeManager manager, String fieldName, String value) {
      Field field = manager.getField(fieldName);
      FieldValueConstraint constraint = query.createConstraint(query.getStepField(field), 7, "%" + value + "%");
      addConstraint(query, constraint);
      query.setCaseSensitive(constraint, false);
   }


   private void addConstraint(NodeQuery query, Constraint constraint) {
      if (query.getConstraint() == null) {
         query.setConstraint(constraint);
      }
      else {
         CompositeConstraint newc = query.createConstraint(query.getConstraint(), 2, constraint);
         query.setConstraint(newc);
      }
   }

}
