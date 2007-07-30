/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.transformers;

import java.io.*;

import org.mmbase.util.logging.*;

/**

 * @author Michiel Meeuwissen
 * @since MMBase-1.9
 * @version $Id: BufferedReaderTransformer.java,v 1.3 2007-07-30 16:48:09 michiel Exp $
 */

public abstract class BufferedReaderTransformer extends ReaderTransformer implements CharTransformer {

    private static Logger log = Logging.getLoggerInstance(BufferedReaderTransformer.class);

    /**
     * Override {@link #transform(PrintWriter, String)}
     */
    public final Writer transform(Reader r, Writer w) {
        try {
            BufferedReader br = new BufferedReader(r);
            PrintWriter bw = new PrintWriter(new BufferedWriter(w));

            String line = br.readLine();
            while (line != null) {
                transform(bw, line);
                line = br.readLine();
                if (line != null) bw.write('\n');
            }
            br.close();
            bw.flush();
        } catch (java.io.IOException e) {
            log.error(e.toString(), e);
        }
        return w;
    }

    protected abstract void transform(PrintWriter bw, String line);

}
