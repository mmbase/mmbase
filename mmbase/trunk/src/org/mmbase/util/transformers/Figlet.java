/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.transformers;
/**
 * An escaper based on the amusing 'figlet' tool, which is available on some system. (Make sure it
 * is in the 'PATH'.
 *
 * Figlet output is only fit for text/plain output, or perhaps for <pre> tags...
 *
 * @author Michiel Meeuwissen 
 * @since MMBase-1.7
 * @version $Id: Figlet.java,v 1.2 2003-05-12 21:21:26 michiel Exp $
 * @todo   no way to specify commandline arguments on the fly.
 */

public class Figlet extends AbstractCommandStringTransformer implements CharTransformer {
    protected String[] getCommand() {
        return new String[] {"figlet", "-w", "160"};
    }
}
