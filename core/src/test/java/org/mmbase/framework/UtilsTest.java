package org.mmbase.framework;

import java.io.*;
import java.net.URL;
import javax.xml.transform.TransformerException;
import junit.framework.TestCase;
import org.mmbase.util.functions.Parameters;

public class UtilsTest extends TestCase {

    public void testXslTransform() throws IOException, TransformerException {
        URL url = new URL("file:///Users/michiel/github/mmbase/mmbase/applications/taglib/src/main/xml/mmbase-taglib.xml");
        StringWriter writer = new StringWriter();
        InputStream inputStream = url.openStream();
        try {
            Utils.xslTransform(Parameters.VOID,
                url,
                inputStream,
                writer,
                new URL("file:///Users/michiel/github/mmbase/mmbase/applications/share/xslt/xml2block.xslt")
            );
            System.out.println(writer.toString());
        } finally {
            inputStream.close();
        }


    }
}
