package com.finalist.newsletter.cao.impl;

import java.util.*;

import net.sf.mmapps.commons.beans.MMBaseNodeMapper;

import org.apache.commons.lang.time.DateFormatUtils;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import com.finalist.cmsc.navigation.NavigationUtil;
import com.finalist.newsletter.cao.NewsletterPublicationCAO;
import com.finalist.newsletter.domain.*;
import com.finalist.newsletter.util.NewsletterUtil;
import com.finalist.newsletter.util.POConvertUtils;



public class NewsletterPublicationCAOImpl implements NewsletterPublicationCAO {

   private static Logger log = Logging.getLoggerInstance(NewsletterPublicationCAOImpl.class.getName());

   Cloud cloud;

   public void setCloud(Cloud cloud) {
      this.cloud = cloud;
   }

   public List<Integer> getIntimePublicationIds() {

      NodeQuery query = cloud.createNodeQuery();

      NodeManager pubManager = cloud.getNodeManager("newsletterpublication");
      Step theStep = query.addStep(pubManager);
      query.setNodeStep(theStep);

      Field field = pubManager.getField("status");
      Constraint titleConstraint = SearchUtil.createEqualConstraint(query, field, Publication.STATUS.READY.toString());
      SearchUtil.addConstraint(query, titleConstraint);

      List<Node> pubNodes = query.getList();

      List<Integer> inTimePUblicationIds = new ArrayList<Integer>();
      for (Node node : pubNodes) {
         inTimePUblicationIds.add(node.getNumber());
      }

      return inTimePUblicationIds;
   }

   public void setStatus(int publicationId, Publication.STATUS status) {
      Node publicationNode = cloud.getNode(publicationId);
      publicationNode.setStringValue("status", status.toString());
      publicationNode.commit();
   }

   public Publication getPublication(int number) {
      Node newsletterPublicationNode = cloud.getNode(number);

      List<Node> relatedNewsletters = newsletterPublicationNode.getRelatedNodes("newsletter");
      log.debug("Get " + relatedNewsletters.size() + " related newsletter");

      Publication pub = new Publication();
      pub.setId(newsletterPublicationNode.getNumber());
      pub.setStatus(Publication.STATUS.valueOf(newsletterPublicationNode.getStringValue("status")));
      pub.setUrl(getPublicationURL(number));
      Newsletter newsletter = new Newsletter();

      Node node = relatedNewsletters.get(0);
      new POConvertUtils<Newsletter>().convert(newsletter, node);
      newsletter.setSendempty(node.getBooleanValue("sendempty"));
      newsletter.setTxtempty(node.getStringValue("txtempty"));
      pub.setNewsletter(newsletter);

      return pub;
   }

   public Node getPublicationNode(int number) {
      return cloud.getNode(number);
   }

   public String getPublicationURL(int publciationId) {

      Node publicationNode = cloud.getNode(publciationId);
      String hostUrl = NewsletterUtil.getServerURL();
      String newsletterPath = getNewsletterPath(publicationNode);
      return "".concat(hostUrl).concat(newsletterPath);
   }

   public int getNewsletterId(int publicationId) {
      Node newsletterPublicationNode = cloud.getNode(publicationId);
      NodeList relatedNewsletters = newsletterPublicationNode.getRelatedNodes("newsletter");

      log.debug("Get " + relatedNewsletters.size() + " related newsletter");

      return relatedNewsletters.getNode(0).getNumber();
   }

   public List<Publication> getAllPublications() {
      NodeQuery query = cloud.createNodeQuery();
      Step step = query.addStep(cloud.getNodeManager("newsletterpublication"));
      query.setNodeStep(step);
      NodeList list = query.getList();
      return MMBaseNodeMapper.convertList(list, Publication.class);
   }

   public List<Publication> getPublicationsByNewsletter(int id, Publication.STATUS status) {
       Node newsletterNode = cloud.getNode(id);
       List<Node> publicationNodes = newsletterNode.getRelatedNodes("newsletterpublication");

       Set<Publication> publications = new HashSet<Publication>();

       for(Node publicationNode:publicationNodes){
          if(null==status||status.toString().equals(publicationNode.getStringValue("status"))){
             publications.add(convertFromNode(publicationNode));
          }
       }

      log.debug(String.format("Get %s publications of newsletter %s in %s status",publications.size(),id,status));
      return new ArrayList<Publication>(publications);
    }

   private Publication convertFromNode(Node node){
      return MMBaseNodeMapper.copyNode(node, Publication.class);
   }


   protected String getNewsletterPath(Node newsletterPublicationNode) {
      return NavigationUtil.getPathToRootString(newsletterPublicationNode, true);
   }


   public Set<Term> getTermsByPublication(int publicationId) {
      Node newsletterPublicationNode = cloud.getNode(publicationId);
      NodeList relatedNewsletters = newsletterPublicationNode.getRelatedNodes("newsletter");
      NodeList terms = relatedNewsletters.getNode(0).getRelatedNodes("term");

      Iterator<Node> termsIt = terms.iterator();
      Set<Term> termSet = new HashSet<Term>();
      for (int i = 0; i < terms.size(); i++) {
         Node termNode = termsIt.next();
         Term term = MMBaseNodeMapper.copyNode(termNode, Term.class);
         termSet.add(term);
      }
      return termSet;
   }

   public void renamePublicationTitle(int publicationId) {

      String now = DateFormatUtils.format(new Date(), "dd-MM-yyyy hh:mm");
      Node publicationNode = cloud.getNode(publicationId);
      String oldTitle =  publicationNode.getStringValue("title");
      String newTile = oldTitle;
      String dateTime = "";
      if(oldTitle.length() > 18) {
         dateTime = oldTitle.substring(oldTitle.length()-16);
      }
      if(dateTime.indexOf("-") > 0 && dateTime.indexOf(":") > 0){
         newTile = oldTitle.substring(0,oldTitle.length()-18);
      }

      publicationNode.setStringValue("title", newTile+"  "+now);
      publicationNode.commit();

   }

//   public Set<Publication> getPublicationsByNewsletterAndPeriod(int newsletterId, String title, String subject, Date startDate, Date endDate, int pagesize, int offset){
//	   NodeManager manager = cloud.getNodeManager("newsletterpublication");
//	   Node newsletterNode = cloud.getNode(newsletterId);
//
//	   NodeQuery nodeQuery = SearchUtil.createRelatedNodeListQuery(newsletterNode, "newsletterpublication", "related");
//	   Step step = nodeQuery.getNodeStep();
//
//	   StepField fieldSubject = nodeQuery.addField(step, manager.getField("subject"));
//	   StepField fieldTitle = nodeQuery.addField(step, manager.getField("title"));
//	   StepField fieldDate = nodeQuery.addField(step, manager.getField("creationdate"));
//
//	   BasicFieldValueConstraint constraintTitle = new BasicFieldValueConstraint(fieldTitle, "%" + title + "%");
//	   constraintTitle.setOperator(FieldCompareConstraint.LIKE);
//	   BasicFieldValueConstraint constraintSubject = new BasicFieldValueConstraint(fieldSubject, "%" + subject + "%");
//	   constraintSubject.setOperator(FieldCompareConstraint.LIKE);
//
//	   BasicCompositeConstraint constraints = new BasicCompositeConstraint(2);
//	   if (startDate != null){
//		   BasicFieldValueBetweenConstraint constraintDate= new BasicFieldValueBetweenConstraint(fieldDate, startDate, endDate);
//		   constraints.addChild(constraintDate);
//	   }
//	   else{
//		   	BasicFieldValueConstraint constraintDate =new BasicFieldValueConstraint(fieldDate, endDate);
//	   		constraintDate.setOperator(FieldCompareConstraint.LESS);
//	   		constraints.addChild(constraintDate);
//	   }
//
//	   constraints.addChild(constraintTitle);
//	   constraints.addChild(constraintSubject);
//	   nodeQuery.setOffset(offset);
//	   nodeQuery.setMaxNumber(pagesize);
//	   nodeQuery.setConstraint(constraints);
//	   List<Node> list = nodeQuery.getList();
//	   Set<Publication> publications = convertPublicationsToMap(list);
//	   return publications;
//   }
   private List<Publication> convertPublicationsToMap(List<Node> publicationNodes) {
	   List<Publication> publications = new ArrayList<Publication>();
		// private Set<Publication> convertPublicationsToMap(List<Node>
		// publicationNodes) {
		// Set<Publication> publications = new HashSet<Publication>();
		// >>>>>>> 1.15
	   for (Node publicationNode : publicationNodes) {
		   publications.add(convertFromNode(publicationNode));
	   }
	   return publications;
	}

	public int getPublicationCountForEdit(int newsletterId, String title, String subject, Date startDate, Date endDate){
		NodeManager manager = cloud.getNodeManager("newsletterpublication");
		   Node newsletterNode = cloud.getNode(newsletterId);

		   NodeQuery nodeQuery = SearchUtil.createRelatedNodeListQuery(newsletterNode, "newsletterpublication", "related");
		   Step step = nodeQuery.getNodeStep();

		   StepField fieldSubject = nodeQuery.addField(step, manager.getField("subject"));
		   StepField fieldTitle = nodeQuery.addField(step, manager.getField("title"));
		   StepField fieldDate = nodeQuery.addField(step, manager.getField("creationdate"));

		   BasicFieldValueConstraint constraintTitle = new BasicFieldValueConstraint(fieldTitle, "%" + title + "%");
		   constraintTitle.setOperator(FieldCompareConstraint.LIKE);
		   BasicFieldValueConstraint constraintSubject = new BasicFieldValueConstraint(fieldSubject, "%" + subject + "%");
		   constraintSubject.setOperator(FieldCompareConstraint.LIKE);

		   BasicCompositeConstraint constraints = new BasicCompositeConstraint(2);
		   if (startDate != null){
			   BasicFieldValueBetweenConstraint constraintDate= new BasicFieldValueBetweenConstraint(fieldDate, startDate, endDate);
			   constraints.addChild(constraintDate);
		} else {
			   	BasicFieldValueConstraint constraintDate =new BasicFieldValueConstraint(fieldDate, endDate);
		   		constraintDate.setOperator(FieldCompareConstraint.LESS);
		   		constraints.addChild(constraintDate);
		   }

		   constraints.addChild(constraintTitle);
		   constraints.addChild(constraintSubject);
		   nodeQuery.setConstraint(constraints);
		   return nodeQuery.getList().size();
	}

	public List<Publication> getPublicationsByNewsletterAndPeriod(int newsletterId, String title, String subject, Date startDate, Date endDate,
			int pagesize, int offset, String order, String direction) {
		NodeManager manager = cloud.getNodeManager("newsletterpublication");
		Node newsletterNode = cloud.getNode(newsletterId);

		NodeQuery nodeQuery = SearchUtil.createRelatedNodeListQuery(newsletterNode, "newsletterpublication", "related");
		Step step = nodeQuery.getNodeStep();

		StepField fieldSubject = nodeQuery.addField(step, manager.getField("subject"));
		StepField fieldTitle = nodeQuery.addField(step, manager.getField("title"));
		StepField fieldDate = nodeQuery.addField(step, manager.getField("creationdate"));

		BasicFieldValueConstraint constraintTitle = new BasicFieldValueConstraint(fieldTitle, "%" + title + "%");
		constraintTitle.setOperator(FieldCompareConstraint.LIKE);
		BasicFieldValueConstraint constraintSubject = new BasicFieldValueConstraint(fieldSubject, "%" + subject + "%");
		constraintSubject.setOperator(FieldCompareConstraint.LIKE);

		BasicCompositeConstraint constraints = new BasicCompositeConstraint(2);
		if (startDate != null) {
			BasicFieldValueBetweenConstraint constraintDate = new BasicFieldValueBetweenConstraint(fieldDate, startDate, endDate);
			constraints.addChild(constraintDate);
		} else {
			BasicFieldValueConstraint constraintDate = new BasicFieldValueConstraint(fieldDate, endDate);
			constraintDate.setOperator(FieldCompareConstraint.LESS);
			constraints.addChild(constraintDate);
		}

		constraints.addChild(constraintTitle);
		constraints.addChild(constraintSubject);
		nodeQuery.setOffset(offset);
		nodeQuery.setMaxNumber(pagesize);
		nodeQuery.setConstraint(constraints);
		String orderBy = "number";
		if (!"number".equals(order.trim()))
			orderBy = order.trim();
		Queries.addSortOrders(nodeQuery, orderBy, direction);
		List<Node> list = nodeQuery.getList();
		List<Publication> publications = convertPublicationsToMap(list);
		return publications;
	}

	
}
