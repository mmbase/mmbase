/*

 This software is OSI Certified Open Source Software.
 OSI Certified is a certification mark of the Open Source Initiative.

 The license (Mozilla version 1.0) can be read at the MMBase site.
 See http://www.MMBase.org/license

 */
package org.mmbase.jumpers;

import java.util.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.mmbase.bridge.Field;
import org.mmbase.cache.Cache;
import org.mmbase.core.CoreField;
import org.mmbase.core.event.NodeEvent;
import org.mmbase.module.core.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.storage.search.*;
import org.mmbase.util.jumpers.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.functions.*;

/**
 * Maintains jumpers for redirecting urls. The data stored in this builder is
 * used to redirect urls based ons a specific key. The jumpers builder is called
 * from the {@link org.mmbase.servlet.JumpersFilter}. <br />
 * The jumpers builder can be configured using two properties: <br />
 * <ul>
 * <li><code>JumperNotFoundURL</code> Determines the default url (such as a
 * home page or error page) when no jumper is found. If not specified nothing
 * will be done if no jumper is found.</li>
 * </ul>
 * <br />
 * XXX:Note that this builder is called directly from a servlet, and may
 * therefor be bound to the cloud context rather than a cloud. This would mean
 * that in a multi-cloud environment, this builder will be shared.
 *
 * @application Tools, Jumpers
 * @author Daniel Ockeloen
 * @author Pierre van Rooden (javadocs)
 * @author Marcel Maatkamp, VPRO Digitaal
 * @version $Id$
 */
public class Jumpers extends MMObjectBuilder {

    /**
     * Default Jump Cache Size. Customization can be done through the central
     * caches.xml Make an entry under the name "JumpersCache" with the size you
     * want.
     */
    private static final int DEFAULT_JUMP_CACHE_SIZE = 1000;

    private static final Logger log = Logging.getLoggerInstance(Jumpers.class);

    /**
     * Cache for URL jumpers.
     */
    protected Cache jumpCache = new  Cache/*<String,String>*/(DEFAULT_JUMP_CACHE_SIZE) {
            public String getName() {
                return "JumpersCache";
            }

            public String getDescription() {
                return "Cache for Jumpers";
            }
        };
    {
        jumpCache.putCache();
        addFunction(new AbstractFunction<String>("jump",
                                                 new Parameter[] {
                                                     new Parameter<String>("key", String.class, true),
                                                     new Parameter<Boolean>("reload", Boolean.class, Boolean.FALSE)
                                                 }) {
                @Override
                public String  getFunctionValue(Parameters parameters) {
                    String key = parameters.getString("key");
                    boolean reload = org.mmbase.util.Casting.toBoolean(parameters.get("reload"));
                    log.debug("Calculating jumper " + reload);
                    return getJump(key, reload);
                }
            });
    }

    /**
     * Default redirect if no jumper can be found. If this field is
     * <code>null</code>, a url will not be 'redirected' if the search for a
     * jumper failed. This may cause a 404 error on your server if the path
     * specified is unavailable. However, you may need it if other servlets rely
     * on specific paths that would otherwise be caught by the jumper servlet.
     * The value fo this field is set using the <code>JumperNotFoundURL</code>
     * property in the builder configuration file.
     */
    protected static String jumperNotFoundURL;

    private MMObjectBuilder jumpercachebuilder = null;

    // jumper calculators
    private final ChainedJumperStrategy strategy = new ChainedJumperStrategy();

    /**
     * Initializes the builder. Determines the jumper cache size, and
     * initializes it. Also determines the default jumper url.
     *
     * @return always <code>true</code>
     */
    public boolean init() {
        super.init();

        // make jumpers builder listen to all node events
        // because of chaining it may want to invalidate jumpercaches.

        MMBase.getMMBase().removeNodeRelatedEventsListener(getTableName(), this);
        org.mmbase.core.event.EventManager.getInstance().addEventListener(this);

        // cache
        jumpercachebuilder = mmb.getMMObject("jumpercache");
        if(jumpercachebuilder == null) {
            log.service("Jumpercache is not found, make sure the builder 'jumpercache' is enabled!");
        }

        setStrategies();

        jumperNotFoundURL = getInitParameter("JumperNotFoundURL");
        return true;
    }

    /**
    * Specify the calculators for jumpers.
    *
    * Calculators define the behaviour of how numbers are being translated into urls.
    * They provide the logic, whereas this builder contains the caching.
    *
    * There are 2 calculators: override and default.
    *
    * default: this is the default jumper, containing the logic as it always was. This file
    * can be copied and enhanced to fit your own need. That will then become the override
    * calculator.
    *
    * Flow: if override.calculate(number) returns an url, that one is used; if it returns
    * a null or just isnt defined in jumpers.xml, the default calculator.calculate(number)
    * is used. The default.calculate(number) will map number into a node and call
    * the method getDefaultUrl(number) of the builder of that node.
    *
    * The override class is defined in jumpers.xml as a property:
    *
    *  WEB-INF/config/builders/core/jumpers.xml
    *
    *       <properties>
    *           <property name="calculator">nl.vpro.mmbase.util.jumpers.JumperCalculator</property>
    *       </properties>
    *
    * @see {org.mmbase.util.jumpers.JumperCalculator}
    */
    private void setStrategies() {
        StringBuilder strategies = new StringBuilder();
        {
            // vpro compatibility
            String override = getInitParameter("calculator.override.strategies");
            if (override != null) {
                strategies.append(override);
            }
            String def     = getInitParameter("calculator.default.strategies");
            if (def != null) {
                if (strategies.length() > 0) strategies.append(',');
                strategies.append(def);
            }
        }
        String strats =  getInitParameter("strategies");
        if (strats != null) {
            if (strategies.length() > 0) strategies.append(',');
            strategies.append(strats);
        }

        // default calculator
        strategy.clear();
        for (String strat : strategies.toString().split(",")) {
            log.service("using " + strat);
            try {
                JumperStrategy js = (JumperStrategy) Class.forName(strat).newInstance();
                log.service("adding " + js.getClass() + " " + js);
                strategy.add(js);
            } catch(java.lang.ClassNotFoundException e) {
                log.error(e.getClass() + " " + strat + ": " + e.getMessage());
            } catch(java.lang.InstantiationException e) {
                log.error(e.getClass() + " " + strat + ": " + e.getMessage());
            } catch(java.lang.IllegalAccessException e) {
                log.error(e.getClass() + " " + strat + ": " + e.getMessage());
            } catch(Exception e) {
                log.error(e.getClass() + " " + strat + ": " + e.getMessage());
            }
        }
        log.service("Using jumper strategy " + strategy);
    }


    /**
     * @since MMBase-1.7.1
     */
    @Override
    public String getGUIIndicator(MMObjectNode node, Parameters args) {
        String field = (String) args.get("field");
        if (field == null || field.equals("url")) {
            String url = node.getStringValue("url");
            HttpServletRequest req  = (HttpServletRequest) args.get(Parameter.REQUEST);
            HttpServletResponse res = (HttpServletResponse) args.get(Parameter.RESPONSE);
            String link;
            if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("ftp:")) {
                link = url;
            } else if (!url.startsWith("/")) { // requested relative to context
                // path
                String context = req == null ? MMBaseContext.getHtmlRootUrlPath() : req.getContextPath();
                String u = context + "/" + url;
                link = res == null ? u : res.encodeURL(u);
            } else {
                String context = req == null ? MMBaseContext.getHtmlRootUrlPath() : req.getContextPath();
                // request relative to host's root
                if (url.startsWith(context + "/")) { // in this context!
                    String u = url.substring(context.length() + 1);
                    link = res == null ? u : res.encodeURL(u);
                } else { // in other context
                    link = url;
                }
            }
            link = org.mmbase.util.transformers.Xml.XMLEscape(link);
            url = org.mmbase.util.transformers.Xml.XMLEscape(url);
            return "<a href=\"" + link + "\" target=\"extern\">" + url + "</a>";
        } else {
            return super.getGUIIndicator(node, args);
        }

    }

    /**
     * Retrieves a jumper for a specified key.
     *
     * @param tok the tokenizer, in which the first token is the key to search
     * for.
     * @return the found alternate url.
     */
    public String getJump(StringTokenizer tok) {
        return getJump(tok, false);
    }
    public String getJump(StringTokenizer tok, boolean reload) {
        String key = tok.nextToken();
        return getJump(key, reload);
    }

    public String getJump(String key){
        return getJump(key, false);
    }

    // jumper.id
    // ---------
    public String getIDJumper(String key) {
        String url = null;
        int ikey = -1;
        try {
            ikey = Integer.parseInt(key);
            if(ikey >= 0)
                url = getJumpByField("id", key);
        } catch (NumberFormatException e) {
            log.debug("this key("+key+") is not a number!");
        }

        return url;
    }

    /**
     * Removes a specified key from the cache.
     *
     * @param key the key to remove
     */
    public void delJumpCache(String key) {
        delJumpCache(key, false);
    }
    // remove from memorycache and database (local change)
    public void delJumpCache(String number, boolean nodeLocalChanged) {
        jumpCache.remove(number);
        if(nodeLocalChanged) {
            jumperDatabaseCache_remove(number);
        }
    }

    // jump on content of 'name' or 'id' field
    private String getJumpByField(String fieldName, String key) {
        NodeSearchQuery query = new NodeSearchQuery(this);
        CoreField field = getField(fieldName); // "name");
        if (field == null) {
            log.error("No field " + fieldName + " in jumpers");
            return null;
        }
        StepField queryField = query.getField(field);
        StepField numberField = query.getField(getField(FIELD_NUMBER));
        query.addSortOrder(numberField); // use 'oldest' jumper
        BasicFieldValueConstraint cons = null;
        if (field.getType() == Field.TYPE_STRING) {
            cons = new BasicFieldValueConstraint(queryField, key);
        } else if (field.getType() == Field.TYPE_INTEGER) {
            try {
                cons = new BasicFieldValueConstraint(queryField, Integer.parseInt(key));
            } catch(NumberFormatException e) { log.error("this key("+key+") should be a number because field("+fieldName+") is of type int!");
                cons = null;
            }
        }
        query.setConstraint(cons);
        query.setMaxNumber(1);
        try {
            List<MMObjectNode> resultList = getNodes(query);
            if (resultList.size() > 0) {
                MMObjectNode node = resultList.get(0);
                return node.getStringValue("url");
            }
        } catch (SearchQueryException sqe) {
            log.error(sqe.getMessage());
        }
        return null;
    }

    /*
    public String getJump(String key, boolean reload) {
        String url = null;
        try {
            // invalid key
            if(key.equals(""))
                return jumperNotFoundURL;

            // cache
            if(!reload) {
                url = (String) jumpCache.get(key);
                if(url!=null) return url;
            }

            // jumper.name
            url = getNameJumper(key);
            if(url!=null) return url;

            // jumper.id
            url = getIDJumper(key);
            if(url!=null) return url;

            // jumper.number
            try { url = getNumberJumper(Integer.parseInt(key), reload); } catch(NumberFormatException e) { }
            if(url!=null) return url;

            // no jumper found
            if(url==null) {
                log.debug("this url("+key+") is not a jumper");
                url = jumperNotFoundURL;
            }
        } catch(Exception e) {
            log.fatal("Exception: jumper("+key+"): "+e.toString());
            url = jumperNotFoundURL;
        }

        return url;
    }
    */
    /**
     * Retrieves a jumper for a specified key.
     *
     * @param key the key to search for.
     * @return the found alternate url.
     */
    public String getJump(String key, boolean reload) {
        String url = null;

        if (key.equals("")) {
            url = jumperNotFoundURL;
        } else {
            if (! reload) {
                url = (String) jumpCache.get(key);
                if (log.isDebugEnabled()) {
                    if (url != null) {
                        log.debug("Jumper - Cache hit on " + key);
                    } else {
                        log.debug("Jumper - Cache miss on " + key);
                    }
                }
            }
            if (url == null) {
                // Search jumpers with name;
                url = getJumpByField("name", key);
                log.debug("jump by name '" + url + "'");
            }
            int ikey = -1;
            if (url == null) {
                try {
                    ikey = Integer.parseInt(key);
                } catch (NumberFormatException e) {}
                // Search jumpers with number (parent);
                if (ikey >= 0) {
                    url = getJumpByField("id", key);
                    log.debug("jump by id '" + url + "'");
                }
            }
            if (url == null) {
                // no direct url call its builder
                if (ikey >= 0) {
                    String cachedDatabaseUrl = null;
                    if (! reload) {
                        url = jumperDatabaseCache_get(key);
                    } else {
                        cachedDatabaseUrl = jumperDatabaseCache_get(key);
                    }
                    if (url == null) {
                        MMObjectNode node = getNode(ikey);
                        if (node != null) {
                            log.debug("Found node " + ikey);
                            synchronized(this) {
                                if (! reload) {
                                    url = (String) jumpCache.get(key);
                                    log.debug("found from jumpcache '" + url + "'");
                                } else {
                                    log.debug("requested reload so jumpcache not consulted");
                                }
                                if (url == null) {
                                    log.debug("Applying " + strategy);
                                    url = strategy.calculate(node);
                                    if (url != null) {
                                        if (! url.equals(cachedDatabaseUrl)) {
                                            jumperDatabaseCache_put(key, url);
                                        }
                                        jumpCache.put(key, url);
                                        log.debug("Found " + url);
                                        return url;
                                    }
                                    log.debug("Not found");
                                }
                            }
                        }
                    }
                }
            }
            if (url != null && url.length() > 0 && !url.equals("null")) {
                jumpCache.put(key, url);
                if (url.equalsIgnoreCase("NOREDIRECT")) { // return null if the
                    // url specified is
                    // NOREDIRECT
                    url = null;
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No jumper found for key '" + key + "'");
                }
                url = jumperNotFoundURL;
            }
        }
        return url;
    }




    // database caches
    // ---------------

    // database.get
    private String jumperDatabaseCache_get(String number) {
        if (jumpercachebuilder == null) return null;
        List nodes;
        try {
            NodeSearchQuery query = new NodeSearchQuery(jumpercachebuilder);
            StepField keyField = query.getField(jumpercachebuilder.getField("key"));
            query.setConstraint(new BasicFieldValueConstraint(keyField,number));
            nodes = jumpercachebuilder.getNodes(query);
        } catch (SearchQueryException e) {
            log.error(e.toString());
            return null;
        }

        String url = null;
        Iterator i = nodes.iterator();
        while(i.hasNext()) {
            MMObjectNode node = (MMObjectNode)i.next();
            if(url == null) {
                url = node.getStringValue("url");
                // remove double
            } else {
                log.warn("dbcache: get: multiple entries detected for number("+number+"): node("+node.getNumber()+"): key("+node.getStringValue("key")+") url("+node.getStringValue("url")+")");
                node.getBuilder().removeNode(node);
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("dbcache: get("+number+"): url("+url+")");
        }
        return url;
    }

    // database.put
    private void jumperDatabaseCache_put(String number, String url) {
        if (jumpercachebuilder == null) return;

        String oldurl = null;
        List nodes = null;
        // if contains
        try {
            NodeSearchQuery query = new NodeSearchQuery(jumpercachebuilder);
            StepField keyField = query.getField(jumpercachebuilder.getField("key"));
            query.setConstraint(new BasicFieldValueConstraint(keyField,number));
            nodes = jumpercachebuilder.getNodes(query);
        } catch (SearchQueryException e) {
            log.error(e.toString());
        }

        Iterator i = nodes.iterator();
        // then update
        if(i.hasNext()) {
            while(i.hasNext()) {
                MMObjectNode node = (MMObjectNode)i.next();
                if(oldurl==null) {
                    oldurl = node.getStringValue("url");
                    node.setValue("url",url);
                    node.commit();
                    log.debug("dbcache: put: update detected for number("+number+"): old("+oldurl+") -> new("+url+")");

                 // and remove double
                 } else {
                    log.warn("dbcache: put: multiple entries detected for number("+number+"): node("+node.getNumber()+"): key("+node.getStringValue("key")+") url("+node.getStringValue("url")+")");
                    node.getBuilder().removeNode(node);
                }
            }
        // else insert
        } else {
            if(log.isDebugEnabled()) log.debug("dbcache: put("+number+","+url+")");
            MMObjectNode jumpercachenode = jumpercachebuilder.getNewNode("jumper");
            jumpercachenode.setValue("key", "" + number);
            jumpercachenode.setValue("url", url);
            jumpercachenode.insert(MMBase.getMMBase().getMachineName());
        }
    }

    // database.remove
    private void jumperDatabaseCache_remove(String number) {
        if (jumpercachebuilder == null) return;

        List nodes = null;
        try {
            NodeSearchQuery query = new NodeSearchQuery(jumpercachebuilder);
            StepField keyField = query.getField(jumpercachebuilder.getField("key"));
            query.setConstraint(new BasicFieldValueConstraint(keyField,number));
            nodes = jumpercachebuilder.getNodes(query);
        } catch (SearchQueryException e) {
            log.error(e.toString());
        }
        if(nodes!=null && nodes.size()>0) {
            Iterator i = nodes.iterator();
            while(i.hasNext()) {
                MMObjectNode node = (MMObjectNode)i.next();
                if(log.isDebugEnabled()) log.debug("dbcache: removed("+node.getNumber()+")");
                node.getBuilder().removeNode(node);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mmbase.module.core.MMObjectBuilder#notify(org.mmbase.core.event.NodeEvent)
     */
    public void notify(final NodeEvent event) {
        if(getTableName().equals(event.getBuilderName())){
            if (log.isDebugEnabled()) {
                log.debug("Jumpers=" + event.getMachine() + " " + event.getBuilderName() + " no="
                          + event.getNodeNumber()+ " " + NodeEvent.newTypeToOldType(event.getType()));
            }
            // delete
            if(event.getType() == NodeEvent.TYPE_DELETE) {
                if(log.isDebugEnabled()) {
                    log.debug("delete detected: removing "+event.getBuilderName()+"("+event.getNodeNumber()+") from cache");
                }
                // remove cache
                jumpCache.remove("" + event.getNodeNumber());
                // locally changed: remove persistent cache
                if(mmb.getMachineName().equals(event.getMachine()))
                    jumperDatabaseCache_remove("" + event.getNodeNumber());

                // field change or relation change
            } else if(	(event.getType() == NodeEvent.TYPE_CHANGE) ||
                        (event.getType() == NodeEvent.TYPE_RELATION_CHANGE)) {
                if(event.getBuilderName().equals("jumpers")) {
                    jumpCache.remove(getNode(event.getNodeNumber()).getStringValue("name"));
                } else {
                    if(log.isDebugEnabled()) log.debug("change detected: removing "+event.getBuilderName()+"("+event.getNodeNumber()+") from cache");
                    // remove cache
                    jumpCache.remove("" + event.getNodeNumber());
                    // locally changed: remove persistent cache
                    if(mmb.getMachineName().equals(event.getMachine()))
                        jumperDatabaseCache_remove("" + event.getNodeNumber());
                }
            }
        } else {
            String nodeNumber = "" + event.getNodeNumber();
            if (event.isLocal() || jumpCache.containsKey(nodeNumber)) {
                MMObjectNode node = Jumpers.this.getNode(nodeNumber);
                if (node != null && strategy.contains(node)) {
                    Jumpers.this.delJumpCache(nodeNumber, event.isLocal());
                }
            }
        }
        super.notify(event);
    }

    @Override
    protected Object executeFunction(MMObjectNode node, String function, List arguments) {
        if (function.equals("gui")) {
            String rtn;
            if (arguments == null || arguments.size() == 0) {
                rtn = getGUIIndicator(node);
            } else {
                rtn = getGUIIndicator(node, Functions.buildParameters(GUI_PARAMETERS, arguments));
            }
            if (rtn != null) return rtn;
        }
        return super.executeFunction(node, function, arguments);
    }



}

