/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.util.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.xml.DocumentReader;
import org.w3c.dom.*;

/**
 *<p>
 * A String which is localized. There are two mechanisms to find and provide translations: They can
 * explicitely be set with {@link #set} (e.g. during parsing an XML), or a resource bundle can be
 * associated with {@link #setBundle}, which will be used to find translations based on the key of
 * this object.
 *</p>
 *<p>
 * The 'set' mechanism can also be driven by {@link #fillFromXml}, which provides a sensible way to fill the LocalizedString with
 * setting from a sub element of XML's.
 *</p>
 *<p>
 * The idea is that objects of this type can be used in stead of normal String objects, for error
 * messages, descriptions and other texts which need localization (e.g. because they are exposed to 
 * end-users).
 *</p>
 *
 * @author Michiel Meeuwissen
 * @version $Id: LocalizedString.java,v 1.22 2006-05-16 21:09:59 michiel Exp $
 * @since MMBase-1.8
 */
public class LocalizedString implements java.io.Serializable, Cloneable {

    private static final Logger log = Logging.getLoggerInstance(LocalizedString.class);

    private static final int serialVersionUID = 1;

    private static Locale defaultLocale = null; // means 'system default' and 'unset'.

    /**
     * Sets a default locale for this JVM or web-app. When not using it, the locale is the system
     * default. Several web-apps do run in one JVM however and it is very imaginable that you want a
     * different default for the Locale.
     *
     * So, this function can be called only once. Calling it the second time will not do
     * anything. It returns the already set default locale then, which should probably prompt you to log an error
     * a throw an exception or so. Otherwise it returns <code>null</code> indicating that the
     * default locale is now what you just set.
     */
    public static Locale setDefault(Locale locale) {
        if (defaultLocale != null) return defaultLocale;
        defaultLocale = locale;
        return null;
    }
    /**
     * Returns the default locale if set, or otherwise the system default ({@link java.util.Locale#getDefault}).
     */
    public static Locale getDefault() {
        return defaultLocale != null ? defaultLocale : Locale.getDefault();
    }

    /**
     * Converts a collection of localized strings to a collection of normal strings.
     * @param col    Collection of LocalizedString objects
     * @param locale Locale to be used for the call to {@link #get(Locale)} which is needed.
     */
    public static Collection toStrings(Collection col, Locale locale) {
        if (col == null || col.size() == 0) return col;
        Collection res = new ArrayList();
        Iterator i = col.iterator();
        while (i.hasNext()) {
            LocalizedString s = (LocalizedString) i.next();
            res.add(s.get(locale));
        }
        return res;
    }

    //    private final String key; cannot be final if cloneable: java sucks.
    private String key;

    private Map    values = null;
    private String bundle = null;

    // just for the contract of Serializable
    protected LocalizedString() {

    }

    /**
     * @param k The key of this String, if k == <code>null</code> then the first set will define it.
     */
    public LocalizedString(String k) {
        key = k;
    }

    /**
     * Gets the key to use as a default and/or for obtaining a value from the bundle
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key to use as a default and/or for obtaining a value from the bundle
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the value for a certain locale. If no match is found, it falls back to the key.
     */
    public String get(Locale locale) {
        if (locale == null) {
            locale = defaultLocale == null ? Locale.getDefault() : defaultLocale;
        }
        if (values != null) {
            String result = (String) values.get(locale);

            if (result != null) return result;

            String variant  = locale.getVariant();
            String country  = locale.getCountry();
            String language = locale.getLanguage();

            if (! "".equals(variant)) {
                result = (String) values.get(new Locale(language, country));
                if (result != null) return result;
            }

            if (! "".equals(country)) {
                result = (String) values.get(new Locale(language));
                if (result != null) return result;
            }

            // Some LocalizedString isntances may have a default value stored with the key 'null'
            // instead of the locale from MMBase. This is the case for values stored while the
            // MMBase module was not yet active
            // This code 'fixes' that reference.
            // It's not nice, but as a proper fix likely requires a total rewrite of Module.java and
            // MMBase.java, this will have to do for the moment.
            if (locale.equals(defaultLocale)) {
                result = (String) values.get(null);
                if (result != null) {
                    values.put(locale, result);
                    return result;
                }
            }
        }

        if (bundle != null) {
            try {
                return ResourceBundle.getBundle(bundle, locale).getString(key);
            } catch (MissingResourceException mre) {
                // fall back to key, invalidate bundle, and log error
                log.debug("Cannot get resource from bundle: " + bundle + ", key: " + key);
            }
        }

        return key;
    }

    /**
     * Sets the value for a certain locale. If the value for a more general locale is still unset,
     * it will also set that (so, it sets also nl when setting nl_BE if nl still is unset).
     */
    public void set(String value, Locale locale) {
        if (key == null) key = value;

        if (values == null) {
            values = new HashMap();
        }

        if (locale == null) {
            locale = defaultLocale;
        }

        values.put(locale, value);

        if (locale != null) {
            String variant  = locale.getVariant();
            String country  = locale.getCountry();
            String language = locale.getLanguage();
            if (! "".equals(variant)) {
                Locale loc = new Locale(language, country);
                if (values.get(loc) == null) {
                    values.put(loc, value);
                }
            }
            if (! "".equals(country)) {
                Locale loc = new Locale(language);
                if (values.get(loc) == null) {
                    values.put(loc, value);
                }
            }
        }
    }

    /**
     * Locale -> description
     */
    public Map asMap() {
        if (values == null) return Collections.EMPTY_MAP;
        return Collections.unmodifiableMap(values);
    }

    /**
     * A resource-bundle with given name can be associated to this LocalizedString. If no
     * translations were explicitely added, it can be used to look up the translation in the bundle,
     * using the key.
     */

    public void setBundle(String b) {
        bundle = b;
    }

    /**
     * {@inheritDoc}
     *
     * For LocalizedString this returns the String for the default Locale (see {@link #getDefault}).
     */
    public String toString() {
        return get(null);
    }



    /**
     * This utility takes care of reading the xml:lang attribute from an element
     */
    public static Locale getLocale(Element element) {
        Locale loc = null;
        String xmlLang = element.getAttribute("xml:lang");
        if (! xmlLang.equals("")) {
            String[] split = xmlLang.split("-");
            if (split.length == 1) {
                loc = new Locale(split[0]);
            } else if (split.length == 2) {
                loc = new Locale(split[0], split[1]);
            } else {
                loc = new Locale(split[0], split[1], split[2]);
            }
        }
        return loc;
    }


    /**
     * This utility determines value of an xml:lang attribute.
     * @since MMBase-1.8.1
     */
    public static String getXmlLang(Locale locale) {
        if (locale == null) return null;
        StringBuffer lang = new StringBuffer(locale.getLanguage());
        String country = locale.getCountry();
        if (country.length() > 0) {
            lang.append("-").append(country);
            String variant = locale.getVariant();
            if (variant != null && variant.length() > 0) {
                lang.append("-").append(variant);
            }
        }
        return lang.toString();
    }
    /**
     * This utility takes care of setting the xml:lang attribute on an element.
     * @since MMBase-1.8.1
     */
    public static void setXmlLang(Element element, Locale locale) {
        String xmlLang = getXmlLang(locale);
        if (xmlLang != null) {
            element.setAttribute("xml:lang", xmlLang);
        }
    }

    /**
     * Given a certain tagname, and a DOM parent element, it configures this LocalizedString, using
     * subtags with this tagname with 'xml:lang' attributes. This boils down to repeative calls to {@link #set(String, Locale)}.
     */

    public void fillFromXml(final String tagName, final Element element) {
        NodeList childNodes = element.getChildNodes();
        for (int k = 0; k < childNodes.getLength(); k++) {
            if (childNodes.item(k) instanceof Element) {
                Element childElement = (Element) childNodes.item(k);
                if (tagName.equals(childElement.getLocalName())) {
                    Locale locale = getLocale(childElement);
                    String description = DocumentReader.getNodeTextValue(childElement);
                    set(description, locale);
                }
            }
        }
    }

    /**
     * @since MMBase-1.8.1
     */
    public void toXml(final String tagName, final String ns, final Element element, final String path) {
        if (values != null) {
            // what there is already:
            org.w3c.dom.NodeList nl  = element.getElementsByTagName(tagName);
            Iterator i = values.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                Locale loc   = (Locale) entry.getKey();
                String value = (String) entry.getValue();
                String xmlLang = getXmlLang(loc);
                // look if such an element is available
                Element child = null;
                for (int j = 0; j < nl.getLength(); j++) {
                    Element cand = (Element) nl.item(j);
                    if (cand.getAttribute("xml:lang").equals(xmlLang)) {
                        child = cand;
                        break;
                    }
                }
                if (child == null) {
                    if (ns != null) {
                        child = element.getOwnerDocument().createElementNS(ns, tagName);
                    } else {
                        child = element.getOwnerDocument().createElement(tagName);
                    }
                    DocumentReader.appendChild(element, child, path);
                    setXmlLang(child, loc);
                }
                DocumentReader.setNodeTextValue(child, value);
            }
        }
    }

    public Object clone() {
        try {
            LocalizedString clone = (LocalizedString)super.clone();
            if (values != null) {
                clone.values = (Map)((HashMap)values).clone();
            }
            return clone;
        } catch (CloneNotSupportedException cnse) {
            // should not happen
            log.error("Cannot clone this LocalizedString");
            throw new RuntimeException("Cannot clone this LocalizedString", cnse);
        }
    }

    public boolean equals(Object o) {
        if (o instanceof LocalizedString) {
            LocalizedString os = (LocalizedString) o;
            return
                key.equals(os.key) &&
                (values == null ? os.values == null : values.equals(os.values)) &&
                (bundle == null ? os.bundle == null : bundle.equals(os.bundle))
                ;
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = 0;
        result = HashCodeUtil.hashCode(result, key);
        result = HashCodeUtil.hashCode(result, values);
        result = HashCodeUtil.hashCode(result, bundle);
        return result;
    }

}
