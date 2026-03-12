/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.tools;

import java.util.*;
import java.util.Map.Entry;

import org.mmbase.bridge.Field;
import org.mmbase.cache.NodeCache;
import org.mmbase.core.CoreField;
import org.mmbase.module.builders.Versions;
import org.mmbase.module.core.*;
import org.mmbase.core.event.EventManager;
import org.mmbase.module.corebuilders.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.util.*;
import org.mmbase.util.xml.applicationdata.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.xml.sax.InputSource;


/**
 * Application installations functionality of MMAdmin.
 *
 * @author Nico Klasens
 * @since MMBase-1.8
 * @version $Id$
 */
class ApplicationInstaller {

    private static final Logger log = Logging.getLoggerInstance(ApplicationInstaller.class);
    private static final String EXPORTSOURCE = "___exportsource";

    private final MMBase mmb;
    private final MMAdmin admin;

    public ApplicationInstaller(MMBase mmb, MMAdmin admin) {
        this.mmb = mmb;
        this.admin = admin;
    }

    public void installApplications() throws SearchQueryException {
        ResourceLoader applicationLoader = ResourceLoader.getConfigurationRoot().getChildResourceLoader("applications");
        for (String appResource :  applicationLoader.getResourcePaths(ResourceLoader.XML_PATTERN, false)) {
            ApplicationResult result = new ApplicationResult();
            if (!installApplication(appResource.substring(0, appResource.length() - 4), -1, null, result, new HashSet<String>(), true)) {
                log.error("Problem installing application : " + appResource + ", cause: " + result.getMessage());
            }
        }
        EventManager.getInstance().propagateEvent(new ApplicationsInstalledEvent());
    }

    /**
     * Installs the application
     * @param applicationName Name of the application file, without the xml extension
     *                        This is also assumed to be the name of teh application itself
     *                        (if not, a warning will be issued)
     * @param result the result object, containing error messages when the installation fails,
     * or the installnotice if succesfull or already installed
     * @param installationSet set of installations that are currently being installed.
     *                        used to check if there are circular dependencies
     * @param autoDeploy if true, the installation is only installed if the application is set to autodeploy
     * @return true if succesfull, false otherwise
     */
    public boolean installApplication(String applicationName, int requiredVersion,
            String requiredMaintainer, ApplicationResult result, Set<String> installationSet,
            boolean autoDeploy) throws SearchQueryException {


        if (installationSet.contains(applicationName)) {
            return result.error("Circular reference to application with name " + applicationName);
        }

        ApplicationReader reader = getApplicationReader(applicationName);
        if (reader != null) {
            Versions ver = (Versions) mmb.getBuilder("versions");
            if (ver == null) throw new IllegalStateException("No versions builder");
            // test autodeploy
            if (autoDeploy) {
                if (!reader.hasAutoDeploy()) {
                    return true;
                } else {
                    if (admin.getIgnoredAutodeployApplications().contains(applicationName)) {
                        log.info("Ignoring auto-deploy '" + applicationName + "' because specified as ignore-auto-deploy parameter of mmadmin");
                        return true;
                    }
                }
            }
            String name = reader.getName();
            String maintainer = reader.getMaintainer();
            if (requiredMaintainer != null && !maintainer.equals(requiredMaintainer)) {
                return result.error("Install error: " + name + " requires maintainer '" + requiredMaintainer +
                                    "' but found maintainer '" + maintainer + "'");
            }
            int version = reader.getVersion();
            if (requiredVersion != -1 && version != requiredVersion) {
                return result.error("Install error: " + name + " requires version '" + requiredVersion +
                                    "' but found version '" + version + "'");
            }
            int installedVersion = ver.getInstalledVersion(name, "application");
            if (installedVersion == -1 || version > installedVersion) {
                if (!name.equals(applicationName)) {
                    result.warn("Application name " + name + " not the same as the base filename " + applicationName + ".\n"
                                + "This may cause problems when referring to this application.");
                }
                // We should possibly check whether the maintainer is valid here (see sample code below).
                // There is currently no way to do this, though, unless we use awful queries.
                // what we need is a getInstalledMaintainer() method on the Versions builder
                /* sample code
                String installedMaintainer=ver.getInstalledMaintainer(name,"application");
                if (!maintainer.equals(installedAppMaintainer)) {
                    return result.error("Install error: "+name+" is of maintainer '"+maintainer+"' but installed application is of maintainer '"+installedMaintainer+"'");
                }
                 */
                // should be installed - add to installation set
                installationSet.add(applicationName);
                List<Map<String,String>> requires = reader.getRequirements();
                for (Map<String, String> reqapp : requires) {
                    String reqType = reqapp.get("type");
                    if (reqType == null || reqType.equals("application")) {
                        String appName = reqapp.get("name");
                        int installedAppVersion = ver.getInstalledVersion(appName, "application");
                        String appMaintainer = reqapp.get("maintainer");
                        int appVersion = -1;
                        try {
                            String appVersionAttr = reqapp.get("version");
                            if (appVersionAttr != null) {
                                appVersion = Integer.parseInt(appVersionAttr);
                            }
                        } catch (Exception e) {}
                        if (installedAppVersion == -1 || appVersion > installedAppVersion) {
                            log.service("Application '" + applicationName + "' requires : " + appName);
                            if (!installApplication(appName, appVersion, appMaintainer,
                                result, installationSet, false)) {
                                return false;
                            }
                        } else if (appMaintainer != null) {
                            // we should possibly check whether the maintainer is valid here (see sample code below).
                            // There is currently no way to do this, though, unless we use awful queries.
                            // what we need is a getInstalledMaintainer() method on the Versions builder
                            /* sample code
                            String installedAppMaintainer=ver.getInstalledMaintainer(name,"application");
                            if (!appMaintainer.equals(installedAppMaintainer)) {
                                return result.error("Install error: "+name+" requires maintainer '"+appMaintainer+"' but found maintainer '"+installedAppMaintainer+"'");
                            }
                             */
                        }
                    }
                }
                // note: currently name and application file name should be the same
                if (installedVersion == -1) {
                    log.info("Installing application : " + name);
                } else {
                    log.info("installing application : " + name + " new version from " + installedVersion + " to " + version);
                }
                if (installBuilders(reader.getNeededBuilders(), "applications/" + applicationName, result)
                    && installRelDefs(reader.getNeededRelDefs(), result)
                    && installAllowedRelations(reader.getAllowedRelations(), result)
                    && installDataSources(reader.getDataSources(), applicationName, result)
                    && installRelationSources(reader.getRelationSources(), applicationName, result)
                    && runAfterDeployment(reader.getAfterDeployment(), installedVersion, version, result)
                    ) {
                    if (installedVersion == -1) {
                        ver.setInstalledVersion(name, "application", maintainer, version);
                    } else {
                        ver.updateInstalledVersion(name, "application", maintainer, version);
                    }

                    log.info("Application '" + name + "' deployed successfully.");
                    result.success("Application loaded oke\n\n"
                                   + "The application has the following install notice for you : \n\n"
                                   + reader.getInstallNotice());
                }
                // installed or failed - remove from installation set
                installationSet.remove(applicationName);
            } else {
                // only return this message if the application is the main (first) application
                // and if it was not auto-deployed (as in that case messages would not be deemed very useful)
                if (installationSet.size() == 1) {
                    result.success(
                        "Application was allready loaded (or a higher version)\n\n"
                            + "To remind you here is the install notice for you again : \n\n"
                            + reader.getInstallNotice());
                }
            }
        } else {
            result.error("Install error: can't find xml file: applications/" + applicationName + ".xml");
        }
        return result.isSuccess();
    }

    /**
     * @javadoc
     * @since MMBase-1.7
     */
    protected boolean installDataSources(List<Map<String,String>> dataSources, String appName, ApplicationResult result) throws SearchQueryException {
        MMObjectBuilder syncbul = mmb.getBuilder("syncnodes");
        if (syncbul != null) {

            List<MMObjectNode> nodeFieldNodes = new ArrayList<MMObjectNode>(); // a temporary list with all nodes that have NODE fields, which should be synced, later.
            for (Map<String, String> bh : dataSources) {
                NodeReader nodeReader = getNodeReader(bh, appName);
                if (nodeReader == null) {
                    continue;
                } else {
                    installDatasource(syncbul, nodeReader, nodeFieldNodes, result);
                }
            }

            if (!treatNodeFields(nodeFieldNodes, syncbul)) {
                return result.error("Could not sync all node fields");
            }
            return result.isSuccess();
        } else {
            return result.error("Application installer : can't reach syncnodes builder"); //
        }
    }

    private void installDatasource(MMObjectBuilder syncbul, NodeReader nodeReader, List<MMObjectNode> nodeFieldNodes, ApplicationResult result) {
        String exportsource = nodeReader.getExportSource();
        int timestamp = nodeReader.getTimeStamp();

        nodeReader.setLoadBinaries(false);

        // loop all nodes , and add to syncnodes.
        for (Iterator<MMObjectNode> n = nodeReader.getNodes(mmb).iterator(); n.hasNext();) {
            try {
                MMObjectNode newNode = n.next();
                nodeReader.loadBinaryFields(newNode);

                int exportnumber = newNode.getIntValue("number");
                if (existsSyncnode(syncbul, exportsource, exportnumber)) {
                    // XXX To do : we may want to load the node and check/change the fields
                    log.debug("node already installed : " + exportnumber);
                } else {
                    newNode.setValue("number", -1);
                    int localnumber = doKeyMergeNode(syncbul, newNode, exportsource, result);
                    if (localnumber != -1) { // this node was not yet imported earlier
                        createSyncnode(syncbul, exportsource, timestamp, exportnumber, localnumber);
                        if (localnumber == newNode.getNumber()) {
                            findFieldsOfTypeNode(nodeFieldNodes, exportsource, newNode);
                        }
                    }
                    NodeCache.getCache().remove(localnumber);
                }
                n.remove();
            } catch (SearchQueryException sqe) {
                log.error(sqe);
            }
        }
    }


    /**
     * @since MMBase-1.9.2
     */
    private boolean runAfterDeployment(Map<Integer, ? extends Runnable> runnables, int installedVersion, int version, ApplicationResult result) {
        for (int i = installedVersion; i <= version; i++) {
            Runnable runnable = runnables.get(i);
            if (runnable != null) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return result.error(e.getMessage());
                }
            }
        }
        Runnable runnable = runnables.get(Integer.MAX_VALUE);
        if (runnable != null) {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return result.error(e.getMessage());
            }
        }
        return true;
    }

    private void findFieldsOfTypeNode(List<MMObjectNode> nodeFieldNodes, String exportsource, MMObjectNode newNode) {
        for (CoreField field : newNode.getBuilder().getFields()) {
            // Fields with type NODE
            if (field.getType() == Field.TYPE_NODE
                && ! field.getName().equals("number")) {
                newNode.storeValue(EXPORTSOURCE, exportsource);
                nodeFieldNodes.add(newNode);
                break;
            }
        }
    }

    private boolean treatNodeFields(List<MMObjectNode> nodeFieldNodes, MMObjectBuilder syncbul) throws SearchQueryException {
        boolean success = true;
        for (MMObjectNode importedNode: nodeFieldNodes) {
            for (CoreField def : importedNode.getBuilder().getFields()) {
                String fieldName = def.getName();
                if (def.getType() == Field.TYPE_NODE &&
                    !fieldName.equals("number") &&
                    !fieldName.equals("otype") &&
                    !fieldName.equals("snumber") &&
                    !fieldName.equals("dnumber") &&
                    !fieldName.equals("rnumber")
                   ) {
                    String exportsource = (String) importedNode.getValues().get(EXPORTSOURCE);
                    boolean s = updateFieldWithTypeNode(syncbul, importedNode, exportsource, fieldName);
                    if (! s) {
                        log.warn("Could not sync field " +  fieldName + " of " + importedNode);
                    }
                    success &= s;
                }
            }
            importedNode.storeValue(EXPORTSOURCE, null); // not needed any more
            if (importedNode.isChanged()) {
                importedNode.commit();
            }


        }
        return success;
    }

    /**
     * @javadoc !!!
     */
    private int doKeyMergeNode(MMObjectBuilder syncbul, MMObjectNode newNode, String exportsource, ApplicationResult result) throws SearchQueryException {
        MMObjectBuilder bul = newNode.getBuilder();
        if (bul != null) {
            Constraint constraint = null;
            NodeSearchQuery query = null;
            boolean nodeFieldsSuccess = true;
            for (CoreField def : bul.getFields()) {
                if (! def.inStorage()) continue;
                if (def.getType() == Field.TYPE_NODE
                    && ! def.getName().equals("number") // wtf this is not a NODE field!
                    && ! def.getName().equals("otype")) {

                    nodeFieldsSuccess &= updateFieldWithTypeNode(syncbul, newNode, exportsource, def.getName());
                }

                // generation of key constraint to check if there is a node already present.
                // if a node is present then we can't insert this one.
                if (def.isUnique()) {
                    int type = def.getType();
                    String name = def.getName();
                    if (type == Field.TYPE_STRING) {
                        String value = newNode.getStringValue(name);
                        if (query == null) {
                            query = new NodeSearchQuery(bul);
                        }
                        StepField field = query.getField(def);
                        Constraint newConstraint = new BasicFieldValueConstraint(field, value);
                        if (constraint == null) {
                            constraint= newConstraint;
                        } else {
                            BasicCompositeConstraint compConstraint = new BasicCompositeConstraint(CompositeConstraint.LOGICAL_AND);
                            compConstraint.addChild(constraint);
                            compConstraint.addChild(newConstraint);
                            constraint = compConstraint;
                        }
                    }
                }
            }
            if (query != null && constraint != null) {
                query.setConstraint(constraint);
                try {
                    List<MMObjectNode> nodes = bul.getNodes(query);
                    if (nodes.size() > 0) {
                        MMObjectNode oldNode = nodes.get(0);
                        return oldNode.getIntValue("number");
                    }
                } catch (SearchQueryException sqe) {
                    result.error("Application installer can't search builder storage (" + sqe.getMessage()+")");
                    return -1;
                }
            }

            int localnumber = newNode.insert("import");
            if (localnumber == -1) {
                result.error("Insert of node " + newNode + " failed.");
            }
            if (! nodeFieldsSuccess) {
                result.addMessage("Not all node fields were successfully merged already");
            }
            return localnumber;

        } else {
            result.error("Application installer can't find builder for : " + newNode);
            return -1;
        }
    }

   /**
    * update the field with the real node number of the referenced node
    *
    * @param syncbul syncnode builder
    * @param importedNode Node to update
    * @param exportsource export source of the node to update
    * @param fieldname name of the field
    * @return Whether it succeeded
    */
   private boolean updateFieldWithTypeNode(MMObjectBuilder syncbul,
                                           MMObjectNode importedNode,
                                           String exportsource,
                                           String fieldname) throws SearchQueryException {


       String exportNumber = (String) importedNode.getValues().get("__" + fieldname);
       if (exportNumber == null) {
           // resolved already?
           return true;
       }
       int exportNode = Integer.parseInt(exportNumber);

       List<MMObjectNode> syncnodes = getSyncnodes(syncbul, exportsource, exportNode);
       int localNumber;
       if (!syncnodes.isEmpty()) {
           MMObjectNode n2 = syncnodes.get(0);
           localNumber = n2.getIntValue("localnumber");
           // clean it up .
           importedNode.storeValue("__" + fieldname, null);
       } else {
           // The syncnode could not be found fill in temporary the syncbuilder itself
           // temporary put in another node number so that the node at least can be committed (the node field can be NOT
           // NULL)
           // This method is called a second time after all imports have been done, it should be possible to resolve it then.
           localNumber = syncbul.getNumber();
       }
       importedNode.setValue(fieldname, localNumber);
       return ! syncnodes.isEmpty();
   }

    /**
     * @javadoc
     */
    boolean installRelationSources(List<Map<String,String>> ds, String appname, ApplicationResult result) throws SearchQueryException {
        MMObjectBuilder syncbul = mmb.getBuilder("syncnodes");
        InsRel insRel = mmb.getInsRel();
        if (syncbul != null) {
            List<MMObjectNode> nodeFieldNodes = new ArrayList<MMObjectNode>(); // a temporary list with all nodes that have NODE fields, which should be synced, later.
            for (Map<String, String> bh : ds) {
                RelationNodeReader nodereader = getRelationNodeReader(appname, bh);
                if (nodereader == null) {
                    continue;
                } else {
                    installRelationSource(syncbul, insRel, nodereader, nodeFieldNodes, result);
                }
            }
            if (! treatNodeFields(nodeFieldNodes, syncbul)) {
                return result.error("Could not sync all node fields of relations");
            }
        } else {
            result.error("Application installer : can't reach syncnodes builder");
        }
        return result.isSuccess();
    }

    private void installRelationSource(MMObjectBuilder syncbul, InsRel insRel, RelationNodeReader nodereader, List<MMObjectNode> nodeFieldNodes, ApplicationResult result) {
        String exportsource = nodereader.getExportSource();
        int timestamp = nodereader.getTimeStamp();

        for (Iterator<MMObjectNode> n = (nodereader.getNodes(mmb)).iterator(); n.hasNext();) {
            try {
                MMObjectNode newNode = n.next();
                int exportnumber = newNode.getIntValue("number");

                if (existsSyncnode(syncbul, exportsource, exportnumber)) {
                    // XXX To do : we may want to load the relation node and check/change the fields
                    log.debug("node allready installed : " + exportnumber);
                } else {
                    newNode.setValue("number", -1);
                    // The following code determines the 'actual' (synced) numbers for the destination and source nodes
                    // This will normally work well, however:
                    // It is _theoretically_ possible that one or both nodes are _themselves_ relation nodes.
                    // (since relations are nodes).
                    // Due to the order in which syncing takles place, it is possible that such structures will fail
                    // to get imported.
                    // ye be warned.

                    // find snumber
                    int snumber = newNode.getIntValue("snumber");
                    List<MMObjectNode> snumberNodes = getSyncnodes(syncbul, exportsource, snumber);
                    if (!snumberNodes.isEmpty()) {
                        MMObjectNode n2 = snumberNodes.get(0);
                        snumber = n2.getIntValue("localnumber");
                    } else {
                        snumber = -1;
                    }
                    newNode.setValue("snumber", snumber);

                    // find dnumber
                    int dnumber = newNode.getIntValue("dnumber");
                    List<MMObjectNode> dnumberNodes = getSyncnodes(syncbul, exportsource, dnumber);
                    if (!dnumberNodes.isEmpty()) {
                        MMObjectNode n2 = dnumberNodes.get(0);
                        dnumber = n2.getIntValue("localnumber");
                    } else {
                        dnumber = -1;
                    }
                    newNode.setValue("dnumber", dnumber);

                    int localnumber = -1;
                    if (snumber != -1 && dnumber != -1) {
                        // test whether a relation with the proposed snumber/dnumber/rnumber already exists
                        // if so, skip this relation when the same
                        if (relationAlreadyExists(insRel, newNode, snumber, dnumber)) {
                            log.warn("Application tries to add relation which already exists. " +
                                    "Skipping relation with exportnumber " + exportnumber);
                        } else {
                            localnumber = newNode.insert("import");
                            if (localnumber != -1) {
                                createSyncnode(syncbul, exportsource, timestamp, exportnumber, localnumber);
                                if (localnumber == newNode.getNumber()) {
                                    findFieldsOfTypeNode(nodeFieldNodes, exportsource, newNode);
                                }
                            }
                        }
                    } else {
                        result.error("Cannot sync relation (exportnumber==" + exportnumber
                                + ", snumber:" + snumber + ", dnumber:" + dnumber + ")");
                    }
                }
            } catch (SearchQueryException sqe) {
                log.error(sqe);
            }
        }
    }

    private boolean existsSyncnode(MMObjectBuilder syncbul, String exportsource, int exportnumber) throws SearchQueryException {
        List<MMObjectNode> nodes = getSyncnodes(syncbul, exportsource, exportnumber);
        return !nodes.isEmpty();
    }

    private List<MMObjectNode> getSyncnodes(MMObjectBuilder syncbul, String exportsource, int exportnumber) throws SearchQueryException {
        NodeSearchQuery existQuery = new NodeSearchQuery(syncbul);
        BasicFieldValueConstraint constraint1 = new BasicFieldValueConstraint(existQuery.getField(syncbul.getField("exportnumber")), exportnumber);
        BasicFieldValueConstraint constraint2 = new BasicFieldValueConstraint(existQuery.getField(syncbul.getField("exportsource")), exportsource);
        BasicCompositeConstraint constraint = new BasicCompositeConstraint(CompositeConstraint.LOGICAL_AND);
        constraint.addChild(constraint1);
        constraint.addChild(constraint2);
        existQuery.setConstraint(constraint);
        List<MMObjectNode> nodes  = syncbul.getNodes(existQuery);
        if (nodes == null) {
            // could this happen?
            nodes = new ArrayList<MMObjectNode>();
        }
        return nodes;
    }

    private void createSyncnode(MMObjectBuilder syncbul, String exportsource, int timestamp, int exportnumber, int localnumber) {
        MMObjectNode syncnode = syncbul.getNewNode("import");
        syncnode.setValue("exportsource", exportsource);
        syncnode.setValue("exportnumber", exportnumber);
        syncnode.setValue("timestamp", timestamp);
        syncnode.setValue("localnumber", localnumber);
        syncnode.insert("import");
    }

    /**
     * This method uses the {@link ResourceLoader} to fetch an application by name. for this purpose
     * it requests the resource by adding <code>applications/</code> to the start of the appName and appends <code>.xml</core> to the end
     * @param appName the name of the application to be read.
     * @return the ApplicationReader for the application, or null is the application wat not found or an exception occured. In the later a message is logged
     */
    private ApplicationReader getApplicationReader(String appName) {
        String resourceName = appName + ".xml";
        try {
            ResourceLoader applicationLoader = ResourceLoader.getConfigurationRoot().getChildResourceLoader("applications");
            InputSource is = applicationLoader.getInputSource(resourceName);
            if (is == null) {
                return null;
            }
            return new ApplicationReader(is);
        } catch (Exception e) {
            log.error("error while reading application from resource " + resourceName  + " : " + e.getMessage() , e);
            return null;
        }
    }

    private NodeReader getNodeReader(Map<String,String> bh, String appName) {
        NodeReader nodeReader = null;

        String path = bh.get("path");
        ResourceLoader applicationLoader = ResourceLoader.getConfigurationRoot().getChildResourceLoader("applications");
        InputSource is = null;
        try {
            is = applicationLoader.getInputSource(path);
        } catch (Exception e) {
            log.info("No datasource resource " + path);
        }
        if (is != null) {
            nodeReader = new NodeReader(is, applicationLoader.getChildResourceLoader(appName));
        }
        return nodeReader;
    }

    private RelationNodeReader getRelationNodeReader(String appname, Map<String,String> bh) {
        RelationNodeReader nodereader = null;

        String path = bh.get("path");
        ResourceLoader applicationLoader = ResourceLoader.getConfigurationRoot().getChildResourceLoader("applications");
        InputSource is = null;
        try {
            is = applicationLoader.getInputSource(path);
        } catch (Exception e) {
            log.info("No relationsource resource " + path);
        }
        if (is != null) {
            nodereader = new RelationNodeReader(is, applicationLoader.getChildResourceLoader(appname));
        }
        return nodereader;
    }

    private boolean relationAlreadyExists(InsRel insRel, MMObjectNode newNode, int snumber, int dnumber) {
        boolean relationAlreadyExists = false;
        MMObjectNode testNode = insRel.getRelation(snumber, dnumber, newNode.getIntValue("rnumber"));
        if (testNode != null) {
            relationAlreadyExists = true;
            Map<String,Object> values = newNode.getValues();
            for (Entry<String, Object> entry : values.entrySet()) {
                String newFieldName = entry.getKey();
                if (!insRel.hasField(newFieldName)) {
                    Object newValue = entry.getValue();
                    Object testValue = testNode.getValue(newFieldName);
                    if (newValue == null) {
                        if (testValue != null) {
                            relationAlreadyExists = false;
                        }
                    } else if (!newValue.equals(testValue)) {
                        relationAlreadyExists = false;
                    }
                }
            }
        }
        return relationAlreadyExists;
    }

    /**
     * Checks and if required installs needed relation definitions.
     * Retrieves, for each reldef entry, the attributes, and passes these on to {@link #installRelDef}
     * @param reldefs a list of hashtables. Each hashtable represents a reldef entry, and contains a list of name-value
     *      pairs (the reldef attributes).
     * @return Always <code>true</code> (?)
     */
    private boolean installRelDefs(List<Map<String,String>> reldefs, ApplicationResult result) {
        for (Map<String, String> bh : reldefs) {
            String source = bh.get("source");
            String target = bh.get("target");
            String direction = bh.get("direction");
            String guisourcename = bh.get("guisourcename");
            String guitargetname = bh.get("guitargetname");
            // retrieve builder info
            int builder = -1;
            if (RelDef.usesbuilder) {
                String buildername = bh.get("builder");
                // if no 'builder' attribute is present (old format), use source name as builder name
                if (buildername == null) {
                    buildername = bh.get("source");
                }
                builder = mmb.getTypeDef().getIntValue(buildername);
            }
            // is not explicitly set to unidirectional, direction is assumed to be bidirectional
            if ("unidirectional".equals(direction)) {
                if (!installRelDef(source, target, 1, guisourcename, guitargetname, builder, result))
                    return false;
            } else {
                if (!installRelDef(source, target, 2, guisourcename, guitargetname, builder, result))
                    return false;
            }
        }
        return true;
    }

    /**
     * Checks and if required installs needed allowed type relations.
     * Retrieves, for each allowed relation entry, the attributes, and passes these on to {@link #installTypeRel}
     * @param relations a list of hashtables. Each hashtable represents a allowedrelation entry, and contains a list of name-value
     *      pairs (the allowed relation attributes).
     * @return <code>true</code> if succesfull, <code>false</code> if an error occurred
     */
    private boolean installAllowedRelations(List<Map<String,String>> relations, ApplicationResult result) {
        for (Map<String, String> bh : relations) {
            String from = bh.get("from");
            String to = bh.get("to");
            String type = bh.get("type");
            if (!installTypeRel(from, to, type, -1, result)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lists the required builders for this application, and makes attempts to install any builders that are
     * not present.
     * If there is a failure, the function returns false.
     * Failure messages are stored in the lastmsg member.
     * @param neededbuilders a list with builder data that need be installed on teh system for this application to work
     *                       each element in teh list is a Map containing builder properties (in particular, 'name').
     * @param applicationRoot the rootpath where the application's configuration files are located
     * @return true if the builders were succesfully installed, false if the installation failed
     */
    private boolean installBuilders(List<Map<String,String>> neededbuilders, String applicationRoot, ApplicationResult result) {
        for (Map<String, String> builderdata : neededbuilders) {
            String name = builderdata.get("name");
            MMObjectBuilder bul = mmb.getBuilder(name);
            // if builder not loaded
            if (bul == null) {
                // if 'inactive' in the config/builder path, fail
                String path = mmb.getBuilderPath(name, "");
                if (path != null) {
                    result.error("The builder '" + name + "' was already on our system, but inactive. " +
                                 "To install this application, make the builder '" + path + name + ".xml ' active");
                    continue;
                }
                ResourceLoader appLoader     = ResourceLoader.getConfigurationRoot().getChildResourceLoader(ResourceLoader.getDirectory(applicationRoot));
                ResourceLoader thisAppLoader = appLoader.getChildResourceLoader(ResourceLoader.getName(applicationRoot));
                ResourceLoader builderLoader = thisAppLoader.getChildResourceLoader("builders");

                // attempt to open the builder file.
                org.w3c.dom.Document config;
                try {
                    config = builderLoader.getDocument(name + ".xml");
                } catch (org.xml.sax.SAXException se) {
                    String msg = "builder '" + name + "':\n" + se.toString();
                    log.error(msg, se);
                    result.error("A XML parsing error occurred (" + se.toString() + "). Check the log for details.");
                    continue;
                } catch (java.io.IOException ioe) {
                    String msg = "builder '" + name + "':\n" + ioe.toString();
                    log.error(msg, ioe);
                    result.error("A file I/O error occurred (" + ioe.toString() + "). Check the log for details.");
                    continue;
                } catch (Throwable t) {
                    String msg = "builder '" + name + "': " + t.getMessage();
                    log.error(msg, t);
                    result.error("An error occured " + t.getClass() + " " + msg);
                    continue;
                }

                if (config == null) {
                    result.error("Could not find the builderfile :  '" + builderLoader.getResource(name + ".xml") + "' (builder '" + name + "')");
                    continue;
                }


                // check the presence of typedef (if not present, fail)
                MMObjectBuilder typeDef = mmb.getTypeDef();
                if (typeDef == null) {
                    return result.error("Could not find the typedef builder.");
                }
                try {
                    // try to add a node to typedef, same as adding a builder...
                    MMObjectNode typeNode = typeDef.getNewNode("system");
                    // fill the name....
                    typeNode.setValue("name", name);
                    typeNode.setValue("config", config);
                    // insert into mmbase
                    typeNode.insert("system");
                } catch (Exception e) {
                    result.error(e.getMessage());
                    continue;
                }
                    // we now made the builder active.. look for other builders...
            }
        }
        return result.isSuccess();
    }

    /**
     * Checks whether a given relation definition exists, and if not, creates that definition.
     * @param sname source name of the relation definition
     * @param dname destination name of the relation definition
     * @param dir directionality (uni or bi)
     * @param sguiname source GUI name of the relation definition
     * @param dguiname destination GUI name of the relation definition
     * @param builder references the builder to use (only in new format)
     * @return <code>true</code> if succesfull, <code>false</code> if an error occurred
     */
    private boolean installRelDef(String sname, String dname, int dir, String sguiname,
                                  String dguiname, int builder, ApplicationResult result) {

        RelDef reldef = mmb.getRelDef();
        if (reldef != null) {
            int rnumber = reldef.getNumberByName(sname );
            if (rnumber == -1) {
                MMObjectNode node = reldef.getNewNode("system");
                node.setValue("sname", sname);
                node.setValue("dname", dname);
                node.setValue("dir", dir);
                node.setValue("sguiname", sguiname);
                node.setValue("dguiname", dguiname);
                if (RelDef.usesbuilder) {
                    // if builder is unknown (falsely specified), use the InsRel builder
                    if (builder <= 0) {
                        builder = mmb.getInsRel().getNumber();
                    }
                    node.setValue("builder", builder);
                }
                int id = reldef.insert("system", node);
                if (id != -1) {
                    log.debug("RefDef (" + sname + "," + dname + ") installed");
                } else {
                    return result.error("RelDef (" + sname + "," + dname + ") could not be installed");
                }
            } else {
                MMObjectNode node = reldef.getNode(rnumber);
                String foundDname = node.getStringValue("dname");
                int foundBuilder = node.getIntValue("builder");
                if (! foundDname.equals(dname)) {
                    // MMB-1727
                    log.warn("Found already an reldef with this sname ('" + sname + "') but with different dname ('" + foundDname + "'). So not using " + sname + "/" + dname + " but " + sname + "/" + foundDname + ".");
                }
                if (foundBuilder != builder) {
                    result.error("Role '" + sname + "' already defined with different builder (" + foundBuilder + "), while this application required it to be " + builder);
                    return false;
                }
            }
        } else {
            return result.error("Can't get reldef builder");
        }
        return true;
    }

    /**
     * Checks and if required installs an allowed type relation (typerel object).
     * @param sname source type name of the type relation
     * @param dname destination type name of the type relation
     * @param rname role name of the type relation
     * @param count cardinality of the type relation
     * @return <code>true</code> if succesfull, <code>false</code> if an error occurred
     */
    private boolean installTypeRel(String sname, String dname, String rname, int count, ApplicationResult result) {
        TypeRel typerel = mmb.getTypeRel();
        if (typerel != null) {
            TypeDef typedef = mmb.getTypeDef();
            if (typedef == null) {
                return result.error("Can't get typedef builder");
            }
            RelDef reldef = mmb.getRelDef();
            if (reldef == null) {
                return result.error("Can't get reldef builder");
            }

            // figure out rnumber
            int rnumber = reldef.getNumberByName(rname);
            if (rnumber == -1) {
                return result.error("No reldef with role '" + rname + "' defined");
            }

            // figure out snumber
            int snumber = typedef.getIntValue(sname);
            if (snumber == -1) {
                return result.error("No builder with name '" + sname + "' defined");
            }

            // figure out dnumber
            int dnumber = typedef.getIntValue(dname);
            if (dnumber == -1) {
                return result.error("No builder with name '" + dname + "' defined");
            }

            if (!typerel.contains(snumber, dnumber, rnumber, TypeRel.STRICT)) {
                MMObjectNode node = typerel.getNewNode("system");
                node.setValue("snumber", snumber);
                node.setValue("dnumber", dnumber);
                node.setValue("rnumber", rnumber);
                node.setValue("max", count);
                int id = typerel.insert("system", node);
                if (id != -1) {
                    log.debug("TypeRel (" + sname + "," + dname + "," + rname + ") installed");
                } else {
                    return result.error("TypeRel (" + sname + "," + dname + "," + rname + ") could not be installed");
                }
            }
            return true;
        } else {
            return result.error("Can't get typerel builder");
        }
    }


}
