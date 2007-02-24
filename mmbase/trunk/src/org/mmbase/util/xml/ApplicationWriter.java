/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml;

import java.io.File;
import java.util.*;

import org.w3c.dom.*;

import org.mmbase.module.core.*;
import org.mmbase.model.*;
import org.mmbase.storage.search.SearchQueryException;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.applicationdata.ContextDepthDataWriter;
import org.mmbase.util.xml.applicationdata.FullBackupDataWriter;
import org.mmbase.util.*;

/**
 * @javadoc
 * @deprecation-used Can use Xerces functionality to write an XML, isn't it? Should at least use StringBuffer.
 * @author DAniel Ockeloen
 * @version $Id: ApplicationWriter.java,v 1.5 2007-02-24 21:57:50 nklasens Exp $
 */
public class ApplicationWriter extends DocumentWriter  {

    private static final Logger log = Logging.getLoggerInstance(ApplicationWriter.class);

    protected ApplicationReader reader;
    protected MMBase mmbase;

    /**
     * Constructs the document writer.
     * The constructor calls its super to  create a basic document, based on the application document type.
     * @param reader the reader for the original application
     * @param mmbase the mmbase instance to get the application data from
     */
    public ApplicationWriter(ApplicationReader reader, MMBase mmbase) throws DOMException {
        super("application", ApplicationReader.PUBLIC_ID_APPLICATION,
                        XMLEntityResolver.DOMAIN + XMLEntityResolver.DTD_SUBPATH + ApplicationReader.DTD_APPLICATION);
        this.reader = reader;
        this.mmbase = mmbase;
        getMessageRetriever("org.mmbase.util.xml.resources.applicationwriter");
    }

    /**
     * Generates the document. Can only be called once.
     * @throws DOMException when an error occurred during generation
     */
    protected void generate() throws DOMException {
        Element root = document.getDocumentElement();
        addComment("application.configuration", reader.getName(), reader.getDescription(), root);
        root.setAttribute("name", reader.getName());
        root.setAttribute("maintainer", reader.getMaintainer());
        root.setAttribute("version", "" + reader.getVersion());
        root.setAttribute("autodeploy", "" + reader.hasAutoDeploy());

        addRequirements(root);
        addNeededBuilderList(root);
        addNeededRelDefList(root);
        addAllowedRelationList(root);
        addDataSourceList(root);
        addRelationSourceList(root);
        addContextSourceList(root);

        addComment("application.installnotice",root);
        addContentElement("installnotice",reader.getInstallNotice(),root);
        addComment("application.description",root);
        addContentElement("description",reader.getDescription(),root);
    }

    protected void addRequirements(Element root) {
        addComment("application.requirements",root);
        Element elementSet = document.createElement("requirements");
        root.appendChild(elementSet);
        List<Map<String,String>> requirements = reader.getRequirements();
        for (Map<String, String> bset : requirements) {
            Element requirement = document.createElement("requirement");
            elementSet.appendChild(requirement);
            requirement.setAttribute("name", bset.get("name"));
            requirement.setAttribute("maintainer", bset.get("maintainer"));
            requirement.setAttribute("version", bset.get("version"));
            String type = bset.get("type");
            if (type == null) type = "application";
            requirement.setAttribute("type", type);
        }
    }

    protected void addNeededBuilderList(Element root) {
        addComment("application.neededbuilderlist",root);
        Element elementSet = document.createElement("neededbuilderlist");
        root.appendChild(elementSet);
        List<Map<String,String>> builders = reader.getNeededBuilders();
        for (Map<String, String> bset : builders) {
            Element builder = document.createElement("builder");
            elementSet.appendChild(builder);
            builder.setAttribute("name", bset.get("name"));
            builder.setAttribute("maintainer", bset.get("maintainer"));
            builder.setAttribute("version", bset.get("version"));
        }
    }

    protected void addNeededRelDefList(Element root) {
        addComment("application.neededreldeflist",root);
        Element elementSet = document.createElement("neededreldeflist");
        root.appendChild(elementSet);
        List<Map<String,String>> reldefs = reader.getNeededRelDefs();
        for (Map<String, String> bset : reldefs) {
            Element reldef = document.createElement("reldef");
            elementSet.appendChild(reldef);
            reldef.setAttribute("source", bset.get("source"));
            reldef.setAttribute("target", bset.get("target"));
            reldef.setAttribute("direction", bset.get("direction"));
            reldef.setAttribute("guisourcename", bset.get("guisourcename"));
            reldef.setAttribute("guitargetname", bset.get("guitargetname"));
            String builder = bset.get("builder");
            if (builder != null) {
                reldef.setAttribute("builder", builder);
            }
        }
    }

    protected void addAllowedRelationList(Element root) {
        addComment("application.allowedrelationlist",root);
        Element elementSet = document.createElement("allowedrelationlist");
        root.appendChild(elementSet);
        List<Map<String,String>> relations = reader.getAllowedRelations();
        for (Map<String, String> bset : relations) {
            Element relation = document.createElement("relation");
            elementSet.appendChild(relation);
            relation.setAttribute("from", bset.get("from"));
            relation.setAttribute("to", bset.get("to"));
            relation.setAttribute("type", bset.get("type"));
        }
    }

    protected void addDataSourceList(Element root) {
        addComment("application.datasourcelist",root);
        Element elementSet = document.createElement("datasourcelist");
        root.appendChild(elementSet);
        List<Map<String,String>> sources = reader.getDataSources();
        for (Map<String, String> bset : sources) {
            Element source = document.createElement("datasource");
            elementSet.appendChild(source);
            source.setAttribute("path", bset.get("path"));
            source.setAttribute("builder", bset.get("builder"));
        }
    }

    protected void addRelationSourceList(Element root) {
        addComment("application.relationsourcelist",root);
        Element elementSet = document.createElement("relationsourcelist");
        root.appendChild(elementSet);
        List<Map<String,String>> sources = reader.getRelationSources();
        for (Map<String, String> bset : sources) {
            Element source = document.createElement("relationsource");
            elementSet.appendChild(source);
            source.setAttribute("path", bset.get("path"));
            source.setAttribute("builder", bset.get("builder"));
        }
    }

    protected void addContextSourceList(Element root) {
        addComment("application.contextsourcelist",root);
        Element elementSet = document.createElement("contextsourcelist");
        root.appendChild(elementSet);
        List<Map<String,String>> sources = reader.getContextSources();
        for (Map<String, String> bset : sources) {
            Element source = document.createElement("contextsource");
            elementSet.appendChild(source);
            source.setAttribute("path", bset.get("path"));
            source.setAttribute("type", bset.get("type"));
            source.setAttribute("goal", bset.get("goal"));
        }
    }

    /**
     * Generates the documents for this application and store it as a set of files in the given path.
     * @param targetPath the filepath (directory) where the configuration is to be stored
     * @param logger This thing must receive the errors
     * @throws TransformerException if one or more documents are malformed
     * @throws IOException if one or more files cannot be written
     * @throws SearchQueryException if data could not be obtained from the database
     */
    public void writeToPath(String targetPath, Logger logger) throws SearchQueryException {
        //writeToFile(targetPath + "/" + reader.getName() + ".xml");
	CloudModel cm = ModelsManager.getModel(reader.getName());
	log.info("CMW="+cm);
        if (cm!=null) cm.writeToFile(targetPath + "/" + reader.getName() + ".xml");

        // now the tricky part starts figure out what nodes to write
        writeDateSources(targetPath, logger);
        // now write the context files itself
        writeContextSources(targetPath);
        // now as a backup write all the needed builders
        // that the application maker claimed we needed
        writeBuilders(targetPath, logger);
        logger.info("Writing Application file : " + targetPath + "/" + reader.getName() + ".xml");
    }

    private void writeDateSources(String targetPath, Logger logger) throws SearchQueryException  {
        List<Map<String,String>> sources = reader.getContextSources();
        for (Map<String, String> bset : sources) {
            String path = bset.get("path");
            String type = bset.get("type");
            String goal = bset.get("goal");

            logger.info("save type : " + type);
            logger.info("save goal : " + goal);

            if (type.equals("depth")) {
                XMLContextDepthReader contextReader = new XMLContextDepthReader(MMBaseContext.getConfigPath() + "/applications/" + path);
                ContextDepthDataWriter.writeContext(reader, contextReader, targetPath, mmbase, logger);
            } else if (type.equals("full")) {
                FullBackupDataWriter.writeContext(reader, targetPath, mmbase, logger);
            }
        }
    }

    private void writeContextSources(String targetPath) {
        List<Map<String,String>> sources = reader.getContextSources();
        for (Map<String, String> bset : sources) {
            String path = bset.get("path");
            String type = bset.get("type");
            if (type.equals("depth")) {
                XMLContextDepthReader contextReader = new XMLContextDepthReader( MMBaseContext.getConfigPath() + "/applications/" + path);
                ContextDepthDataWriter.writeContextXML(contextReader, targetPath + "/" + path);
            }
        }
    }

    private void writeBuilders(String targetPath, Logger logger) {
        // create the dir for the Data & resource files
        File file = new File(targetPath + "/" + reader.getName() + "/builders");
        file.mkdirs();
	// get the default model.
	CloudModel cm = ModelsManager.getModel("default");
	log.info("CM="+cm);
        List<Map<String,String>> builders = reader.getNeededBuilders();
        for (Map<String, String> bset : builders) {
            String name = bset.get("name");
            MMObjectBuilder builder = mmbase.getBuilder(name);
            if (builder != null) {
                logger.info("save builder : " + name);
		CloudModelBuilder cmb = cm.getModelBuilder(name);
		cmb.writeToFile(targetPath + "/" + reader.getName() + "/builders/" + name + ".xml");
		
		/*
                BuilderWriter builderOut = new BuilderWriter(builder);
                builderOut.setIncludeComments(includeComments());
                builderOut.setExpandBuilder(false);
                builderOut.writeToFile(targetPath + "/" + reader.getName() + "/builders/" + name + ".xml");
		*/
            }
        }
    }

}
