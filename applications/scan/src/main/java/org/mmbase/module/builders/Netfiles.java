/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;

/**
 * The Netfiles builder stores information on files that need to be transferred
 * to other servers.
 * It is used in the CACHE PAGE routine, that is used to cache SCAN pages.
 * Information in NetFiles is used by a number of VWMs (i.e. PageMaster and ImageMaster), which schedules the transfer
 * of the files.<br />
 * Each NetFile entry contains the following information:<br />
 * <ul>
 * <li><code>filename</code> : the name of the file</li>
 * <li><code>mmserver</code> : the server that should handle the file transfer</li>
 * <li><code>service</code> : the main 'service' to be performed.
 *             Together with subservice, this determines the VWM that handles the transfer,
 *             i.e. 'pages/main' is handled by the PageMaster VWM.</li>
 * <li><code>subservice</code> : the subservice to perform. i.e. in PageMaster, 'main' determines mirror sites and
 *                schedules tasks for mirroring (by creating netfile entries), while 'mirror'
 *                performs the actual transfer to a mirror</li>
 *                Often one VWM handles mutliple subservices, but this is not a given.</li>
 * <li><code>filesize</code> : the size of the file. Not currently used, value is always -1</li>
 * <li><code>ctime</code> : probably the time of creation. Currently not used (for future development). </li>
 * <li><code>ntime</code> : probably the last change time. Currently not used (for future development).</li>
 * <li><code>status</code> : The state of the netfile entry. This can be a {@link #STATUS_REQUEST} when a file waits to be transferred,
 *            {@link #STATUS_ON_ITS_WAY} when it is being transferred, {@link #STATUS_DONE} when
 *            the transfer was handled, {@link #STATUS_CHANGED} when a change occured in a file, (indicating it may
 *            become elligible for resending), and {@link #STATUS_CALC_PAGE} when the page needs to be recalculated
 *            (by SCAN).</li>
 * <li><code>crc</code> : Cyclic Redundancy Check. For use in checking file validity. Currently not used (for future development)</li>
 * </ul>
 *
 * @author Daniel Ockeloen
 * @version $Id$
 */
public class Netfiles extends MMObjectBuilder {
    /**
     * Status for a netfile indicating a service request
     */
    public static final int STATUS_REQUEST = 1;
    /**
     * Status for a netfile indicating its request is being handled
     */
    public static final int STATUS_ON_ITS_WAY = 2;
    /**
     * Status for a netfile indicating its request has been handled
     */
    public static final int STATUS_DONE = 3;
    /**
     * Status for a netfile indicating a change
     */
    public static final int STATUS_CHANGED = 4;
    /**
     * Status for a netfile indicating a request to be recalculated
     */
    public static final int STATUS_CALC_PAGE = 5;

    // Logger class
    private static Logger log = Logging.getLoggerInstance(Netfiles.class.getName());

    /**
     * Reference to the NetFileServ builder
     */
    NetFileSrv netfilesrv;

    /**
     * What should a GUI display for this node/field combo.
     * For "status', it returns a description of the current netfile state.
     * Otherwise it returns <code>null</code>.
     * @param node The node to display
     * @param field the name field of the field to display
     * @return the display of the node's field as a <code>String</code>, null if not specified
     */
    @Override
    public String getGUIIndicator(String field, MMObjectNode node) {
        if (field.equals("status")) {
            int val=node.getIntValue("status");
            switch(val) {
                case 1: return "Verzoek";    // return "Request";
                case 2: return "Onderweg";   // return "On Its Way";
                case 3: return "Gedaan";     // return "Done";
                case 4: return "Aangepast";  // return "Changed";
                case 5: return "CalcPage";   // return "Recalculate";
                default: return "Onbepaald"; // return "Unknown";
            }
        }
        return null;
    }

    /**
     * Called when a remote node is changed.
     * This routine invokes the NetFileServ builder to handle the change (which involves
     * calling the VWM that handles the service).
     * Only active for certain servers.
     * The check what servers support this is currently specific for the VPRO and should be changed.
     * @param number Number of the changed node as a <code>String</code>
     * @param builder type of the changed node
     * @param ctype command type, 'c'=changed, 'd'=deleted', 'r'=relations changed, 'n'=new
     * @return <code>true</code> if maintenance was performed, <code>false</code> (the default) otherwise
     */
    @Override
    public boolean nodeRemoteChanged(String machine, String number, String builder, String ctype) {
        super.nodeRemoteChanged(machine, number,builder,ctype);
        // vpro-thingy, has to go
        if (mmb.getMachineName().equals("twohigh")) {
            log.debug("Change : "+number+" "+builder+" "+ctype);
            if (netfilesrv==null) {
                netfilesrv=(NetFileSrv)mmb.getMMObject("netfilesrv");
                if (netfilesrv!=null) netfilesrv.fileChange(number,ctype);
            } else {
                netfilesrv.fileChange(number,ctype);
           }
        }
        return true;
    }

    /**
     * Called when a local node is changed.
     * This routine invokes the NetFileServ builder to handle the change (which involves
     * calling the VWM that handles the service).
     * Only active for certain servers.
     * The check what servers support this is currently specific for the VPRO and should be changed.
     * @param number Number of the changed node as a <code>String</code>
     * @param builder type of the changed node
     * @param ctype command type, 'c'=changed, 'd'=deleted', 'r'=relations changed, 'n'=new
     * @return <code>true</code> if maintenance was performed, <code>false</code> (the default) otherwise
     */
    @Override
    public boolean nodeLocalChanged(String machine, String number, String builder, String ctype) {
        super.nodeLocalChanged(machine, number,builder,ctype);
        // vpro-thingy, has to go
        if (mmb.getMachineName().equals("twohigh")) {
            log.debug("Change : "+number+" "+builder+" "+ctype);
            if (netfilesrv==null) {
                netfilesrv=(NetFileSrv)mmb.getMMObject("netfilesrv");
                if (netfilesrv!=null) netfilesrv.fileChange(number,ctype);
            } else {
                netfilesrv.fileChange(number,ctype);
            }
        }
        return true;
    }
}
