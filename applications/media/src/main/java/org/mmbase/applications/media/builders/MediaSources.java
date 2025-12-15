 /*

 This software is OSI Certified Open Source Software.
 OSI Certified is a certification mark of the Open Source Initiative.

 The license (Mozilla version 1.0) can be read at the MMBase site.
 See http://www.MMBase.org/license
  */

package org.mmbase.applications.media.builders;

import java.util.*;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.module.core.MMObjectBuilder;

import org.mmbase.bridge.Node;

import org.mmbase.util.functions.*;
import org.mmbase.util.MimeType;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import org.mmbase.applications.media.urlcomposers.URLComposer;
import org.mmbase.applications.media.filters.MainFilter;
import org.mmbase.applications.media.Format;
import org.mmbase.applications.media.Codec;
import org.mmbase.applications.media.State;



/**
 * The MediaSource builder describes a specific type of media that can be retrieved (real/mp3/etc). Information about
 * format, quality, and status will be maintained in this object. A MediaSource belongs
 * to a MediaFragement that describes the piece of media, the MediaSource is the
 * real audio/video itself. A MediaSource is connected to provider objects that indicate
 * where the real audio/video files can be found.
 *
 * @author Rob Vermeulen
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.7
 */
public class MediaSources extends MMObjectBuilder {
    private static final Logger log = Logging.getLoggerInstance(MediaSources.class);

    // typo checks
    public static final String FUNCTION_URLS           = "urls";
    public static final String FUNCTION_FILTEREDURLS   = "filteredurls";
    public static final String FUNCTION_URL            = "url";
    public static final String FUNCTION_AVAILABLE      = "available";
    public static final String FUNCTION_FORMAT         = "format";
    public static final String FUNCTION_CODEC          = "codec";
    public static final String FUNCTION_MIMETYPE       = "mimetype";


    // parameter definitions (making use of reflection utility for functions)
    public final static Parameter[] URLS_PARAMETERS         = { new Parameter("node",  org.mmbase.bridge.Node.class),
                                                                new Parameter.Wrapper(MediaFragments.URLS_PARAMETERS) };
    public final static Parameter[] FILTEREDURLS_PARAMETERS = URLS_PARAMETERS;
    public final static Parameter[] URL_PARAMETERS          = URLS_PARAMETERS;
    public final static Parameter[] AVAILABLE_PARAMETERS    = {};
    public final static Parameter[] FORMAT_PARAMETERS       = URLS_PARAMETERS;
    public final static Parameter[] CODEC_PARAMETERS        = {};
    public final static Parameter[] MIMETYPE_PARAMETERS     = {};





    /**
     * @deprecated
     */
    public final static int    STATE_DONE     = State.DONE.getValue();
    /**
     * @deprecated
     */
    public final static int    STATE_SOURCE   = State.SOURCE.getValue();
    /**
     * @deprecated
     */
    public final static int    STATE_REMOVED =  State.REMOVED.getValue();
    public final static String STATES_RESOURCE = "org.mmbase.applications.media.builders.resources.states";

    public final static int MONO   = 1;
    public final static int STEREO = 2;




    private String defaultProvider = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean init() {
        boolean result = super.init();

        defaultProvider = getInitParameter("default.provider.alias");

        return result;
    }


    public MediaSources() {
    }

    /**
     * create a new mediasource, and relate it with specified mediafragment.
     *
     * @param mediafragment the media fragment to which this source belongs
     * @param status of the media source
     * @param format the format of the source (Real, Mp3)
     * @param channels stereo/mono
     * @param url the file name of the media source
     * @param owner creator of the new object
     * @return the new <code>MediaSource</code>
     */
    public MMObjectNode createSource(MMObjectNode mediafragment, int status, int format, int speed, int channels, String url, String owner) {
        // creating media source
        MMObjectNode source = getNewNode(owner);
        source.setValue("state", status);
        source.setValue("format", format);
        source.setValue("codec", Codec.UNKNOWN.toInt());
        source.setValue("bitrate", speed);
        source.setValue("channels",channels);
        source.setValue("url",url);
        source.insert(owner);

        // creating relation between media source and media fragment
        MMObjectNode insrel = mmb.getInsRel().getNewNode(owner);
        insrel.setValue("snumber", mediafragment.getIntValue("number"));
        insrel.setValue("dnumber", source.getIntValue("number"));
        insrel.setValue("rnumber", mmb.getRelDef().getNumberByName("related"));
        int ret = insrel.insert(owner);
        if(ret<0) {
            log.error("Cannot create relation between mediafragment and mediasource "+insrel);
        } else {
            log.debug("created "+insrel);
        }

        return source;
    }


    /**
     * Resolve the url of the mediasource. (e.g. pnm://www.mmbase.org/test/test.ra)
     *
     * @param source the media source
     * @param info extra info (i.e. HttpRequestIno, bitrate, etc.)
     * @return the url of the media source
     */
    protected String getURL(MMObjectNode source, Map<String, Object> info) {
        List<URLComposer> urls = getFilteredURLs(source, null, info);
        if (urls.size() == 0) return "[could not compose URL]";
        URLComposer ri = urls.get(0);
        return ri.getURL();
    }


    /**
     * Resolve the mimetype for a certain media source
     *
     * @param source the media source
     * @return the content type
     */
    MimeType getMimeType(MMObjectNode source) { // package because it is used in URLResolver
        return getFormat(source).getMimeType();

    }



    /**
     * Used in the editors
     */
    @Override
    public String getGUIIndicator(MMObjectNode source) {
	    return "" + Format.get(source.getIntValue("format")) + "/" + source.getStringValue("bitrate") + "/" + source.getStringValue("channels");
	/*
        List urls = getFilteredURLs(source, null, null);
        if (urls.size() == 0) return "[could not compose URL]";
        URLComposer ri = (URLComposer) urls.get(0);
        String url = ri.getURL();
        if (url.startsWith("/")) {
            url = MMBaseContext.getHtmlRootUrlPath() + url.substring(1);
        }
        if (ri.isAvailable()) {
            return "<a href='" + url + "'>" + Format.get(source.getIntValue("format")) + "</a>";
        } else {
            return "[<a href='" + url + "'>" + Format.get(source.getIntValue("format")) + "</a>]";
        }
	*/
    }

    public int getSpeed(MMObjectNode node) {
        return node.getIntValue("bitrate");
    }

    public int getChannels(MMObjectNode node) {
        return node.getIntValue("channels");
    }

    /**
     * The format field is an integer, this function returns a string-presentation
     */
    protected Format getFormat(MMObjectNode source) {
        return Format.get(source.getIntValue("format"));
    }


    protected  String getFormat(MMObjectNode source, Map<String, Object> info)   {
        log.debug("Getting format of a source.");
        List<URLComposer> urls = getFilteredURLs(source, null, info);
        if (urls.size() > 0) {
            return urls.get(0).getFormat().toString();
        } else {
            return ""; //no sources
        }
    }

    /**
     * The codec field is an integer, this function returns a string-presentation
     */
    protected Codec getCodec(MMObjectNode source) {
        return Codec.get(source.getIntValue("codec"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object executeFunction(MMObjectNode node, String function, List<?> args) {
        if (log.isDebugEnabled()) {
            log.debug("executeFunction  " + function + "(" + args + ") on mediasources " + node);
        }
        if (function.equals("info")) {
            List<Object> empty = new ArrayList<Object>();
            java.util.Map<String, String> info = (java.util.Map<String, String>) super.executeFunction(node, function, empty);
            info.put("absoluteurl", "(<??>)");
            info.put("urlresult", "(<??>) ");
            info.put(FUNCTION_URLS, "(fragment) A list of all possible URLs to this source/fragment (Really URLComposer.URLComposer's)");
            info.put(FUNCTION_FORMAT, "() Shorthand for gui(format)");
            info.put(FUNCTION_CODEC, "() Shorthand for gui(codec)");
            info.put(FUNCTION_MIMETYPE, "() Returns the mime-type for this source");
            info.put("gui", "(state|channels|codec|format|..) Gui representation of this object.");

            if (args == null || args.size() == 0) {
                return info;
            } else {
                return info.get(args.get(0));
            }
        } else if (FUNCTION_URLS.equals(function) || FUNCTION_FILTEREDURLS.equals(function)) {

            Parameters parameters = Functions.buildParameters(URLS_PARAMETERS, args);

            MMObjectNode fragment;

            Object f = parameters.get("node");
            if (f == null) {
                fragment = null;
            } else {
                if (f instanceof MMObjectNode) {
                    fragment = (MMObjectNode) f;
                } else if (f instanceof org.mmbase.bridge.Node) {
                    fragment = getNode(((org.mmbase.bridge.Node) f).getNumber());
                } else if (f instanceof String) {
                    fragment = getNode((String) f);
                } else {
                    throw new IllegalArgumentException("Argument of function " + FUNCTION_URLS + " must be a Node");
                }
            }

            if (FUNCTION_FILTEREDURLS.equals(function)) {
                return getFilteredURLs(node, fragment, MediaFragments.translateURLArguments(args, null));
            } else {
                return getURLs(node, fragment, MediaFragments.translateURLArguments(args, null), null,null);
            }
        } else if (FUNCTION_URL.equals(function)) {
            return getURL(node, MediaFragments.translateURLArguments(args, null));
        } else if (FUNCTION_AVAILABLE.equals(function)) {
            Iterator<MMObjectNode> providers = getProviders(node).iterator();
            while (providers.hasNext()) {
                // if one of the providers is online, then this source is availabe.
                MMObjectNode provider = providers.next();
                if (provider.getIntValue("state") == MediaProviders.STATE_ON) return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } else if (FUNCTION_FORMAT.equals(function)) {
            return getFormat(node, MediaFragments.translateURLArguments(args, null));
        } else if (FUNCTION_CODEC.equals(function)) {
            return getCodec(node);
        } else if (FUNCTION_MIMETYPE.equals(function)) {
            return getMimeType(node);
        } else if (args != null && args.size() > 0) {
            if (function.equals("gui")) {
                if ("state".equals(args.get(0))) {
                    String val = node.getStringValue("state");
                    ResourceBundle bundle;
                    if (args.size() > 1) {
                        bundle = ResourceBundle.getBundle(STATES_RESOURCE, new Locale((String) args.get(1), ""), getClass().getClassLoader());
                    } else {
                        bundle = ResourceBundle.getBundle(STATES_RESOURCE, new Locale(mmb.getLanguage(), ""), getClass().getClassLoader());
                    }
                    try {
                        return bundle.getString(val);
                    } catch (java.util.MissingResourceException e) {
                        return val;
                    }
                } else if ("channels".equals(args.get(0))) {
                    int val = node.getIntValue("channels");
                    switch(val) {
                        case MONO:   return "Mono";
                        case STEREO: return "Stereo";
                        default:     return "Undefined";
                    }
                } else if ("codec".equals(args.get(0))) {
                    return getCodec(node);
                } else if ("format".equals(args.get(0))) {
                    Locale locale =  new Locale(mmb.getLanguage(), "");
                    if(args.size() > 1) {
                        locale = new Locale((String) args.get(1), "");
                    }
                    return getFormat(node).getGUIIndicator(locale);
                } else if ("".equals(args.get(0)) || args.get(0) == null) {
                    return super.executeFunction(node, function, args); // call getGUIIndicator
                } else {
                    return node.getStringValue((String) args.get(0));
                }
            }
        } else {
            String arg = null;
            if (args != null && args.size() > 0) {
                arg = (String) args.get(0);
            } else {
                arg = "";
            }
            if (function.equals("absoluteurl")) {
                Map<String, Object> info = new HashMap<String, Object>();
                info.put("server", arg);
                return getURL(node, info);
            } else if (function.equals("urlresult")) {
                //return getURLResult(node, arg);
            }
        }
        log.debug("Function not matched in mediasources");
        return super.executeFunction(node, function, args);
    }

    /**
     * remove this MediaSource, check configuration and check all places where this
     * file is reproduced.
     */
    /*
    public void remove() {
    }
     */


    /**
     * Returns all possible URLs for this source. (A source can be on different providers)
     */
    protected List<URLComposer> getURLs(MMObjectNode source, MMObjectNode fragment, Map<String, Object> info, List<URLComposer> urls, Set<MMObjectNode> cacheExpireObjects) {
        if (urls == null) urls = new ArrayList<URLComposer>();
        log.debug("Getting urls for source " + source.getNumber());
        List<MMObjectNode> providers = getProviders(source);
        if (providers.size() == 0) {
            if (defaultProvider != null) {
                MMObjectNode provider = getNode(defaultProvider);
                if (provider == null) {
                    log.warn("Specified default provider '" + defaultProvider + "' is not an existing node");
                } else {
                    MediaProviders bul = (MediaProviders) provider.getBuilder(); // cast everytime, because it can be extended
                    bul.getURLs(provider, source, fragment, info, urls, cacheExpireObjects);
                }
            }

        } else {
            Iterator<MMObjectNode> i = providers.iterator();
            while (i.hasNext()) {
                MMObjectNode provider = i.next();
                if (log.isDebugEnabled()) {
                    log.debug("Found provider " + provider.getNumber() + " source: " + source.getNumber());
                }
                MediaProviders bul = (MediaProviders) provider.getBuilder(); // cast everytime, because it can be extended
                bul.getURLs(provider, source, fragment, info, urls, cacheExpireObjects);
            }
        }
        return urls;
    }

    /**
     * Returns all URLs for this source, but filtered, with the best ones on top.
     */
    protected List<URLComposer> getFilteredURLs(MMObjectNode source, MMObjectNode fragment, Map<String, Object> info) {
        List<URLComposer> urls = getURLs(source, fragment, info, null, null);
        return MainFilter.getInstance().filter(urls);
    }

    /**
     * Relates a source with given provider. Only if there is one provider matching.
     *
     * @param source the media source.
     * @param providername the name of the provider that is going to be related.
     * @param owner the owner name that is creating the relation.
     */
    public void addProvider(MMObjectNode source, String providername, String owner) {
        MMObjectBuilder providers = mmb.getMMObject("mediaproviders");

	/** should be used in 1.7
        NodeSearchQuery query = new NodeSearchQuery(providers);
        StepField namefield = query.getField(providers.getField("name"));
        BasicFieldValueConstraint constraint = new BasicFieldValueConstraint(namefield,providername);
        query.setConstraint(constraint);

        List providerlist=null;
        try{
            providerlist = providers.getNodes(query);
        } catch (SearchQueryException sqe) {
            log.error("Exception while querying "+sqe);
	}
	*/

	int providernumber = 0;
	Enumeration<MMObjectNode> e = providers.search("WHERE name='"+providername+"'");
        if (!e.hasMoreElements()) {
            log.error("No media provider found with name "+providername);
            return;
	} else {
		MMObjectNode provider = e.nextElement();
   		providernumber = provider.getIntValue("number");
		log.debug("found provider with number = "+providernumber);
	}
	if (e.hasMoreElements()) {
            log.error("Multiple media providers with name "+providername+" found");
            return;
	}

        MMObjectNode insrel = mmb.getInsRel().getNewNode(owner);
        insrel.setValue("snumber", source.getValue("number"));
        insrel.setValue("dnumber", providernumber);
        insrel.setValue("rnumber", mmb.getRelDef().getNumberByName("related"));

        int ret = insrel.insert(owner);
        if(ret<0) {
            log.error("cannot create relation "+insrel);
        } else {
            log.debug("relation created "+insrel);
        }
    }


    /**
     * Get all mediaproviders belonging to this mediasource
     * @param source the mediasource
     * @return All mediaproviders related to the given mediasource
     */
    protected List<MMObjectNode> getProviders(MMObjectNode source) {
        if (log.isDebugEnabled()) {
            log.debug("mediasource " + source.getStringValue("number"));
        }
        if (source.getNumber() > 0) {
            return source.getRelatedNodes("mediaproviders");
        } else {
            // e.g. new nodes don't have relations
            return new ArrayList<MMObjectNode>();
        }
    }


    private void checkFields(MMObjectNode node) {
        if (log.isDebugEnabled()) {
            log.debug("format: " + node.getValue("format"));
        }
        if (node.getValue("format") == null || node.getIntValue("format") == -1) {
            String url = node.getStringValue("url");
            int dot = url.lastIndexOf('.');
            if (dot > 0) {
                String extension = url.substring(dot + 1).toLowerCase();
                if (log.isDebugEnabled()) {
                    log.debug("Format of was unset, trying to autodetect by using 'url' field '" + url + "' with extension '" + extension + "'");
                }
                node.setValue("format", Format.get(extension).toInt());
            }
        }
    }

    @Override
    public boolean setValue(MMObjectNode node,String fieldName, Object value) {
        if ("format".equals(fieldName)) {
        }
        return super.setValue(node, fieldName, value);
    }

    /**
     * The commit can be used to automatically fill unfilled fields. For
     * example the format can well be guessed by the URL.
     * (todo: which of commit,insert must be overriden?)
     */
    @Override
    public synchronized boolean commit(MMObjectNode node) {
        checkFields(node);
        return super.commit(node);
    }

    @Override
    public synchronized int insert(String owner, MMObjectNode node) {
        checkFields(node);
        return super.insert(owner, node);
    }



}
