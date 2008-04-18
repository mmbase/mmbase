/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework.basic;
import java.util.*;

import org.mmbase.framework.FrameworkException;
import org.mmbase.util.functions.*;

/**
 * Responsible for the proper handling of urls within the basic framework {@link BasicFramework}.
 * You should implement UrlConverter if you want to create and resolve your own
 * user-friendly links within {@link BasicFramework}
 *.
 * You can configure several UrlConverters in 'framework.xml'.
 *
 * They will be chained one after another.

 * @author Michiel Meeuwissen
 * @version $Id: UrlConverter.java,v 1.7 2008-04-18 13:47:13 michiel Exp $
 * @since MMBase-1.9
 */
public interface UrlConverter {


    Parameter[] getParameterDefinition();

    /**
     * See {@link org.mmbase.framework.Framework#getUrl(String, Map, Parameters, boolean)}.
     * But it can also return <code>null</code> which mean, 'I don't know.'
     * @param path The path (generally a relative URL) to create an URL for.
     * @param parameters Parameters The parameters to be passed to the page
     * @param frameworkParameters The parameters that are required by the framework
     * @param escapeAmps <code>true</code> if parameters should be added with an escaped &amp; (&amp;amp;).
     *                   You should escape &amp; when a URL is exposed (i.e. in HTML), but not if the url is
     *                   for some reason called directly.
     * @return An URL relative to the root of this web application (i.e. without a context
     * path). <code>null</code> if not determinable.
     * @throws FrameworkException thrown when something goes wrong in the Framework
     */
    String getUrl(String path,
                  Map<String, Object> parameters,
                  Parameters frameworkParameters,
                  boolean escapeAmps) throws FrameworkException;

    /**
     * @return An URL relative to the root of this web application (i.e. without a context  path). Never <code>null</code>
     */
    String getProcessUrl(String path,
                         Map<String, Object> parameters,
                         Parameters frameworkParameters,
                         boolean escapeAmps) throws FrameworkException;



    /**
     * See {@link org.mmbase.framework.Framework#getInternalUrl(String, Map, Parameters)}.
     * But it can also return <code>null</code> which mean, 'I don't know'.
     * @param path The page (e.g. image/css) provided by the component to create an URL for
     * @param params Extra parameters for that path
     * @param frameworkParameters The parameters that are required by the framework, such as the
     *                            'request' and 'cloud' objects
     * @return A valid internal URL, or <code>null</code> if nothing framework specific could be
     *         determined (this would make it possible to 'chain' frameworks).
     * @throws FrameworkException thrown when something goes wrong in the Framework
     */
    String getInternalUrl(String path,
                          Map<String, Object> params,
                          Parameters frameworkParameters) throws FrameworkException;


}
