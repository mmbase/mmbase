/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.util;

import java.util.Iterator;

import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;

/**
 * {@inheritDoc}
 *
 * This version is automaticly growing with the same 'branch' every when that is possible. For that
 * it needs a kind of template query for every branch, which is defined by the constructor.
 *
 *
 * @author  Michiel Meeuwissen
 * @version $Id: GrowingTreeList.java,v 1.12 2005-10-31 13:04:20 michiel Exp $
 * @since   MMBase-1.7
 */

public  class GrowingTreeList extends TreeList {

    protected Constraint cleanConstraint;
    protected NodeQuery  pathElementTemplate;
    protected Constraint cleanLeafConstraint = null;
    protected NodeQuery  leafElementTemplate = null;
    protected int maxNumberOfSteps;

    /**
     * @param q              The 'base' query defining the minimal depth of the tree elements. The trunk of the tree.
     * @param maxDepth       You must supply a maximal depth of the nodes, because MMBase is basicly a network rather then a tree, so
     *                        tree representations could be infinitely deep.
     * @param nodeManager    Destination Nodemanager in the tree
     * @param role           Role of the relations in the tree
     * @param searchDir      Direction of the relations in the tree
     * @since MMBase-1.7.1
     */
    public GrowingTreeList(NodeQuery q, int maxDepth, NodeManager nodeManager, String role, String searchDir) {
        super(q);
        if (nodeManager == null) nodeManager = cloud.getNodeManager("object");
        pathElementTemplate = cloud.createNodeQuery();
        //shiftElementTemplate = cloud.createNodeQuery();
        Step step = pathElementTemplate.addStep(cloud.getNodeManager("object"));
        pathElementTemplate.setAlias(step, "object0");
        pathElementTemplate.setNodeStep(pathElementTemplate.addRelationStep(nodeManager, role, searchDir).getNext());

        setMaxDepth(maxDepth);
    }

    public GrowingTreeList(NodeQuery q, int maxDepth) {
        super(q);
        pathElementTemplate = cloud.createNodeQuery();
        Step step = pathElementTemplate.addStep(cloud.getNodeManager("object"));
        pathElementTemplate.setAlias(step, "object0");

        setMaxDepth(maxDepth);
    }

    /**
     * As long as the tree is not 'started' yet, max depth can still be changed.
     * @param maxDepth max number of Steps
     * @since MMBase-1.7.1
     */

    public void setMaxDepth(int maxDepth) {
        maxNumberOfSteps = 2 * maxDepth - 1; // dont consider relation steps.


        if (maxNumberOfSteps < numberOfSteps) {
            throw new IllegalArgumentException("Query is already deeper than maxdepth");
        }
    }

    /**
     * Returns the Query which is used as a template for the leaves to 'grow' the query. You can change it, add
     * sort-orders and add constraints before the tree is 'started'.  All but the first step of this
     * query are added. This query itself is never executed, though marked used, to avoid changes on
     * it after the list has started.
     *
     * @return Query which is used as a template
     * @since MMBase-1.7.1
     */

    public NodeQuery getTemplate() {
        return pathElementTemplate;
    }

    /**
     * The leave template is the 'last' template. This is the same as getTemplate, only, the
     * constraints set on this, are only used if the template is used 'on the end'.
     * 
     * It boils down to the fact that constraints set on the query don't change the tree itself, but
     * only constraint the 'leaves', so it makes for a kind of tree-search.
     * @since MMBase-1.8
     */
    public NodeQuery getLeafTemplate() {
        if (leafElementTemplate == null) {
            leafElementTemplate = (NodeQuery) pathElementTemplate.clone();
        }
        return leafElementTemplate;
    }


    public int size() {
        while (! foundEnd) {
            addPathElement();
        }
        return super.size();
    }

    protected NodeList getList(int queryNumber) {
        while (queryNumber >= branches.size() && (!foundEnd)) {
            addPathElement();
        }
        return super.getList(queryNumber);
    }
    protected NodeList getLeafList(int queryNumber) {
        while (queryNumber >= branches.size() && (!foundEnd)) {
            addPathElement();
        }
        return super.getLeafList(queryNumber);
    }


    /**
     * Grows the branches of the tree, with the leave.
     *
     */
    protected void addPathElement() {
        if (numberOfSteps + 2  > maxNumberOfSteps) {
            foundEnd = true;
        } else {
            if (! pathElementTemplate.isUsed()) {
                pathElementTemplate.markUsed();
                cleanConstraint = pathElementTemplate.getCleanConstraint();
            }
            if (leafElementTemplate != null && ! leafElementTemplate.isUsed()) {
                leafElementTemplate.markUsed();
                cleanLeafConstraint = leafElementTemplate.getCleanConstraint();
            }

            Iterator steps = pathElementTemplate.getSteps().iterator();;
            steps.next(); // ignore first step
            while (steps.hasNext()) {
                RelationStep relationStepTemplate = (RelationStep) steps.next();
                Step         stepTemplate         = (Step)         steps.next();
                String role;
                {   // it's a pity but role cannot be requested directly from RelationStep
                    // some hackery
                    Integer      reldef = relationStepTemplate.getRole();
                    if (reldef == null) {
                        role = null;
                    } else {
                        role = cloud.getNode(reldef.intValue()).getStringValue("sname");
                    }
                }

                RelationStep newStep = grow(cloud.getNodeManager(stepTemplate.getTableName()),
                                            role,
                                            RelationStep.DIRECTIONALITY_DESCRIPTIONS[relationStepTemplate.getDirectionality()]);
                if (newStep == null) {
                    foundEnd = true;
                    break;
                }
                // Step doesn't have a .getQuery() method, so we'll have to fall back to this:
                Branch branch = (Branch) branches.get(branches.size() - 1);
                Query newQuery = branch.getQuery();

                // add sortorder to the query
                Step nextStep = newStep.getNext();

                if (cleanConstraint != null) {
                    Constraint newStepConstraint         = Queries.copyConstraint(cleanConstraint, stepTemplate, newQuery, nextStep);
                    Constraint newRelationStepConstraint = Queries.copyConstraint(cleanConstraint, relationStepTemplate, newQuery, newStep);
                    Queries.addConstraint(newQuery, newStepConstraint);
                    Queries.addConstraint(newQuery, newRelationStepConstraint);
                }


                Queries.copySortOrders(pathElementTemplate.getSortOrders(), stepTemplate, newQuery, nextStep);
                Queries.copySortOrders(pathElementTemplate.getSortOrders(), relationStepTemplate, newQuery, newStep);

                if (cleanLeafConstraint != null) {
                    Constraint newLeafStepConstraint         = Queries.copyConstraint(cleanLeafConstraint, stepTemplate, newQuery, nextStep);
                    Constraint newLeafRelationStepConstraint = Queries.copyConstraint(cleanLeafConstraint, relationStepTemplate, newQuery, newStep);
                    if (newLeafStepConstraint != null && newLeafRelationStepConstraint != null) {
                        CompositeConstraint comp = newQuery.createConstraint(newLeafStepConstraint, CompositeConstraint.LOGICAL_AND, newLeafRelationStepConstraint);
                        setLeafConstraint(comp);
                    } else if (newLeafStepConstraint != null) {
                        setLeafConstraint(newLeafStepConstraint);
                    } else if (newLeafRelationStepConstraint != null) {
                        setLeafConstraint(newLeafRelationStepConstraint);
                    } else {
                        // both null, ignore
                    }
                }


                if (numberOfSteps + 2  > maxNumberOfSteps) {
                    foundEnd = true;
                    break;
                }
            }
        }
    }


    public static void  main(String[] args) {
        Cloud cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase");
        //NodeQuery q = getQuery(args);
        NodeQuery q = Queries.createNodeQuery(cloud.getNode(args[0]));

        NodeManager object = cloud.getNodeManager("segments");

        GrowingTreeList tree = new GrowingTreeList(q, 40, object, "index", "destination");

        NodeQuery temp = tree.getTemplate();
        Queries.addSortOrders(temp, "index.pos", "up");
        NodeQuery template = tree.getLeafTemplate();
        Constraint cons1 = Queries.createConstraint(template, "title", FieldCompareConstraint.LIKE, "%asdf%");
        Constraint cons2 = Queries.createConstraint(template, "body",  FieldCompareConstraint.LIKE, "%asdf%");
        Constraint compConstraint = template.createConstraint(cons1, CompositeConstraint.LOGICAL_OR, cons2);
        template.setConstraint(compConstraint);

        System.out.println("size " + tree.size());
        int k = 0;
        TreeIterator i = tree.treeIterator();
        while (i.hasNext()) {
            Node n = i.nextNode();
            k++;
            System.out.println(k + " " + i.currentDepth() + " " + n.toString());
        }
    }
}
