package org.mmbase.framework;

import java.io.StringWriter;
import java.net.MalformedURLException;
import junit.framework.TestCase;
import org.mmbase.util.functions.Parameters;

public class ConnectionRendererTest extends TestCase {

    public void testTaglibReference() throws MalformedURLException, FrameworkException {

        ConnectionRenderer renderer = new ConnectionRenderer(Renderer.Type.BODY, new Block("test", "text/html", new BasicComponent("bla"), "cla"));
        renderer.setUrl("https://raw.githubusercontent.com/mmbase/mmbase/MMBase-1_9/applications/taglib/src/main/xml/mmbase-taglib.xml");
        renderer.setXslt("https://raw.githubusercontent.com/mmbase/mmbase/MMBase-1_9/applications/share/xslt/xml2block.xslt");
        StringWriter writer = new StringWriter();
        renderer.render(Parameters.VOID, writer, new RenderHints(renderer, WindowState.MAXIMIZED, "1", "clazz", RenderHints.Mode.NORMAL));
        System.out.println(writer);
    }

}
