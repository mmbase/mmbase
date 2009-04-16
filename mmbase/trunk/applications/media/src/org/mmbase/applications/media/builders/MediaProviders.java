/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.applications.media.builders;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.applications.media.urlcomposers.URLComposer;
import org.mmbase.applications.media.urlcomposers.URLComposerFactory;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import java.util.*;
import java.lang.reflect.Method;
import org.mmbase.util.functions.*;
import org.mmbase.bridge.*;

/**
 * A MediaProvider builder describes a service that offers a media service. The mediaprovider
 * is related to the mediasources that are available on the mediaprovider. A mediaprovider can
 * be online/offline.
 *
* @author Michiel Meeuwissen
 * @version $Id: MediaProviders.java,v 1.16 2009-04-16 13:28:21 michiel Exp $
 * @since MMBase-1.7
 */
public class MediaProviders extends MMObjectBuilder {
    private static final Logger log = Logging.getLoggerInstance(MediaProviders.class);

    public final static int STATE_ON  = 1;
    public final static int STATE_OFF = 2;


    {
        addFunction(new NodeFunction<String>("url", new Parameter[] { Parameter.REQUEST, Parameter.CLOUD }) {
                {
                    setDescription("");
                }
                public String getFunctionValue(Node node, Parameters parameters) {
                    String protocol = node.getStringValue("protocol");
                    String host = node.getStringValue("host");
                    //String rootpath = node.getStringValue("rootpath").replace("${CONTEXT}", MMBaseContext.getServletContext().getContextPath());  // servlet >= 2.5
                    String rootpath = node.getStringValue("rootpath").replace("${CONTEXT}", MMBaseContext.getHtmlRootUrlPath());  // servlet < 2.5
                    if ("".equals(host)) {
                        return rootpath;
                    } else {
                        return protocol + "://" + host + rootpath;
                    }
                }
            });
    }

    private URLComposerFactory urlComposerFactory;

    @Override
    public boolean init() {
        if (super.init()) {
            try {
                String clazz = getInitParameter("URLComposerFactory");
                if (clazz == null) {
                    clazz = org.mmbase.applications.media.urlcomposers.URLComposerFactory.class.getName();
                }
                Method m = Class.forName(clazz).getMethod("getInstance", (Class[])null);
                urlComposerFactory = (URLComposerFactory) m.invoke(null, (Object[])null);
                return true;
            } catch (Exception e) {
                log.error("Could not get URLComposerFactory because: " + e.toString());
                return false;
            }
        }
        return false;

    }



    /**
     * A MediaProvider can provide one or more URL's for every source
     * @return A List of URLComposer's
     */

    protected List<URLComposer> getURLs(MMObjectNode provider, MMObjectNode source, MMObjectNode fragment, Map<String, Object> info, List<URLComposer> urls, Set<MMObjectNode> cacheExpireObjects) {
        return urlComposerFactory.createURLComposers(provider, source, fragment, info, urls, cacheExpireObjects);
    }


    @Override
    protected String getNodeGUIIndicator(MMObjectNode node, org.mmbase.util.functions.Parameters params) {
        return node.getStringValue("name") + " " + node.getStringValue("protocol") + "://" + node.getStringValue("host") + node.getStringValue("rootpath");
    }

}
