/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

import java.util.*;
import java.lang.reflect.*;
import org.mmbase.cache.Cache;
import org.mmbase.util.logging.*;

/**
 * A bit like {@link java.util.ResourceBundle} (on which it is based), but it creates
 * SortedMap's. The order of the entries of the Map can be influenced in tree ways. You can
 * associate the keys with JAVA constants (and their natural ordering can be used), you can wrap the
 * keys in a 'wrapper' (which can be of any type, the sole restriction being that there is a
 * constructor with String argument or of the type of the assiocated JAVA constant if that happened
 * too, and the natural order of the wrapper can be used (a wrapper of some Number type would be
 * logical). Finally you can also explicitely specify a {@link java.util.Comparator} if no natural
 * order is good.
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.8
 * @todo   THIS CLASS IS EXPERIMENTAL
 * @version $Id: SortedBundle.java,v 1.9 2005-10-13 09:49:24 michiel Exp $
 */
public class SortedBundle {

    private static final Logger log = Logging.getLoggerInstance(SortedBundle.class);

    /**
     * Constant which can be used as an argument for {@link #getResource}
     */
    public static final Class      NO_WRAPPER    = null;
    /**
     * Constant which can be used as an argument for {@link #getResource}
     */
    public static final Comparator NO_COMPARATOR = null;
    /**
     * Constant which can be used as an argument for {@link #getResource}
     */
    public static final Class      NO_CONSTANTSPROVIDER = null;

    // cache of maps.
    private static Cache knownResources = new Cache(100) {
            public String getName() {
                return "ConstantBundles";
            }
            public String getDescription() {
                return "A cache for constant bundles, to avoid a lot of reflection.";
            }
        };

    static {
        knownResources.putCache();
    }

    /**
     * You can specify ValueWrapper.class as a value for the wrapper argument. The keys will be objects with natural order of the values.
     * @todo EXPERIMENTAL
     */

    public static class ValueWrapper implements Comparable {
        private String key;
        private Comparable value;
        protected ValueWrapper(String k, Comparable v) {
            key = k;
            value = v;
        }
        public  int compareTo(Object o) {
            ValueWrapper other = (ValueWrapper) o;
            int result = value.compareTo(other.value);
            return result == 0 ? key.compareTo(other.key) : result;
        }
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o == null) return false;
            if (getClass() == o.getClass()) {
                ValueWrapper other = (ValueWrapper) o;
                return key.equals(other.key) && (value == null ? other.value == null : value.equals(other.value));
            }
            return false;
        }
        public String toString() {
            return key;
        }
        /**
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            int result = 0;
            result = HashCodeUtil.hashCode(result, key);
            result = HashCodeUtil.hashCode(result, value);
            return result;
        }
    }

    /**
     * @param baseName A string identifying the resource. See {@link java.util.ResourceBundle#getBundle(java.lang.String, java.util.Locale, java.lang.ClassLoader)} for an explanation of this string.
     *
     * @param locale   the locale for which a resource bundle is desired
     * @param loader   the class loader from which to load the resource bundle
     * @param constantsProvider the class of which the constants must be used to be associated with the elements of this resource.
     * @param wrapper           the keys will be wrapped in objects of this type (which must have a
     *                          constructor with the right type (String, or otherwise the type of the variable given by the constantsProvider), and must be Comparable.
     *                          You could specify e.g. Integer.class if the keys of the
     *                          map are meant to be integers. This can be <code>null</code>, in which case the keys will remain unwrapped (and therefore String).
     * @param comparator        the elements will be sorted (by key) using this comparator or by natural key order if this is <code>null</code>.
     *
     * @throws NullPointerException      if baseName or locale is <code>null</code>  (not if loader is <code>null</code>)
     * @throws MissingResourceException  if no resource bundle for the specified base name can be found
     * @throws IllegalArgumentExcpetion  if wrapper is not Comparable.
     */
    public static SortedMap getResource(String baseName, Locale locale, ClassLoader loader, Class constantsProvider, Class wrapper, Comparator comparator) {
        String resourceKey = baseName + '/' + locale + (constantsProvider == null ? "" : constantsProvider.getName()) + "/" + (comparator == null ? "" : "" + comparator.hashCode()) + "/" + (wrapper == null ? "" : wrapper.getName());
        SortedMap m = (SortedMap) knownResources.get(resourceKey);
        if (locale == null) locale = LocalizedString.getDefault();

        if (m == null) { // find and make the resource
            ResourceBundle bundle;
            if (loader == null) {
                bundle = ResourceBundle.getBundle(baseName, locale);
            } else {
                bundle = ResourceBundle.getBundle(baseName, locale, loader);
            }
            m = new TreeMap(comparator);
            Enumeration keys = bundle.getKeys();
            while (keys.hasMoreElements()) {
                String bundleKey = (String) keys.nextElement();

                Object key;

                // if the key is numeric then it will be sorted by number
                //key Double

                Class providerClass = constantsProvider; // default class (may be null)
                int lastDot = bundleKey.lastIndexOf('.');
                if (lastDot > 0) {
                    String className = bundleKey.substring(0, lastDot);
                    try {
                        providerClass = Class.forName(className);
                    } catch (ClassNotFoundException cnfe) {
                        log.warn("No class found with name " + className + " for resource " + baseName);
                        providerClass = constantsProvider;
                    }
                }

                if (providerClass != null) {
                    try {
                        Field constant = providerClass.getDeclaredField(bundleKey);
                        key = constant.get(null);
                    } catch (NoSuchFieldException nsfe) {
                        log.info("No java constant with name " + bundleKey);
                        key = bundleKey;
                    } catch (IllegalAccessException ieae) {
                        log.warn("The java constant with name " + bundleKey + " is not accessible");
                        key = bundleKey;

                    }
                } else {
                    key = bundleKey;
                }

                if (wrapper != null) {
                    if (! Comparable.class.isAssignableFrom(wrapper)) {
                        throw new IllegalArgumentException("Key wrapper " + wrapper + " is not Comparable");
                    }
                    try {
                        if (wrapper.isAssignableFrom(ValueWrapper.class)) {
                            Constructor c = wrapper.getConstructor(new Class[] {String.class, Comparable.class});
                            key = c.newInstance(new Object[] { key, (Comparable) bundle.getObject(bundleKey)});
                        } else {
                            Constructor c = wrapper.getConstructor(new Class[] {key.getClass()});
                            key = c.newInstance(new Object[] { key });
                        }
                    } catch (NoSuchMethodException nsme) {
                        log.warn(nsme.getClass().getName() + ". Could not convert " + key.getClass().getName() + " " + key + " to " + wrapper.getName() + " : " + nsme.getMessage());
                        continue;
                    } catch (SecurityException se) {
                        log.error(se.getClass().getName() + ". Could not convert " + key.getClass().getName() + " " + key + " to " + wrapper.getName() + " : " + se.getMessage());
                        continue;
                    } catch (InstantiationException ie) {
                        log.error(ie.getClass().getName() + ". Could not convert " + key.getClass().getName() + " " + key + " to " + wrapper.getName() + " : " + ie.getMessage());
                        continue;
                    } catch (InvocationTargetException ite) {
                        log.error(ite.getClass().getName() + ". Could not convert " + key.getClass().getName() + " " + key + " to " + wrapper.getName() + " : " + ite.getMessage());
                        continue;
                    } catch (IllegalAccessException iae) {
                        log.error(iae.getClass().getName() + ". Could not convert " + key.getClass().getName() + " " + key + " to " + wrapper.getName() + " : " + iae.getMessage());
                        continue;
                    }
                }

                m.put(key, bundle.getObject(bundleKey));
            }
            m = Collections.unmodifiableSortedMap(m);
            knownResources.put(resourceKey, m);
        }
        return m;
    }
}
