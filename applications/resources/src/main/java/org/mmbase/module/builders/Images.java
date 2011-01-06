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
import org.mmbase.util.*;
import org.mmbase.util.transformers.UrlEscaper;
import org.mmbase.util.transformers.Xml;
import org.mmbase.util.images.*;
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.*;
import org.mmbase.servlet.FileServlet;
import java.io.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;

/**
 * If this class is used as the class for your builder, then an 'handle' byte field is assumed to
 * contain an image. You builder will work together with 'icaches', and with imagemagick (or jai).
 *
 * This means that is has the following properties: ImageConvertClass,
 * ImageConvert.ConverterCommand, ImageConvert.ConverterRoot and MaxConcurrentRequests
 *
 * @author Daniel Ockeloen
 * @author Rico Jansen
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class Images extends AbstractImages {

    private static final Logger log = Logging.getLoggerInstance(Images.class);

    // This cache connects templates (or ckeys, if that occurs), with node numbers,
    // to avoid querying icaches.
    private static final CKeyCache templateCacheNumberCache = new CKeyCache(500) {
        @Override
        public String getName() {
            return "CkeyNumberCache";
        }
        @Override
        public String getDescription() {
            return "Connection between image conversion templates and icache node numbers";
        }
    };
    static {
        templateCacheNumberCache.putCache();
    }

    public final static Parameter[] CACHE_PARAMETERS = {
        new Parameter("template",  String.class)
    };


    public final static Parameter[] CACHEDNODE_PARAMETERS = CACHE_PARAMETERS;
    public final static Parameter[] HEIGHT_PARAMETERS = CACHE_PARAMETERS;
    public final static Parameter[] WIDTH_PARAMETERS = CACHE_PARAMETERS;
    public final static Parameter[] DIMENSION_PARAMETERS = CACHE_PARAMETERS;


    public final static Parameter[] GUI_PARAMETERS = {
        new Parameter.Wrapper(MMObjectBuilder.GUI_PARAMETERS),
        new Parameter("template", String.class)
    };



    /**
     * Supposed image type if not could be determined (configurable)
     */
    protected String defaultImageType = "jpg";

    private int maxArea = Integer.MAX_VALUE;

    /**
     * Read configurations (imageConvertClass, maxConcurrentRequest),
     * checks for 'icaches', inits the request-processor-pool.
     */
    @Override
    public boolean init() {
        if (oType != -1) return true; // inited already

        if (!super.init()) return false;

        String tmp = getInitParameter("DefaultImageType");

        if (tmp != null) {
            defaultImageType = tmp;
        }
        String ma = getInitParameter("MaxArea");
        if (ma != null && !"".equals(ma)) {
            maxArea = Integer.parseInt(ma);
        }

        ImageCaches imageCaches = (ImageCaches) mmb.getBuilder("icaches");
        if(imageCaches == null) {
            log.warn("Builder with name 'icaches' wasn't loaded. Cannot do image-conversions.");
        }

        Map<String, String> map = getInitParameters("mmbase/imaging"); // TODO, this would conflict
                                                                       // with module with name 'imaging'?
        map.put("configfile", getConfigResource());

        if (! Factory.isInited()) {
            Factory.init(map);
        } else {
            log.warn("Image conversion factory is already inited. Ignoring " + map + " of " + getTableName());
        }

        return true;
    }

    @Override
    public void shutdown() {
        templateCacheNumberCache.clear();
    }

    /**
     * The executeFunction of this builder adds the 'cache' function.
     * The cache function accepts a conversion template as argument and returns the cached image
     * node number. Using this you order to pre-cache an image.
     *
     * @since MMBase-1.6
     */
    @Override
    protected Object executeFunction(MMObjectNode node, String function, List<?> args) {
        if (log.isDebugEnabled()) {
            log.debug("executeFunction " + function + "(" + args + ") of images builder on node " + node);
        }
        if ("info".equals(function)) {
            List<Object> empty = new ArrayList<Object>();
            Map<String,String> info = (Map<String,String>) super.executeFunction(node, function, empty);
            info.put("cache", "" + CACHE_PARAMETERS + " The node number of the cached converted image (icaches node)");
            if (args == null || args.isEmpty()) {
                return info;
            } else {
                return info.get(args.get(0));
            }
        } else if ("cache".equals(function)) {
            if (args == null || args.size() < 1) {
                throw new RuntimeException("Images cache functions needs 1 argument (now: " + args + ")");
            }
            return Integer.valueOf(getCachedNode(node, (String) args.get(0)).getNumber());
        } else if ("cachednode".equals(function)) {
            try {
                if (args == null || args.size() < 1) {
                    throw new RuntimeException("Images cache functions needs 1 argument (now: " + args + ")");
                }
                return getCachedNode(node, (String) args.get(0));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        } else if ("height".equals(function)) {
            if (args.isEmpty()) {
                return Integer.valueOf(getDimension(node).getHeight());
            } else {
                return Integer.valueOf(getDimension(node, (String) args.get(0)).getHeight());
            }
        } else if ("width".equals(function)) {
            if (args.isEmpty()) {
                return Integer.valueOf(getDimension(node).getWidth());
            } else {
                return Integer.valueOf(getDimension(node, (String) args.get(0)).getWidth());
            }
        } else if ("dimension".equals(function)) {
            if (args.isEmpty()) {
                return getDimension(node);
            } else {
                return getDimension(node, (String) args.get(0));
            }
        } else {
            return super.executeFunction(node, function, args);
        }
    }
    /**
     * @since MMBase-1.7.4
     */
    protected Dimension getDimension(MMObjectNode node, String template) {
        if (template == null || template.equals("")) { // no template given, return dimension of node itself.
            return getDimension(node);
        }
        ImageCaches imageCaches = (ImageCaches) mmb.getMMObject("icaches");
        if(imageCaches == null) {
            throw new UnsupportedOperationException("The 'icaches' builder is not availabe");
        }
        MMObjectNode icacheNode = imageCaches.getCachedNode(node.getNumber(), template);
        if (icacheNode != null) {
            return imageCaches.getDimension(icacheNode);
        } else {
            // no icache available? Only return prediction.
            return  Imaging.predictDimension(getDimension(node), Imaging.parseTemplate(template));
        }
    }



    /**
     * @since MMBase-1.9.2
     */
    protected void fillImageCacheNode(MMObjectNode image, MMObjectNode icacheNode, String template) {
        final ImageCaches imageCaches = (ImageCaches) mmb.getBuilder("icaches");
        String ckey = Factory.getCKey(image.getNumber(), template).toString();
        icacheNode.setValue(Imaging.FIELD_CKEY, ckey);
        icacheNode.setValue(ImageCaches.FIELD_ID, image.getNumber());
        if (imageCaches.storesDimension() || imageCaches.storesFileSize()) {
            final Dimension dimension          = getDimension(image);
            final Dimension predictedDimension = Imaging.predictDimension(dimension, Imaging.parseTemplate(template));
            if (log.isDebugEnabled()) {
                log.debug("" + dimension + " " + ckey + " --> " + predictedDimension);
            }
            if (predictedDimension.getArea() > maxArea) {
                throw new IllegalArgumentException("The conversion '" + template + "' leads to an image which is too big (" + predictedDimension + ")");
            }
            if (imageCaches.storesDimension()) {
                icacheNode.setValue(FIELD_HEIGHT, predictedDimension.getHeight());
                icacheNode.setValue(FIELD_WIDTH,  predictedDimension.getWidth());
            }
            if (imageCaches.storesFileSize()) {
                icacheNode.setValue(FIELD_FILESIZE, Imaging.predictFileSize(dimension, getFileSize(image), predictedDimension));
            }
        }
    }
    /**
     * Returns a icache node for given image node and conversion template. If such a node does not exist, it is created.
     *
     * @since MMBase-1.8
     */
    public MMObjectNode getCachedNode(final MMObjectNode node, final String template) {
        final ImageCaches imageCaches = (ImageCaches) mmb.getBuilder("icaches");
        if(imageCaches == null) {
            throw new UnsupportedOperationException("The 'icaches' builder is not availabe");
        }

        if (node.getNumber() > 0) {
            MMObjectNode icacheNode = imageCaches.getCachedNode(node.getNumber(), template);

            if (icacheNode == null) {
                icacheNode = imageCaches.getNewNode("imagesmodule");
                fillImageCacheNode(node, icacheNode, template);
                int icacheNumber = icacheNode.insert("imagesmodule");
                if (log.isDebugEnabled()) {
                    log.debug("Inserted " + icacheNode);
                }
                if (icacheNumber < 0) {
                    throw new RuntimeException("Can't insert cache entry id=" + node.getNumber() + " key=" + template);
            }
            }
            return icacheNode;
        } else {
            final MMObjectNode icacheNode = new VirtualNode(imageCaches);
            fillImageCacheNode(node, icacheNode, template);
            return icacheNode;

        }
    }

    static Map<File, Dimension> dimensions = new java.util.concurrent.ConcurrentHashMap();
    static Timer deleter = new Timer(true);


    public static File createTemporaryFile(SerializableInputStream in, String template) throws IOException {
        long lt = 120;
        FileServlet fileServlet = FileServlet.getInstance();
        File temporaryImages = new File(FileServlet.getDirectory(), "temporary_images");
        temporaryImages.mkdirs();
        final File thumb = new File(temporaryImages, in.getName() + "." + template + ".png");
        thumb.deleteOnExit();
        Dimension dim;
        if (! thumb.exists()) {
            FileReceiver receiver = new FileReceiver(thumb);
            ImageConversionRequest req = Factory.getImageConversionRequest(in, "gif", receiver, Imaging.parseTemplate("f(png)+" + template));
            req.waitForConversion();
            dim = receiver.getDimension();
            dimensions.put(thumb, dim);
            if (lt > 0) {
                deleter.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        thumb.delete();
                        dimensions.remove(thumb);
                    }
                }, lt * 1000);
            }
        } else {
            dim = dimensions.get(thumb);
            if (dim == null) {
                dim = Factory.getImageInformer().getDimension(new FileInputStream(thumb));
                dimensions.put(thumb, dim);
            }
        }
        return thumb;
    }

    /**
     * @since MMBase-1.9.2
     */
    protected String getGuiForNewImage(MMObjectNode node, String alt, Parameters args) throws IOException  {
        FileServlet instance = FileServlet.getInstance();
        String number = node.getStringValue("_number");
        SerializableInputStream is = Casting.toSerializableInputStream(node.getInputStreamValue("handle"));
        if (is.getFileName() != null) {
            if (instance == null) {
                return "<span class='mm_gui nofileservlet'>NO FILE SERVLET</span>";
            } else {
                File thumb = createTemporaryFile(is, ImageCaches.GUI_IMAGETEMPLATE);
                log.debug("Found for " + node.getNumber() + "(" + number + "): " + thumb);
                String files = FileServlet.getBasePath("files").substring(1);
                String root = MMBaseContext.getHtmlRootUrlPath();
                String thumbUrl = root + files + "temporary_images/" + UrlEscaper.INSTANCE.transform(thumb.getName());
                String origUrl = root + files + "uploads/" + UrlEscaper.INSTANCE.transform(is.getFileName()); // hmm, is this 'uploads' certain?
                return "<a class='mm_gui' href='" + Xml.ATTRIBUTES.transform(origUrl) + "'><img src='" + Xml.ATTRIBUTES.transform(thumbUrl) +"' /></a>";
            }
        } else {
            return "<span class='mm_gui'>--</span>";
        }
    }
    /**
     * The GUI-indicator of an image-node also needs a res/req object.
     * @since MMBase-1.6
     */
    @Override
    protected String getGUIIndicatorWithAlt(MMObjectNode node, String alt, Parameters args) {
        int num = node.getNumber();
        log.debug("gui for image " + num);
        if (num < 0 || node.getChanged().contains("handle")) {
            try {
                return getGuiForNewImage(node, alt, args);
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        }
        String ses = getSession(args, num);
        StringBuilder servlet = new StringBuilder();
        HttpServletRequest req = args.get(Parameter.REQUEST);
        boolean urlConvert = false;
        if (req != null) {
            ServletContext sx = MMBaseContext.getServletContext();
            if (sx != null && "true".equals(sx.getInitParameter("mmbase.taglib.url.makerelative"))) {
                servlet.append(getServletPath(UriParser.makeRelative(new java.io.File(req.getServletPath()).getParent(), "/")));
            } else {
                servlet.append(getServletPath());
            }
            if (sx != null) {
                urlConvert = "true".equals(sx.getInitParameter("mmbase.taglib.image.urlconvert"));
            }
        } else {
            servlet.append(getServletPath());
        }
        servlet.append(usesBridgeServlet && ses != null ? "session=" + ses + "+" : "");
        String template = (String) args.get("template");
        if (template == null) template = ImageCaches.GUI_IMAGETEMPLATE;


        MMObjectNode icache;

        String imageThumb;
        if (urlConvert) {
            icache = null;
            imageThumb = servlet.toString() + node.getNumber() + "+" + UrlEscaper.INSTANCE.transform(template);
        } else {
            icache = getCachedNode(node, template);
            if (icache == null) {
                throw new RuntimeException("No icache found!");
            }
            imageThumb = servlet.toString() + (icache != null ? "" + icache.getNumber() : "");
        }

        servlet.append(node.getNumber());
        String image;
        HttpServletResponse res = args.get(Parameter.RESPONSE);
        if (res != null) {
            imageThumb = res.encodeURL(imageThumb);
            image      = res.encodeURL(servlet.toString());
        } else {
            image = servlet.toString();
        }
        String heightAndWidth;
        ImageCaches imageCaches = (ImageCaches) mmb.getMMObject("icaches");
        if (imageCaches != null && imageCaches.storesDimension()) {
            Dimension dim;
            if (icache != null) {
                dim = imageCaches.getDimension(icache);
            } else {
                List ar = new ArrayList();
                if (template != null) {
                    ar.add(template);
                }
                dim = (Dimension) node.getFunctionValue("dimension", ar);
            }
            StringBuilder buf = new StringBuilder();
            if(dim.getHeight() > 0) {
                buf.append("height=\"").append(dim.getHeight()).append("\" ");
            } else {
                log.warn("Found non-positive height.");
            }
            if (dim.getWidth() > 0) {
                buf.append("width=\"").append(dim.getWidth()).append("\" ");
            } else {
                log.warn("Found non-positive width.");
            }
            heightAndWidth = buf.toString();
        } else {
            heightAndWidth = "";
        }

        String title;
        String field = (String) args.get("field");
        if (field == null || field.equals("")) {
            // gui for the node itself.
            title = "";
        } else {
            if (storesDimension()) {
                title = " title=\"" + getMimeType(node) + " " + getDimension(node) + "\"";
            } else {
                title = " title=\"" + getMimeType(node) + "\"";
            }
        }
        if(addFileName(node, image)) {
            StringBuilder buf = new StringBuilder(image);
            buf.append('/');
            image = getFileName(node, buf).toString();
        }
        if(addFileName(node, imageThumb)) {
            StringBuilder buf = new StringBuilder(imageThumb);
            buf.append('/');
            imageThumb = getFileName(node, buf).toString();
        }


        return
            "<a href=\"" + image + "\" class=\"mm_gui\" onclick=\"window.open(this.href); return false;\"><img src=\"" + imageThumb + "\" " +
            heightAndWidth +
            "border=\"0\" alt=\"" +
            Xml.XMLAttributeEscape(alt, '\"') +
            "\"" + title + " /></a>";
    }

    @Override
    protected String getSGUIIndicatorForNode(MMObjectNode node, Parameters args) {
        if (hasField("title")) {
            return getGUIIndicatorWithAlt(node, node.getStringValue("title"), args);
        } else {
            return getGUIIndicatorWithAlt(node, "" + node.getNumber(), args);
        }
    }

    @Override
    public String getDefaultImageType() {
        return defaultImageType;
    }


    @Override
    public boolean commit(MMObjectNode node) {
        Collection<String> changed = node.getChanged();
        // look if we need to invalidate the image cache...
        boolean imageCacheInvalid = changed.contains("handle");
        // do the commit
        if(super.commit(node)) {
            // when cache is invalid, invalidate
            if(imageCacheInvalid) {
                invalidateImageCache(node);
                templateCacheNumberCache.remove(node.getNumber());
            }
            return true;
        }
        return false;
    }

    /**
     * Override the MMObjectBuilder removeNode, to invalidate the Image Cache AFTER a deletion of the
     * image node.
     * Remove a node from the cloud.
     * @param node The node to remove.
     */
    @Override
    public void removeNode(MMObjectNode node) {
        invalidateImageCache(node);
        templateCacheNumberCache.remove(node.getNumber());
        super.removeNode(node);
    }



    /* (non-Javadoc)
     * @see org.mmbase.module.core.MMObjectBuilder#notify(org.mmbase.core.event.NodeEvent)
     */
    @Override
    public void notify(NodeEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Changed " + event.getMachine() + " " + event.getNodeNumber() +
                " " + event.getBuilderName() + " "+ NodeEvent.newTypeToOldType(event.getType()));
        }
        invalidateTemplateCacheNumberCache(event.getNodeNumber());
        super.notify(event);
    }


    /**
     * Invalidate the Image Cache, if there is one, for a specific ImageNode
     * @param node The image node, which is the original
     */
    private void invalidateImageCache(MMObjectNode node) {
        ImageCaches icache = (ImageCaches) mmb.getMMObject("icaches");
        if(icache != null) {
            // we have a icache that is active...
            icache.invalidate(node);
        }
    }

    /**
     * @javadoc
     */
    void invalidateTemplateCacheNumberCache(int number) {
        templateCacheNumberCache.remove(number);
    }

    /**
     * @since MMBase-1.7
     */
    void invalidateTemplateCacheNumberCache(String ckey) {
        templateCacheNumberCache.remove(ckey);
    }




}

