/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.util.*;
import org.mmbase.util.logging.*;

/**
 * Extends and wraps LocalizedString. It extends to look like a 'normal' LocalizedString, but it
 * overrides 'get' to do token-replacements first.
 *
 * This functionality is not in LocalizedString itself, because now you can have different
 * replacements on the same value set represented by a LocalizedString withouth having to copy
 * everything every time.
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id: ReplacingLocalizedString.java,v 1.8 2008-11-14 10:07:37 michiel Exp $
 * @since MMBase-1.8
 */
public class ReplacingLocalizedString extends WrappedLocalizedString {

    private static final Logger log = Logging.getLoggerInstance(ReplacingLocalizedString.class);

    private List<Map.Entry<String, String>> replacements = new ArrayList<Map.Entry<String, String>>();


    // just for the contract of Serializable
    protected ReplacingLocalizedString() {

    }

    /**
     * @param s The wrapped LocalizedString.
     */
    public ReplacingLocalizedString(LocalizedString s) {
        super(s);
    }

    public void replaceAll(String regexp, String replacement) {
        replacements.add(new Entry<String, String>(regexp, replacement));
    }

    protected String replace(String input) {
        String output = input;
        for (Map.Entry<String, String> entry : replacements) {
            try {
                output = output.replaceAll(entry.getKey(), entry.getValue());
            } catch (Throwable t) {
                log.warn("Could not replace " + entry + " in " + input + " because " + t);
            }
        }
        return output;
    }

    // javadoc inherited
    public String get(Locale locale) {
        return replace(super.get(locale));
    }

    /**
     * {@inheritDoc}
     *
     * Also takes into account the replacements in the values (but only 'lazily', when actually requested).
     */
    public Map<Locale, String> asMap() {
        final Map<Locale, String> map = super.asMap();
        return new AbstractMap<Locale, String>() {
            public Set<Map.Entry<Locale, String>> entrySet() {
                return new AbstractSet<Map.Entry<Locale, String>>() {
                    public int size() {
                        return map.size();
                    }
                    public Iterator<Map.Entry<Locale, String>> iterator() {
                        final Iterator<Map.Entry<Locale, String>> it = map.entrySet().iterator();
                        return new Iterator<Map.Entry<Locale, String>>() {
                            public boolean hasNext() {
                                return it.hasNext();
                            }
                            public Map.Entry<Locale, String> next() {
                                final Map.Entry<Locale, String> value = it.next();
                                return new Map.Entry<Locale, String>() {
                                    public Locale getKey() {
                                        return value.getKey();
                                    }
                                    public String  getValue() {
                                        return replace(value.getValue());
                                    }
                                    public String setValue(String v) {
                                        throw new UnsupportedOperationException(); // map is umodifiable
                                    }
                                };
                            }
                            public void remove() {
                                throw new UnsupportedOperationException(); // map is umodifiable
                            }
                        };
                    }
                };
            }
        };
    }


    @SuppressWarnings("unchecked")
    public ReplacingLocalizedString clone() {
        ReplacingLocalizedString clone = (ReplacingLocalizedString) super.clone();
        clone.replacements = (List)((ArrayList)replacements).clone();
        return clone;

    }
    /**
     * Utility method for second argument of replaceAll
     */
    public static String makeLiteral(String s) {
        // sometimes, implementing java looks rather idiotic, but honestely, this is correct!
        s =  s.replaceAll("\\\\", "\\\\\\\\");
        s =  s.replaceAll("\\.", "\\\\.");
        return s.replaceAll("\\$", "\\\\\\$");
    }


    public static void main(String argv[]) {
        ReplacingLocalizedString s = new ReplacingLocalizedString(new LocalizedString("abcd"));
        s.replaceAll("b", makeLiteral(argv[0]));
        System.out.println(s.get((Locale) null));
    }

}
