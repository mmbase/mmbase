/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;

/**
 * A TEI class for Writer Tags that (on default) produce String jsp vars.
 *
 * @author Michiel Meeuwissen
 * @version $Id: StringWriterTEI.java,v 1.3 2003-06-06 10:03:09 pierre Exp $ 
 */

public class StringWriterTEI extends  WriterTEI {
    protected String defaultType() {
        return "String";
    }        
}
