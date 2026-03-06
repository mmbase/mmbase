/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib.functions;


import org.mmbase.bridge.jsp.taglib.*;

import java.util.*;
import org.mmbase.bridge.*;
import org.mmbase.util.*;
import org.mmbase.util.transformers.CharTransformer;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Functions for EL variables, and XSL.
 * Like this:

<mm:import id="nodelist" vartype="list">1,2,123</mm:import>
<mm:cloud>
  <mm:node number="124" id="node" />
  <c:choose>
    <c:when test="${mm:contains(nodelist, node)}">
      YES!
    </c:when>
    <c:otherwise>
      NO!
    </c:otherwise>
  </c:choose>
</mm:cloud>
 * @author  Michiel Meeuwissen
 * @since   MMBase-1.8
 * @version $Id$
 * @todo    EXPERIMENTAL
 */
public class Functions {
    private static final Logger log = Logging.getLoggerInstance(Functions.class);

    /**
     * MMBase specific 'contains' (for Collections). For strings use fn:contains.
     */
    public static boolean contains(Collection col, Object obj) {
        if (col == null) return false;
        if (obj instanceof Node) {
            if (col instanceof NodeList) {
                if (col.contains(obj)) return true;
            } else {
                obj = ((Node) obj).getNumber();
            }
        }
        if (col.contains(obj)) return true;
        return col.contains(Casting.toString(obj));
    }

    /**
     * MMBase specific 'remove' (for Collections).
     */
    public static void remove(Collection col, Object obj) {
        if (col == null) return;
        if (obj instanceof Collection) { // like removeAll
            Iterator i = ((Collection) obj).iterator();
            while (i.hasNext()) {
                remove(col, i.next());
            }
        } else {
            if (obj instanceof Node) {
                col.remove(((Node) obj).getNumber());
            }
            col.remove(Casting.toString(obj));
            col.remove(obj);
        }
    }



    /**
     * This version of {@link #escape(String, Object)} simply asks for a string. This often work
     * better in XSL {taglib:escape('text/xml', @link)} may otherwise give e.g. the toString of a
     * NodeIterator, which we cannot recognize withouth introducing dependencies.
     * @since MMBase-1.8.7
     */
    public static String escapeString(String escaper, String string) {
        return escape(escaper, string);
    }
    /**
     * Provides the 'escape' functionality of taglib. Can be used in EL (using mm:escape('p', value)) and XSLT (using taglib:escape('p', mytag))
     */
    public static String escape(String escaper, Object string) {
        try {
            javax.servlet.jsp.PageContext pageContext = ContextReferrerTag.getThreadPageContext();
            ContextTag tag = (ContextTag) pageContext.getAttribute(ContextTag.CONTEXTTAG_KEY);
            CharTransformer ct = ContentTag.getCharTransformer(escaper, tag);
            return ct == null ? Casting.toString(Casting.unWrap(string)) : ct.transform(Casting.toString(Casting.unWrap(string)));
        } catch (Exception e) {
            String mes = "Could not escape " + string + " with escape " + escaper + " : " + e.getMessage();
            log.debug(mes, e);
            return mes;
        }
    }

    public static String directory(String file) {
        if (file.endsWith("/")) return file;
        return org.mmbase.util.ResourceLoader.getDirectory(file);
    }

    /**
     * MMBase url generation for EL
     * @since MMBase-1.8.2
     */
    public static String url(String page, javax.servlet.jsp.PageContext pageContext) {
        javax.servlet.http.HttpServletRequest req = (javax.servlet.http.HttpServletRequest) pageContext.getRequest();
        StringBuilder show = new StringBuilder();
        if (page.length() == 0) { // means _this_ page
            String requestURI = req.getRequestURI();
            if (requestURI.endsWith("/")) {
                page = ".";
            } else {
                page = new java.io.File(requestURI).getName();
            }
        }
        if (page.charAt(0) == '/') { // absolute on servletcontext
            if (req == null) throw new RuntimeException("No request on pageContext " + pageContext);
            show.append(req.getContextPath());
        }
        show.append(page);
        return show.toString();

    }
    /**
     * @since MMBase-1.8.5
     */
    public static String link(String page) {
        return url(page, ContextReferrerTag.getThreadPageContext());
    }


    /**
     * @since MMBase-1.8.6
     */
    public static String managerProperty(String nodeManager, String name) {
        Cloud cloud = (Cloud) ContextReferrerTag.getThreadPageContext().getAttribute(CloudTag.KEY, CloudTag.SCOPE);
        return cloud.getNodeManager(nodeManager).getProperty(name);
    }

    /**
     * @since MMBase-1.8.6
     */
    public static String moduleProperty(String module, String name) {
        Cloud cloud = (Cloud) ContextReferrerTag.getThreadPageContext().getAttribute(CloudTag.KEY, CloudTag.SCOPE);
        return cloud.getCloudContext().getModule(module).getProperty(name);
    }

    /**
     * @since MMBase-1.8.6
     */
    public static Object property(String name) {
        Cloud cloud = (Cloud) ContextReferrerTag.getThreadPageContext().getAttribute(CloudTag.KEY, CloudTag.SCOPE);
        return cloud.getProperty(name);
    }

    /**
     * @since MMBase-1.8.4
     */
    public static String treefile(String page, javax.servlet.jsp.PageContext pageContext, Object objectList) throws javax.servlet.jsp.JspTagException, java.io.IOException {
        Cloud cloud = (Cloud) pageContext.getAttribute(CloudTag.KEY, CloudTag.SCOPE);
        if (cloud == null) throw new IllegalStateException("No current cloud (key '" + CloudTag.KEY + "', can not execute treefile");
        org.mmbase.bridge.jsp.taglib.pageflow.TreeHelper th = new org.mmbase.bridge.jsp.taglib.pageflow.TreeHelper();
        th.setCloud(cloud);
        javax.servlet.http.HttpServletRequest req = (javax.servlet.http.HttpServletRequest) pageContext.getRequest();
        String t = th.findTreeFile(page, Casting.toString(objectList), pageContext.getSession());
        if (t == null || "".equals(t)) {
            // not found, avoid Exceptions, this will generaly produce a 404 in stead. Which is
            // clear enough.
            t = page;
        }
        return req.getContextPath() + ((t.length() > 1 && t.charAt(0) == '/') ? "" : "/") + t;
    }
    /**
     * @since MMBase-1.8.5
     */
    public static String treelink(String page,  Object objectList) throws javax.servlet.jsp.JspTagException, java.io.IOException {
        return treefile(page, ContextReferrerTag.getThreadPageContext(), objectList);
    }



    /**
     * @since MMBase-1.9
     */
    public static LocalizedString string(LocalizedString s) {
        javax.servlet.jsp.PageContext pageContext = ContextReferrerTag.getThreadPageContext();
        LocaleLocalizedString result = new LocaleLocalizedString(s);
        Locale locale = (Locale) pageContext.getAttribute(LocaleTag.KEY, LocaleTag.SCOPE);
        if (locale == null) {
            locale = LocalizedString.getDefault();
        }
        result.setLocale(locale);
        return result;
    }

    /**
     * Checks if the supplied node has the supplied alias
     *
     * @param node the MMBase node to check for existing alias
     * @param alias Name of the alias to check
     * @return true if the alias exists for the node or false if not
     */
    public static boolean hasAlias(Node node, String alias) {
         return node.getAliases().contains(alias);
    }

    /**
     * @since MMBase-1.8.7
     */
    public static Map<String, String> applicationContext(String path) {
      return ApplicationContextReader.getCachedProperties(path);
    }


}
