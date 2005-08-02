
/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.corebuilders;

import java.util.*;

import org.mmbase.bridge.Field;
import org.mmbase.util.*;
import org.mmbase.module.core.*;
import org.mmbase.core.CoreField;
import org.mmbase.core.util.Fields;
import org.mmbase.storage.search.implementation.BasicRelationStep;
import org.mmbase.storage.search.RelationStep;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * TypeRel defines the allowed relations between two object
 * types. Every relations also specifies a 'role', which is a
 * reference to the RelDef table.
 *
 * Relations do principally have a 'source' and a 'destination' object
 * type, but most functions of this class do ignore this distinction.
 *
 * TypeRel is a 'core' MMBase builder. You can get a reference to it
 * via the MMBase instance.
 *
 * @author Daniel Ockeloen
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: TypeRel.java,v 1.60 2005-08-02 12:17:50 nklasens Exp $
 * @see    RelDef
 * @see    InsRel
 * @see    org.mmbase.module.core.MMBase
 */
public class TypeRel extends MMObjectBuilder implements MMBaseObserver {

    private static final Logger log = Logging.getLoggerInstance(TypeRel.class);

    /**
     * Constant for {@link #contains}: return only typerels that
     * exactly match.
     */
    public static final int STRICT = 0;
    /**
     * Constant for {@link #contains}: return typerels where source/destination match
     * with a builder or its descendants
     */
    public static final int INCLUDE_DESCENDANTS = 1;
    /**
     * Constant for {@link #contains}: return typerels where source/destination match
     * with a builder or its parents
     */
    public static final int INCLUDE_PARENTS = 2;

    /**
     * Constant for {@link #contains}: return typerels where source/destination match
     * with a builder, its descendants, or its parents
     */
    public static final int INCLUDE_PARENTS_AND_DESCENDANTS = 3;

    /**
     * TypeRel should contain only a limited amount of nodes, so we
     * can simply cache them all, and avoid all further querying.
     */
    private TypeRelSet        typeRelNodes;           // for searching destinations
    private TypeRelSet        parentTypeRelNodes;     // for caching typerels for 'parent' builders
    private InverseTypeRelSet inverseTypeRelNodes;    // for searching sources

    public boolean init() {
        if (oType != -1) return true;
        super.init();
        // during init not yet all builder are available so inhertiance is not yet possible
        // This means that calls to getAllowedRelations do not consider inheritance during initializion of MMBase.
        // This occurs e.g. in one of the Community-builders.
        readCache(false);
        return true;
    }

    /**
     * The TypeRel cache contains all TypeRels MMObjectNodes.
     * Called after init by MMBase, and when something changes.
     * @since MMBase-1.6.2
     */
    public void readCache() {
        readCache(true);
    }

    /**
     * @since MMBase-1.6.2
     */
    private void readCache(boolean buildersInitialized) {
        log.debug("Reading in typerels");
        typeRelNodes = new TypeRelSet();
        parentTypeRelNodes = new TypeRelSet();
        inverseTypeRelNodes = new InverseTypeRelSet();

        TypeDef typeDef = mmb.getTypeDef();
        typeDef.init();
        // Find all typerel nodes
        List alltypes = getNodes();
        for (Iterator iter = alltypes.iterator(); iter.hasNext();) {
            // For every reltype node :
            MMObjectNode typerel = (MMObjectNode) iter.next();
            addCacheEntry(typerel, buildersInitialized);
        }
        log.debug("Done reading typerel cache " + (buildersInitialized ? "(considered inheritance)" : "") + ": " + typeRelNodes );
    }

    /**
     * Addes one typerel cache entries, plus inherited relations (if builder are initialized)
     * @return A Set with the added entries, which can be used for logging or so, or can be disregarded
     * @since  MMBase-1.6.2
     */
    protected TypeRelSet addCacheEntry(MMObjectNode typeRel, boolean buildersInitialized) {

        TypeRelSet added = new TypeRelSet(); // store temporary, which will enable nice logging of what happened

        // Start to add the actual definition, this is then afterwards again, except if one of the builders could not be found
        added.add(typeRel);

        RelDef reldef = mmb.getRelDef();

        MMObjectNode reldefNode = reldef.getNode(typeRel.getIntValue("rnumber"));
        if (reldefNode == null) {
            throw new RuntimeException("Could not find reldef-node for rnumber= " + typeRel.getIntValue("rnumber"));
        }

        boolean bidirectional = (!InsRel.usesdir) ||
                                (reldefNode.getIntValue("dir") > 1);

        inheritance:
        if(buildersInitialized) { // handle inheritance, which is not possible during initialization of MMBase.

            TypeDef typeDef = mmb.getTypeDef();

            String sourceBuilderName = typeDef.getValue(typeRel.getIntValue("snumber"));
            MMObjectBuilder sourceBuilder      = sourceBuilderName != null ? mmb.getBuilder(sourceBuilderName) : null;

            String destinationBuilderName = typeDef.getValue(typeRel.getIntValue("dnumber"));
            MMObjectBuilder destinationBuilder = destinationBuilderName != null ? mmb.getBuilder(destinationBuilderName) : null;


            if (sourceBuilder == null) {
                if (destinationBuilder == null) {
                    log.warn("Both source and destination of " + typeRel + " are not active builders. Cannot follow descendants.");
                } else {
                    log.warn("The source of relation type " + typeRel + " is not an active builder. Cannot follow descendants.");
                }
                break inheritance;
            }

            if (destinationBuilder == null) {
                log.warn("The destination of relation type " + typeRel + " is not an active builder. Cannot follow descendants.");
                break inheritance;
            }

            int rnumber = typeRel.getIntValue("rnumber");

            List sources = sourceBuilder.getDescendants();
            sources.add(sourceBuilder);

            List destinations = destinationBuilder.getDescendants();
            destinations.add(destinationBuilder);


            Iterator i = sources.iterator();
            while (i.hasNext()) {
                MMObjectBuilder s = (MMObjectBuilder) i.next();
                Iterator j = destinations.iterator();
                while (j.hasNext()) {
                    MMObjectBuilder d = (MMObjectBuilder) j.next();
                    MMObjectNode vnode = new VirtualTypeRelNode(s.oType, d.oType, rnumber);
                    added.add(vnode);
                }
            }

            // seek all parents and store typerels for them
            // this cache is used by contains(INCLUDE_PARENTS / INCLUDE_PARENTS_AND_DESCENDANTS));
            MMObjectBuilder sourceParent=sourceBuilder;
            while (sourceParent!=null) {
                MMObjectBuilder destinationParent=destinationBuilder;
                while (destinationParent!=null) {
                  MMObjectNode vnode = new VirtualTypeRelNode(sourceParent.oType, destinationParent.oType, rnumber);
                  parentTypeRelNodes.add(vnode);
                  destinationParent=destinationParent.getParentBuilder();
                }
                sourceParent=sourceParent.getParentBuilder();
            }

            added.add(typeRel); // replaces the ones added in the 'inheritance' loop (so now not any more Virtual)
        }
        typeRelNodes.addAll(added);
        if (bidirectional) inverseTypeRelNodes.addAll(added);
        if (log.isDebugEnabled()) {
            log.debug("Added to typerelcache: " + added);
        }
        return added;
    }

    /**
     * Insert a new object (content provided) in the cloud, including an entry for the object alias (if provided).
     * This method indirectly calls {@link #preCommit}.
     * If the typerel node specified already exists (i.e. same snumber, dnumber,a nd rnumber fielfds),
     * the typerel creation fails and returns -1.
     * @param owner The administrator creating the node
     * @param node The object to insert. The object need be of the same type as the current builder.
     * @return An <code>int</code> value which is the new object's unique number, -1 if the insert failed.
     */
    public int insert(String owner, MMObjectNode node) {
        int snumber = node.getIntValue("snumber");
        int dnumber = node.getIntValue("dnumber");
        int rnumber = node.getIntValue("rnumber");
        if (contains(snumber, dnumber, rnumber, STRICT)) {
            log.error("The typerel with snumber=" + snumber + ", dnumber=" + dnumber+ ", rnumber=" + rnumber + " already exists");
            throw new RuntimeException("The typerel with snumber=" + snumber + ", dnumber=" + dnumber+ ", rnumber=" + rnumber + " already exists");
        }
        int res = super.insert(owner, node);
        return res;
    }

    /**
     * Remove a node from the cloud.
     * @param node The node to remove.
     */
    public void removeNode(MMObjectNode node) {
        super.removeNode(node);
    }

    /**
     *  Retrieves all relations which are 'allowed' for a specified node, that is,
     *  where the node is either allowed to be the source, or to be the destination (but where the
     *  corresponing relation definition is bidirectional). The allowed relations are determined by
     *  the type of the node
     *  @param mmnode The node to retrieve the allowed relations of.
     *  @return An <code>Enumeration</code> of nodes containing the typerel relation data
     */
    public Enumeration getAllowedRelations(MMObjectNode mmnode) {
        return getAllowedRelations(mmnode.getBuilder().oType);
    }

    public Enumeration getAllowedRelations(int otype) {
        // wrap into a set, because result of getBySource is unmodifiable
        Set res = new HashSet(typeRelNodes.getBySource(otype)); // order does not matter
        res.addAll(inverseTypeRelNodes.getByDestination(otype));
        return Collections.enumeration(res);
    }

    /**
     *  Retrieves all relations which are 'allowed' between two
     *  specified nodes. No distinction between source / destination.
     *  @param n1 The first objectnode
     *  @param n2 The second objectnode
     *  @return An <code>Enumeration</code> of nodes containing the typerel relation data
     */
    public Enumeration getAllowedRelations(MMObjectNode n1, MMObjectNode n2) {
        int builder1 = n1.getOType();
        int builder2 = n2.getOType();
        return getAllowedRelations(builder1, builder2);

    }


    /**
     * An enumeration of all allowed relations between two
     * builders. No distinction is made between source and
     * destination.
     *
     */
    public Enumeration getAllowedRelations(int builder1, int builder2) {
        Set res = new HashSet(typeRelNodes.getBySourceDestination(builder1, builder2));
        res.addAll(inverseTypeRelNodes.getByDestinationSource(builder2, builder1));
        return Collections.enumeration(res);
    }
    /**
     * A Set of all allowed relations of a certain role between two
     * builders. No distinction between source and destination.
     *
     * @since MMBase-1.6.2
     */
    public Set getAllowedRelations(int builder1, int builder2, int role) {
        Set res = new HashSet(typeRelNodes.getBySourceDestinationRole(builder1, builder2, role));
        res.addAll(inverseTypeRelNodes.getByDestinationSourceRole(builder2, builder1, role));
        return res;
    }

    /**
     *  Retrieves all relations which are 'allowed' between two specified nodes.
     *  @param snum The first objectnode type (the source)
     *  @param dnum The second objectnode type (the destination)
     *  @return An <code>Enumeration</code> of nodes containing the reldef (not typerel!) sname field
     */
    protected Vector getAllowedRelationsNames(int number1 ,int number2) {
        Vector results=new Vector();
        for(Enumeration e=getAllowedRelations(number1, number2); e.hasMoreElements();) {
            MMObjectNode node=(MMObjectNode)e.nextElement();
            int rnumber=node.getIntValue("rnumber");
            MMObjectNode snode=mmb.getRelDef().getNode(rnumber);
            results.addElement(snode.getStringValue("sname"));
        }
        return results;
    }

    /**
     *  Retrieves the identifying number of the relation definition that is 'allowed' between two specified node types.
     *  The results are dependent on there being only one type of relation between two node types (not enforced, thus unpredictable).
     *  Makes use of a typeRelNodes.
     *  @param snum The first objectnode type (the source)
     *  @param dnum The second objectnode type (the destination)
     *  @return the number of the found relation, or -1 if either no relation was found, or more than one was found.
     */
     public int getAllowedRelationType(int snum,int dnum) {
        Set set = new HashSet(typeRelNodes.getBySourceDestination(snum, dnum));
        set.addAll(inverseTypeRelNodes.getByDestinationSource(dnum, snum));

        if (set.size() != 1) {
           return -1;
        } else {
           MMObjectNode n =  (MMObjectNode) set.iterator().next();
           return n.getIntValue("rnumber");
        }
     }

    /**
     *  Returns the display string for this node
     *  It returns a commbination of objecttypes and rolename : "source->destination (role)".
     *  @param node Node from which to retrieve the data
     *  @return A <code>String</code> describing the content of the node
     */
    public String getGUIIndicator(MMObjectNode node) {
        try {
            String source      = mmb.getTypeDef().getValue(node.getIntValue("snumber"));
            String destination = mmb.getTypeDef().getValue(node.getIntValue("dnumber"));
            MMObjectNode role  = mmb.getRelDef().getNode(node.getIntValue("rnumber"));
            return source + "->"+ destination + " (" + (role != null ? role.getGUIIndicator() : "???" ) +")";
        } catch (Exception e) {
            log.warn(e);
        }
        return null;
    }

    /**
     *  Returns the display string for a specified field.
     *  Returns, for snumber and dnumber, the name of the objecttype they represent, and for
     *  rnumber the display (GUI) string for the indicated relation definition.
     *  @param field The name of the field to retrieve
     *  @param node Node from which to retrieve the data
     *  @return A <code>String</code> describing the content of the field
     */
    public String getGUIIndicator(String field, MMObjectNode node) {
        try {
            if (field.equals("snumber")) {
                return mmb.getTypeDef().getValue(node.getIntValue("snumber"));
            } else if (field.equals("dnumber")) {
                return mmb.getTypeDef().getValue(node.getIntValue("dnumber"));
            } else if (field.equals("rnumber")) {
                MMObjectNode reldef=mmb.getRelDef().getNode(node.getIntValue("rnumber"));
                return (reldef != null ? reldef.getGUIIndicator() : "???");
            }
        } catch (Exception e) {}
        return null;
    }

    /**
     * Processes the BUILDER-typerel-ALLOWEDRELATIONSNAMES in the LIST command, and (possibly) returns a Vector containing
     * requested data (based on the content of TYPE and NODE, which can be retrieved through tagger).
     * @javadoc parameters
     */
    public Vector getList(PageInfo sp, StringTagger tagger, StringTokenizer tok) {
        if (tok.hasMoreTokens()) {
            String cmd=tok.nextToken();    //Retrieving command.
            if (cmd.equals("ALLOWEDRELATIONSNAMES")) {
                try {
                    String tmp=tagger.Value("TYPE");
                    int number1=mmb.getTypeDef().getIntValue(tmp);
                    tmp=tagger.Value("NODE");
                    int number2=Integer.parseInt(tmp);
                    MMObjectNode node=getNode(number2);
                    return getAllowedRelationsNames(number1,node.getOType());
                } catch(Exception e) {
                    log.error(Logging.stackTrace(e));
                }
            }
        }
        return null;
    }

    /**
     * Tests if a specific relation type is defined.
     * <p>
     * Note that this routine returns false both when a snumber/dnumber
     * are swapped, and when a typecombo does not exist -
     * it is not possible to derive whether one or the other has occurred.
     * <p>
     * @deprecated use {@link #contains} instead
     * @param n1 The source type number.
     * @param n2 The destination type number.
     * @param r The relation definition (role) number, or -1 if the role does not matter
     * @return <code>true</code> when the relation exists, false otherwise.
     *
     */
    public boolean reldefCorrect(int n1,int n2, int r) {
        return contains(n1, n2, r);
    }

    /**
     * Tests if a specific relation type is defined.
     * The method also returns true if the typerel occurs as a virtual (derived) node.
     * <p>
     * Note that this routine returns false both when a snumber/dnumber
     * are swapped, and when a typecombo does not exist -
     * it is not possible to derive whether one or the other has occurred.
     * <p>
     *
     * @param n1 The source type number.
     * @param n2 The destination type number.
     * @param r The relation definition (role) number, or -1 if the role does not matter
     * @return <code>true</code> when the relation exists, false otherwise.
     *
     * @since MMBase-1.6.2
     */
    public boolean contains(int n1,int n2, int r) {
        return contains(n1, n2, r, INCLUDE_DESCENDANTS);
    }

    /**
     * Tests if a specific relation type is defined.
     * <p>
     * Note that this routine returns false both when a snumber/dnumber
     * are swapped, and when a typecombo does not exist -
     * it is not possible to derive whether one or the other has occurred.
     * <p>
     *
     * @param n1 The source type number.
     * @param n2 The destination type number.
     * @param r The relation definition (role) number, or -1 if the role does not matter
     *          r can only be -1 if virtual is <code>true</code>
     * @param restriction if {@link #STRICT}, contains only returns true if the typerel occurs as-is in the database.
     *                    if {@link #INCLUDE_DESCENDANTS}, contains returns true if the typerel occurs as a virtual
     *                    (derived) node, where source or destination may also be descendants of the specified type.
     *                    if {@link #INCLUDE_PARENTS}, contains returns true if the typerel occurs as a virtual
     *                    (derived) node, where source or destination may also be parents of the specified type.
     *                    if {@link #INCLUDE_PARENTS_AND_DESCENDANTS}, contains returns true if the typerel occurs as a virtual
     *                    (derived) node, where source or destination may also be descendants or parents of the specified type.
     * @return <code>true</code> when the relation exists, false otherwise.
     *
     * @since MMBase-1.6.2
     */
    public boolean contains(int n1,int n2, int r, int restriction) {
        switch(restriction) {
        case INCLUDE_DESCENDANTS:             return typeRelNodes.contains(new VirtualTypeRelNode(n1, n2, r));
        case INCLUDE_PARENTS:                 return parentTypeRelNodes.contains(new VirtualTypeRelNode(n1, n2, r));
        case INCLUDE_PARENTS_AND_DESCENDANTS: return typeRelNodes.contains(new VirtualTypeRelNode(n1, n2, r)) || parentTypeRelNodes.contains(new VirtualTypeRelNode(n1, n2, r));
        default: // STRICT
            SortedSet existingNodes = typeRelNodes.getBySourceDestinationRole(n1,n2,r);
            return (existingNodes.size() > 0 && !((MMObjectNode)existingNodes.first()).isVirtual());
        }
    }

    public boolean nodeRemoteChanged(String machine,String number,String builder,String ctype) {
        super.nodeRemoteChanged(machine,number,builder,ctype);
        return nodeChanged(machine,number,builder,ctype);
    }

    public boolean nodeLocalChanged(String machine,String number,String builder,String ctype) {
        super.nodeLocalChanged(machine,number,builder,ctype);
        return nodeChanged(machine,number,builder,ctype);
    }

    /**
     * Watch for changes on relation types and adjust our memory table accordingly
     * @todo Should update artCache en relDefCorrectCache as wel
     */
    public boolean nodeChanged(String machine,String number,String builder,String ctype) {
        if (log.isDebugEnabled()) log.debug("Seeing change on "+number+" : "+ctype);
        if (builder.equals(getTableName())) {
            if (ctype.equals("n")) { // adding can be safely done
                log.service("Added to typerelcache: " + addCacheEntry(getNode(number), true));
            } else {
                // something else changed in a typerel node? reread the complete typeRelNodes Set
                readCache();
            }
        }
        return true;
    }

    /**
     * Optimize as relation step by considering restrictions of TypeRel. TypeRel defines which type of relations may be created, ergo can exist.
     *
     * @since MMBase-1.7
     */
    public boolean  optimizeRelationStep(BasicRelationStep relationStep, int sourceType, int destinationType, int roleInt, int searchDir) {
        // Determine in what direction(s) this relation can be followed:

        // Check directionality is requested and supported.
        if (searchDir != RelationStep.DIRECTIONS_ALL && InsRel.usesdir) {
            relationStep.setCheckedDirectionality(true);
        }

        // this is a bit confusing, can the simple cases like explicit 'source' or 'destination' not be handled first?

        boolean sourceToDestination = searchDir != RelationStep.DIRECTIONS_SOURCE      && mmb.getTypeRel().contains(sourceType, destinationType, roleInt, INCLUDE_PARENTS_AND_DESCENDANTS);
        boolean destinationToSource = searchDir != RelationStep.DIRECTIONS_DESTINATION && mmb.getTypeRel().contains(destinationType, sourceType, roleInt, INCLUDE_PARENTS_AND_DESCENDANTS);


        if (destinationToSource && sourceToDestination && (searchDir == RelationStep.DIRECTIONS_EITHER)) { // support old
            destinationToSource= false;
        }

        if (destinationToSource) {
            // there is a typed relation from destination to src
            if (sourceToDestination) {
                // there is ALSO a typed relation from src to destination - make a more complex query
                relationStep.setDirectionality(RelationStep.DIRECTIONS_BOTH);
            } else {
                // there is ONLY a typed relation from destination to src - optimized query
                relationStep.setDirectionality(RelationStep.DIRECTIONS_SOURCE);
            }
        } else {
            if (sourceToDestination) {
                // there is no typed relation from destination to src (assume a relation between src and destination)  - optimized query
                relationStep.setDirectionality(RelationStep.DIRECTIONS_DESTINATION);
            } else {
                // no results possible, do something any way

                if (searchDir == RelationStep.DIRECTIONS_SOURCE) { // explicitely asked for source, it would be silly to try destination now
                    relationStep.setDirectionality(RelationStep.DIRECTIONS_SOURCE);
                } else {
                    relationStep.setDirectionality(RelationStep.DIRECTIONS_DESTINATION); // the 'normal' way
                }
                return false;
            }
        }
        return true;
    }


    /**
     * Implements equals for a typerel node.
     * Two nodes are equal if the snumber and dnumber fields are the same, and
     * the rnumber fields are the same, or one of these is '-1' (don't care).
     * @since MMBase-1.6.2
     */
    public boolean equals(MMObjectNode o1, MMObjectNode o2) {
        if (o2.parent instanceof TypeRel) {
            int r1=o1.getIntValue("rnumber");
            int r2=o2.getIntValue("rnumber");
            return o1.getIntValue("snumber") == o2.getIntValue("snumber") &&
                   o1.getIntValue("dnumber") == o2.getIntValue("dnumber") &&
                   (r1==-1 || r2==-1 || r1==r2);
        }
        return false;
    }

    /**
     * Implements for MMObjectNode
     * @since MMBase-1.6.2
     */
    public int hashCode(MMObjectNode o) {
        return 127 * o.getIntValue("snumber");
    }


    public String toString(MMObjectNode n) {
        try {
            int snumber = n.getIntValue("snumber");
            int dnumber = n.getIntValue("dnumber");
            int rnumber = n.getIntValue("rnumber");

            String sourceName = mmb.getTypeDef().getValue(snumber);
            String destName   = mmb.getTypeDef().getValue(dnumber);

            if (sourceName == null) sourceName = "unknown builder '" + snumber + "'";
            if (destName   == null) destName = "unknown builder '" + dnumber + "'";

            // unfilled should only happen during creation of the node.
            String source      = snumber > -1 ? sourceName : "[unfilled]";
            String destination = dnumber > -1 ? destName : "[unfilled]";
            MMObjectNode role  = rnumber > -1 ? mmb.getRelDef().getNode(rnumber) : null;
            return source + "->"+ destination + " (" + (role != null ? role.getStringValue("sname")  : "???" ) +")";
        } catch (Exception e) {
            log.warn(e);
        }
        return "typerel-node";
    }

    /**
     * Of course, virtual typerel nodes need a virtual typerel
     * builder. Well 'of course', the reason is not quite obvious to
     * me, it has to do with the bridge/temporarynodemanager which
     * sometimes needs to know it.
     *
     * @since MMBase-1.6.2
     */
    static class VirtualTypeRel extends TypeRel {
        static VirtualTypeRel virtualTypeRel = null;
        VirtualTypeRel(TypeRel t) {
            mmb = t.mmb;
            CoreField field = Fields.createField("snumber", Field.TYPE_NODE, Field.TYPE_UNKNOWN, Field.STATE_PERSISTENT, null);
            field.setEditPosition(2);
            field.finish();
            addField(field);
            field = Fields.createField("dnumber", Field.TYPE_NODE, Field.TYPE_UNKNOWN, Field.STATE_PERSISTENT, null);
            field.setEditPosition(2);
            field.finish();
            addField(field);
            field = Fields.createField("rnumber", Field.TYPE_NODE, Field.TYPE_UNKNOWN, Field.STATE_PERSISTENT, null);
            field.setEditPosition(2);
            field.finish();
            addField(field);
            tableName = "virtual_typerel";
            virtual   = true;
        }
        static VirtualTypeRel getVirtualTypeRel(TypeRel t) {
            if (virtualTypeRel == null) virtualTypeRel = new VirtualTypeRel(t);
            return virtualTypeRel;
        }
    }

    /**
     * A TypeRelSet is a Set of typerel nodes. The TypeRel builder
     * maintains such a Set of all typerel nodes for quick
     * reference. TypeRelSets are also instantiated when doing queries
     * on TypeRel like getAllowedRelations(MMObjectBuilder) etc.
     *
     * @since MMBase-1.6.2
     */
    protected class TypeRelSet extends TreeSet {
        protected TypeRelSet() {
            super(new Comparator() {
                    // sorted by source, destination, role
                    public int compare(Object o1, Object o2) {
                        MMObjectNode n1 = (MMObjectNode) o1;
                        MMObjectNode n2 = (MMObjectNode) o2;

                        int i1 = n1.getIntValue("snumber");
                        int i2 = n2.getIntValue("snumber");
                        if (i1 != i2) return i1 - i2;

                        i1 = n1.getIntValue("dnumber");
                        i2 = n2.getIntValue("dnumber");
                        if (i1 != i2) return i1 - i2;

                        i1 = n1.getIntValue("rnumber");
                        i2 = n2.getIntValue("rnumber");
                        if (i1!=-1 && i2!=-1 && i1 != i2) return i1 - i2;

                        return 0;
                    }
                });
        }
        // make sure only MMObjectNode's are added
        public boolean add(Object object) {
            return super.add(object);
        }

        // find some subsets:
        SortedSet getBySource(MMObjectBuilder source) {
            return Collections.unmodifiableSortedSet(subSet(new VirtualTypeRelNode(source.oType),
                          new VirtualTypeRelNode(source.oType +1)));
        }

        SortedSet getBySource(int sourceOType) {
            return Collections.unmodifiableSortedSet(subSet(new VirtualTypeRelNode(sourceOType),
                          new VirtualTypeRelNode(sourceOType +1)));
        }

        SortedSet getBySourceDestination(int source, int destination) {
            return Collections.unmodifiableSortedSet(
                                   subSet(new VirtualTypeRelNode(source, destination),
                                          new VirtualTypeRelNode(source, destination + 1)));
        }

        SortedSet getBySourceDestination(MMObjectBuilder source, MMObjectBuilder destination) {
            return Collections.unmodifiableSortedSet(subSet(new VirtualTypeRelNode(source.oType, destination.oType),
                          new VirtualTypeRelNode(source.oType, destination.oType + 1)));
        }

        SortedSet getBySourceDestinationRole(int source, int destination, int role) {
            return Collections.unmodifiableSortedSet(subSet(new VirtualTypeRelNode(source, destination, role),
                          new VirtualTypeRelNode(source, destination, role + 1)));
        }
    }

    /**
     * An InverseTypeRelSet is a Set of typerel nodes. The TypeRel builder
     * maintains such a Set of all typerel nodes for quick
     * reference.
     *
     * @since MMBase-1.6.2
     */
    protected class InverseTypeRelSet extends TreeSet {
        protected InverseTypeRelSet() {
            super(new Comparator() {
                    // sorted by destination, source, role
                    public int compare(Object o1, Object o2) {
                        MMObjectNode n1 = (MMObjectNode) o1;
                        MMObjectNode n2 = (MMObjectNode) o2;

                        int i1 = n1.getIntValue("dnumber");
                        int i2 = n2.getIntValue("dnumber");
                        if (i1 != i2) return i1 - i2;

                        i1 = n1.getIntValue("snumber");
                        i2 = n2.getIntValue("snumber");
                        if (i1 != i2) return i1 - i2;

                        i1 = n1.getIntValue("rnumber");
                        i2 = n2.getIntValue("rnumber");
                        if (i1 != i2) return i1 - i2;

                        return 0;
                    }
                });
        }
        // make sure only MMObjectNode's are added
        public boolean add(Object object) {
            return super.add(object);
        }

        SortedSet getByDestination(MMObjectBuilder destination) {
            return Collections.unmodifiableSortedSet(subSet(new VirtualTypeRelNode(-1, destination.oType),
                                                            new VirtualTypeRelNode(-1, destination.oType + 1)));
        }

        SortedSet getByDestination(int destinationOType) {
            return Collections.unmodifiableSortedSet(subSet(new VirtualTypeRelNode(-1, destinationOType),
                                                            new VirtualTypeRelNode(-1, destinationOType + 1)));
        }

        SortedSet getByDestinationSource(int source, int destination) {
            return Collections.unmodifiableSortedSet(subSet(new VirtualTypeRelNode(source,     destination),
                                                            new VirtualTypeRelNode(source + 1, destination)));
        }

        SortedSet getByDestinationSource(MMObjectBuilder source, MMObjectBuilder destination) {
            return Collections.unmodifiableSortedSet(subSet(new VirtualTypeRelNode(source.oType,  destination.oType),
                                                            new VirtualTypeRelNode(source.oType +1, destination.oType)));
        }

        SortedSet getByDestinationSourceRole(int source, int destination, int role) {
            return Collections.unmodifiableSortedSet(subSet(new VirtualTypeRelNode(source, destination, role),
                                                            new VirtualTypeRelNode(source, destination, role + 1)));
        }
    }

    /**
     * A VirtualTypeRelNode is a MMObjectNode which is added to the
     * typerelset with extensions of the actual builders specified. So
     * these entries are not in the database.
     *
     * @since MMBase-1.6.2
     */
    protected class VirtualTypeRelNode extends VirtualNode {

        VirtualTypeRelNode(int snumber, int dnumber) { // only for use in lookups
            // We don't use this-constructor because some jvm get confused then
            super(VirtualTypeRel.getVirtualTypeRel(TypeRel.this));
            setValue("snumber", snumber);
            setValue("dnumber", dnumber);
            setValue("rnumber", -1);
        }
        VirtualTypeRelNode(int snumber) {             // only for use in lookups
            // We don't use this-constructor because some jvm get confused then
            super(VirtualTypeRel.getVirtualTypeRel(TypeRel.this));
            setValue("snumber", snumber);
            setValue("dnumber", -1);
            setValue("rnumber", -1);
        }

        VirtualTypeRelNode(int snumber, int dnumber, int rnumber) {
            super(VirtualTypeRel.getVirtualTypeRel(TypeRel.this));
            setValue("snumber", snumber);
            setValue("dnumber", dnumber);
            setValue("rnumber", rnumber);
        }
    }

}

