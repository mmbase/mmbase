package com.finalist.cmsc.repository.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.mmapps.modules.cloudprovider.CloudProvider;
import net.sf.mmapps.modules.cloudprovider.CloudProviderFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Field;
import org.mmbase.bridge.FieldIterator;
import org.mmbase.bridge.FieldList;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.NodeManager;
import org.mmbase.bridge.NodeQuery;
import org.mmbase.bridge.util.Queries;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.storage.search.Constraint;
import org.mmbase.storage.search.Step;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import com.finalist.cmsc.mmbase.PropertiesUtil;
import com.finalist.cmsc.repository.AssetElementUtil;
import com.finalist.cmsc.repository.RepositoryUtil;
import com.finalist.cmsc.resources.forms.QueryStringComposer;
import com.finalist.cmsc.services.publish.Publish;
import com.finalist.cmsc.services.workflow.Workflow;
import com.finalist.cmsc.struts.PagerAction;

public class AssetSearchAction extends PagerAction {

   public static final String GETURL = "geturl";

   public static final String PERSONAL = "personal";
   public static final String MODE = "mode";
   public static final String AUTHOR = "author";
   public static final String OBJECTID = "objectid";
   public static final String PARENTCHANNEL = "parentchannel";
   public static final String ASSETTYPES = "assettypes";

   public static final String REPOSITORY_SEARCH_RESULTS_PER_PAGE = "repository.search.results.per.page";

   /**
    * MMbase logging system
    */
   private static final Logger log = Logging.getLoggerInstance(AssetSearchAction.class.getName());

   @Override
   public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
         HttpServletResponse response, Cloud cloud) throws Exception {

      log.debug("Starting the search:");

      // Initialize
      AssetSearchForm searchForm = (AssetSearchForm) form;

      String deleteAssetRequest = request.getParameter("deleteAssetRequest");

      if (StringUtils.isNotEmpty(deleteAssetRequest)) {
         if (deleteAssetRequest.startsWith("massDelete:")) {
            massDeleteAsset(deleteAssetRequest.substring(11));
         } else {
            deleteAsset(deleteAssetRequest);
         }

         // add a flag to let search result page refresh the channels frame,
         // so that the number of item in recyclebin can update
         request.setAttribute("refreshChannels", "refreshChannels");
      }

      // First prepare the typeList, we'll need this one anyway:
      List<LabelValueBean> typesList = new ArrayList<LabelValueBean>();

      List<NodeManager> types = AssetElementUtil.getAssetTypes(cloud);
      List<String> hiddenTypes = AssetElementUtil.getHiddenAssetTypes();
      for (NodeManager manager : types) {
         String name = manager.getName();
         if (!hiddenTypes.contains(name)) {
            LabelValueBean bean = new LabelValueBean(manager.getGUIName(), name);
            typesList.add(bean);
         }
      }
      addToRequest(request, "typesList", typesList);

      // Switching tab, no searching.
      if ("false".equalsIgnoreCase(searchForm.getSearch())) {
         return mapping.getInputForward();
      }

      NodeManager nodeManager = cloud.getNodeManager(searchForm.getAssettypes());
      QueryStringComposer queryStringComposer = new QueryStringComposer();
      if (StringUtils.isNotEmpty(request.getParameter(MODE))) {
         queryStringComposer.addParameter(MODE, request.getParameter(MODE));
      }
      NodeQuery query = cloud.createNodeQuery();

      // First we add the assettype parameter
      queryStringComposer.addParameter(ASSETTYPES, searchForm.getAssettypes());

      // First add the proper step to the query.
      Step theStep = null;
      if (StringUtils.isNotEmpty(searchForm.getParentchannel())) {
         Step step = query.addStep(cloud.getNodeManager(RepositoryUtil.CONTENTCHANNEL));
         query.addNode(step, cloud.getNode(searchForm.getParentchannel()));
         theStep = query.addRelationStep(nodeManager, RepositoryUtil.CREATIONREL, "SOURCE").getNext();
         query.setNodeStep(theStep);
         queryStringComposer.addParameter(PARENTCHANNEL, searchForm.getParentchannel());
      } else {
         theStep = query.addStep(nodeManager);
         query.setNodeStep(theStep);
      }

      // Order the result by:
      String order = searchForm.getOrder();

      // set default order field
      if (StringUtils.isEmpty(order)) {
         if (nodeManager.hasField("title")) {
            order = "title";
         }
         if (nodeManager.hasField("name")) {
            order = "name";
         }
      }
      if (StringUtils.isNotEmpty(order)) {
         queryStringComposer.addParameter(ORDER, searchForm.getOrder());
         queryStringComposer.addParameter(DIRECTION, "" + searchForm.getDirection());
         query.addSortOrder(query.getStepField(nodeManager.getField(order)), searchForm.getDirection());
      }

      query.setDistinct(true);

      // Set some date constraints.
      queryStringComposer.addParameter(AssetElementUtil.CREATIONDATE_FIELD, "" + searchForm.getCreationdate());
      SearchUtil
            .addDayConstraint(query, nodeManager, AssetElementUtil.CREATIONDATE_FIELD, searchForm.getCreationdate());
      queryStringComposer.addParameter(AssetElementUtil.PUBLISHDATE_FIELD, "" + searchForm.getPublishdate());
      SearchUtil.addDayConstraint(query, nodeManager, AssetElementUtil.PUBLISHDATE_FIELD, searchForm.getPublishdate());
      queryStringComposer.addParameter(AssetElementUtil.EXPIREDATE_FIELD, "" + searchForm.getExpiredate());
      SearchUtil.addDayConstraint(query, nodeManager, AssetElementUtil.EXPIREDATE_FIELD, searchForm.getExpiredate());
      queryStringComposer.addParameter(AssetElementUtil.LASTMODIFIEDDATE_FIELD, "" + searchForm.getLastmodifieddate());
      SearchUtil.addDayConstraint(query, nodeManager, AssetElementUtil.LASTMODIFIEDDATE_FIELD, searchForm
            .getLastmodifieddate());

      // Perhaps we have some more constraints if the nodetype was specified (=>
      // not
      // assetelement).
      if (!AssetElementUtil.ASSETELEMENT.equalsIgnoreCase(nodeManager.getName())) {
         FieldList fields = nodeManager.getFields();
         FieldIterator fieldIterator = fields.fieldIterator();

         while (fieldIterator.hasNext()) {
            Field field = fieldIterator.nextField();
            String paramName = nodeManager.getName() + "." + field.getName();
            String paramValue = request.getParameter(paramName);
            if (StringUtils.isNotEmpty(paramValue)) {
               SearchUtil.addLikeConstraint(query, field, paramValue.trim());
            }
            queryStringComposer.addParameter(paramName, paramValue);
         }
      }

      // Add the title constraint:
      if (StringUtils.isNotEmpty(searchForm.getTitle())) {

         queryStringComposer.addParameter(AssetElementUtil.TITLE_FIELD, searchForm.getTitle().trim());
         Field field = nodeManager.getField(AssetElementUtil.TITLE_FIELD);
         Constraint titleConstraint = SearchUtil.createLikeConstraint(query, field, searchForm.getTitle().trim());
         SearchUtil.addConstraint(query, titleConstraint);
      }

      // Set the objectid constraint
      if (StringUtils.isNotEmpty(searchForm.getObjectid())) {
         String stringObjectId = searchForm.getObjectid().trim();
         Integer objectId = null;
         if (stringObjectId.matches("^\\d+$")) {
            objectId = Integer.valueOf(stringObjectId);
         } else {
            if (cloud.hasNode(stringObjectId)) {
               objectId = Integer.valueOf(cloud.getNode(stringObjectId).getNumber());
            } else {
               objectId = Integer.valueOf(-1);
            }
         }
         SearchUtil.addEqualConstraint(query, nodeManager, AssetElementUtil.NUMBER_FIELD, objectId);
         queryStringComposer.addParameter(OBJECTID, stringObjectId);
      }

      // Add the user personal:
      if (StringUtils.isNotEmpty(searchForm.getPersonal())) {

         String useraccount = cloud.getUser().getIdentifier();
         if (AssetElementUtil.LASTMODIFIER_FIELD.equals(searchForm.getPersonal())) {
            SearchUtil.addEqualConstraint(query, nodeManager, AssetElementUtil.LASTMODIFIER_FIELD, useraccount);
         }
         if (AUTHOR.equals(searchForm.getPersonal())) {
            SearchUtil.addEqualConstraint(query, nodeManager, AssetElementUtil.CREATOR_FIELD, useraccount);
         }
         queryStringComposer.addParameter(PERSONAL, searchForm.getPersonal());
      }

      // Add the user
      if (StringUtils.isNotEmpty(searchForm.getUseraccount())) {
         String useraccount = searchForm.getUseraccount();
         SearchUtil.addEqualConstraint(query, nodeManager, AssetElementUtil.LASTMODIFIER_FIELD, useraccount);
      }

      // Set the maximum result size.
      String resultsPerPage = PropertiesUtil.getProperty(REPOSITORY_SEARCH_RESULTS_PER_PAGE);
      if (resultsPerPage == null || !resultsPerPage.matches("\\d+")) {
         query.setMaxNumber(25);
      } else {
         query.setMaxNumber(Integer.parseInt(resultsPerPage));
      }

      // Set the offset (used for paging).
      if (searchForm.getOffset() != null && searchForm.getOffset().matches("\\d+")) {
         query.setOffset(query.getMaxNumber() * Integer.parseInt(searchForm.getOffset()));
         queryStringComposer.addParameter(OFFSET, searchForm.getOffset());
      }

      log.debug("QUERY: " + query);

      int resultCount = Queries.count(query);
      NodeList results = cloud.getList(query);

      // Set everything on the request.
      searchForm.setResultCount(resultCount);
      searchForm.setResults(results);
      request.setAttribute(GETURL, queryStringComposer.getQueryString());

      return super.execute(mapping, form, request, response, cloud);
   }

   private void massDeleteAsset(String deleteAsset) {
      if (StringUtils.isNotBlank(deleteAsset)) {
         String[] deleteAssets = deleteAsset.split(",");
         for (String asset : deleteAssets) {
            deleteAsset(asset);
         }
      }
   }

   private void deleteAsset(String deleteAssetRequest) {
      StringTokenizer commandAndNumber = new StringTokenizer(deleteAssetRequest, ":");
      String command = commandAndNumber.nextToken();
      String nunmber = commandAndNumber.nextToken();

      if ("moveToRecyclebin".equals(command)) {
         moveAssetToRecyclebin(nunmber);
      }

      if ("permanentDelete".equals(command)) {
         deleteAssetPermanent(nunmber);
      }

   }

   private void deleteAssetPermanent(String objectnumber) {
      CloudProvider provider = CloudProviderFactory.getCloudProvider();
      Cloud cloud = provider.getCloud();

      Node objectNode = cloud.getNode(objectnumber);
      if (Workflow.hasWorkflow(objectNode)) {
         // at this time complete is the same as remove
         Workflow.complete(objectNode);
      }
      objectNode.delete(true);

   }

   private void moveAssetToRecyclebin(String nunmber) {
      CloudProvider provider = CloudProviderFactory.getCloudProvider();
      Cloud cloud = provider.getCloud();

      Node objectNode = cloud.getNode(nunmber);
      RepositoryUtil.removeCreationRelForContent(objectNode);
      RepositoryUtil.addAssetToChannel(objectNode, RepositoryUtil.getTrash(cloud));

      // unpublish and remove from workflow
      Publish.remove(objectNode);
      Publish.unpublish(objectNode);
      Workflow.remove(objectNode);
   }

}
