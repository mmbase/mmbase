/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.lucene;

import org.mmbase.module.Module;
import org.mmbase.applications.crontab.*;
import org.mmbase.util.functions.Parameters;

/**
 * Can be scheduled in MMBase crontab.

 * @author Michiel Meeuwissen
 * @version $Id: FullIndex.java,v 1.3 2006-03-21 19:02:22 michiel Exp $
 **/
public class FullIndex implements CronJob {

    private String index;
    public void init(CronEntry entry) {
        index = entry.getConfiguration();
    }

    public void run() {
        Lucene lucene = (Lucene) Module.getModule("lucene");
        if (lucene != null) {
            Parameters params = lucene.fullIndexFunction.createParameters();
            params.set("index", index);
            lucene.fullIndexFunction.getFunctionValue(params);
        } else {
            throw new RuntimeException("Lucene module not found");
        }
    }
    public void stop() {
    }
}
