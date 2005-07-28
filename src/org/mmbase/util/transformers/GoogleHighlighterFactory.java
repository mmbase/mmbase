/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.transformers;

import java.util.*;
import java.util.regex.Pattern;
import java.net.URL;

import org.mmbase.util.Entry;

import org.mmbase.util.functions.*;
import org.mmbase.util.logging.*;

/**
 * An example for parameterized transformers. The Google highlighter transformers have a REQUEST
 * parameter, which are used to explore the 'Referer' HTTP header and highlight the google search
 * words.
 * This can be used in taglib e.g. by &lt;mm:content postprocessor="google" expires="0" /&gt;
 *
 * Because you need expires=0, you need be reluctant to use this, because this means that you page
 * cannot be cached in front-proxies. Perhap's it is better to find some client-side solution.
 *
 * It produces instances of extensions of {@link RegexpReplacer}
 *
 * @author Michiel Meeuwissen
 * @since MMBase-1.8
 */

public class GoogleHighlighterFactory  implements ParameterizedTransformerFactory {
    private static final Logger log = Logging.getLoggerInstance(GoogleHighlighterFactory.class);

    private static final Parameter[] PARAM = new Parameter[] {
        new Parameter("format", String.class, "<span class=\"google\">$1</span>"),
        new Parameter("host",   String.class, "google"),
        Parameter.REQUEST,
    };

    public Transformer createTransformer(final Parameters parameters) {
        parameters.checkRequiredParameters();
        if (log.isDebugEnabled()) {
            log.debug("Creating transformer, with " + parameters);
        }
        URL referrer;
        String referer = ((javax.servlet.http.HttpServletRequest) parameters.get(Parameter.REQUEST)).getHeader("Referer");
        if (referer == null) return CopyCharTransformer.INSTANCE;

        try {
            referrer = new URL(referer);
        } catch (java.net.MalformedURLException mfue) {
            log.warn(mfue.getMessage() + " for '" + referer + "'", mfue);
            return CopyCharTransformer.INSTANCE;
        }
        log.debug("Using referrer " + referrer);
        if (referrer.getHost().indexOf((String) parameters.get("host")) == -1) { // did not refer
                                                                                 // from google
            log.debug("Wrong host, returning COPY");
            return CopyCharTransformer.INSTANCE;
        }
        String queryString = referrer.getQuery();
        if (queryString == null) {
            // odd
            log.debug("No query, returning COPY");
            return CopyCharTransformer.INSTANCE;
        }
        String[] query = queryString.split("&");

        String s = null;
        for (int i = 0; i < query.length; i++) {
            String q = query[i];
            if (q.startsWith("q=")) {
                try {
                    s = java.net.URLDecoder.decode(q.substring(2), "UTF-8");
                } catch (java.io.UnsupportedEncodingException uee) { // cannot happen
                    s = q.substring(2);
                }
                break;
            }
        }
        if (s == null) {
            // odd
            log.debug("No search, returning COPY");
            return CopyCharTransformer.INSTANCE;
        }
        final String search = s;
        log.debug("Using search " + search);

        RegexpReplacer trans = new RegexpReplacer() {
                private Collection patterns = new ArrayList();
                {
                    Pattern p        = Pattern.compile("(" + search.replace('+', '|') + ")");
                    patterns.add(new Entry(p, parameters.get("format")));
                }
                public Collection getPatterns() {
                    return patterns;
                }
            };
        if (log.isDebugEnabled()) {
            log.debug("Using google transformer " + trans);
        }
        return trans;

    }
    public Parameters createParameters() {
        return new ParametersImpl(PARAM);
    }

}
