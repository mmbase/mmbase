/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.packaging.projects.creators.dataapptools;

import java.io.*;
import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;

import org.mmbase.module.corebuilders.*;
import org.mmbase.applications.packaging.projects.*;

/**
 */
public class Apps1DataWriter {

    /**
     * Logging instance
     */
    private static Logger log = Logging.getLoggerInstance(Apps1DataWriter.class.getName());

    public static boolean write(HashSet<Integer> fb,String alias,String builder,String where,int depth,String datafile,String datapath,Target target) {

	int startnode = getStartNode(alias,builder,where);

        // the trick is to get all nodes until depth x and filter them
        HashSet<Integer> relnodes = new HashSet<Integer>();
        HashSet<Integer> nodes = new HashSet<Integer>();
        getSubNodes(startnode,depth,fb, nodes,relnodes);

        writeDataXML(fb,datafile,target);
        writeNodes(fb,nodes,relnodes,datapath);

	/*

        // create the dir for the Data & resource files
        File file = new File(targetpath+"/"+app.getApplicationName());
        try {
            file.mkdirs();
        } catch(Exception e) {
            log.error("Can't create dir : "+targetpath+"/"+app.getApplicationName());
        }
        */

        return true;
    }

    static void writeNodes(HashSet<Integer> builders,HashSet<Integer> nodes,HashSet<Integer> relnodes, String targetpath) {

        // create a list of writer objects for the nodes
        Hashtable<String, NodeWriter> nodeWriters = new Hashtable<String, NodeWriter>();
        Iterator<Integer> res = builders.iterator();
        while (res.hasNext()) {
            Integer i = res.next();
	    String nb = MMBase.getMMBase().getTypeDef().getValue(i.intValue());
	    MMObjectBuilder bul=MMBase.getMMBase().getMMObject(nb);
	    boolean isRelation = false;
	    if (bul instanceof InsRel) isRelation = true;
            // Create nodewriter for this builder
            NodeWriter nw = new NodeWriter(targetpath, nb, isRelation);
            // and store in table
            nodeWriters.put(nb,nw);
        }

        MMObjectBuilder bul = MMBase.getMMBase().getMMObject("typedef"); // get Typedef object
        int nrofnodes=0;	// set total nodes to export to zero (is this used?).
        // Store all the nodes that apply using their corresponding NodeWriter object
        for (Integer integer : nodes) {
        // retrieve the node to export
            int nr = integer.intValue();
            MMObjectNode node = bul.getNode(nr);
            String name = node.getName();
            NodeWriter nodeWriter = nodeWriters.get(name);
            // export the node if the writer was found
            if (nodeWriter!=null) {
                nodeWriter.write(node);
                nrofnodes++;
            } else {
		log.info("error missing nodeWriter : "+name);
	    }
        }

        for (Integer integer : relnodes) {
        // retrieve the node to export
            int nr = integer.intValue();
            MMObjectNode node = bul.getNode(nr);
            String name = node.getName();
            NodeWriter nodeWriter = nodeWriters.get(name);
            // export the node if the writer was found
            if (nodeWriter!=null) {
                nodeWriter.write(node);
                nrofnodes++;
            } else {
		log.info("error missing nodeWriter : "+name);
	    }
        }

        // close the files.
        for (Enumeration<String> e = nodeWriters.keys(); e.hasMoreElements();) {
            String name = e.nextElement();
            NodeWriter nodeWriter;
            nodeWriter = nodeWriters.get(name);
            nodeWriter.done();
        }
    }

    /**
    *  Determines the number of the node referenced by another node.
    *  @param nodeNumber number of the referencing node
    *  @param relationNode node from the relationtable containing the relation data
    *  @returns An <code>int</code> value for the number of the node referenced
    */
    static int getRelatedNode(int nodeNumber, MMObjectNode relationNode) {
        int snumber = relationNode.getIntValue("snumber"); // referenced node is either source
        if (snumber == nodeNumber) {
            return relationNode.getIntValue("dnumber"); // or destination
        } else {
            return snumber;
        }
    }

    static void getSubNodes(int startnodenr, int maxdepth, HashSet<Integer> fb, HashSet<Integer> nodesdoneSet, HashSet<Integer> relationnodesSet) {
	MMBase mmb=MMBase.getMMBase();
        HashSet<Integer> nodesSet_current = null;	// holds all nodes not yet 'done' that are on the current level
        HashSet<Integer> nodesSet_next = new HashSet<Integer>();  // holds all nodes not yet 'done' that are on the next level
        InsRel bul = mmb.getInsRel();		// builder for collecting relations. should be changed to MMRelations later on!
        Integer type = new Integer(bul.getNodeType(startnodenr));	// retrieve node type (new method in MMObjectBuiilder)
        if (!fb.contains(type)) {   // exit if the type of this node conflicts.
            // essentially, no nodes are added. This can only occur if the context of
            // an application specified an invalid node.
            return;
        }
        nodesSet_next.add(new Integer(startnodenr)); // add the very first node to the set...
        // For each depth of the tree, traverse the nodes on that depth
        for (int curdepth=1;curdepth<=maxdepth;curdepth++) {
            nodesSet_current = nodesSet_next;	// use the next level of nodes to tarverse
            nodesSet_next = new HashSet<Integer>();          // and create a new holder for the nodes one level deeper

            // since the nodes on this level are 'almost done', and therefor should be skipped
            // when referenced in the next layer, add the current set to the set of nodes that are 'done'
            //
            nodesdoneSet.addAll(nodesSet_current);
            // iterate through the current level
            for (Integer thisnodenr : nodesSet_current) {
                for (MMObjectNode relnode : bul.getRelationsVector(thisnodenr.intValue())) {
                    // get the relation node and node number
                    Integer relnumber=new Integer(relnode.getIntValue("number"));
                    // check whether to add the referenced node
                    // and the relation between this node and the referenced one.
                    // if relation is in pool, save trouble and do not traverse further
                    if (!relationnodesSet.contains(relnumber)) {
                        // determine node referenced
                        int nodenumber=getRelatedNode(thisnodenr.intValue(),relnode);
                        // check type of referenced node
                        type = new Integer(bul.getNodeType(nodenumber));
                        if (fb.contains(type)) {	// good node? then proceed
                            // add the relation node
                            relationnodesSet.add(relnumber);
                            // if the node has been 'done', don't add it!
                            Integer nodeNumber=new Integer(nodenumber);
                            if (!nodesdoneSet.contains(nodeNumber)) {
                                // because we use a set, no double nodes will be added (cool, uh?)
                                nodesSet_next.add(nodeNumber);
                            }
                        }
                    }
                }
            }
        }
        // add the last retrieved set to the set of nodes that are 'done'
        nodesdoneSet.addAll(nodesSet_next);
        return;
    }

	/*
    static HashSet getWantedBuilders(Vector filter,TypeDef bul) {
        HashSet resultset=new HashSet();
        for(Iterator res=filter.iterator(); res.hasNext(); ) {
            Hashtable bset=(Hashtable)res.next();
            String name=(String)bset.get("name");
            int value=bul.getIntValue(name);
            if (value!=-1) {
                resultset.add(new Integer(value));
            } else {
                log.error("XMLContextDepthWriter -> can't get intvalue for : "+name);
            }
        }
        return resultset;
    }
	*/


    static int getStartNode(String alias,String builder,String where) {
        // first check for an alias
        if (alias!=null) {
            // if so, get the node associated with that alias
            OAlias bul=(OAlias)MMBase.getMMBase().getMMObject("oalias");
            int number=bul.getNumber(alias);
            if (number==-1) log.error("Invalid Start Node Alias please make sure its valid");
            return number;
        } else {
            // otherwise, get a builder and the where clause to run on that builder

            // retrieve the actual builder
            MMObjectBuilder bul=MMBase.getMMBase().getMMObject(builder);
            if (bul!=null) {
                // find the nodes that match
                Enumeration<MMObjectNode> results=bul.search(where);
                // check if there are any nodes
                if (results.hasMoreElements()) {
                    // then return the first node found.
                    MMObjectNode node=results.nextElement();
                    return node.getIntValue("number");
                }
            } else {
                log.error("ContextDepthWriter-> can't find builder ("+builder+")");
            }
        }
        log.error("Invalid Start Node please fix your 'where' settings or use a alias");
        return -1;
    }

    /**
     * Saves a string value to a file.
     * @param filename Name of the file to save.
     * @param value string to store in the file
     * @return True if succesfull, false if an error occurred.
     */
    static boolean saveFile(String filename,String value) {
        File sfile = new File(filename);
        try {
            DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
            scan.writeBytes(value);
            scan.flush();
            scan.close();
        } catch(Exception e) {
            log.error(e);
            log.error(Logging.stackTrace(e));
            return false;
        }
        return true;
    }

    /**
     * Saves an array of byte to a file.
     * @param filename Name of the file to save.
     * @param value array to stiore in the file
     * @return True if succesfull, false if an error occurred.
     */
    static boolean saveFile(String filename,byte[] value) {
        File sfile = new File(filename);
        try {
            DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
            scan.write(value);
            scan.flush();
            scan.close();
        } catch(Exception e) {
            log.error(e);
            log.error(Logging.stackTrace(e));
            return false;
        }
        return true;
    }

    public static boolean writeDataXML(HashSet<Integer> fb,String datafile,Target target) {
        String body="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        body+="<!DOCTYPE dataset PUBLIC \"-//MMBase/DTD dataset config 1.0//EN\" \"https://www.mmbase.org/dtd/dataset_1_0.dtd\">\n";
        body+="<dataset>\n";
	/* not sure if ill keep these
        body+="\t<creationinfo>\n";
        body+="\t\t<creator name=\"example\" maintainer=\"submarine\" version=\"2\" />\n";
        body+="\t\t<creationtime>Thu Oct 28 13:07:25 CEST 2004</creationtime>\n";
        body+="\t\t<creatorcomments>This is a fake file made by hand</creatorcomments>\n";
        body+="\t</creationinfo>\n";
	*/
        body+="\t<objectsets>\n";
        Iterator<Integer> res = fb.iterator();
        while (res.hasNext()) {
            Integer i = res.next();
            String nb = MMBase.getMMBase().getTypeDef().getValue(i.intValue());
            MMObjectBuilder bul=MMBase.getMMBase().getMMObject(nb);
            if (!(bul instanceof InsRel)) {
		body+="\t\t<objectset builder=\""+nb+"\" path=\"data/"+nb+".xml\" />\n";
	    }
	}
        body+="\t</objectsets>\n";
        body+="\t<relationsets>\n";
        res = fb.iterator();
        while (res.hasNext()) {
            Integer i = res.next();
            String nb = MMBase.getMMBase().getTypeDef().getValue(i.intValue());
            MMObjectBuilder bul=MMBase.getMMBase().getMMObject(nb);
            if (bul instanceof InsRel) {
		body+="\t\t<relationset builder=\""+nb+"\" path=\"data/"+nb+".xml\" />\n";
	    }
	}
        body+="\t</relationsets>\n";
	String type=(String)target.getItem("type");
        body+="\t<selection type=\""+type+"\">\n";
	if (type.equals("depth")) {
        	body+="\t\t<model name=\""+target.getItem("depthname")+"\" maintainer=\""+target.getItem("depthmaintainer")+"\" version=\""+target.getItem("depthversion")+"\" />\n";
		String alias=(String)target.getItem("depthalias");
		if (alias!=null && !alias.equals("")) {
        		body+="\t\t<startnode alias=\""+alias+"\">\n";
		} else {
        		body+="\t\t<startnode>\n";
		}
		String builder=(String)target.getItem("depthbuilder");
		if (builder!=null && !builder.equals("")) {
        		body+="\t\t\t<builder>"+builder+"</builder>\n";
        		body+="\t\t\t<where>"+target.getItem("depthwhere")+"</where>\n";
		}
       		body+="\t\t</startnode>\n";
       		body+="\t\t<depth>"+target.getItem("depth")+"</depth>\n";
	}
        body+="\t</selection>\n";
        body+="</dataset>\n";
        saveFile(datafile,body);
        return true;
    }

}
