 /*
  
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
  
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
  
  */

package org.mmbase.module.builders.media;

import java.util.*;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;

import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.module.core.MMObjectNode;
import javax.servlet.http.HttpServletRequest;

import org.mmbase.util.FileWatcher;
import org.mmbase.util.StringObject;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.media.MediaProviderFilter;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * The MediaSource class describes pieces of media (audio / video). Information about
 * format, quality, and status will be maintained in this object. A MediaSource belongs
 * to a MediaFragement that describes the piece of media, the MediaSource is the
 * real audio/video itself. A MediaSource is connected to provider objects that indicate
 * where the real audio/video files can be found.
 *
 *
 */
public class MediaSource extends MMObjectBuilder {
    
    
    private static Logger log = Logging.getLoggerInstance(MediaSource.class.getName());
    
    // Audio formats
    public final static int MP3_FORMAT         = 1;
    public final static int RA_FORMAT          = 2;
    public final static int WAV_FORMAT         = 3;
    public final static int PCM_FORMAT         = 4;
    public final static int MP2_FORMAT         = 5;
    public final static int SURESTREAM_FORMAT  = 6;
    
    // Video formats
    public final static int MPG_FORMAT = 11;
    public final static int RM_FORMAT  = 12;
    public final static int MOV_FORMAT = 13;
    
    // Status
    public final static int REQUEST = 1;
    public final static int BUSY = 2;
    public final static int DONE = 3;
    public final static int SOURCE = 4;
    
    private final static int MONO = 1;
    private final static int STEREO = 2;
    
    // MediaProviderFilter helps by resolving the best media provider
    private static MediaProviderFilter mediaProviderFilter = null;
    
    // MediaProviderBuilder
    private static MediaProvider mediaProviderBuilder = null;
    
    
    private org.mmbase.cache.Cache cache = new org.mmbase.cache.Cache(50) {
        public String getName()        { return "mediasource cache"; }
        public String getDescription() { return "If the server gives ram's you can read them, results are stored in this cache."; }
    };
    private Map    servers;
    private Map    descriptions;
    private String defaultServer;
    
    
    private FileWatcher configWatcher = new FileWatcher(true) {
        protected void onChange(File file) {
            readConfiguration(file);
        }
    };
    
    public boolean init() {
        boolean result = super.init();
        
        // set the mediaproviderfilter
        if(mediaProviderFilter==null) {
            mediaProviderFilter = new MediaProviderFilter(this);
        }
        
        // Retrieve a reference to the MediaProvider builder
        if(mediaProviderBuilder==null) {
            mediaProviderBuilder = (MediaProvider) mmb.getMMObject("mediaproviders");
            
            if(mediaProviderBuilder==null) {
                log.error("Builder mediaproviders is not loaded.");
            } else {
                log.debug("The builder mediaproviders is retrieved.");
            }
        }
        return result;
    }
    
    
    public MediaSource() {
        
        /*
        cache.putCache();
        log.debug("static init");
        File configFile = new File(org.mmbase.module.core.MMBaseContext.getConfigPath(), "mediaservers.xml");
        if (! configFile.exists()) {
            log.warn("Configuration file for mediasources " + configFile + " does not exist");
        }
        readConfiguration(configFile);
        configWatcher.add(configFile);
        configWatcher.setDelay(10 * 1000); // check every 10 secs if config changed
        configWatcher.start();
         */
    }
    
    /**
     * After changing the configuration file, you could call this
     * method. (No need to restart MMBase then).
     **/
    
    private void readConfiguration(File configFile) {
        servers      = new HashMap();
        descriptions = new HashMap();
        try {
            log.debug("reading " + configFile);
            DOMParser parser = new DOMParser();
            parser.parse(configFile.toString());
            
            Document doc = parser.getDocument();
            org.w3c.dom.Node n = doc.getFirstChild();
            while (n != null) {
                if (n.getNodeName().equals("mediaservers")) {
                    NamedNodeMap map1= n.getAttributes();
                    defaultServer = map1.getNamedItem("default").getNodeValue();
                    
                    org.w3c.dom.Node n2 = n.getFirstChild();
                    while (n2 != null) {
                        if (n2.getNodeName().equals("server")) {
                            NamedNodeMap map= n2.getAttributes();
                            String id      = map.getNamedItem("id").getNodeValue();
                            
                            descriptions.put(id, map.getNamedItem("description").getNodeValue());
                            servers.put(id, n2.getFirstChild().getNodeValue());;
                            // check:
                            StringObject s = new StringObject((String)servers.get(id));
                            if (s.indexOf("%s", 0) < 0 ) {
                                throw new RuntimeException("No %s in servert");
                            }
                            
                            if (log.isDebugEnabled()) {
                                log.info("found script in configuration file mediaservers.xml. Id: " + id + " source: " + servers.get(id));
                            }
                        }
                        n2 = n2.getNextSibling();
                    }
                }
                n = n.getNextSibling();
            }
        } catch (org.xml.sax.SAXException e) {
            String message = "Error reading mediaservers.xml " + e.toString();
            throw new RuntimeException(message);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error reading mediaservers.xml " + e.toString());
        }
    }
    
    
    
    
    /**
     * Resolves the url
     */
    protected String getURL(MMObjectNode node, String srv) {
        if (srv == null || srv.equals("")) srv = defaultServer;
        log.debug("Getting URL for node " + node + " and server " + srv);
        String serverTemplate = (String) servers.get(srv); // not yet decided how to do anything else then default
        if (serverTemplate == null) {
            throw new RuntimeException("No server with id = " + serverTemplate + " is not defined");
        }
        
        StringObject serverObj = new StringObject(serverTemplate);
        serverObj.replace("%s", node.getStringValue("url"));
        if (log.isDebugEnabled()) log.debug("new url: " + serverObj.toString() + " with " + node.getStringValue("url"));
        return  serverObj.toString();
    }
    
    /**
     * Gets the first line of a ram file. This can be needed in smil.
     */
    protected String getURLResult(MMObjectNode node, String src) {
        String url = getURL(node, src);
        String result = (String) cache.get(url);
        if (result == null) {
            try {
                URL u = new URL(url);
                URLConnection con = u.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                result = in.readLine();
                in.close();
            } catch (java.net.MalformedURLException e) {
                throw new RuntimeException(e.toString());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e.toString());
            }
            cache.put(url, result);
        }
        return result;
    }
    
    /**
     * used in the editors
     */
    public String getGUIIndicator(MMObjectNode node) {
        return node.getStringValue("url");
    }
    
    public int getSpeed(MMObjectNode node) {
        return node.getIntValue("speed");
    }
    
    public int getChannels(MMObjectNode node) {
        return node.getIntValue("channels");
    }
    
    
    public String getGUIIndicator(String field, MMObjectNode node) {
        return "" + getValue(node, "str("+field+")");
    }
    
    /**
     * return some human readable strings
     */
    public Object executeFunction(MMObjectNode node, String function, String field) {
        if (log.isDebugEnabled()) log.debug("executeFunction  " + function + "(" + field + ") on" + node);
        if (function.equals("str")) {
            if (field.equals("status")) {
                int val=node.getIntValue("status");
                switch(val) {
                    case REQUEST: return "Request";
                    case BUSY: return "Busy";
                    case DONE: return "Done";
                    case SOURCE: return "Source";
                    default: return "Undefined";
                }
            } else if (field.equals("channels")) {
                int val=node.getIntValue("channels");
                switch(val) {
                    case MONO: return "Mono";
                    case STEREO: return "Stereo";
                    default: return "Undefined";
                }
            } else if (field.equals("format")) {
                int val=node.getIntValue("format");
                switch(val) {
                    case MP3_FORMAT: return "mp3";
                    case RA_FORMAT: return "ra";
                    case WAV_FORMAT: return "wav";
                    case PCM_FORMAT: return "pcm";
                    case MP2_FORMAT: return "mp2";
                    case SURESTREAM_FORMAT: return "g2/sure";
                    case MPG_FORMAT: return "mpg";
                    case RM_FORMAT: return "Rm";
                    case MOV_FORMAT: return "Mov";
                    default: return "Undefined";
                }
            } else if (field.equals("issurestream")) {
                int val=node.getIntValue("issurestream");
                switch( val ) {
                    case 0	: return "false";
                    case 1	: return "true";
                    default	: return "Undefined";
                }
            } else {
                return field;
            }
        } else if (function.equals("absoluteurl")) {
            return getURL(node, field);
        } else if (function.equals("urlresult")) {
            return getURLResult(node, field);
        }
        return super.executeFunction(node, function, field);
    }
    
    /**
     * remove this MediaSource, check configuration and check all places where this
     * file is reproduced.
     */
    public void remove() {
    }
    
    
    /**
     * get the url of the mediasource. This url will be created by the MediaProvider
     * and an uriFilter.
     * @param mediafragment the mediafragment (maybe not needed...)
     * @param mediasource the mediasource
     * @param request the request of the user
     * @param wantedspeed
     * @param wantedchannels
     * @return the url
     */
    public String getUrl(MMObjectNode mediafragment, MMObjectNode mediasource, HttpServletRequest request, int wantedspeed, int wantedchannels) {
        
        // Find the provider for the url
        MMObjectNode mediaProvider = mediaProviderFilter.filterMediaProvider(mediasource, request, wantedspeed, wantedchannels);
        if(mediaProvider==null) {
           log.error("Cannot selected mediaprovider, check mediaproviderfilter configuration");
           return null;
        } 
        
        log.debug("Selected mediaprovider is "+mediaProvider.getNumber());
        // Get the protocol and the hostinfomation of the provider.
        String firstPartUrl = mediaProviderBuilder.getProtocol(mediasource) + mediaProvider.getStringValue("rooturl");
        
        return firstPartUrl+"/rest";
    }
    
    /**
     * get all mediaproviders belonging to this mediasource
     * @param mediasource the mediasource
     * @return All mediaproviders related to the given mediasource
     */
    public Vector getMediaProviders(MMObjectNode mediasource) {
        return mediasource.getRelatedNodes("mediaproviders");
    }
}
