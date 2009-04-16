/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */

package org.mmbase.applications.media.urlcomposers;

import org.mmbase.applications.media.builders.MediaProviders;
import org.mmbase.applications.media.builders.MediaSources;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.util.HashCodeUtil;
import org.mmbase.applications.media.Format;

import java.util.*;

/**
 * URLComposer is a wrapper/container class  around an URL.  It contains besides the
 * URL some extra meta information about it, like the original source
 * object of the resource it represents and if it is currently
 * available or not.  An URL can be unavailable because of two
 * reasons: Because the provider is offline, or because the fragment
 * where it belongs to is not valid (e.g. because of publishtimes)
 *
 * It is used by the Media builders to pass around information (mainly
 * as entry in Lists)
 *
 * @author Michiel Meeuwissen
 * @author Rob Vermeulen (VPRO)
 */
public class URLComposer  {
    protected MMObjectNode  source;
    protected MMObjectNode  provider;
    protected Map<String, Object>           info;

    protected int preference = 0;

    public void init(MMObjectNode provider, MMObjectNode source, MMObjectNode fragment, Map<String, Object> info, Set<MMObjectNode> cacheExpireObjects) {
        if(cacheExpireObjects!=null) {
            cacheExpireObjects.add(provider);
            cacheExpireObjects.add(source);

        }

        if (source   == null) throw new RuntimeException("Source may not be null in a URLComposer object");
        if (provider == null) throw new RuntimeException("Provider may not be null in a URLComposer object");
        this.provider = provider;
        this.source   = source;
        this.info     = info;
        if (this.info == null) this.info = new java.util.Hashtable<String, Object>();
    }

    public MMObjectNode getSource()   { return source;  }
    public MMObjectNode getProvider() { return provider;}
    public Map<String, Object>          getInfo()     { return info; }

    /**
     * The format of the produced URL. This is not necessarily the format of the source.
     * (Though it normally would be)
     */
    public Format       getFormat()   {
        return Format.get(source.getIntValue("format"));
    }

    /**
     * The mime-type of the produced URL. This is not necessarily the mimetype of the source.
     * (Though it normally would be)
     */
    public String       getMimeType() {
        return getFormat().getMimeType();
    }



    public String getGUIIndicator(Map<String,Locale> options) {
        Locale locale = options.get("locale");
        return getFormat().getGUIIndicator(locale);
    }

    public String getDescription(Map<String,Locale> options) {
        return null; // no informative description
    }

    /**
     * Returns true. This can be overridden if the URLComposer not
     * always can do it's job. It then returns false if it is (can be?)
     * taken from consideration.
     */
    public boolean canCompose() {
        return true;
    }

    /**
     * Extension will normally create URL's differently. They override this function.
     *
     */
    protected StringBuffer getURLBuffer() {
        StringBuffer buff = new StringBuffer(provider.getStringValue("protocol") + "://" + provider.getStringValue("host") + provider.getStringValue("rootpath") + source.getStringValue("url"));
        return buff;
    }
    /**
     * Returns the URL as a String. To encourage efficient coding,
     * this method is final. Override getURLBuffer instead.
     */

    public final String  getURL() {
        return getURLBuffer().toString();
    }

    public boolean      isAvailable() {
        boolean sourceAvailable    = (source != null && source.getIntValue("state") == MediaSources.STATE_DONE);
        boolean providerAvailable  = (provider.getIntValue("state") == MediaProviders.STATE_ON);
        return providerAvailable && sourceAvailable;
    }

    public String toString() {
        // for verboseness:
        String className = getClass().getName().substring(getClass().getName().lastIndexOf(".") + 1);
        if (isAvailable()) {
            return className + "/" + getFormat() + ": " + getURL();
        } else {
            return "{" + className + "/" +  getFormat() + ": " + getURL() + "}";
        }
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass().equals(getClass())) {
            URLComposer r = (URLComposer) o;
            return
            (source == null ? r.source == null : source.getNumber() == r.source.getNumber()) &&
            (provider == null ? r.provider == null : provider.getNumber() == r.provider.getNumber()) &&
            (info == null ? r.info == null : info.equals(r.info));
        }
        return false;
    }


    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int result = 0;
        result = HashCodeUtil.hashCode(result, source == null ? 0 : source.getNumber());
        result = HashCodeUtil.hashCode(result, provider == null ? 0 : provider.getNumber());
        result = HashCodeUtil.hashCode(result, info == null ? 0 : info.hashCode());
        return result;
    }

}
