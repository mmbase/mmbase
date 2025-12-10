package org.mmbase.util.transformers;

import java.io.StringReader;
import java.io.StringWriter;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ClosureJavascriptCompressorTest {


    @Test
    public void basics() {
        ClosureJavaScriptCompressor compressor = new ClosureJavaScriptCompressor();

        StringWriter writer = new StringWriter();
        compressor.transform(
            new StringReader("function test() {\n  var a = 1 + 1; return a; }"),
            writer);
        assertEquals("\n'use strict';function test(){return 2};", writer.toString());

    }
}
