/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml.applicationdata;

import java.io.File;
import java.util.*;

import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.util.logging.*;

/**
 * This is used to export a full backup, by writing all nodes to XML.
 *
 * @since MMBase-1.8
 * @author Pierre van Rooden
 * @version $Id$
 */
public class FullBackupDataWriter {

    /**
     * Logging instance
     */
    private static final Logger log = Logging.getLoggerInstance(FullBackupDataWriter.class);

    /**
     * Writes all nodes to XML.
     * @param reader A <code>ApplicationReader</code> initialised to read the application's description (xml) file
     * @param targetPath The path where to save the application
     * @param mmbase Reference to the MMbase processormodule. Used to retrieve the nodes to write.
     * @param logger Storage for messages which can be displayed to the user.
     * @throws SearchQueryException if data could not be obtained from the database
     */
    public static void writeContext(ApplicationReader reader, String targetPath, MMBase mmbase, Logger logger) throws SearchQueryException {
        // Create directory for data files.
        String subTargetPath = targetPath + "/" + reader.getName() + "/";
        File file = new File(subTargetPath);
        file.mkdirs();
        // Write the nodes
        writeNodes(subTargetPath, mmbase, logger);
        logger.info("Full backup finished.");
    }

    /**
     * Searches the MMBase cloud, collecting all nodes and storing them in data files.
     *
     * @param subTargetPath The path where to save the application data
     * @param mmbase MMBase object used to retrieve builder information
     * @param logger Storage for messages which can be displayed to the user.
     * @throws SearchQueryException if data could not be obtained from the database
     */
    static void writeNodes(String subTargetPath, MMBase mmbase, Logger logger) throws SearchQueryException {
        for (MMObjectBuilder element : mmbase.getBuilders()) {
            MMObjectBuilder builder = element;

            // Skip virtual builders and a set of system builders
            String builderName = builder.getTableName();
            if (builder.isVirtual() ||
                builderName.equals("reldef") ||
                builderName.equals("typerel") ||
                builderName.equals("versions") ||
                builderName.equals("syncnodes") ||
                builderName.equals("daymarks") ||
                builderName.equals("oalias") ||
                builderName.equals("icaches")) {
                continue;
            }
            boolean isRelation = builder instanceof InsRel;

            NodeSearchQuery query = new NodeSearchQuery(builder);
            StepField otypeField = query.getField(builder.getField(MMObjectBuilder.FIELD_OBJECT_TYPE));
            BasicFieldValueConstraint constraint = new BasicFieldValueConstraint(otypeField, builder.getObjectType());
            query.setConstraint(constraint);

            // Add this builder's nodes to set (by nodenumber).
            List<MMObjectNode> nodes = builder.getStorageConnector().getNodes(query, false);
            writeNodes(subTargetPath, mmbase, logger, builder, nodes, isRelation);
        }
    }

    /**
     * Writes the nodes of a particular type to the corresponding XML file.
     * @param builder The builder.
     * @param nodes The nodes, must type corresponding to the builder.
     * @param subTargetPath Path where the XML file is written.
     * @param mmbase MMBase object used to retrieve builder information
     * @param logger Used to store messages that can be shown to the user
     * @param isRelation Indicates whether the nodes to write are data (false) or relation (true) nodes
     */
    static void writeNodes(String subTargetPath, MMBase mmbase, Logger logger, MMObjectBuilder builder, List<MMObjectNode> nodes, boolean isRelation) {

        // Create nodewriter for this builder
        NodeWriter nodeWriter = new NodeWriter(mmbase, logger, subTargetPath, builder.getTableName(), isRelation);

        Iterator<MMObjectNode> iNodes = nodes.iterator();
        int nrWritten = 0;
        while (iNodes.hasNext()) {
            MMObjectNode node =  iNodes.next();
            // Write node if it's of the correct type.
            if (node.getBuilder() == builder) {
                nodeWriter.write(node);
                nrWritten++;
            }
        }
        nodeWriter.done();

        log.debug("Builder " + builder.getTableName() + ": " + nrWritten + (isRelation ? " relations" : " nodes") + " written.");
    }

}
