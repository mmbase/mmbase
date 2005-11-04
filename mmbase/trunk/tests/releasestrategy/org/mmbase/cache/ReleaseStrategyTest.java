/*
 * Created on 25-okt-2005
 *
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative.
 *
 * The license (Mozilla version 1.0) can be read at the MMBase site.
 * See http://www.MMBase.org/license
 */
package org.mmbase.cache;

import java.util.HashMap;
import java.util.Map;

import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.NodeManager;
import org.mmbase.bridge.NotFoundException;
import org.mmbase.bridge.Query;
import org.mmbase.bridge.Relation;
import org.mmbase.bridge.RelationManager;
import org.mmbase.bridge.util.Queries;
import org.mmbase.core.event.NodeEvent;
import org.mmbase.core.event.NodeEventHelper;
import org.mmbase.core.event.RelationEvent;
import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.storage.search.AggregatedField;
import org.mmbase.storage.search.Step;
import org.mmbase.storage.search.implementation.BasicFieldValueConstraint;
import org.mmbase.storage.search.implementation.BasicLegacyConstraint;
import org.mmbase.tests.BridgeTest;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Test class for {@link org.mmbase.cache.BetterStrategy}. 
 * TODO: the way the NodeEvent now works makes it core dependant. i can not do the tests
 *  without using the core. this dous not mean it won't work without the core (rmmci) but i think it 
 *  must be changed.
 * @author Ernst Bunders
 *
 */
public class ReleaseStrategyTest extends BridgeTest {

    static protected Cloud cloud = null;
    
    static protected RelationManager posrelManager; 
    static protected NodeManager relDefManager;
    static protected NodeManager typeRelManager;
    static protected NodeManager insRelManager; 
    static protected NodeManager newsManager; 
    static protected NodeManager urlsManager; 
    static protected NodeManager peopelManager; 
    static protected NodeList    createdNodes;
    
    static protected ReleaseStrategy strategy;
    
    protected Query twooStepQuery;
    protected Query oneStepQuery;


    private static final Logger log = Logging.getLoggerInstance(ReleaseStrategyTest.class);
    
    protected final static String TEST_RELATION_ROLE = "test";
    protected final static String NEWS_TITLE = "title";
    protected final static String URLS_NAME = "name";

    protected static final int POSREL_POS = 0;

    static protected Node newsNode;

    static protected Node urlsNode;

    static protected Relation posrelNode;
    
    public ReleaseStrategyTest(String name){
        super(name);
    }
    
       
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception {
        log.debug("method: setup()");
        if (cloud == null) {   
            log.debug("still there 1");
            startMMBase();
            log.debug("still there 2");
            cloud = getCloud();
            log.debug("still there 3");
            
            try{
                newsManager = cloud.getNodeManager("news");
                urlsManager = cloud.getNodeManager("urls");
                posrelManager = cloud.getRelationManager("posrel");
                relDefManager = cloud.getNodeManager("reldef");
                typeRelManager = cloud.getNodeManager("typerel");
                insRelManager  = cloud.getNodeManager("insrel");
                peopelManager = cloud.getNodeManager("people");
            }catch(NotFoundException e ){
                throw new Exception("Test cases cannot be performed because " + e.getMessage() + " Please arrange this in your cloud before running this TestCase.");
            }
            strategy = new BetterStrategy();
            createdNodes = cloud.createNodeList();
            
            
            //create an alternative typerel: people > posrel > urls
            Node typerel = typeRelManager.createNode();
            typerel.setNodeValue("snumber", peopelManager);
            typerel.setNodeValue("dnumber", urlsManager);
            typerel.setNodeValue("rnumber", posrelManager);
            typerel.commit();
            createdNodes.add(typerel);
            
            //create some basic nodes
            
            newsNode = newsManager.createNode();
            newsNode.setStringValue("title", NEWS_TITLE);
            newsNode.commit();
            createdNodes.add(newsNode);
            
            urlsNode = urlsManager.createNode();
            urlsNode.setStringValue("name", URLS_NAME);
            urlsNode.commit();
            createdNodes.add(urlsNode);
            
            posrelNode = newsNode.createRelation(urlsNode, posrelManager);
            posrelNode.setIntValue("pos", POSREL_POS);
            posrelNode.commit();
            createdNodes.add(posrelNode);
         
        }
    }
    
    /**
     * here go all the tests for checks that are done for every type of node event
     */
    public void testNodeEvent(){
        //a node event that has a type that is not part of the query path should not flush the query
        
        NodeEvent event = NodeEventHelper.createNodeEventInstance(newsNode, NodeEvent.EVENT_TYPE_NEW, null);
        Query q1 = Queries.createQuery(cloud, null, "people,posrel,urls", "urls.name", null, null, null, null, false);
        
        assertFalse("a node event should not flush a query if it's type is not in the path",
            strategy.evaluate(event, q1, null).shouldRelease());
    }
    
    
    
    
    
    /**
     * here go all the tests for checks that are done for every type of relation event
     */
    public void testRelaionEvent(){
        
        RelationEvent relEvent = NodeEventHelper.createRelationEventInstance(posrelNode, RelationEvent.EVENT_TYPE_NEW, null);
        
        //if the query has one step, a relaion event should not flush the query
        Query q1 = Queries.createQuery(cloud, null, "urls", "urls.name", null, null, null, null, false);
        
        assertFalse("relation event should not flush query with one step",
            strategy.evaluate(relEvent, q1, null).shouldRelease());
        
        //if either source, destination or role dous not match, a relation event should not flush the query
        Query q2 = Queries.createQuery(cloud, null, "people,posrel,urls", "urls.name", null, null, null, null, false);
        Query q3 = Queries.createQuery(cloud, null, "news,sorted,urls", "news.title", null, null, null, null, false);
        Query q4 = Queries.createQuery(cloud, null, "news,posrel,attachments", "news.title", null, null, null, null, false);
        
        assertFalse("relation event of new relation between nodes in multi step query (where source dous not match) should not be flushed",
            strategy.evaluate(relEvent, q2, null).shouldRelease());
        assertFalse("relation event of new relation between nodes in multi step query (where role dous not match) should not be flushed",
            strategy.evaluate(relEvent, q3, null).shouldRelease());
        assertFalse("relation event of new relation between nodes in multi step query (where destinantion dous not match) should not be flushed",
            strategy.evaluate(relEvent, q4, null).shouldRelease());
        
        //but if the role is not specified the query should be flushed
        Query q5 = Queries.createQuery(cloud, null, "news,urls", "urls.name", null, null, null, null, false);
        assertTrue("relation event of new relation between nodes in multi step query (where role is not specified and source and destination match) should be flushed",
            strategy.evaluate(relEvent, q5, null).shouldRelease());
        
        //unless source or destination do not match
        Query q6 = Queries.createQuery(cloud, null, "people,urls", "urls.name", null, null, null, null, false);
        Query q7 = Queries.createQuery(cloud, null, "news,attachments", "news.title", null, null, null, null, false);
        
        assertFalse("relation event of new relation between nodes in multi step query where is not specified (and source dous not match) should not be flushed",
            strategy.evaluate(relEvent, q6, null).shouldRelease());
        assertFalse("relation event of new relation between nodes in multi step query where is not specified (and destination dous not match) should not be flushed",
            strategy.evaluate(relEvent, q7, null).shouldRelease());
        
    }

    
    
    
    
    
    public void testNewNode(){
        log.debug("method: testMultiStepQueryNewNode()");
        Query q1 = Queries.createQuery(cloud, null, "news,posrel,urls", "news.title,urls.name",null, null, null, null, false);
        
        //a new node should not flush a multistep query frome the cache
        NodeEvent event = NodeEventHelper.createNodeEventInstance(newsNode, NodeEvent.EVENT_TYPE_NEW, null);
        assertFalse("node event of new node on multi step query should not release the query",
            strategy.evaluate(event, q1, null).shouldRelease());
        
    }
    
    
    public void testNewRelation(){
        log.debug("method: testMultiStepQueryNewRelation()");
        Query q1 = Queries.createQuery(cloud, null, "news,posrel,urls", "news.title,urls.name",null, null, null, null, false);
        
        //a new relation node should not flush cache the cache
        NodeEvent event = NodeEventHelper.createNodeEventInstance(posrelNode, NodeEvent.EVENT_TYPE_NEW, null);
        assertFalse("node event of new relation node on multi step query should not release the query",
            strategy.evaluate(event, q1, null).shouldRelease());
        
        //but the subsequent relation event should
        RelationEvent relEvent = NodeEventHelper.createRelationEventInstance(posrelNode, RelationEvent.EVENT_TYPE_NEW, null);
        assertTrue("relation event of new relation between nodes in multi step query should flush the cache",
            strategy.evaluate(relEvent, q1, null).shouldRelease());
     
    }
    
    
    
    
    public void testChangedNode(){
        log.debug("method: testMultiStepQueryChangedNode()");
        
        //if none of the fields that have changed are part of the select or the constraint part of the query it should not be flushed
       
        //MMObjectNode testNode = MMBase.getMMBase().getBuilder("object").getNode(newsNode.getNumber());
        //testNode.setValue("title", "another title");
        
        NodeEvent event = new NodeEvent(null, "news", 0, getMap("title", "oldTitle"), getMap("title", "newtitle"), NodeEvent.EVENT_TYPE_CHANGED);
        
        Query q1 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle", "news.number < 1000", null, null, null, false);
        Query q2 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle", "news.title = 'hallo'", null, null, null, false);
        Query q3 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.title", "news.number < 1000", null, null, null, false);
        
        assertFalse("changed field is not used by query: it should not be flused",
            strategy.evaluate(event, q1, null).shouldRelease());
        assertTrue("changed field is in constraints section of query: it should be flused",
            strategy.evaluate(event, q2, null).shouldRelease());
        assertTrue("changed field is in select section of query: it should be flused",
            strategy.evaluate(event, q3, null).shouldRelease());
        
        //also test  composite constraints
        Query q4 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle", "news.number < 1000 AND urls.name = 'hi'", null, null, null, false);
        Query q5 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle", "news.title='something' AND urls.name = 'hi'", null, null, null, false);
        
        assertFalse("changed field is not used by (composite) constraint: it should not be flused",
            strategy.evaluate(event, q4, null).shouldRelease());
        assertTrue("changed field is used by (composite) constraint: it should be flused",
            strategy.evaluate(event, q5, null).shouldRelease());
        
        //also test legacy constraints
        Query q6 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle", null, null, null, null, false);
        q6.setConstraint(new BasicLegacyConstraint("news.number < 1000 AND urls.name = 'hi'"));
        
        assertFalse("changed field is not used by (legacy) constraint: it should not be flused",
            strategy.evaluate(event, q6, null).shouldRelease());
        
        q6.setConstraint(new BasicLegacyConstraint("news.title='something' AND urls.name = 'hi'"));
        
        assertTrue("changed field is used by (legacy) constraint: it should be flused",
            strategy.evaluate(event, q6, null).shouldRelease());
        
        //*************************
        //if the step(s) of the changed field(s) are (all) aggregate fields of type count, and this field is not used
        //in the constraint as well, the query shouldn't be flushed.
        
        Query q7;
        Step newsStep;
        q7 = cloud.createAggregatedQuery();
        newsStep = q7.addStep(newsManager);
        AggregatedField titleField = q7.addAggregatedField(newsStep, newsManager.getField("title"), 2);
        
        assertFalse("aggregate queries (type count) where the changed field(s) match the step(s) should not be flushed",
        		strategy.evaluate(event, q7, null).shouldRelease());
        
        //but whit this field in the constraint it should be flushed
        q7.setConstraint(new BasicFieldValueConstraint(titleField, "disco"));
        assertTrue("aggregate queries (type count) where the changed field(s) match the step(s) but there are constraints on the step(s) should be flushed",
        		strategy.evaluate(event, q7, null).shouldRelease());
        
        //but other types of aggregation should flush (they deal with the contents of the field
        int[] aggregations = new int[] {
        		AggregatedField.AGGREGATION_TYPE_COUNT_DISTINCT,
        		AggregatedField.AGGREGATION_TYPE_GROUP_BY,
        		AggregatedField.AGGREGATION_TYPE_MAX,
        		AggregatedField.AGGREGATION_TYPE_MIN};
        for(int i = 0; i < aggregations.length; i++){
        	q7 = cloud.createAggregatedQuery();
            newsStep = q7.addStep(newsManager);
            q7.addAggregatedField(newsStep, newsManager.getField("title"),aggregations[i]);
            
            assertTrue ("aggregate queries (type " + AggregatedField.AGGREGATION_TYPE_DESCRIPTIONS[i] + ") where the changed field(s) match the step(s) should be flushed",
            		strategy.evaluate(event, q7, null).shouldRelease());
        }
    }
    
    
    public void testChangedRelation(){
        //if a none of the fields that have changed are part of the select or the constraint part of the query it should not be flushed
    	
    	RelationEvent relEvent = NodeEventHelper.createRelationEventInstance(posrelNode, NodeEvent.EVENT_TYPE_CHANGED, null);
    	relEvent.addChangedField("pos", new Integer(0), new Integer(1));
    	
    	Query q1 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle", "news.number < 10 ", null, null, null, false);
    	Query q2 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle", "posrel.pos < 10 ", null, null, null, false);
    	Query q3 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle,posrel.pos", "news.number < 10 ", null, null, null, false);
    	
    	assertFalse("changed relation field is not used by query: it should not be flused",
                strategy.evaluate(relEvent, q1, null).shouldRelease());
        assertTrue("changed relation field is in constraints section of query: it should be flused",
                strategy.evaluate(relEvent, q2, null).shouldRelease());
        assertTrue("changed relation field is in select section of query: it should be flused",
                strategy.evaluate(relEvent, q3, null).shouldRelease());
        
        //also test  composite constraints
        Query q4 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle", "news.number < 1000 AND urls.name = 'hi'", null, null, null, false);
        Query q5 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle", "posrel.pos < 1 AND urls.name = 'hi'", null, null, null, false);
        
        assertFalse("changed relation field is not used by (composite) constraint: it should not be flused",
            strategy.evaluate(relEvent, q4, null).shouldRelease());
        assertTrue("changed relation field is used by (composite) constraint: it should be flused",
            strategy.evaluate(relEvent, q5, null).shouldRelease());
        
        //also test legacy constraints
        Query q6 = Queries.createQuery(cloud, null, "news,posrel,urls" ,"news.subtitle", null, null, null, null, false);
        q6.setConstraint(new BasicLegacyConstraint("news.number < 1000 AND urls.name = 'hi'"));
        
        assertFalse("changed relation field is not used by (legacy) constraint: it should not be flused",
            strategy.evaluate(relEvent, q6, null).shouldRelease());
        
        q6.setConstraint(new BasicLegacyConstraint("news.title='something' AND posrel.pos < 1"));
        
        assertTrue("changed relation field is used by (legacy) constraint: it should be flused",
            strategy.evaluate(relEvent, q6, null).shouldRelease());
        
    }
    
    
    protected Node createRelDefNode(String role, int dir) {
        // create a new relation-definition
        Node reldef = relDefManager.createNode();
        reldef.setValue("sname", role);
        reldef.setValue("dname", "d" + role );
        reldef.setValue("sguiname", role);
        reldef.setValue("dguiname", "d" + role);
        reldef.setIntValue("dir", dir);
        reldef.setNodeValue("builder", insRelManager);
        reldef.commit();
        createdNodes.add(reldef);
        return reldef;
    }
    
    private Map getMap(Object key, Object value){
    	Map m = new HashMap();
    	m.put(key, value);
    	return m;
    }
  
}
