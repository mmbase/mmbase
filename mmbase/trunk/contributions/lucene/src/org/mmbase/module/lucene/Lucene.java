/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
sQuery
*/
package org.mmbase.module.lucene;

import java.util.*;
import java.io.File;
import org.w3c.dom.*;
import org.w3c.dom.NodeList;
import java.net.URL;
import javax.sql.DataSource;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.storage.search.*;
import org.mmbase.cache.CachePolicy;
import org.mmbase.module.ReloadableModule;

import org.mmbase.core.event.*;
import org.mmbase.module.core.*;
import org.mmbase.bridge.util.xml.query.*;
import org.mmbase.bridge.util.BridgeCollections;
import org.mmbase.util.*;
import org.mmbase.util.xml.XMLWriter;
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.*;
import org.mmbase.storage.implementation.database.DatabaseStorageManagerFactory;
import org.mmbase.storage.StorageManagerFactory;

import java.util.concurrent.*;


import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.mmbase.module.lucene.extraction.*;

/**
 * This is the implementation of a 'Lucene' module. It's main job is to bootstrap mmbase lucene
 * indexing, and provide some functions to give access to lucene functionality in an MMBase way.
 *
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: Lucene.java,v 1.86 2007-07-23 15:48:16 michiel Exp $
 **/
public class Lucene extends ReloadableModule implements NodeEventListener, RelationEventListener, IdEventListener {

    public static final String PUBLIC_ID_LUCENE_2_0 = "-//MMBase//DTD luceneindex config 2.0//EN";
    public static final String DTD_LUCENE_2_0 = "luceneindex_2_0.dtd";

    /** Most recent Lucene config DTD */
    public static final String PUBLIC_ID_LUCENE = PUBLIC_ID_LUCENE_2_0;
    public static final String DTD_LUCENE = DTD_LUCENE_2_0;

    /**
     * But we use XSD now!
     * @todo Support for DTD's can be dropped, it was never released.
     */
    public static final String XSD_LUCENE_1_0 = "luceneindex.xsd";
    public static final String NAMESPACE_LUCENE_1_0 = "http://www.mmbase.org/xmlns/luceneindex";

    /**
     * Most recend namespace
     */
    public static final String NAMESPACE_LUCENE = NAMESPACE_LUCENE_1_0;


    /**
     * Parameter constants for Lucene functions.
     */
    protected final static Parameter/*<String>*/ VALUE = new Parameter("value", String.class);
    static { VALUE.setDescription("the search term(s)"); }

    protected final static Parameter/*<String>*/ INDEX = new Parameter("index", String.class);
    static { INDEX.setDescription("the name of the index to search in"); }

    protected final static Parameter/*<Class>*/ CLASS = new Parameter("class", Class.class, IndexDefinition.class);
    static { INDEX.setDescription("the class of indices to search in (default to all classes)"); }
 
    protected final static Parameter/*<String>*/ SORTFIELDS = new Parameter("sortfields", String.class);
    protected final static Parameter/*<String>*/ FIELDS     = new Parameter("fields", String.class);
    protected final static Parameter/*<String>*/ ANALYZER   = new Parameter("analyzer", String.class);
    protected final static Parameter/*<Integer>*/  OFFSET = new Parameter("offset", Integer.class, 0);
    static { OFFSET.setDescription("for creating sublists"); }

    protected final static Parameter/*<Integer>*/ MAX = new Parameter("max", Integer.class, Integer.MAX_VALUE);
    static { MAX.setDescription("for creating sublists"); }

    protected final static Parameter/*<String>*/  EXTRACONSTRAINTS = new Parameter("extraconstraints", String.class);
    static { EXTRACONSTRAINTS.setDescription("@see org.mmbase.module.lucene.Searcher#createQuery()"); }

    protected final static Parameter/*<String>*/  FILTER = new Parameter("filter", String.class);

    /*
    protected final static Parameter EXTRACONSTRAINTSLIST = new Parameter("constraints", List.class);
    static { EXTRACONSTRAINTSLIST.setDescription("@see org.mmbase.module.lucene.Searcher#createQuery()"); }
    */

    protected final static Parameter/*<String>*/ IDENTIFIER = new Parameter("identifier", String.class);
    static { IDENTIFIER.setDescription("Normally a node number, identifier (a number of) lucene document(s) in an index."); }

    static {
        XMLEntityResolver.registerPublicID(PUBLIC_ID_LUCENE_2_0, DTD_LUCENE_2_0, Lucene.class);
        XMLEntityResolver.registerPublicID(NAMESPACE_LUCENE_1_0, XSD_LUCENE_1_0, Lucene.class);
        XMLEntityResolver.registerPublicID(PUBLIC_ID_LUCENE, DTD_LUCENE, Lucene.class);
    }

    // initial wait time after startup, default 2 minutes
    private static final long INITIAL_WAIT_TIME = 2 * 60 * 1000;
    // wait time bewteen individual checks, default 5 seconds
    private static final long WAIT_TIME = 5 * 1000;
    // default path to the lucene data
    private static final String INDEX_CONFIG_FILE = "utils/luceneindex.xml";

    private static final Logger log = Logging.getLoggerInstance(Lucene.class);

    public static Lucene getLucene() {
        //return getModule(Lucene.class); // java 1.5
        return (Lucene) getModule("lucene");
    }

    /**
     * The MMBase instance, used for low-level access
     */
    protected MMBase mmbase = null;

    private long initialWaitTime = INITIAL_WAIT_TIME;
    private long waitTime = WAIT_TIME;
    private String indexPath = null;
    private Scheduler scheduler = null;
    private String defaultIndex = null;
    private final Set<String>           disableIndexes = new HashSet<String>();
    private final Map<String, Indexer>  indexerMap    = new ConcurrentHashMap<String, Indexer>();
    private final Map<String, Searcher> searcherMap   = new ConcurrentHashMap<String, Searcher>();
    private boolean readOnly = false;

    private List<String> configErrors = new ArrayList<String>();
    private Date configReadTime = new Date(0);

    /**
     * Returns whether an element has a certain attribute, either an unqualified attribute or an attribute that fits in the
     * lucene namespace
     */
    static public boolean hasAttribute(Element element, String localName) {
        return element.hasAttributeNS(NAMESPACE_LUCENE, localName) || element.hasAttribute(localName);
    }

    /**
     * Returns the value of a certain attribute, either an unqualified attribute or an attribute that fits in the
     * lucene namespace
     */
    static public String getAttribute(Element element, String localName) {
        if (element.hasAttributeNS(NAMESPACE_LUCENE, localName)) {
            return element.getAttributeNS(NAMESPACE_LUCENE, localName);
        } else {
            return element.getAttribute(localName);
        }
    }

    /**
     * This function starts a full Index of Lucene.
     * This may take a while.
     * This function can be called through the function framework.
     */
    //protected Function<Void> fullIndexFunction = new AbstractFunction<Void>("fullIndex", INDEX) {
    protected Function/*<Void>*/ fullIndexFunction = new AbstractFunction/*<Void>*/("fullIndex", new Parameter[] {INDEX}, ReturnType.VOID) {
        public Void getFunctionValue(Parameters arguments) {
            if (scheduler == null) throw new RuntimeException("Read only");
            String index = (String) arguments.get(INDEX);
            if (index == null || "".equals(index)) {
                scheduler.fullIndex();
            } else {
                scheduler.fullIndex(index);
            }
            return null;
        }
    };
    {
        addFunction(fullIndexFunction);
    }

    /**
     * This function deletes an indexed entry from an index
     * if the Parameter 'index' has value null, all indexes are iterated over, otherwise
     * the right index is addressed.
     */
    //protected Function<Void> deleteIndexFunction = new AbstractFunction<Void>("deleteIndex", INDEX, IDENTIFIER, CLASS) {
    protected Function/*<Void>*/ deleteIndexFunction = new AbstractFunction/*<Void>*/("deleteIndex", new Parameter[] {INDEX, IDENTIFIER, CLASS}, ReturnType.VOID) {
            public Void getFunctionValue(Parameters arguments) {
                if (scheduler == null) throw new RuntimeException("Read only");
                if(!readOnly){
                    String index      = (String) arguments.get(INDEX);
                    String identifier = (String) arguments.get(IDENTIFIER);
                    Class  klass      = (Class)  arguments.get(CLASS);
                    if(index == null || "".equals(index)){
                        scheduler.deleteIndex(identifier, klass);
                    } else {
                        scheduler.deleteIndex(identifier, identifier);
                    }
                }
                return null;
            }
        };
    {
        addFunction(deleteIndexFunction);
    }



    /**
     * This function can be called through the function framework.
     * It (re)loads the index for a specific item (identified by 'identifier' parameter).
     */
    //protected Function<Void> updateIndexFunction = new AbstractFunction<Void>("updateIndex", new Parameter(IDENTIFIER, true),  CLASS) {
    protected Function updateIndexFunction = new AbstractFunction("updateIndex", new Parameter[] {new Parameter(IDENTIFIER, true),  CLASS}, ReturnType.VOID) {
            public Void getFunctionValue(Parameters arguments) {
                if (scheduler == null) throw new RuntimeException("Read only");
                scheduler.updateIndex(arguments.getString(IDENTIFIER), (Class) arguments.get(CLASS));
                return null;
            }
        };
    {
        addFunction(updateIndexFunction);
    }


    /**
     * This function returns the status of the scheduler. For possible values see: Lucene.Scheduler
     */
    //protected Function<Integer> statusFunction = new AbstractFunction<Integer>("status") {
    protected Function/*<Integer>*/ statusFunction = new AbstractFunction/*<Integer>*/("status", Parameter.EMPTY, ReturnType.INTEGER) {
        public Integer getFunctionValue(Parameters arguments) {
            return scheduler == null ? Scheduler.READONLY : scheduler.getStatus();
        }
    };
    {
        addFunction(statusFunction);
    }
    //protected Function<String> statusDescriptionFunction = new AbstractFunction<String>("statusdescription", Parameter.LOCALE) {
    protected Function/*<String>*/ statusDescriptionFunction = new AbstractFunction/*<String>*/("statusdescription", new Parameter[] { Parameter.LOCALE}, ReturnType.UNKNOWN) {
        public String getFunctionValue(Parameters arguments) {
            Locale locale = (Locale) arguments.get(Parameter.LOCALE);
            SortedMap map = SortedBundle.getResource("org.mmbase.module.lucene.resources.status",  locale,
                                                     getClass().getClassLoader(),
                                                     SortedBundle.getConstantsProvider(Scheduler.class), Integer.class, null);
            String desc = "" + map.get(new Integer(scheduler == null ? Scheduler.READONLY : scheduler.getStatus()));
            Object ass = (scheduler == null ? null : scheduler.getAssignment());
            return desc + (ass == null ? "" : " " + ass);
        }
    };
    {
        addFunction(statusDescriptionFunction);
    }
    
    //protected Function <Scheduler.Assignment> assignmentFunction = new AbstractFunction<Scheduler.Assignment/ ("assignment") {
    protected Function /*<Scheduler.Assignment>*/ assignmentFunction = new AbstractFunction /*<Scheduler.Assignment>*/ ("assignment", Parameter.EMPTY, ReturnType.UNKNOWN) {
        public Scheduler.Assignment getFunctionValue(Parameters arguments) {
            return scheduler == null ? null : scheduler.getAssignment();
        }
    };
    {
        addFunction(assignmentFunction);
    }
    //protected Function<Collection<Scheduler.Assignment>> queueFunction = new AbstractFunction("queue") {
    protected Function queueFunction = new AbstractFunction("queue", Parameter.EMPTY, ReturnType.UNKNOWN) {
        public Collection<Scheduler.Assignment> getFunctionValue(Parameters arguments) {
            return scheduler == null ? Collections.EMPTY_LIST : scheduler.getQueue();
        }
    };
    {
        addFunction(queueFunction);
    }

    //protected Function<Boolean> readOnlyFunction = new AbstractFunction<Boolean>("readOnly"){
    protected Function /*<Boolean>*/ readOnlyFunction = new AbstractFunction /*<Boolean>*/("readOnly", Parameter.EMPTY, ReturnType.BOOLEAN){
        public Boolean getFunctionValue(Parameters arguments) {
            return readOnly;
        }
    };
    {
        addFunction(readOnlyFunction);
    }

    /**
     * This function returns Set with the names of all configured indexes (ordered alphabeticly)
     */
    //protected Function /*<Set<String>>*/ listFunction = new AbstractFunction /*<Set<String>>*/ ("list") {
    protected Function  listFunction = new AbstractFunction ("list", Parameter.EMPTY, ReturnType.SET) {
            public Set<String> getFunctionValue(Parameters arguments) {
                return new TreeSet<String>(indexerMap.keySet());
            }

        };
    {
        addFunction(listFunction);
    }

    /**
     *This function returns the description as configured for a specific index and a specific locale.
     */
    //protected Function/*<String>*/  descriptionFunction = new AbstractFunction/*<String>*/("description", INDEX, Parameter.LOCALE) {
    protected Function/*<String>*/  descriptionFunction = new AbstractFunction/*<String>*/("description", new Parameter[] {INDEX, Parameter.LOCALE}, ReturnType.STRING) {
            public String getFunctionValue(Parameters arguments) {
                String key    = arguments.getString(INDEX);
                Locale locale = (Locale) arguments.get(Parameter.LOCALE);
                Indexer index = indexerMap.get(key);
                return index.getDescription().get(locale);
            }

        };
    {
        addFunction(descriptionFunction);
    }

    /**
     * This function starts a search fro a given string.
     * This function can be called through the function framework.
     */
    //protected Function<org.mmbase.bridge.NodeList> searchFunction = new AbstractFunction("search", VALUE, INDEX, FIELDS, SORTFIELDS, OFFSET, MAX, EXTRACONSTRAINTS, FILTER, Parameter.CLOUD, ANALYZER) {
    protected Function searchFunction = new AbstractFunction("search", new Parameter[] {VALUE, INDEX, FIELDS, SORTFIELDS, OFFSET, MAX, EXTRACONSTRAINTS, FILTER, Parameter.CLOUD, ANALYZER}, ReturnType.NODELIST) {
            public org.mmbase.bridge.NodeList getFunctionValue(Parameters arguments) {
                String value       = (String) arguments.getString(VALUE);
                String index       = (String) arguments.getString(INDEX);
                String sortFields  = (String) arguments.get(SORTFIELDS);
                String[] sortFieldArray = sortFields == null ? null : (String []) StringSplitter.split(sortFields).toArray(new String[] {});
                String fields      = (String) arguments.get(FIELDS);
                String[] fieldArray = fields == null || "".equals(fields) ? getSearcher(index).allIndexedFields : (String []) StringSplitter.split(fields).toArray(new String[] {});
                Analyzer analyzer = null;
                String an = (String) arguments.get(ANALYZER);
                if (an != null && ! "".equals(an)) {
                    try {
                        Class clazz = Class.forName(an);
                        analyzer = (Analyzer) clazz.newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                int offset         = (Integer) arguments.get(OFFSET);
                int max            = (Integer) arguments.get(MAX);
                String extraConstraints = arguments.getString(EXTRACONSTRAINTS);
                String filter     = arguments.getString(FILTER);
                log.debug("using analyzer " + analyzer);
                /*
                List moreConstraints = (List) arguments.get(EXTRACONSTRAINTSLIST);
                if (moreConstraints != null && moreConstraints.size() > 0) {
                    StringBuffer ec = new StringBuffer(extraConstraints == null ? "" : extraConstraints + " ");
                    Iterator i = moreConstraints.iterator();
                    while (i.hasNext()) {
                        ec.append(i.next().toString());
                        ec.append(" ");
                    }
                    extraConstraints = ec.toString().trim();
                }
                */
                Cloud cloud         = (Cloud) arguments.get(Parameter.CLOUD);
                cloud.setProperty(Cloud.PROP_XMLMODE, "flat"); //
                try {
                    return getSearcher(index).search(cloud, value, 
                                                     Searcher.createFilter(filter),
                                                     Searcher.getSort(sortFieldArray), 
                                                     analyzer,
                                                     Searcher.createQuery(extraConstraints), 
                                                     fieldArray,
                                                     offset, max);
                } catch (ParseException pe) {
                    // search function is typically used in a JSP and the 'value' parameter filled by web-site users.
                    // They may not fill the log with errors!
                    if (log.isDebugEnabled()) {
                        log.debug(pe);
                    }
                    return BridgeCollections.EMPTY_NODELIST;
                }
            }
        };
    {
        addFunction(searchFunction);
    }

    /**
     * This function returns the size of a query on an index.
     */
    //protected Function<Integer> searchSizeFunction = new AbstractFunction<Integer>("searchsize", VALUE, INDEX, FIELDS,EXTRACONSTRAINTS, FILTER, Parameter.CLOUD, ANALYZER ) {
    protected Function searchSizeFunction = new AbstractFunction("searchsize", new Parameter[] {VALUE, INDEX, FIELDS,EXTRACONSTRAINTS, FILTER, Parameter.CLOUD, ANALYZER}, ReturnType.INTEGER) {
        public Integer getFunctionValue(Parameters arguments) {
            String value = (String) arguments.getString(VALUE);
            String index = (String) arguments.getString(INDEX);
            String extraConstraints = (String) arguments.getString(EXTRACONSTRAINTS);
            String filter = (String) arguments.getString(FILTER);
            String fields      = (String) arguments.get(FIELDS);
            String[] fieldArray = fields == null || "".equals(fields) ? getSearcher(index).allIndexedFields : (String[]) StringSplitter.split(fields).toArray(new String[] {});
            Cloud cloud  =  (Cloud) arguments.get(Parameter.CLOUD);
            Analyzer analyzer = null;
            String an = (String) arguments.get(ANALYZER);
            if (an != null && ! "".equals(an)) {
                try {
                    Class clazz = Class.forName(an);
                    analyzer = (Analyzer) clazz.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return getSearcher(index).searchSize(cloud, value,  Searcher.createFilter(filter), analyzer, Searcher.createQuery(extraConstraints), fieldArray);
        }
    };
    {
        addFunction(searchSizeFunction);
    }


    //protected Function<Integer> unAssignFunction = new AbstractFunction<Integer>("unassign", new Parameter("id", Integer.class, true)) {
    protected Function/*<Integer>*/ unAssignFunction = new AbstractFunction/*<Integer>*/("unassign", new Parameter[] {new Parameter("id", Integer.class, true)}, ReturnType.INTEGER) {
            public Integer getFunctionValue(Parameters arguments) {
                int id = (Integer) arguments.get("id");
                if (scheduler != null) {
                    return scheduler.unAssign(id);
                } else {
                    return 0;
                }
            }
        };
    {
        addFunction(unAssignFunction);
    }
    //protected Function<String> interruptFunction = new AbstractFunction<String>("interrupt") {
    protected Function/*<String>*/ interruptFunction = new AbstractFunction/*<String>*/("interrupt", Parameter.EMPTY, ReturnType.STRING) {
            public String getFunctionValue(Parameters arguments) {
                if (scheduler != null) {
                    if (scheduler.getStatus() > Scheduler.IDLE) {
                        scheduler.interrupt();
                        return "Interrupted";
                    } else {
                        scheduler.interrupt();
                        return "Interrupted (though idle)";
                    }
                } else  {
                    return "not yet running";
                }

            }
        };
    {
        addFunction(interruptFunction);
    }

    //protected Function<Date> lastFullIndexFunction = new AbstractFunction<Date>("last", INDEX) {
    protected Function/*<Date>*/ lastFullIndexFunction = new AbstractFunction/*<Date>*/("last", new Parameter[] {INDEX}, ReturnType.UNKNOWN) {
        public Date getFunctionValue(Parameters arguments) {
            String key = (String) arguments.get(INDEX);
            Indexer index = indexerMap.get(key);
            if (index != null) {
                return index.getLastFullIndex();
            } else {
                return null;
            }
        }
    };
    {
        addFunction(lastFullIndexFunction);

        //addFunction(new AbstractFunction<Indexer>("default") {
        addFunction(new AbstractFunction/*<Indexer>*/("default", Parameter.EMPTY, ReturnType.UNKNOWN){ 
                public Indexer getFunctionValue(Parameters arguments) {
                    return indexerMap.get(defaultIndex);
                }
            });

        //addFunction(new AbstractFunction<List<String>>("errors", INDEX, OFFSET, MAX) {
        addFunction(new AbstractFunction("errors", new Parameter[] {INDEX, OFFSET, MAX}, ReturnType.LIST) {
                public List<String> getFunctionValue(Parameters arguments) {
                    String index = arguments.getString(INDEX);
                    List<String> errors = indexerMap.get(index).getErrors();
                    int offset = (Integer) arguments.get(OFFSET);
                    int toIndex = offset + (Integer) arguments.get(MAX);
                    if (toIndex > errors.size()) toIndex = errors.size();
                    return indexerMap.get(index).getErrors().subList(offset, toIndex);
                }
            });
        //addFunction(new AbstractFunction<Long>("nodes", INDEX) {
        addFunction(new AbstractFunction("nodes", new Parameter[] {INDEX}, ReturnType.LONG) {
            public Long getFunctionValue(Parameters arguments) {
                String index = arguments.getString(INDEX);
                return searcherMap.get(index).getNumberOfProducedNodes();
            }
            });
        //addFunction(new AbstractFunction<Date>("config") {
        addFunction(new AbstractFunction("config",Parameter.EMPTY, ReturnType.UNKNOWN) {
            public Date getFunctionValue(Parameters arguments) {
                return configReadTime;
            }
            });
    }

    private ContentExtractor factory;

    public void init() {
        super.init();


        ThreadPools.jobsExecutor.execute(new Runnable() {
                public void run() {
                    // Force init of MMBase
                    mmbase = MMBase.getMMBase();

                    factory = ContentExtractor.getInstance();


                    String path = getInitParameter("indexpath");
                    if (path != null) {
                        indexPath = path;
                        log.service("found module parameter for lucene index path : " + indexPath);
                    } else {
                        //try to get the index path from the strorage configuration
                        try {
                            DatabaseStorageManagerFactory dsmf = (DatabaseStorageManagerFactory)mmbase.getStorageManagerFactory();
                            indexPath = dsmf.getBinaryFileBasePath();
                            if(indexPath != null) indexPath = indexPath + dsmf.getDatabaseName() + File.separator + "lucene";
                        } catch(Exception e){}
                    }

                    if(indexPath != null) {
                        log.service("found storage configuration for lucene index path : " + indexPath);
                    } else {
                        // expand the default path (which is relative to the web-application)
                        indexPath = MMBaseContext.getServletContext().getRealPath(indexPath);
                        log.service("fall back to default for lucene index path : " + indexPath);
                    }


                    String readOnlySetting = getInitParameter("readonly");
                    while (readOnlySetting != null && readOnlySetting.startsWith("system:")) {
                        readOnlySetting = System.getProperty(readOnlySetting.substring(7));
                    }
                    if (readOnlySetting != null) {
                        if (readOnlySetting.startsWith("host:")) {
                            String host = readOnlySetting.substring(5); 
                            try {
                                boolean write = 
                                    java.net.InetAddress.getLocalHost().getHostName().equals(host) ||
                                    (System.getProperty("catalina.base") + "@" + java.net.InetAddress.getLocalHost().getHostName()).equals(host);
                                readOnly = ! write;
                            } catch (java.net.UnknownHostException uhe) {
                                log.error(uhe);
                            }
                        } else {
                            readOnly = "true".equals(readOnlySetting);
                        }
                    }
                    if (readOnly) {
                        log.info("Lucene module of this MMBase will be READONLY");
                    }

                    // initial wait time?
                    String time = getInitParameter("initialwaittime");
                    if (time != null) {
                        try {
                            initialWaitTime = Long.parseLong(time);
                            log.debug("Set initial wait time to " + time + " milliseconds");
                        } catch (NumberFormatException nfe) {
                            log.warn("Invalid value '" + time + "' for property 'initialwaittime'");
                        }
                    }
                    time = getInitParameter("waittime");
                    if (time != null) {
                        try {
                            waitTime = Long.parseLong(time);
                            log.debug("Set wait time to " + time + " milliseconds");
                        } catch (NumberFormatException nfe) {
                            log.warn("Invalid value '" + time +" ' for property 'iwaittime'");
                        }
                    }
                    while(! mmbase.getState()) {
                        if (mmbase.isShutdown()) break;
                        try {
                            log.service("MMBase not yet up, waiting for 10 seconds.");
                            Thread.sleep(10000);
                        } catch (InterruptedException ie) {
                            log.info(ie);
                            return;
                        }
                    }

                    ResourceWatcher watcher = new ResourceWatcher() {
                            public void onChange(String resource) {
                                readConfiguration(resource);
                            }
                        };
                    watcher.add(INDEX_CONFIG_FILE);
                    watcher.onChange();
                    watcher.start();

                    if (!readOnly) {
                        scheduler = new Scheduler();
                        log.service("Module Lucene started");
                        // full index ?
                        String fias = getInitParameter("fullindexatstartup");
                        if (initialWaitTime < 0 || "true".equals(fias)) {
                            scheduler.fullIndex();
                        }
                    }
                }
            });

    }



    private Cloud cloud = null;
    protected Cloud getCloud() {
        if (cloud != null && !cloud.getUser().isValid()) cloud = null;
        while (cloud == null) {
            try {
                cloud = LocalContext.getCloudContext().getCloud("mmbase", "class", null);
                cloud.setProperty(Cloud.PROP_XMLMODE, "flat");
                log.info("Using cloud of " + cloud.getUser().getIdentifier() + "(" + cloud.getUser().getRank() + ") to lucene index.");
            } catch (Throwable t) {
                log.error(t.getClass().getName() + " " + t.getMessage());
            }
            if (cloud == null) {
                try {
                    log.info("No cloud found, waiting for 5 seconds");
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    return null;
                }
            }
        }
        return cloud;
    }

    public void shutdown() {
        if (scheduler != null) {
            log.service("Stopping Lucene Scheduler");
            scheduler.interrupt();
        }
        indexerMap.clear();
        for (Searcher searcher : searcherMap.values()) {
            searcher.shutdown();
        }
        searcherMap.clear();
        scheduler = null;
        mmbase = null;
    }

    public void reload() {
        shutdown();
        init();
    }

    public String getModuleInfo() {
        return "This module performs lucene searches and maintains indices";
    }

    /**
     * MMBase Queries and sub-queries
     */
    protected MMBaseIndexDefinition createIndexDefinition (Element queryElement, Set<String> allIndexedFieldsSet, boolean storeText, boolean mergeText, String relateFrom, Analyzer analyzer) {
        try {
            if (Lucene.hasAttribute(queryElement,"optimize")) {
                String optimize = Lucene.getAttribute(queryElement,"optimize");
                storeText = optimize.equals("none");
                mergeText = optimize.equals("full");
            }

            QueryConfigurer configurer = new IndexConfigurer(allIndexedFieldsSet, storeText, mergeText);

            MMBaseIndexDefinition queryDefinition = (MMBaseIndexDefinition) QueryReader.parseQuery(queryElement, configurer, getCloud(), relateFrom);
            queryDefinition.setAnalyzer(analyzer);
            // do not cache these queries
            queryDefinition.query.setCachePolicy(CachePolicy.NEVER);

            // MM: I think the follwing functionality should be present on MMBaseIndexDefinition itself. and not on Lucene.
            // And of course, the new event-mechanism must be used.
            if (!readOnly) {
                // register. Unfortunately this can currently only be done through the core
                //for (Step step : queryDefinition.query.getSteps() ) {
                for (Iterator i = queryDefinition.query.getSteps().iterator(); i.hasNext();) {
                    Step step = (Step) i.next();
                    MMObjectBuilder builder = mmbase.getBuilder(step.getTableName());
                    log.service("Observing for builder " + builder.getTableName() + " for index " + queryElement.getAttribute("name"));
                    builder.addEventListener(this);
                }
            }



            String elementName = queryDefinition.elementManager.getName();
            NodeList childNodes = queryElement.getChildNodes();
            for (int k = 0; k < childNodes.getLength(); k++) {
                if (childNodes.item(k) instanceof Element) {
                    Element childElement = (Element) childNodes.item(k);
                    if ("related".equals(childElement.getLocalName())) {
                        MMBaseIndexDefinition subIndex = createIndexDefinition(childElement, allIndexedFieldsSet, storeText, mergeText, elementName, analyzer);
                        queryDefinition.subQueries.add(subIndex);
                    }
                }
            }

            if (log.isDebugEnabled()) {
                 log.debug("Configured builder " + elementName + " with query:" + queryDefinition.query);
            }
            return queryDefinition;
        } catch (Exception e) {
            configErrors.add(e.getMessage());
            log.warn("Invalid query for index " + XMLWriter.write(queryElement, true, true), e);
            return null;
        }
    }

    protected final IdEventListener idListener = new IdEventListener() {
            // wrapping to avoid also registring it as a NodeEventListener
            public void notify(IdEvent idEvent) {
                Lucene.this.notify(idEvent);
            }
            public String toString() {
                return Lucene.this.toString();
            }
        };

    protected void readConfiguration(String resource) {
        indexerMap.clear();
        searcherMap.clear();
        disableIndexes.clear();
        defaultIndex = null;
        factory.clear();
        List<URL> configList = ResourceLoader.getConfigurationRoot().getResourceList(resource);
        log.service("Reading " + configList);
        for(URL url : configList) {
            try {
                if (! url.openConnection().getDoInput()) continue;
                Document config = ResourceLoader.getDocument(url, true, Lucene.class);
                log.service("Reading lucene search configuration from " + url);
                Element root = config.getDocumentElement();
                disableIndexes.addAll(StringSplitter.split(root.getAttribute("disableIndexes")));

                NodeList extractorElements = root.getElementsByTagName("extractor");
                for (int i = 0; i < extractorElements.getLength(); i++) {
                    Element extractorElement = (Element) extractorElements.item(i);
                    String className = extractorElement.getAttribute("class");
                    String mimeType = extractorElement.getAttribute("mimetype");
                    if ("".equals(mimeType)) mimeType = null;
                    factory.addExtractor(className, mimeType);
                }

                NodeList indexElements = root.getElementsByTagName("index");
                for (int i = 0; i < indexElements.getLength(); i++) {
                    Element indexElement = (Element) indexElements.item(i);
                    String indexName = "default";
                    if (Lucene.hasAttribute(indexElement, "name")) {
                        indexName = Lucene.getAttribute(indexElement, "name");
                    }
                    if (disableIndexes.contains(indexName)) {
                        log.service("Index with name '" + indexName + "' was disabled");
                        continue;
                    }
                    if (indexerMap.containsKey(indexName)) {
                        log.warn("Index with name " + indexName + " already exists");
                    } else {
                        boolean storeText = false; // default: no text fields are stored in the index unless noted otherwise
                        boolean mergeText = true; // default: all text fields have the "fulltext" alias unless noted otherwise
                        if (Lucene.hasAttribute(indexElement, "optimize")) {
                            String optimize = Lucene.getAttribute(indexElement, "optimize");
                            storeText = optimize.equals("none");
                            mergeText = optimize.equals("full");
                        }
                        if (defaultIndex == null) {
                            defaultIndex = indexName;
                            log.service("Default index: " + defaultIndex);
                        }
                        Set<String> allIndexedFieldsSet = new HashSet<String>();
                        List<IndexDefinition> queries = new ArrayList<IndexDefinition>();
                        // lists
                        NodeList childNodes = indexElement.getChildNodes();
                        Analyzer analyzer = null;
                        int lists = 0;
                        for (int k = 0; k < childNodes.getLength(); k++) {
                            if (childNodes.item(k) instanceof Element) {
                                Element childElement = (Element) childNodes.item(k);
                                String name = childElement.getLocalName();
                                if ("list".equals(name)||
                                    "builder".equals(name) || // backward comp. old finalist lucene
                                    "table".equals(name)) { // comp. finalist lucene
                                    IndexDefinition id = createIndexDefinition(childElement, allIndexedFieldsSet, storeText, mergeText, null, analyzer);
                                    if (id == null) continue;
                                    id.setId(indexName + "_" + (++lists));
                                    queries.add(id);
                                    log.service("Added mmbase index definition " + id);
                                } else if ("jdbc".equals(name)) {
                                    DataSource ds =  ((DatabaseStorageManagerFactory) mmbase.getStorageManagerFactory()).getDataSource();
                                    IndexDefinition id = new JdbcIndexDefinition(ds, childElement,
                                                                                 allIndexedFieldsSet, storeText, mergeText, analyzer, false);
                                    if (id == null) continue;
                                    id.setId(indexName + "_" + (++lists));
                                    queries.add(id);
                                    EventManager.getInstance().addEventListener(idListener);
                                    log.service("Added mmbase jdbc definition " + id);
                                } else if ("analyzer".equals(name)) {
                                    String className = childElement.getAttribute("class");
                                    try {
                                        Class clazz = Class.forName(className);
                                        analyzer = (Analyzer) clazz.newInstance();
                                    } catch (Exception e) {
                                        configErrors.add("Cloud not instantiate analyzer " + className);
                                        log.error("Could not instantiate analyzer " + className + " for index '" + indexName + "', falling back to default. " + e);
                                    }
                                }
                            }
                        }
                        Indexer indexer = new Indexer(indexPath, indexName, queries, getCloud(), analyzer, readOnly);
                        for (String s : configErrors) {
                            indexer.addError(url.toString() + ": " + s);
                        }
                        configErrors.clear();
                        indexer.getDescription().fillFromXml("description", indexElement);
                        log.service("Add lucene index with name " + indexName);
                        indexerMap.put(indexName, indexer);
                        String[]  allIndexedFields = allIndexedFieldsSet.toArray(new String[0]);
                        Searcher searcher = new Searcher(indexer, allIndexedFields);
                        searcherMap.put(indexName, searcher);
                    }
                }
            } catch (Exception e) {
                log.warn("Can't read Lucene configuration: "+ e.getMessage(), e);
            }
        }
        configReadTime = new Date();
    }

    public Searcher getSearcher(String indexName) {
        if (indexName == null || indexName.equals("")) indexName = defaultIndex;
        Searcher searcher = searcherMap.get(indexName);
        if (searcher == null) {
            throw new IllegalArgumentException("Index with name " +indexName + " does not exist. Existing are " + searcherMap.keySet());
        }
        return searcher;
    }


    public void notify(NodeEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Received node event " + event);
        }
        if (scheduler != null) {
            switch(event.getType()) {
            case Event.TYPE_NEW:
            case Event.TYPE_CHANGE:
                scheduler.updateIndex("" + event.getNodeNumber(), MMBaseIndexDefinition.class);
                break;
            case Event.TYPE_DELETE:
                scheduler.deleteIndex("" + event.getNodeNumber(), MMBaseIndexDefinition.class);
                break;
            }
        }
    }
    public void notify(RelationEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Received relation event " + event);
        }
        if (scheduler != null) {
            switch(event.getType()) {
            case Event.TYPE_NEW:
            case Event.TYPE_CHANGE:
            case Event.TYPE_DELETE:
                scheduler.updateIndex("" + event.getRelationSourceNumber(), MMBaseIndexDefinition.class);
                scheduler.updateIndex("" + event.getRelationDestinationNumber(), MMBaseIndexDefinition.class);
                break;
            }
        }
    }
    public void notify(IdEvent event) {
        if (scheduler != null) {
            switch(event.getType()) {
            case Event.TYPE_DELETE:
                scheduler.deleteIndex(event.getId(), JdbcIndexDefinition.class);
                break;
            default:
                scheduler.updateIndex(event.getId(), JdbcIndexDefinition.class);
                break;

            }
        }
    }

    private static int assignmentIds = 0;

    /**
     * Queue for index operations.
     */
    // public because the constants need to be visible for the SortedBundle
    public class Scheduler extends Thread {

        public static final int READONLY         = -100;
        public static final int IDLE             = 0;
        public static final int IDLE_AFTER_ERROR = -1;
        public static final int BUSY_INDEX       = 1;
        public static final int BUSY_FULL_INDEX  = 2;

        // status of the scheduler
        private int status = IDLE;
        private Assignment assignment = null;

        // assignments: tasks to run
        private BlockingQueue<Scheduler.Assignment> indexAssignments = new DelayQueue<Scheduler.Assignment>();

        Scheduler() {
            super("Lucene.Scheduler");
            setDaemon(true);
            start();
        }

        public int getStatus() {
            return status;
        }
        public Assignment getAssignment() {
            return assignment;
        }
        public Collection<Assignment> getQueue() {
            return Collections.unmodifiableCollection(indexAssignments);
        }

        public void run() {
            log.service("Start Lucene Scheduler");
            try {
                if (initialWaitTime > 0) {
                    log.info("Sleeping " + (initialWaitTime / 1000) + " seconds for initialisation");
                    Thread.sleep(initialWaitTime);
                }
            } catch (InterruptedException ie) {
                //return;
            }
            while (mmbase != null && !mmbase.isShutdown()) {
                if (log.isTraceEnabled()) {
                    log.trace("Obtain Assignment from " + indexAssignments);
                }
                try {
                    assignment = indexAssignments.take();
                    log.debug("Running " + assignment);
                    // do operation...
                    assignment.run();
                    status = IDLE;

                } catch (InterruptedException e) {
                    log.service(Thread.currentThread().getName() +" was interruped.");
                    status = IDLE;
                    continue;
                } catch (RuntimeException rte) {
                    log.error(rte.getMessage(), rte);
                    status = IDLE_AFTER_ERROR;
                } finally {
                    assignment = null;
                }
            }
        }
        public int unAssign(int id) {
            int tot = 0;
            Assignment[] assigns = indexAssignments.toArray(new Assignment[]{}); // makes a copy, to avoid ConcurrentModification exceptions
            for (Assignment assign : assigns) {
                if (assign.getId() == id) { if (indexAssignments.remove(assign)) { tot++;} }
            }
            return tot;
        }

        public abstract class Assignment implements Runnable, Delayed {

            private final int id = assignmentIds++;

            private final long endTime = System.currentTimeMillis() + Lucene.this.waitTime;

            public int getId() {
                return id;
            }
            public int hashCode() {
                return idString().hashCode();
            }

            public boolean equals(Object o) {
                if (o == null) return false;
                if (! o.getClass().equals(getClass())) return false;
                Assignment a = (Assignment) o;
                return id == a.getId() || idString().equals(a.idString());
            }
            public long  getDelay(TimeUnit unit) {
                return unit.convert(endTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            }
            public int compareTo(Delayed o) {
                return (int) (getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
            }
            public Date getDate() {
                return new Date(endTime - Lucene.this.waitTime);
            }
            abstract String idString();
        }
        void assign(Assignment a) {
            if (assigned(a)) {
                log.service("Canceling " + a + ", because already queued");
                return;
            }
            if (a.equals(assignment)) {
                log.service("Canceling " + a + ", because it is currently running already");
                return;
            }
            indexAssignments.offer(a);

        }
        /**
         * Checks whether the given assignment is scheduled already
         */
        boolean assigned(Assignment a) {
            Assignment[] assigns = indexAssignments.toArray(new Assignment[]{}); // makes a copy, to avoid ConcurrentModification exceptions
            for (Assignment assign : assigns) {
                if (assign.equals(a)) return true;
            }
            return false;
        }


        void updateIndex(final String number, final Class klass) {
            assert klass != null;
            assign(new Assignment() {
                    public void run() {
                        log.service("Update index for " + number);
                        status = BUSY_INDEX;
                        for (Indexer indexer : indexerMap.values()) {
                            int updated = indexer.updateIndex(number, klass);
                            if (updated > 0) {
                                log.service(indexer.getName() + ": Updated " + updated + " index entr" + (updated > 1 ? "ies" : "y"));
                            }
                        }
                    }
                    public String idString() {
                        return klass.getName() + number;
                    }
                    public String toString() {
                        return "UPDATE for " + number + " " + klass;
                    }

                });
        }

        void deleteIndex(final String number, final Class klass) {
            assert klass != null;
            assign(new Assignment() {
                    public void run() {
                        log.debug("delete index for " + number); // already logged in indexer.deleteIndex
                        status = BUSY_INDEX;
                        for (Indexer indexer : indexerMap.values()) {
                            indexer.deleteIndex(number, klass);
                        }
                    }
                    public String idString() {
                        return klass.getName() + number;
                    }
                    public String toString() {
                        return "DELETE for " + number + " " + klass;
                    }
                });
        }

        void deleteIndex(final String number, final String indexName) {
            assign(new Assignment() {
                    public void run() {
                        log.service("delete index for " + number);
                        status = BUSY_INDEX;
                        Indexer indexer = indexerMap.get(indexName);
                        if (indexer == null) {
                            log.error("No such index '" + indexName + "'");
                        } else {
                            indexer.deleteIndex(number, IndexDefinition.class);
                        }
                    }
                    public String idString() {
                        return indexName + " " + number;
                    }
                    public String toString() {
                        return "DELETE for " + number + " " + indexName;
                    }
                });
        }

        private final Assignment ALL_FULL_INDEX = new Assignment() {
                Indexer indexer = null;
                public void run() {
                    status = BUSY_FULL_INDEX;
                    log.service("start full index");
                    SortedSet<String> keys = new TreeSet<String>(indexerMap.keySet());
                    for (String i : keys) {
                        indexer = indexerMap.get(i);
                        if (indexer != null) {
                            indexer.fullIndex();
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            log.info("Interrupted");
                            return;
                        }
                    }
                    indexer = null;
                }
                public String idString() {
                    return "";
                }
                public String toString() {
                    return "FULLINDEX(" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(getDate()) + (indexer == null ? "" : (", " + indexer.getName())) + ")";
                }
                public long getDelay(TimeUnit unit) {
                    return 0;
                }
            };

        void fullIndex() {
            if (status != BUSY_FULL_INDEX) {
                assign(ALL_FULL_INDEX);
                log.service("Scheduled full index");
                // only schedule a full index if none is currently busy.
            } else {
                log.service("Cannot schedule full index because it is busy with " + getAssignment());
            }
        }
        void fullIndex(final String index) {
            if (status != BUSY_FULL_INDEX || ! assignment.equals(ALL_FULL_INDEX)) {
                if (! assigned(ALL_FULL_INDEX)) {
                    // only schedule a full index if no complete full index ne is currently busy or scheduled already.
                    Assignment a = new Assignment() {
                            public void run() {
                                status = BUSY_FULL_INDEX;
                                log.service("start full index for index '" + index + "'");
                                Indexer indexer = indexerMap.get(index);
                                if (indexer == null) {
                                    log.error("No such index '" + index + "'");
                                } else {
                                    indexer.fullIndex();
                                }
                            }
                            public String idString() {
                                return index;
                            }
                            public String toString() {
                                return "FULLINDEX for " + index + " (" + getDate() + ")";
                            }
                            public long getDelay(TimeUnit unit) {
                                return 0;
                            }
                        };
                    assign(a);
                    log.service("Scheduled full index for '" + index + "'");
                } else {
                    log.service("Scheduled full index for '" + index + "' because full index on every index is scheduled already");
                }
            } else {
                log.service("Cannot schedule full index for '" + index + "' because it is busy with " + getAssignment());
            }
        }

    }

    /**
     * Main for testing
     */
    public static void main(String[] args) {
        final BlockingQueue<String> q = new LinkedBlockingQueue<String>();
        ThreadPools.jobsExecutor.execute(new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000000; i++) {
                        q.offer("" + i);
                        Thread.yield();
                    }
                    q.offer("z");
                }
            });
        ThreadPools.jobsExecutor.execute(new Runnable() {
                public void run() {
                    while(true) {
                        try {
                            String t = q.take();
                            if ("z".equals(t)) return;
                        } catch (InterruptedException ie) { System.err.println("interrupted take"); return ; };
                    }
                }
            });
        ThreadPools.jobsExecutor.execute(new Runnable() {
                public void run() {
                    while(true) {
                        String[] t = q.toArray(new String[] {});
                        System.out.print("." + q.contains("123"));
                        Thread.yield();
                    }
                }
            });
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ie) {
            System.err.println("interrupted sleep");
            return;
        }

    }


}
