/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import java.util.*;

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
 * @version $Id$
 * @since MMBase-1.8
 */
public class ReplacingLocalizedString extends WrappedLocalizedString {
    private static final long serialVersionUID = 0L;

    private static final Logger log = Logging.getLoggerInstance(ReplacingLocalizedString.class);

    private List<Map.Entry<String, String>> replacements = new ArrayList<Map.Entry<String, String>>();


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

    @Override
    public String get(Locale locale) {
        return replace(super.get(locale));
    }

    /**
     * {@inheritDoc}
     *
     * Also takes into account the replacements in the values (but only 'lazily', when actually requested).
     */
    @Override
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
                                        String s = value.getValue();
                                        return ReplacingLocalizedString.this.replace(s);
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
    @Override
    public ReplacingLocalizedString clone() {
        ReplacingLocalizedString clone = (ReplacingLocalizedString) super.clone();
        clone.replacements = (List)((ArrayList)replacements).clone();
        return clone;

    }
    /**
     * Utility method for second argument of replaceAll
     */
    public static String makeLiteral(String s) {
        if (s == null) {
            return null;
        }
        // Single-pass replacement using StringBuilder for better performance
        // Escapes special regex characters: \ . + $
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                case '.':
                case '+':
                case '$':
                    sb.append('\\');
                    // Fall through to append the character
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }


    public static void main(String argv[]) {
        ReplacingLocalizedString s = new ReplacingLocalizedString(new LocalizedString("abcd"));
        s.replaceAll("b", makeLiteral(argv[0]));
        System.out.println(s.get((Locale) null));
    }

}
