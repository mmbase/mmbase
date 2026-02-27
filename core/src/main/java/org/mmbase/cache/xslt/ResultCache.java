/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache.xslt;

import org.mmbase.cache.Cache;

import javax.xml.transform.*;
import java.util.*;
import org.w3c.dom.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Caches the results of XSL transformations.
 *
 * @todo Cache entries must be invalidated if XSL template changes (now getSystemId is used as cache
 * entry). See TemplatesCache (which uses a FileWatcher).
 *
 * @author  Michiel Meeuwissen
 * @version $Id$
 * @since   MMBase-1.6
 */
public class ResultCache extends Cache<String, String> {

    private static Logger log = Logging.getLoggerInstance(ResultCache.class);

    private static int cacheSize = 50;
    private static ResultCache cache;


    @Override
    protected int getDefaultMaxEntrySize() {
        return 1500;
    }

    /**
     * Returns the XSLT Result cache.
     */
    public static ResultCache getCache() {
        return cache;
    }

    static {
        cache = new ResultCache(cacheSize);
        cache.putCache();
    }

    @Override
    public String getName() {
        return "XSLTResults";
    }
    @Override
    public String getDescription() {
        return "XSL Transformation Results";
    }

    /**
     * Creates the XSL Result Cache.
     */
    private ResultCache(int size) {
        super(size);
    }

    /**
     * You can only put Source/Templates values in the cache, so this throws an Exception.
     *
     * @throws RuntimeException
     **/

    public String put(Object key, Templates value) {
        throw new RuntimeException("wrong types in cache");
    }


    /**
     * Generating a key for a document. Keep it simple...
     *
     * @todo Generate this key faster and smaller
     */
    @SuppressWarnings("fallthrough")
    private StringBuilder append(StringBuilder buf, Node node) {
        switch(node.getNodeType()) {
        case Node.ATTRIBUTE_NODE:
            buf.append(node.getNodeName()).append(node.getNodeValue());
            break;
        case Node.ELEMENT_NODE: {
            NodeList nl = node.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                append(buf, nl.item(i));
            }
        }
        case Node.ENTITY_NODE:
        case Node.ENTITY_REFERENCE_NODE:
            buf.append(node.getNodeName());
            break;
        case Node.CDATA_SECTION_NODE:
        case Node.TEXT_NODE:
            buf.append(node.getNodeValue().hashCode());
            break;
        default:
            log.debug("Unknown nodetype " + node.getNodeType());
            break;
        }

        return buf;
    }
    /**
     * Generates the key which is to be used in the Cache Map.
     *
     * @todo Generate this key faster and smaller
     */
    private String getKey(Source xsl, Map params, Properties props, Document src) {
        if (xsl.getSystemId() == null) {
            log.warn("Cannot generate key for XSLT Result cache, because systemid is null");
            return null;
        }
        StringBuilder key = new StringBuilder(xsl.getSystemId());
        key.append('/');
        if (params != null) {
            key.append(params.toString());
        }
        key.append('/');
        if (props != null) {
          key.append(props.toString());
        }
        key.append('/');
        return append(key, src.getDocumentElement()).toString();
    }

    /**
     * This is an intelligent get, which also does the put if it
     * cannot find the requested result. So, it never returns null.
     *
     * @param temp The Templates from which the transformer must be created (if necessary)
     * @param xsl  The XSL Source. This only used to produce the key, because with the Templates it
     *             is difficult
     * @param params  Parameters for the XSL Transformation
     * @param src     The Document which must be transformed.
     * @return The transformation result. It does not return null.
     */
    public String get(Templates temp, Source xsl, Map params, Properties props, Document src) {
        String key = null;
        String result = null;
        if (isActive()) {
            key = getKey(xsl, params, props, src);
            if (key != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Getting result of XSL transformation: " + key);
                }
                result = get(key);
            }
        }
        if (result == null) {
            try {
                // do the transformation, and cache the result if cache is active:
                Transformer transformer = temp.newTransformer();
                // add the params:
                if (params != null) {
                    Iterator i = params.entrySet().iterator();
                    while (i.hasNext()) {
                        Map.Entry entry = (Map.Entry) i.next();
                        transformer.setParameter((String) entry.getKey(), entry.getValue());
                    }
                }
                if (props != null) {
                    transformer.setOutputProperties(props);
                }

                java.io.StringWriter res = new java.io.StringWriter();
                transformer.transform(new javax.xml.transform.dom.DOMSource(src),
                                      new javax.xml.transform.stream.StreamResult(res));
                result = res.toString();
            } catch (TransformerException e) {
                result = e.toString();
            }
            // if result is not too big, then it can be cached:
            if (isActive() && key != null) {
                if (result.length() < getMaxEntrySize()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Put xslt Result in cache with key " + key);
                    }
                    super.put(key, result);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("xslt Result of key " + key.substring(100) + " is too big to put in cache. " + result.length() + " >= " +  getMaxEntrySize());
                    }
                }
            }

        }

        return result;

    }

}
