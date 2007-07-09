/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.transformers;

import java.io.*;
import org.mmbase.util.ReaderInputStream;
import org.mmbase.util.logging.*;

/**
 * A CharTransformer which wraps a ByteToCharTransformer.
 * 
 * It uses the <em>UTF-8</em> bytes (on default).
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.8
 * @version $Id: ByteCharTransformer.java,v 1.3 2006-10-02 14:31:01 michiel Exp $
 */

public class ByteCharTransformer extends ReaderTransformer implements CharTransformer {
    private static Logger log = Logging.getLoggerInstance(ByteCharTransformer.class);

    private ByteToCharTransformer byteToChars;
    private String encoding = "UTF-8";
    public ByteCharTransformer(ByteToCharTransformer b) {
        byteToChars = b;
    }
    public ByteCharTransformer(ByteToCharTransformer b, String enc) {
        this(b);
        encoding = enc;
    }

    // javadoc inherited
    public Writer transform(Reader reader, Writer writer) {
        return byteToChars.transform(new ReaderInputStream(reader, encoding), writer);
    }

    public String toString() {
        return "CHAR "  + byteToChars ;
    }
}
