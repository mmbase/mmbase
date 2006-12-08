/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;
import javax.servlet.jsp.PageContext;
import java.util.*;
import java.io.*;
import org.mmbase.bridge.Cloud;
import org.mmbase.util.Entry;
import org.mmbase.util.functions.Parameters;

/**
 * A Framework is the place where components are displayed in. 
 *
 * @author Johannes Verelst
 * @version $Id: Framework.java,v 1.16 2006-12-08 14:36:45 michiel Exp $
 * @since MMBase-1.9
 */
public interface Framework {

    public final static String COMPONENT_ID_KEY    = "componentId";
    public final static String COMPONENT_CLASS_KEY = "componentClassName";

    /** Return the name of the framework */
    public String getName();

    /** 
     * Return a URL that can be presented to the user (to be put into HTML) to a specific block
     * for a component. The url might be different based on the WindowState of the block.
     *
     * @param block The block to create an URL for, or a page (e.g. image/css) provided by the component
     * @param component The component to use to search the file for
     * @param cloud The cloud to use to find objects if required
     * @param pageContext The current page context, can be used to get the request, response, etc.
     * @param blockParameters The parameters that were set on the block using referids and sub-&lt;mm:param&gt; tags
     * @param frameworkParameters The parameters that are required by the framework, for instance containing the 'request' and 'cloud'.
     */
    public StringBuilder getBlockUrl(String block, Component component, Parameters blockParameters, Parameters frameworkParameters, boolean escapeAmps);

    /** 
     * Return a modified URL for a given page. This method is called from within the mm:url
     * tag, and can perhaps be exposed to the outside world. Within a components's head you will for
     * example use &lt;mm:url page="/css/style.css" /&gt;, this method will then be called. 
     * If you need some treefile/leaffile type of functionality in your framework, you can implement that
     * here in your code.
     *
     * @param page The page to create an URL for.
     * @param component The component in which the mm:url is being called
     * @param urlParameters The parameters to be passed to the page
     * @param frameworkParameters The parameters that are required by the framework
     * @param escapeAmps Boolean indicating whether the parameters should be added with an &amp; or with &amp;amp;
     */
    public StringBuilder getUrl(String page, Component component, Parameters urlParameters, Parameters frameworkParameters, boolean escapeAmps);

    /**
     * URL generating needed for rendering, so for internal use. The resulting URL will later be used to pass on to the
     * RequestDispatcher that will include the URL.
     */
    public StringBuilder getInternalUrl(String block, Renderer renderer, Component component, Parameters blockParameters, Parameters frameworkParameters);

    /**
     * URL generating needed for rendering, so for internal use.
     */
    public StringBuilder getInternalUrl(String block, Processor processor, Component component, Parameters blockParameters, Parameters frameworkParameters);

    /**
     * Return a Parameters object that needs to be passed on to the getUrl() call. The following parameters will be auto-filled
     * if they are returned here:
     * <ul>
     *  <li>Parameter.CLOUD</li>
     *  <li>Parameter.REQUEST</li>
     *  <li>Parameter.RESPONSE</li>
     * </ul>
     * TODO this list is not complete. Perhaps it's better not to documentate it explicitely,
     * because it is taglib dependent.
     */
    public Parameters createFrameworkParameters(); 

    public void render(Renderer renderer, Parameters blockParameters, Parameters frameworkParameters, Writer w, Renderer.WindowState state) throws IOException;
    public void process(Processor renderer, Parameters blockParameters, Parameters frameworkParameters) throws IOException;
}
