/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.module.builders;

import java.util.*;

import org.mmbase.module.core.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;

/**
 * This builder implements additional functionality and methods to handle
 * community objects.
 * Added functionality involve opening and closing all channels related to
 * this community, and expanding a URL obtained form the 'maps' builder with
 * a community number (SCAN only).
 *
 * @author Dirk-Jan Hoekstra
 * @author Pierre van Rooden
 * @version 28 May 2001
 */

public class Community extends MMObjectBuilder {

    /** Community type : chatbox */
    public static final String STR_CHATBOX = "chatbox";
    /** Community type : forum */
    public static final String STR_FORUM = "forum";

    //logger
    private static Logger log = Logging.getLoggerInstance(Community.class.getName());

    private Channel channelBuilder;
    private MMObjectBuilder mapBuilder;

    /**
     * Constructor
     */
    public Community() {
    }

    /**
     * Initializes the builder.
     * Retrieves associated builders.
     * @return Always true.
     */
    public boolean init() {
        boolean result = super.init();
        mapBuilder = mmb.getMMObject("maps");
        channelBuilder = (Channel)mmb.getMMObject("channel");
        return result;
    }

    /**
     * Opens all the channels that are connected to this community
     *
     * @param community The community node of which to open all the channels.
     */
    public void openAllCommunities() {
        //ensure the communitybuilder is initialized.
        init();
        if (channelBuilder == null) {
            log.error("No channel builder");
            return;
        }
        ClusterBuilder cluster=mmb.getClusterBuilder();

        Vector builders = new Vector();
        builders.add("community");
        builders.add("channel");

        Vector fields = new Vector();
        fields.add("community.number");
        fields.add("channel.number");
        Vector allchannels=cluster.searchMultiLevelVector(null,fields,"YES",builders,
               "WHERE channel.open = "+channelBuilder.OPEN+" OR channel.open = "+channelBuilder.WANT_OPEN,
               null,null,ClusterBuilder.SEARCH_EITHER);
        if (allchannels!=null) {
            for (Iterator channels=allchannels.iterator(); channels.hasNext(); ) {
                MMObjectNode channel = (MMObjectNode)channels.next();
                log.info("open channel"+channel);
                channelBuilder.open(channel.getNodeValue("channel"),channel.getNodeValue("community"));
            }
        }
    }

    /**
     * Opens all the channels that are connected to this community
     *
     * @param community The community node of which to open all the channels.
     */
    public void openAllChannels(MMObjectNode community) {
        if (channelBuilder == null) {
            log.error("No channel builder");
            return;
        }
        Enumeration relatedChannels = mmb.getInsRel().getRelated(community.getNumber(), channelBuilder.oType);
        while (relatedChannels.hasMoreElements()) {
            channelBuilder.open((MMObjectNode)relatedChannels.nextElement());
        }
    }

    /**
     * Closes all the channels of the community.
     *
     * @param community The community of which to close all the channels.
     */
    public void closeAllChannels(MMObjectNode community) {
        if (channelBuilder == null) {
            log.error("No channel builder");
            return;
        }
        Enumeration relatedChannels = mmb.getInsRel().getRelated(community.getNumber(), channelBuilder.oType);
        while (relatedChannels.hasMoreElements()) {
            channelBuilder.close((MMObjectNode)relatedChannels.nextElement());
        }
    }

    /**
     * Handles the $MOD-MMBASE-BUILDER-community-commands.
     * Commands handled by this command are:
     * <ul>
     * <li> communitynr-OPEN : opens all channels that are connected to this community</li>
     * <li> communitynr-CLOSE: closes all channels that are connected to this community</li>
     * </ul>
     * @param scanpage the current page context
     * @param tok the tokenized command
     * @return the empty string
     */
    public String replace(scanpage sp, StringTokenizer tok) {
        // The first thing we expect is a community number.
        if (!tok.hasMoreElements()) {
            log.error("replace(): community number expected after $MOD-BUILDER-community-.");
            return "";
        }
        MMObjectNode community = getNode(tok.nextToken());

        if (tok.hasMoreElements()) {
            String cmd = tok.nextToken();
            if (cmd.equals("OPEN")) openAllChannels(community);
            if (cmd.equals("CLOSE")) closeAllChannels(community);
        }
        return "";
    }

    /**
     * Retrieves a URL from a related Map object, and append the community number
     * to the URL.
     * This requires the presence of a 'maps' builder, which should have a
     * functional 'getDefaultURL' method.
     *
     * @param src The number of the community MMObjectNode.
     * @return the resulting URL, or <code>null</code> if not map-node was
     *         associated with this community.
     * @deprecated There is no maps definition available in cvs.
     *    In addition, this method only produces SCAN-format urls.
     */
    public String getDefaultUrl(int src) {
        if (mapBuilder==null) return null;
        Enumeration e= mmb.getInsRel().getRelated(src, mapBuilder.oType);
        if (!e.hasMoreElements()) {
            log.debug("GetDefaultURL Could not find related map for community node " + src);
            return null;
        }
        MMObjectNode mapNode = (MMObjectNode)e.nextElement();
        String URL = mapBuilder.getDefaultUrl(mapNode.getNumber());
        if (URL!=null) {
            URL += "+" + src;
        }
        return URL;
    }
}
