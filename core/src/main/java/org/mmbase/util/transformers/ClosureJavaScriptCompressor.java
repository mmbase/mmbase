/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.transformers;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Javascript compressor based on closure.

 * @author Michiel Meeuwissen
 * @since MMBase-1.9.7
 */

public class ClosureJavaScriptCompressor extends  ReaderTransformer {
    private static final long serialVersionUID = 0L;
    private static final Logger LOG = Logging.getLoggerInstance(ClosureJavaScriptCompressor.class);

    private boolean initialNewline = true;


    public ClosureJavaScriptCompressor() {
    }



    @Override
    public Writer transform(Reader reader, Writer writer) {
        // Setup Closure Compiler
        Compiler compiler = new Compiler();
        CompilerOptions options = new CompilerOptions();
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        WarningLevel.DEFAULT.setOptionsForWarningLevel(options);

        StringWriter writerBuffer = new StringWriter();
        CopyCharTransformer.INSTANCE.transform(reader, writerBuffer);

        try {


            SourceFile input = SourceFile.fromCode("input.js", writerBuffer.toString());

            // Compile (no externs)
            Result result = compiler.compile(
                Collections.emptyList(),
                Collections.singletonList(input),
                options
            );

            if (result.warnings != null && !result.warnings.isEmpty()) {
                LOG.warn("JavaScript compilation warning: " + result.warnings);
            }
            if (result.success) {
                if (initialNewline) {
                    writer.write('\n');
                }
                String compiled = compiler.toSource();
                writer.write(compiled);
                LOG.debug("Ready (compiled)");
                return writer;
            } else {
                LOG.error("JavaScript compilation failed: " + result.errors);
            }
        } catch (Exception e) {
            LOG.error("Error during JavaScript compression: " + e.getMessage(), e);
        }
        // unsuccessful, return original
        CopyCharTransformer.INSTANCE.transform(new StringReader(writerBuffer.toString()), writer);

        return writer;

    }

    public void setInitialNewline(boolean i) {
        initialNewline = i;
    }


}
