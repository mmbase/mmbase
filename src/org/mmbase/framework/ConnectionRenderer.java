/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.util.*;
import java.net.*;
import java.io.*;
import javax.servlet.http.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import org.mmbase.util.functions.*;
import org.mmbase.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A {@link Renderer} implementation based on an external connection.
 * An example of a {@link ConnectionRenderer} is the following one:
 *
 * <p>
 *    &lt;block name="mmbase_news"<br />
 *          classification="mmbase.about:100"<br />
 *          mimetype="text/html"&gt;<br />
 *     &lt;title xml:lang="nl"&gt;Nieuws&lt;/title&gt;<br />
 *     &lt;title xml:lang="en"&gt;News&lt;/title&gt;<br />
 *     &lt;description xml:lang="en"&gt;Shows latest news from the mmbase site&lt;/description&gt;<br />
 *     &lt;body&gt;<br />
 *       &lt;class name="org.mmbase.framework.ConnectionRenderer"&gt;<br />
 *         &lt;param name="url"&gt;http://www.mmbase.org/rss&lt;/param&gt;<br />
 *         &lt;param name="xslt"&gt;xslt/rss.xslt&lt;/param&gt;<br />
 *       &lt;/class&gt;<br />
 *     &lt;/body&gt;<br />
 *   &lt;/block&gt;
 * </p>
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id: ConnectionRenderer.java,v 1.7 2008-08-25 21:45:19 michiel Exp $
 * @since MMBase-1.9
 */
public class ConnectionRenderer extends AbstractRenderer {
    private static final Logger log = Logging.getLoggerInstance(ConnectionRenderer.class);


    protected URL url;
    protected int timeOut = 2000;
    protected String xsl = null;
    protected boolean decorate = true;

    public ConnectionRenderer(String t, Block parent) {
        super(t, parent);
    }

    public void setUrl(String u) throws MalformedURLException {
        url = new URL(u);
    }

    public void setXslt(String x) throws MalformedURLException {
        xsl = x;
    }
    public void setTimeOut(int t) {
        timeOut = t;
    }
    public void setDecorate(boolean d) {
        decorate = d;
    }

    public  Parameter[] getParameters() {
        return new Parameter[] {Parameter.REQUEST}; // hmm.
    }


    @Override
    public void render(Parameters blockParameters, Parameters frameworkParameters,
                       Writer w, WindowState state) throws FrameworkException {


        if (w == null) throw new NullPointerException();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Rendering with " + blockParameters);
            }
            if (decorate) {
                HttpServletRequest request   = blockParameters.get(Parameter.REQUEST);
                decorateIntro(request, w, null);
            }
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeOut);
            connection.setReadTimeout(timeOut);
            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            InputStream inputStream = connection.getInputStream();
            if (responseCode == 200) {
                log.debug("" + xsl);
                if (xsl == null) {
                    String encoding = GenericResponseWrapper.getEncoding(contentType);
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream, encoding));
                    char[] buf = new char[1000];
                    int c;
                    while ((c = r.read(buf, 0, 1000)) > 0) {
                        w.write(buf, 0, c);
                    }
                } else {
                    /// convert using the xsl and spit out that.
                    Source xml = new StreamSource(inputStream);
                    URL x = ResourceLoader.getConfigurationRoot().getResource(xsl);

                    Result res = new StreamResult(w);
                    XSLTransformer.transform(xml, x, res, new HashMap<String, Object>());
                }


            } else {
                log.debug("" + responseCode);
                throw new FrameworkException("" + responseCode);
            }
        } catch (java.net.ConnectException ce) {
            throw new FrameworkException(ce.getMessage(), ce);
        } catch (java.net.SocketTimeoutException ste) {
            throw new FrameworkException(ste.getMessage(), ste);
        } catch (IOException ioe) {
            throw new FrameworkException(ioe.getMessage(), ioe);
        } catch (javax.xml.transform.TransformerException te) {
            throw new FrameworkException(te.getMessage(), te);
        } catch (RuntimeException e) {
            log.debug(e.getMessage(), e);
            throw e;
        } catch(FrameworkException fe) {
            log.debug(fe.getMessage(), fe);
            throw fe;
        } finally {
            if (decorate) {
                log.debug("Decorating");
                try {
                    HttpServletRequest request   = blockParameters.get(Parameter.REQUEST);
                    decorateOutro(request, w);
                } catch (Exception e) {
                }
            } else {
                log.debug("no decoration");
            }
        }

    }


    public String toString() {
        return url.toString();
    }

    public java.net.URI getUri() {
        try {
            return url.toURI();
        } catch (URISyntaxException use) {
            log.warn(use);
            return null;
        }
    }
}
