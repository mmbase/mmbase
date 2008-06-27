/*

 This software is OSI Certified Open Source Software.
 OSI Certified is a certification mark of the Open Source Initiative.

 The license (Mozilla version 1.0) can be read at the MMBase site.
 See http://www.MMBase.org/license

 */
package org.mmbase.module.builders;

import java.util.*;

import org.mmbase.core.event.NodeEvent;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;

/**
 * @javadoc
 * @author Daniel Ockeloen
 * @version $Id: Versions.java,v 1.22 2007-12-06 08:03:47 michiel Exp $
 */
public class Versions extends MMObjectBuilder implements MMBaseObserver {

    private static final Logger log = Logging.getLoggerInstance(Versions.class);

    private Hashtable<String, Vector<VersionCacheNode>> cacheVersionHandlers = new Hashtable<String, Vector<VersionCacheNode>>();

    private Map<String, Integer> versionsCache = new Hashtable<String, Integer>();

    private boolean initialized = false;

    /**
     * @javadoc
     */
    public boolean init() {
        if (!initialized) {
            super.init();
            startCacheTypes();
            List<MMObjectNode> versionNodes = getNodes();
            for (MMObjectNode versionNode : versionNodes) {
                String name = versionNode.getStringValue("name");
                String type = versionNode.getStringValue("type");
                Integer number = versionNode.getNumber();

                String key = type + "_" + name;
                if (versionsCache.containsKey(key)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("versions node[number,version,maintainer]:");
                    sb.append("[");
                    sb.append(versionNode.getNumber());
                    sb.append(",");
                    sb.append(versionNode.getIntValue("version"));
                    sb.append(",");
                    sb.append(versionNode.getStringValue("maintainer"));
                    sb.append("]");
                    log.warn("more than one version was found for " + type + " with name " + name + " ." + sb.toString());
                } else {
                    versionsCache.put(key, number);
                }
            }

            initialized = true;
        }

        return true;
    }

    /**
     * @param name the name of the component we want to get know the version information about
     * @param type the type of tye component we want to get information about (application/builder)
     * @return the node that contains version information about "name", "type" or null if no version
     * information is avaiable
     * @throws SearchQueryException
     * @since MMBase-1.7
     */
    public MMObjectNode getVersionNode(String name, String type) {
        MMObjectNode retval = null;

        String key = type + "_" + name;
        if (versionsCache.containsKey(key)) {
            Integer number = versionsCache.get(key);
            retval = getNode(number.intValue());
        }
        return retval;
    }

    /**
     * @javadoc
     */
    public int getInstalledVersion(String name, String type) {
        MMObjectNode node = getVersionNode(name, type);
        if (node == null) { return -1; }
        return node.getIntValue("version");
    }

    /**
     * @javadoc
     */
    public void setInstalledVersion(String name, String type, String maintainer, int version) {

        MMObjectNode node = getVersionNode(name, type);
        if (node == null) {
            node = getNewNode("system");
            node.setValue("name", name);
            node.setValue("type", type);
            node.setValue("maintainer", maintainer);
            node.setValue("version", version);
            int number = insert("system", node);

            String key = type + "_" + name;
            versionsCache.put(key, number);
        } else {
            node.setValue("maintainer", maintainer);
            node.setValue("version", version);
            node.commit();
        }
    }

    /**
     * @javadoc
     */
    public void updateInstalledVersion(String name, String type, String maintainer, int version) {
        setInstalledVersion(name, type, maintainer, version);
    }

    /**
     * @javadoc
     */
    public void startCacheTypes() {
        // is there a CacheVersion file ?
        String cacheversionfile = getInitParameter("cacheversionfile");

        if (cacheversionfile != null && !cacheversionfile.equals("")) {
            VersionXMLCacheNodeReader parser = new VersionXMLCacheNodeReader(cacheversionfile);
            parser.setBuilder(this);
            cacheVersionHandlers = parser.getCacheVersions(cacheVersionHandlers);
        }
        for (Enumeration<String> e = cacheVersionHandlers.keys(); e.hasMoreElements();) {
            String bname = e.nextElement();
            MMObjectBuilder builder = mmb.getBuilder(bname);
            if (builder != null) {
                builder.addLocalObserver(this);
                builder.addRemoteObserver(this);
            } else {
                log.error("ERROR: Can't find builder : " + bname);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.mmbase.module.core.MMObjectBuilder#notify(org.mmbase.core.event.NodeEvent)
     */
    public void notify(NodeEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Changed " + event.getMachine() + " " + event.getNodeNumber() + " "
                + event.getBuilderName() + " " + NodeEvent.newTypeToOldType(event.getType()));
        }
        String builder = event.getBuilderName();
        Vector<VersionCacheNode> subs = cacheVersionHandlers.get(builder);
        int inumber = event.getNodeNumber();
        if (subs != null) {
            for (VersionCacheNode cnode : subs) {
                cnode.handleChanged(builder, inumber);
            }
        }
        super.notify(event);
    }
}
