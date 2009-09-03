 /*

 This software is OSI Certified Open Source Software.
 OSI Certified is a certification mark of the Open Source Initiative.

 The license (Mozilla version 1.0) can be read at the MMBase site.
 See http://www.MMBase.org/license
 */

package org.mmbase.applications.media;

import org.mmbase.bridge.*;
import org.mmbase.datatypes.processors.CommitProcessor;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class FragmentTypeFixer implements CommitProcessor {
    private static Logger log = Logging.getLoggerInstance(FragmentTypeFixer.class);

    private static final long serialVersionUID = 1L;

    public void commit(Node node, Field field) {
        if (! node.isNew()) {
            Cloud ntCloud = node.getCloud().getNonTransactionalCloud();
            Node ntNode = ntCloud.getNode(node.getNumber());
            NodeList fragments = ntNode.getRelatedNodes(ntCloud.getNodeManager("mediafragments"), "related", "source");
            NodeManager targetType = ntCloud.getNodeManager(ntNode.getNodeManager().getProperty("org.mmbase.media.containertype"));
            if (targetType == null) {
                throw new IllegalStateException("No such node manager " + ntNode.getNodeManager().getProperty("org.mmbase.media.containertype") + " (container type of " + ntNode.getNodeManager().getName() + ", node " + node.getNumber() + ")");
            }
            for (Node fragment : fragments) {
                if (fragment == null) {
                    log.error("Fragment is null?");
                    continue;
                }
                if (! fragment.getNodeManager().equals(targetType)) {
                    log.info("Fixing fragment type of " + node.getNumber() + " fragment " + fragment + " Setting from " + fragment.getNodeManager().getName() + " " + targetType.getName() + " (because " + ntNode.getNodeManager().getName() + " " + ntNode.getNumber() + ")");
                    fragment.setNodeManager(targetType);
                    fragment.commit();
                } else {
                    log.debug("Fragment of " + node.getNumber() + " has correct fragment already");
                }
            }
        }

    }


}

