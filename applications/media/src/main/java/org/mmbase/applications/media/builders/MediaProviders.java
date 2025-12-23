/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.applications.media.builders;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.mmbase.applications.media.urlcomposers.URLComposer;
import org.mmbase.applications.media.urlcomposers.URLComposerFactory;
import org.mmbase.bridge.Cloud;
import static org.mmbase.bridge.ContextProvider.*;
import org.mmbase.bridge.Node;
import org.mmbase.module.core.MMBaseContext;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.util.functions.GuiFunction;
import org.mmbase.util.functions.NodeFunction;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A MediaProvider builder describes a service that offers a media service. The mediaprovider
 * is related to the mediasources that are available on the mediaprovider. A mediaprovider can
 * be online/offline.
 *
* @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.7
 */
public class MediaProviders extends MMObjectBuilder {
    private static final Logger log = Logging.getLoggerInstance(MediaProviders.class);

    public final static int STATE_ON  = 1;
    public final static int STATE_OFF = 2;


    {
        final NodeFunction urlFunction = new NodeFunction<String>("url", new Parameter[] { Parameter.REQUEST, Parameter.CLOUD }) {
            {
                setDescription("");
            }
            @Override
            public String getFunctionValue(Node node, Parameters parameters) {
                String protocol;
                HttpServletRequest req = parameters.get(Parameter.REQUEST);
                if (req == null) {
                    req = getCurrentRequest();
                }
                String nodesProtocol =  node.getStringValue("protocol");

                if (req != null) {
                    protocol =  req.getScheme();
                    if (!nodesProtocol.isEmpty() && ! nodesProtocol.equals(protocol)) {
                        log.warn("Protocol mismatch between request (" + protocol + ") and node (" + nodesProtocol + ")");
                    }
                } else {
                    protocol = nodesProtocol;
                }

                String host = node.getStringValue("host");
                int port = -1;
                if ("".equals(host)) {
                    if (req != null) {
                        host = req.getServerName();
                        port = req.getServerPort();
                    } else {
                        log.debug("No request found");

                    }
                }
                //String rootpath = node.getStringValue("rootpath").replace("${CONTEXT}", MMBaseContext.getServletContext().getCo());  // servlet >= 2.5
                String rootpath = node.getStringValue("rootpath")
                    .replace("${CONTEXT}", MMBaseContext.getHtmlRootUrlPath());  // servlet < 2.5
                if ("".equals(host)) {
                    return rootpath;
                } else {
                    StringBuilder buf = new StringBuilder(protocol);
                    buf.append("://");
                    buf.append(host);
                    if (port > -1) {
                        if (("http".equals(protocol) && port != 80) ||
                            ("https".equals(protocol) && port != 443)) {
                            buf.append(':');
                            buf.append(port);
                        }
                    }
                    buf.append(rootpath);
                    return buf.toString();
                }
            }
        };
        addFunction(urlFunction);


        addFunction(new GuiFunction() {
                @Override
                public String getFunctionValue(Node node, Parameters parameters) {
                    String fieldName = parameters.get(Parameter.FIELD);
                    if (fieldName == null || fieldName.length() == 0) {
                        Parameters urlParams = urlFunction.createParameters();
                        urlParams.setAllIfDefined(parameters);
                        return node.getStringValue("name") + " " + urlFunction.getFunctionValue(urlParams);
                    } else {
                        return super.getFunctionValue(node, parameters);
                    }
                }
            });
    }

    private URLComposerFactory urlComposerFactory;


    private HttpServletRequest getCurrentRequest() {

        // TODO: a bit of a hack, the function in MediaFragments should be updated to decently pass Request objects as a parameters
        //Cloud cloud = node.getCloud();
        Cloud cloud = org.mmbase.bridge.util.CloudThreadLocal.currentCloud();
        if (cloud != null) {
            return  (HttpServletRequest) cloud.getProperty(org.mmbase.bridge.Cloud.PROP_REQUEST);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No cloud found ", new Exception());
            }
            cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null);
            return  (HttpServletRequest) cloud.getProperty(org.mmbase.bridge.Cloud.PROP_REQUEST);
        }
    }

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



}
