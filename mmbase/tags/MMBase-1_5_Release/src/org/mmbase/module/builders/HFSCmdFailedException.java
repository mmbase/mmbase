/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

/**
 * @author David V van Zeventer
 * @version 6 Jan 1999
 */
public class HFSCmdFailedException extends Exception {
        public String errval;
        public String explanation;

        public HFSCmdFailedException(String errval,String explanation){
                this.errval = errval;
                this.explanation = explanation;
        }
}

