/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util.magicfile;

/**
 * DetectorProvider classes are meant to provide a list of Detectors,
 * which can be used by MagicFile.
 * @version $Id: DetectorProvider.java,v 1.2 2007-02-24 21:57:50 nklasens Exp $
 * @author Michiel Meeuwissen
 */

public interface DetectorProvider {
    public java.util.List<Detector> getDetectors();
}

