/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.util.functions.*;
import org.mmbase.util.UriParser;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.util.logging.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * ImageCaches (aka as 'icaches') is a system-like builder used by
 * builders with the 'Images' class. It contains the converted images.
 *
 * @author Daniel Ockeloen
 * @author Michiel Meeuwissen
 * @version $Id: ImageCaches.java,v 1.41 2004-12-06 15:25:19 pierre Exp $
 */
public class ImageCaches extends AbstractImages {

    private static final Logger log = Logging.getLoggerInstance(ImageCaches.class);

    static final String GUI_IMAGETEMPLATE = "s(100x60)";

    private CKeyCache handleCache = new CKeyCache(128) {  // a few images are in memory cache.
            public String getName()        { return "ImageHandles"; }
            public String getDescription() { return "Handles of Images (ckey -> handle)"; }
        };

    public ImageCaches() {
        handleCache.putCache();
    }

    /**
     * Returns the original images, for which this node is a cached image.
     *
     * @since MMBase-1.6
     **/
    private MMObjectNode originalImage(MMObjectNode node) {
        return getNode(node.getIntValue("id"));
    }

    /**
     * The GUI indicator of an image can have an alt-text.
     *
     * @since MMBase-1.6
     **/

    protected String getGUIIndicatorWithAlt(MMObjectNode node, String title, Parameters a) {
        StringBuffer servlet = new StringBuffer();
        HttpServletRequest req = (HttpServletRequest) a.get(Parameter.REQUEST);
        if (req != null) {
            servlet.append(getServletPath(UriParser.makeRelative(new java.io.File(req.getServletPath()).getParent(), "/")));
        } else {
            servlet.append(getServletPath());
        }
        String ses = (String) a.get("session");
        servlet.append(usesBridgeServlet && ses != null ? "session=" + ses + "+" : "");
        MMObjectNode origNode = originalImage(node);
        String imageThumb;
        HttpServletResponse res = (HttpServletResponse) a.get(Parameter.RESPONSE);
        String heightAndWidth = "";
        if (origNode != null) {

            List cacheArgs =  new ParametersImpl(Images.CACHE_PARAMETERS).set("template", GUI_IMAGETEMPLATE);
            MMObjectNode thumb = (MMObjectNode) origNode.getFunctionValue("cachednode", cacheArgs);
            //heightAndWidth = "heigth=\"" + getHeight(thumb) + "\" with=\"" + getWidth(thumb) + "\" ";
            heightAndWidth = ""; // getHeight and getWidth not yet present in AbstractImages
            imageThumb = servlet.toString() + thumb.getNumber();
            if (res != null) {
                imageThumb = res.encodeURL(imageThumb);
            }
        } else {
            imageThumb = "";
        }
        String image      = servlet.toString() + node.getNumber();
        if (res != null) image = res.encodeURL(image);
        return "<a href=\"" + image + "\" target=\"_new\"><img src=\"" + imageThumb + "\" border=\"0\" " + heightAndWidth + "alt=\"" + title + "\" /></a>";
    }

    // javadoc inherited
    protected String getSGUIIndicatorForNode(MMObjectNode node, Parameters a) {
        MMObjectNode origNode = originalImage(node);
        return getGUIIndicatorWithAlt(node, (origNode != null ? origNode.getStringValue("title") : ""), a);
    }


    /**
     * Given a certain ckey, return the cached image node number, if there is one, otherwise return -1.
     * This functions always does a query. The caching must be done somewhere else.
     * This is done because caching on ckey is not necesarry when caching templates.
     * @since MMBase-1.6
     **/
    protected MMObjectNode getCachedNode(String ckey) {
        log.debug("Getting cached noded for " + ckey);
        List nodes;
        try {
            NodeSearchQuery query = new NodeSearchQuery(this);
            query.setMaxNumber(2); // to make sure this is a cheap query.
            StepField ckeyField = query.getField(getField("ckey"));
            query.setConstraint(new BasicFieldValueConstraint(ckeyField, ckey));
            nodes = getNodes(query);
        } catch (SearchQueryException e) {
            log.error(e.toString());
            return null;
        }

        if (nodes.size() == 0) {
            log.debug("Did not find cached images with key ("+ ckey +")");
            return null;
        }
        if (nodes.size() > 1) {
            log.warn("found more then one cached image with key ("+ ckey +")");
        }
        return (MMObjectNode) nodes.get(0);
    }

    /**
     * Returns the bytes of a cached image. It accepts a list, just
     * because it is also like this in Images.java. But of course a
     * cached image only uses the first element (number of the node).
     * It also works if the the node is a real image in stead of a
     * cached image, in which case simple the unconverted image is
     * returned.
     *
     * If the node does not exists, it returns empty byte array
     */
    public byte[] getImageBytes(List params) {
        MMObjectNode node = getNode("" + params.get(0));
        if (node == null) {
            return null;
        } else {
            return node.getByteValue("handle");
        }
    }

    /**
     * Return a @link{ ByteFieldContainer} containing the bytes and object number
     * for the cached image with a certain ckey, or null, if not cached.
     * @param ckey the ckey to search for
     * @return null, or a @link{ByteFieldContainer} object
     * @since MMBase-1.7
     */
    public ByteFieldContainer getCkeyNode(String ckey) {
        log.debug("getting ckey node with " + ckey);
        if(handleCache.contains(ckey)) {
            // found the node in the cache..
            log.debug("Found in handleCache!");
            ByteFieldContainer result = (ByteFieldContainer) handleCache.get(ckey);
            log.debug("Found number " + result.number);
        }
        log.debug("not found in handle cache, getting it from database.");
        MMObjectNode node = getCachedNode(ckey);
        if (node == null) {
            // we dont have a cachednode yet, return null
            log.debug("cached node not found for key (" + ckey + "), returning null");
            return null;
        }
        // find binary data
        byte data[] = node.getByteValue("handle");
        if (data == null) {
            // if it didn't work, also cache this result, to avoid concluding that again..
            // should this trow an exception every time? I think so, otherwise we would generate an
            // image every time it is requested, which also net very handy...
            String msg = "The node(#"+node.getNumber()+") which should contain the cached result for ckey:" + ckey + " had as value <null>, this means that something is really wrong.(how can we have an cache node with node value in it?)";
            log.error(msg);
            throw new RuntimeException(msg);
        }

        ByteFieldContainer result = new ByteFieldContainer(node.getNumber(), data);
        // is this not configurable?
        // only cache small images.
        if (data.length< (100 * 1024))  {
            handleCache.put(ckey, result);
        }
        return result;
    }

    /**
     * It is unknown where this is good for.
     * @javadoc
     */
    private String toHexString(String str) {
        StringBuffer b=new StringBuffer();
        char[] chb;
        chb=str.toCharArray();
        for (int i=0;i<chb.length;i++) {
            b.append(Integer.toString((int)chb[i],16)+",");
        }
        return b.toString();
    }

    /**
     * Invalidate the Image Cache for a specific Node
     * method only accessable on package level, since only Images should call it..
     *
     * @param node The image node, which is the original of the cached modifications
     * @since MMBase-1.7
     */
    protected void invalidate(MMObjectNode imageNode) {
        if (log.isDebugEnabled()) {
            log.debug("Going to invalidate the node, where the original node # " + imageNode.getNumber());
        }
        // first get all the nodes, which are currently invalid....
        // this means all nodes from icache where the field 'ID' == node it's number
        List nodes;
        try {
            NodeSearchQuery query = new NodeSearchQuery(this);
            StepField idField = query.getField(getField("id"));
            query.setConstraint(new BasicFieldValueConstraint(idField, new Integer(imageNode.getNumber())));
            nodes = getNodes(query);
        } catch (SearchQueryException e) {
            log.error(e.toString());
            nodes = new java.util.ArrayList(); // do nothing
        }
        Iterator i = nodes.iterator();
        while(i.hasNext()) {
            // delete the icache node
            MMObjectNode invalidNode = (MMObjectNode) i.next();
            removeNode(invalidNode);
            if (log.isDebugEnabled()) {
                log.debug("deleted node with number#" + invalidNode.getNumber());
            }
        }
        handleCache.remove(imageNode.getNumber());
    }

    /**
     * Override the MMObjectBuilder removeNode, to invalidate the LRU ImageCache, when a node gets deleted.
     * Remove a node from the cloud.
     *
     * @param node The node to remove.
     */
    public void removeNode(MMObjectNode node) {
        String ckey = node.getStringValue("ckey");
        log.service("Icaches: removing node " + node.getNumber() + " " + ckey);
        ((Images) mmb.getMMObject("images")).invalidateTemplateCacheNumberCache(node.getIntValue("id"));
        // also delete from LRU Cache
        handleCache.remove(ckey);
        super.removeNode(node);

    }

    public boolean nodeLocalChanged(String machine,String number,String builder,String ctype) {
        if (log.isDebugEnabled()) {
            log.debug("Changed " + machine + " " + number + " " + builder + " "+ ctype);
        }
        if (ctype.equals("d")) {
            handleCache.removeCacheNumber(Integer.parseInt(number));
        }
        return super.nodeLocalChanged(machine, number, builder, ctype);
    }



    /**
     * Returns the image format.
     *
     * @since MMBase-1.6
     */
    protected String getImageFormat(MMObjectNode node) {
        String format = "jpg";
        if (node != null) {
            String ckey    = node.getStringValue("ckey");
            // stupid method, I think the format must be a field of iaches table.
            int fi = ckey.indexOf("f(");
            if (fi > -1) {
                int fi2 = ckey.indexOf(")", fi);
                format = ckey.substring(fi + 2, fi2);
            }
        }
        return format;
    }

    public String getImageMimeType(List params) {
        return getImageMimeType(getNode("" + params.get(0)));
    }

    public int insert(String owner, MMObjectNode node) {
        int res = super.insert(owner, node);
        // make sure there is no such thing with this ckey cached
        ((Images) mmb.getMMObject("images")).invalidateTemplateCacheNumberCache(node.getStringValue("ckey"));
        return res;
    }



}

