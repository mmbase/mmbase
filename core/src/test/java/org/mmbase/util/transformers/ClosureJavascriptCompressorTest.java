package org.mmbase.util.transformers;

import java.io.StringReader;
import java.io.StringWriter;
import org.junit.Test;

public class ClosureJavascriptCompressorTest {


    @Test
    public void basics() {
        ClosureJavaScriptCompressor compressor = new ClosureJavaScriptCompressor();

        StringWriter writer = new StringWriter();
        compressor.transform(
            new StringReader("function test() {\n  var a = 1 + 1; return a; }"),
            writer);
        assert(writer.toString().contains("function test(){return 2}"));

    }
}
