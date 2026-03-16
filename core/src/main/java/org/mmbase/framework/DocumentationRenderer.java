/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;


import java.net.*;


import org.mmbase.util.functions.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Currently renders documentation directly from subversion, using an XSLT to convert docbook to
 * HTML. The idea is that a fall-back could be added to render the documentation from the xml's in
 * a/the jar.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.9.1

 */
public class DocumentationRenderer extends CachedRenderer {
    private static final Logger log = Logging.getLoggerInstance(DocumentationRenderer.class);

    private String repository  = "https://raw.githubusercontent.com/mmbase";
    private String project     = "mmdocs";
    private String module      = "src/docbook";
    private String branch      = "refs/heads/main";

    private String docbook     = null;

    public void setDocbook(String s) {
        docbook = s;
    }
    public void setModule(String m) {
        module = m;
    }
    public void setRepository(String r) {
        repository = r;
    }
    public void setProject(String p) {
        project = p;
    }
    @Override
    public Parameter<?>[] getParameters() {
        String baseUrl = null;
        try {
            baseUrl = getBaseUrl().toString();
        } catch (MalformedURLException e) {
            log.error(e.getMessage(), e);
        }
        return new Parameter<?>[]{
            new Parameter<String>("docbook", String.class, docbook),
            new Parameter<String>("module", String.class, module),
            new Parameter<String>("project", String.class, project),
            new Parameter<String>("repository", String.class, repository),
            new Parameter<String>("branch", String.class, branch),
            new Parameter<String>("baseurl", String.class, baseUrl)
        };
    }


    public DocumentationRenderer(Type t, Block parent) {
        super(t, parent);
        setWait(5000);
    }
    @Override public Renderer getWraps() {
        if (wrapped == null) {
            try {
                ConnectionRenderer connection = new ConnectionRenderer(getType(), getBlock()) {
                        @Override public URI getUri(Parameters blockParameters, RenderHints hints) {
                            try {
                                String db = blockParameters != null ? blockParameters.getString("docbook") : null;
                                if (db == null || "".equals(db)) {
                                    db = DocumentationRenderer.this.docbook;
                                    //if (db == null) throw new IllegalArgumentException("docbook parameter not set on parameters, nor as renderer property");
                                }
                                //https://raw.githubusercontent.com/mmbase/mmdocs/refs/heads/main/src/docbook/index.xml
                                //https://raw.githubusercontent.com/mmbase/mmdocs/refs/head/main/src/docbook/index.xml
                                //https://raw.githubusercontent.com/mmbase/mmdocs/src/docbook/refs/head/main/index.xml
                                URL baseUrl = getBaseUrl();
                                URI url = baseUrl.toURI().resolve(db);
                                log.debug("Resolved " + url);
                                return url;
                            } catch (MalformedURLException mfe) {
                                throw new RuntimeException(mfe.getMessage(), mfe);
                            } catch (URISyntaxException use) {
                                throw new RuntimeException(use.getMessage(), use);
                            }
                        }
                    };
                connection.setXslt("xslt/docbook2block.xslt");
                connection.setDecorate(true);
                wrapped = connection;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return wrapped;
    }

    URL getBaseUrl() throws MalformedURLException {
        //https://raw.githubusercontent.com/mmbase/mmdocs/refs/heads/main/src/docbook/index.xml
        //https://raw.githubusercontent.com/mmbase/mmdocs/refs/head/main/src/docbook/index.xml
        //https://raw.githubusercontent.com/mmbase/mmdocs/src/docbook/refs/head/main/index.xml
        return new URL(repository + "/" + project + "/" + branch + "/" + module + "/");

    }

}
