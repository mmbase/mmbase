/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;
import javax.servlet.jsp.JspTagException;

import java.io.*;
import java.util.*;
import javax.servlet.jsp.PageContext;

import org.mmbase.bridge.*;
import org.mmbase.util.logging.*;
import org.mmbase.bridge.jsp.taglib.util.*;




/**
 * <p>
 * A ContextTag is like parentheses, and as such can act as
 * a 'namespace' (if it has an id) or as 'scope' (if it doesn't).
 * </p>
 * <p>
 * The context can be seen as a container for variables and their values.
 * </p><p>
 * (ContextReferrer) Tags living under a context tag can 'register'
 * themselves in the context (by use of the 'id' attribute') and in
 * that way become a variable. Other tags can refer to such variables.
 * </p><p>
 * A ContextTag is a ContextReferrer itself too, and therefore it is
 * possible to 'nest' contextes. And perhaps we will also make it
 * possible to treat contextes as variables and e.g. pass them to
 * another page as a whole.
 * </p><p>
 * It is also possible to put something into the Context by hand. For
 * that you can use the `ImportTag'.
 * </p><p>
 * Writing out the value of a variable can be done with the `Write' Tag.
 * </p>
 *
 * @author Michiel Meeuwissen
 * @see    ImportTag
 * @see    WriteTag
 */

public class ContextTag extends ContextReferrerTag implements ContextProvider {
    private static Logger log = Logging.getLoggerInstance(ContextTag.class);

    static String CONTEXTTAG_KEY = "__context";
    public static String DEFAULTENCODING_KEY = "__defaultencoding";

    private ContextContainer container = null;
    private ContextProvider  parent = null;
    private boolean    searchedParent = false;

    private CloudContext cloudContext;

    /**
     * This context can also serve as a 'cloudcontext'.
     * That means that the cloud context commmunicates its cloudcontext to the context.
     */
    public void setCloudContext(CloudContext cc) {
        cloudContext = cc;
        String def = (String) pageContext.getAttribute(DEFAULTENCODING_KEY);
        if (def == null && cloudContext != null) {
            pageContext.setAttribute(DEFAULTENCODING_KEY, cloudContext.getDefaultCharacterEncoding());            
        }
    }


    public void setPageContext(PageContext pc) {
        super.setPageContext(pc); // This will call fillVars for the 'page' Context.
        log.debug("setting page context");
    }

    void createContainer(ContextContainer c) {
        container = new ContextContainer(getId(), c);
    }

    public ContextContainer getContainer() {
        return container;
    }

    public int doStartTag() throws JspTagException {
        log.debug("Start tag of ContextTag");
        parent = null;
        searchedParent = false;
        createContainer(getContextProvider().getContainer());
        setCloudContext(getContextTag().cloudContext);
        if (getId() != null) {
            if (log.isDebugEnabled()) {
                log.debug("registering container " + getId() + " with context " + getContextProvider().getContainer().getId());
            }
            getContextProvider().getContainer().register(getId(), container);
        }
        log.debug("out");
        return EVAL_BODY_INCLUDE;
    }

    private ContextProvider getParentContext() throws JspTagException {
        if (! searchedParent) {
            try {
                parent = getContextProvider();
            } catch (JspTagException e) {
                throw new JspTagException("Could not find parent context!");
            }
            searchedParent = true;
        }
        return parent;
    }

    /**
     * Precisely like 'register', only it wants a Node.
     *
     * @param key the key (id) of the node to register
     * @param node the node to put in the hashmap
     * @deprecated Use getContextProvider().getContainer().registerNode
     */
    public void  registerNode(String key, Node n) throws JspTagException {
        container.registerNode(key, n);
    }


    /**
     * Searches a key in request, postparameters, session, parent
     * context and registers it in this one.
     *
     *  Returns null if it could not be found.
     * @deprecated Use getContextProvider().getContainer().findAndRegister
     */
    public Object findAndRegister(int from, String referid, String newid) throws JspTagException {
        return container.findAndRegister(pageContext, from, referid, newid, true);
    }

    /**
     * @deprecated Use getContextProvider().getContainer().findAndRegister
     */
    protected Object findAndRegister(int from, String referid, String newid, boolean check) throws JspTagException {
        return container.findAndRegister(pageContext, from, referid, newid, check);
    }

    /**
     * @deprecated Use getContextProvider().getContainer().findAndRegister
     */
    public Object findAndRegister(String externid, String newid) throws JspTagException {
        return container.findAndRegister(pageContext, externid, newid);
    }


    /**
     * @deprecated Use getContextProvider().getContainer().register
     */
    public void register(String newid, Object n, boolean check) throws JspTagException {
        container.register(newid, n, check);
    }

    /**
     * Register an Object with a key in the context. If the Context is
     * a session context, then it will be put in the session, otherwise in the hashmap.
     * @deprecated Use getContextProvider().getContainer().register
     */
    public void register(String newid, Object n) throws JspTagException {
        container.register(newid, n);
    }


    /**
     * @deprecated Use getContextProvider().getContainer().unRegister
     */
    public void unRegister(String key) throws JspTagException {
        container.unRegister(key);
    }

    /**
     * Registers an variable again. This can be used to change the type of a variable, e.g.
     *
     * @since MMBase-1.6
     * @deprecated Use getContextProvider().getContainer().reregister
     */
    public void reregister(String id, Object n) throws JspTagException {
        container.reregister(id, n);
    }


    /**
     * 'present' means 'not null'. 'null' means 'registered, but not present'.
     *  Not registered is not present, of course.
     * @deprecated Use getContextProvider().getContainer().isPresent
     */

    public boolean isPresent(String key) throws JspTagException {
        return container.isPresent(key);
    }
    
    /**
     * @deprecated Use getContextProvider().getContainer().isRegistered
     */
    public boolean isRegistered(String key) throws JspTagException {
        return container.isRegistered(key);
    }
    /**
     * @deprecated Use getContextProvider().getContainer().isRegisteredSomewhere
     */
    private boolean isRegisteredSomewhere(String key) throws JspTagException {
        return container.containsKey(key, true); // do check parent.
    }

    /**
     * @deprecated Use getContextProvider().getContainer().findAndRegister
     */
    public Object findAndRegister(String id) throws JspTagException {
        return container.findAndRegister(pageContext, id);
    }
    /**
     * @deprecated Use getContextProvider().getContainer().findAndRegisterString
     */
    public String findAndRegisterString(String id) throws JspTagException {
        return container.findAndRegisterString(pageContext, id);
    }

    /**
     * @deprecated Use getContextProvider().getContainer().getObject
     */

    public Object getContainerObject(String key) throws JspTagException {
        return container.getObject(key);

    }

    /**
     * hmm.. This kind of stuf must move to ImportTag, I think.
     */

    public byte[] getBytes(String key) throws JspTagException {
        return MultiPart.getMultipartRequest(pageContext).getBytes(key);

    }

    /*
    public int doAfterBody() throws JspTagException {
        if (log.isDebugEnabled()) {
            log.debug("after body of context " + getId());
        }

        try {
            if (bodyContent != null) {
                bodyContent.writeOut(bodyContent.getEnclosingWriter());
            }
            return SKIP_BODY;
        } catch (IOException ioe){
            throw new JspTagException(ioe.toString());
        }
    }
    */

}


