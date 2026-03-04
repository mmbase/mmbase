package org.mmbase.framework;

import java.io.IOException;
import java.io.StringWriter;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mmbase.bridge.CloudContext;
import org.mmbase.bridge.mock.MockCloudContext;
import org.mmbase.datatypes.DataTypes;
import org.mmbase.util.functions.Parameters;
import static org.junit.Assert.*;


public class DocumentationRendererTest {

    public CloudContext getCloudContext() {
        return MockCloudContext.getInstance();
    }
    @BeforeClass()
    public static void setUp() throws Exception {
        DataTypes.initialize();
        MockCloudContext.getInstance().clear();
        MockCloudContext.getInstance().addCore();
        MockCloudContext.getInstance();
    }

    @Test
    @Ignore
    public void testRenderDocumentation() throws FrameworkException, IOException {
        DocumentationRenderer renderer = new DocumentationRenderer(Renderer.Type.BODY, new Block("test", "text/html", new BasicComponent("bla"), "cla"));
        renderer.setDocbook("backenddevelopers/components/index.xml");
        StringWriter writer = new StringWriter();
        renderer.render(Parameters.VOID, writer, new RenderHints(renderer, WindowState.MAXIMIZED, "1", "clazz", RenderHints.Mode.NORMAL));
        System.out.println(writer);
    }


    @Test
    public void url() {
        DocumentationRenderer renderer = new DocumentationRenderer(Renderer.Type.BODY, new Block("test", "text/html", new BasicComponent("bla"), "cla"));
        renderer.setProject("mmbase");
        renderer.setModule("contributions/lucene");
        renderer.setDocbook("documentation/lucene.xml");
        assertEquals(renderer.getUri().toString(), "https://raw.githubusercontent.com/mmbase/mmbase/refs/heads/main/contributions/lucene/documentation/lucene.xml");


    }

}
