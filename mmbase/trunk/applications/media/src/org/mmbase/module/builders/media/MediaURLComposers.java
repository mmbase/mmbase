/*
  
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
  
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
  
*/

package org.mmbase.module.builders.media;

import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import java.util.*;
import java.net.*;


/**
 * Provides the functionality to create URL's (or URI's) for a certain
 * fragment, source, provider combination.
 *
 * @author Michiel Meeuwissen
 * @version $Id: MediaURLComposers.java,v 1.6 2003-01-21 17:46:24 michiel Exp $
 * @since MMBase-1.7
 */
public class MediaURLComposers extends MMObjectBuilder {    
    private static Logger log = Logging.getLoggerInstance(MediaURLComposers.class.getName());
    
    protected class RamResponseInfo extends FragmentResponseInfo {
        protected  String          url;
        RamResponseInfo(String url, MMObjectNode source, MMObjectNode fragment, Map info) {
            super(source, fragment, info);
            this.url = url;
        }
        public String  getURL() {
            return url + "?fragment=" + fragment.getNumber() + "&source=" + source.getNumber();
        }
        public Format  getFormat()   { 
            return Format.RAM; 
        } 
        public boolean equals(Object o) {
            if (o instanceof RamResponseInfo) {
                RamResponseInfo r = (RamResponseInfo) o;
                return url.equals(r.url) && fragment.getNumber() == r.fragment.getNumber() && source.getNumber() == r.source.getNumber() && info.equals(r.info);
            }
            return false;
        }
    }

    protected class MediaResponseInfo extends FragmentResponseInfo {
        protected MMObjectNode    composer;
        protected MMObjectNode    provider;

        MediaResponseInfo(MMObjectNode composer, MMObjectNode provider, MMObjectNode source, MMObjectNode fragment, Map info) {
            super(source, fragment, info);
            this.composer = composer; 
            this.provider = provider;
        }
        public String       getURL() {
            StringBuffer args = new StringBuffer(composer.getStringValue("protocol") + "://" + provider.getStringValue("host") + composer.getStringValue("rootpath") + source.getStringValue("url"));
            return getArgs(args).toString();
        }
        public boolean      isAvailable() { 
            boolean res = super.isAvailable();
            boolean providerAvailable = (provider.getIntValue("state") == MediaProviders.STATE_ON); // todo: use symbolic constant
            return res && providerAvailable;
        }

    } // inner class MediaResponseInfo

    
    protected ResponseInfo createResponseInfo(MMObjectNode composer, MMObjectNode provider, MMObjectNode source, MMObjectNode fragment, Map info) {
        return new MediaResponseInfo(composer, provider, source, fragment, info);           
    };



    /**
     * Returns just an abstract respresentation of an urlcomposer object. Only useful for editors.
     */

    public String getGUIIndicator(MMObjectNode n) {
        return n.getStringValue("protocol") + "://" + "&lt;host&gt;" +  n.getStringValue("rootpath");
    }

    /** 
     * A MediaURLComposer can construct one or more URL's for every
     * fragment/source/provider/composer/info combination
     *
     * For this all nodes of importance can be used
     * (form the composer to fragment and possibly some other
     * preferences stored in the Map argument)..
     * 
     * @throws IllegalArgumentException if provider or source is null
     * @returns A List of ResponseInfo's
     */

     public List getURLs(MMObjectNode composer, MMObjectNode provider, MMObjectNode source, MMObjectNode fragment, Map info) {
         if (provider == null) throw new IllegalArgumentException("Cannot create URL without a provider");
         if (source   == null) throw new IllegalArgumentException("Cannot create URL without a source");
         List result       = new ArrayList();
         result.add(createResponseInfo(composer, provider, source, fragment, info));
         if ( ((MediaSources) source.parent).getFormat(source) == Format.RM) {
             ResponseInfo r = new RamResponseInfo("/rams", source, fragment, info);
             if (! result.contains(r)) result.add(r);
         }
         return result;
     }

    
    protected Object executeFunction(MMObjectNode node, String function, List args) {
        if (log.isDebugEnabled()) {
            log.debug("Executing function " + function + " on node " + node.getNumber() + " with argument " + args);
        } 
        
        if (function.equals("info")) {
            List empty = new ArrayList();
            Map info = (Map) super.executeFunction(node, "info", empty);
            if (args == null || args.size() == 0) {
                return info;
            } else {
                return info.get(args.get(0));
            }
        }
        return super.executeFunction(node, function, args);
    }

}
