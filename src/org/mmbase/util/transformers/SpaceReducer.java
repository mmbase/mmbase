/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.transformers;

import java.io.Reader;
import java.io.Writer;

import org.mmbase.util.logging.*;

/**
 * Replace 1 or more spaces by 1 space, and 1 or more newlines by 1
 * newline. Any other combination of newlines and spaces is replaced
 * by one newline. Except if they are in between "<pre>" and
 * "</pre>". (Note: perhaps this last behaviour should be made
 * configurable.
 *
 * @todo 'pre' stuff not yet implemented
 *
 * @author Michiel Meeuwissen 
 * @since MMBase-1.7
 */

public class SpaceReducer extends ReaderTransformer implements CharTransformer {

    private static Logger log = Logging.getLoggerInstance(SpaceReducer.class.getName());

    public Writer transform(Reader r, Writer w) {

        int space = 0;  // 'open' spaces
        int nl = 0;     // 'open' newlines
        
        StringBuffer indent = new StringBuffer();  // 'open' indentation of white-space
        int l = 0; // number of non-white-space (letter) on the current line

        int lines = 0; // for debug: the total number of lines read.
        try {
            log.debug("Starting spacereducing");
            while (true) {
                int c = r.read();
                if (c == -1) break;
                if (c == '\n') {
                    if (nl == 0) w.write(c);                    
                    space++;
                    nl++;
                    l = 0;
                } else if (Character.isWhitespace((char) c)) {
                    if (space == 0 && l > 0) w.write(' ');
                    if (l == 0) indent.append((char) c);
                    space ++;
                } else {                
                    if (l == 0 && space > 0) {
                        w.write(indent.toString());
                        indent.setLength(0);
                    }
                    space = 0; lines += nl; nl = 0; l++;
                    
                    w.write(c);
                }
            }
            log.debug("Finished: read " + lines + " lines");
        } catch (java.io.IOException e) {
            log.error(e.toString());
        }
        return w;
    }


    public String toString() {
        return "SPACEREDUCER";
    }
}
