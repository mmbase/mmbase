/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.corebuilders;

import java.util.*;
import org.mmbase.module.core.*;

import org.mmbase.storage.search.implementation.*;
import org.mmbase.storage.search.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * RelDef, one of the meta stucture nodes, is used to define the possible relation types.
 * <p>
 * A Relation Definition consists of a source and destination, and a descriptor
 * (direction) for it's use (unidirectional or bidirectional).
 * </p><p>
 * Relations are mapped to a builder.<br />
 * This is so that additional functionality can be added by means of a builder (i.e. AuthRel).<br />
 * The old system mapped the relations to a builder by name.
 * Unfortunately, this means that some care need be taken when naming relations, as unintentionally
 * naming a relation to a builder can give bad (if not disastrous) results.<br />
 * Relations that are not directly mapped to a builder are mapped (internally) to the {@link InsRel} builder instead.
 * </p><p>
 * The new system uses an additional field to map to a builder.
 * This 'builder' field contains a reference (otype) to the builder to be used.
 * If null or 0, the builder is assumed to refer to the {@link InsRel} builder.
 * <code>sname</code> is now the name of the relation and serves no function.
 * </p><p>
 * This patched version of RelDef can make use of either direct builder references (through the builder field), or the old system of using names.
 * The system used is determined by examining whether the builder field has been defined in the builder's configuration (xml) file.
 * See the documentation of the relations project at http://www.mmbase.org for more info.
 * </p>
 *
 * @todo Fix cache so it will be updated using multicast.
 * @author Daniel Ockeloen
 * @author Pierre van Rooden
 * @version $Id$
 */
public class RelDef extends MMObjectBuilder {

    private static final Logger log = Logging.getLoggerInstance(RelDef.class);

    /** Value of "dir" field indicating unidirectional relations. */
    public final static int DIR_UNIDIRECTIONAL = 1;

    /** Value of "dir" field indicating bidirectional relatios. */
    public final static int DIR_BIDIRECTIONAL = 2;

    /**
     * Indicates whether the relationdefinitions use the 'builder' field (that is, whether the
     * field has been defined in the xml file). Used for backward compatibility.
     */
    public static boolean usesbuilder = false;

    // cache of relation definitions
    // sname or sname/dname -> rnumber
    private final Map<String, Integer> relCache = new HashMap<String, Integer>();

    // cache of valid relationbuilders
    // otype of relations builder -> MMObjectBuilder
    private  Map<Integer, MMObjectBuilder> relBuilderCache = null;

    // rnumber -> MMObjectBuilder Name
    private  Map<Integer, String> rnumberCache = new HashMap<Integer, String>();

    /**
     *  Contruct the builder
     */
    public RelDef() {
    }

    /**
     *  Initializes the builder by reading the cache. Also determines whether the 'builder' field is used.
     *  @return A <code>boolean</code> value, always success (<code>true</code>), as any exceptions are
     *         caught and logged.
     */
    @Override
    public boolean init() {
       super.init();
       usesbuilder = getField("builder") != null;
       return readCache();
    }

    /**
     * Puts a role in the reldef cache.
     * The role is entered both with its sname (primary identifier) and
     * it's sname/dname combination.
     */
    private void addToCache(MMObjectNode node) {
        Integer rnumber = node.getNumber();
        relCache.put(node.getStringValue("sname"), rnumber);
        relCache.put(node.getStringValue("sname") + "/" + node.getStringValue("dname"), rnumber);

        rnumberCache.put(rnumber, findBuilderName(node));
    }

    /**
     * Removes a role from the reldef cache.
     * The role is removed both with its sname (primary identifier) and
     * it's sname/dname combination.
     */
    private void removeFromCache(MMObjectNode node) {
        relCache.remove(node.getStringValue("sname"));
        relCache.remove(node.getStringValue("sname") + "/" + node.getStringValue("dname"));

        rnumberCache.remove(Integer.valueOf(node.getNumber()));
    }

    /**
     * @since MMBase-1.7.1
     */
    private void removeFromCache(int rnumber) {
        Integer r = Integer.valueOf(rnumber);
        Iterator<Map.Entry<String, Integer>> i = relCache.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, Integer> entry = i.next();
            Integer value = entry.getValue();
            if (r.equals(value)) {
                i.remove();
            }
        }
        rnumberCache.remove(r);
    }


    /**
     * Reads all relation definition names in an internal cache.
     * The cache is used by {@link #isRelationTable}
     * @return A <code>boolean</code> value, always success (<code>true</code>), as any exceptions are
     *         caught and logged.
     */
    private boolean readCache() {
        rnumberCache.clear();
        relCache.clear();        // add insrel (default behavior)
        relCache.put("insrel", Integer.valueOf(-1));
        // add relation definiation names
        try {
            for (MMObjectNode n :getNodes(new NodeSearchQuery(this))) {
                addToCache(n);
            }
        } catch (org.mmbase.storage.search.SearchQueryException sqe) {
            log.error("Error while reading reldef cache" + sqe.getMessage(), sqe);
        }
        return true;
    }

    /**
     * Returns a GUI description of a relation definition.
     * The description is dependent on the direction (uni/bi) of the relation
     * @param node Relation definition to describe
     * @return A <code>String</code> of descriptive text
     */
    @Override
    public String getGUIIndicator(MMObjectNode node) {
        int dir = node.getIntValue("dir");
        if (dir == DIR_UNIDIRECTIONAL) {
            return node.getStringValue("sguiname");
        } else {
            String st1 = node.getStringValue("sguiname");
            String st2 = node.getStringValue("dguiname");
            return st1 + "/" + st2;
        }
    }


    /**
     * @param reldefNodeNumber rnumber
     * @since MMBase-1.7
     */

    public String getBuilderName(Integer reldefNodeNumber) {
        return  rnumberCache.get(reldefNodeNumber);
    }


    /**
     * @since MMBase-1.7
     */
    protected String findBuilderName(MMObjectNode node) {
        String bulname = null;
        if (usesbuilder) {
            int builder = node.getIntValue("builder");
            if (builder <= 0) {
                bulname = node.getStringValue("sname");
            } else {
                bulname = mmb.getTypeDef().getValue(builder);
              }
        } else {
            // fix for old mmbases that have no builder field
            bulname = node.getStringValue("sname");
            if (mmb.getMMObject(bulname) == null) bulname=null;
        }
        if (bulname == null) {
            return "insrel";
        } else {
            return bulname;
        }
    }

    /**
     * Returns the builder name of a relation definition.
     * If the buildername cannot be accurately determined, the <code>sname</code> field will be returned instead.
     * @param  node The reldef Node
     * @return the builder name
     */
    public String getBuilderName(MMObjectNode node) {
        if (node == null) return "NULL";
        return rnumberCache.get(Integer.valueOf(node.getNumber()));
    }


    /**
     * Returns the builder of a relation definition.
     * @return the builder
     */
    public InsRel getBuilder(int rnumber) {
        return getBuilder(getNode(rnumber));
    }

    /**
     * Returns the builder of a relation definition.
     * @return the builder
     */
    public InsRel getBuilder(MMObjectNode node) {
        String builderName = getBuilderName(node);
        if (builderName == null) {
            throw new RuntimeException("Node " + node + " has no builder?");
        }
        MMObjectBuilder builder = mmb.getBuilder(builderName);
        if (builder == null) {
            return mmb.getInsRel();
        } else {
            if (builder instanceof InsRel) {
                return (InsRel) builder;
            } else {
                log.warn("The builder " + builderName + " of node " + node.getNumber() + " is no InsRel (but " + builder.getClass() + ").  Perhaps it is inactive? Impossible here. Returing InsRel any way.");
                return mmb.getInsRel();

            }
        }
    }

    /**
     * Returns the first occurrence of a reldef node of a relation definition.
     * used to set the default reldef for a specific builder.
     * @return the default reldef node, or <code>null</code> if not found.
     */
    public MMObjectNode getDefaultForBuilder(InsRel relBuilder) {
        MMObjectNode node = null;
        NodeSearchQuery query = new NodeSearchQuery(this);
        if (usesbuilder) {
            Integer value = relBuilder.getNumber();
            Constraint constraint = new BasicFieldValueConstraint(query.getField(getField("builder")), value);
            query.setConstraint(constraint);
        } else {
            // backward compatibility with older reldefs builders.
            // this should become obsolete at some point.
            Constraint constraint1 = new BasicFieldValueConstraint(query.getField(getField("sname")), relBuilder.getTableName());
            Constraint constraint2 = new BasicFieldValueConstraint(query.getField(getField("dname")), relBuilder.getTableName());
            BasicCompositeConstraint constraint = new BasicCompositeConstraint(CompositeConstraint.LOGICAL_OR);
            constraint.addChild(constraint1);
            constraint.addChild(constraint2);
            query.setConstraint(constraint);
        }
        query.setMaxNumber(1);
        try {
            List<MMObjectNode> reldefs = getNodes(query);
            if (reldefs.size() != 0) {
                node = reldefs.get(0);
            }
        } catch (SearchQueryException sqe) {
            // should never happen
            log.error(sqe);
        }
        return node;
    }

    /**
     * Tests whether the data in a node is valid (throws an exception if this is not the case).
     * @param node The node whose data to check
     */
    @Override
    public void testValidData(MMObjectNode node) throws InvalidDataException{
        int dir=node.getIntValue("dir");
        if ((dir!=DIR_UNIDIRECTIONAL) && (dir!=DIR_BIDIRECTIONAL)) {
            throw new InvalidDataException("Invalid directionality ("+dir+") specified","dir");
        }
        if (usesbuilder) {
            int builder=node.getIntValue("builder");
            if (builder<=0) {
                builder=mmb.getInsRel().getNumber();
            }
            if (!isRelationBuilder(builder)) {
                throw new InvalidDataException("Builder ("+builder+") is not a relationbuilder","builder");
            }
        }
    };

    /**
     * Insert a new object, and updated the cache after an insert.
     * This method indirectly calls {@link #preCommit}.
     * @param owner The administrator creating the node
     * @param node The object to insert. The object need be of the same type as the current builder.
     * @return An <code>int</code> value which is the new object's unique number, -1 if the insert failed.
     */
    @Override
    public int insert(String owner, MMObjectNode node) {
        // check RelDef for duplicates
        String sname = node.getStringValue("sname");
        String dname = node.getStringValue("dname");
        if (getNumberByName(sname + '/' + dname) != -1) {
            // log.error("The reldef with sname=" + sname + " and dname=" + dname + " already exists");
            throw new RuntimeException("The reldef with sname=" + sname + " and dname=" + dname + " already exists");
        }
        int number = super.insert(owner,node);
        log.service("Created new reldef " + sname + "/" + dname);
        if (number != -1) {
            addToCache(node);
        }
        return number;
    };


    /**
     * Commit changes to this node and updated the cache. This method indirectly calls {@link #preCommit}.
     * This method does not remove names from the cache, as currently, unique names are not enforced.
     * @param node The node to be committed
     * @return a <code>boolean</code> indicating success
     */
    @Override
    public boolean commit(MMObjectNode node) {
        boolean success = super.commit(node);
        if (success) {
            addToCache(node);
        }
        return success;
   }

    /**
     * Remove a node from the cloud.
     * @param node The node to remove.
     */
    @Override
    public void removeNode(MMObjectNode node) {
        // check occurrences in TypeRel
        // perhaps this can also be done using getAllowedRelations() ?
        try {
            MMObjectBuilder typeRel = mmb.getTypeRel();
            NodeSearchQuery query = new NodeSearchQuery(typeRel);
            Integer value = node.getNumber();
            Constraint constraint = new BasicFieldValueConstraint(query.getField(typeRel.getField("rnumber")), value);
            query.setConstraint(constraint);
            List<MMObjectNode> typerels = typeRel.getNodes(query);
            if (typerels.size() > 0) {
                throw new RuntimeException("Cannot delete reldef, it is referenced by typerels: " + typerels);
            }
        } catch (SearchQueryException sqe) {
            // should never happen
            log.error(sqe);
        }

        // check occurrences in the relation builders
        try {
            MMObjectBuilder insRel = mmb.getInsRel();
            NodeSearchQuery query = new NodeSearchQuery(insRel);
            Integer value = node.getNumber();
            Constraint constraint = new BasicFieldValueConstraint(query.getField(insRel.getField("rnumber")), value);
            query.setConstraint(constraint);
            int i = insRel.count(query);
            if (i > 0) {
                throw new RuntimeException("Cannot delete reldef node, it is still used in " + i + " relations");
            }
        } catch (SearchQueryException sqe) {
            // should never happen
            log.error(sqe);
        }

        super.removeNode(node);
        removeFromCache(node);
    }

    /**
     * Sets defaults for a new relation definition.
     * Initializes a relation to be bidirectional, and, if applicable, to use the 'insrel' builder.
     *    @param node Node to be initialized
     */
    @Override
    public void setDefaults(MMObjectNode node) {
        node.setValue("dir", DIR_BIDIRECTIONAL);
        if (usesbuilder) {
            node.setValue("builder", mmb.getInsRel().getNumber());
        }
    }

    /**
     * Returns all possible rnumbers ('roles').
     * @since MMBase-1.9
     */
    public Set<Integer> getRoles() {
        return Collections.unmodifiableSet(rnumberCache.keySet());
    }

    /**
     * Retrieve descriptors for a relation definition's fields,
     * specifically a descriptive text for the relation's direction (dir)
     * @param field Name of the field whose description should be returned.
     *              valid values : 'dir'
     * @param node Relation definition containing the field's information
     * @return A descriptive text for the field's contents, or null if no description could be generated
     */

    @Override
    public String getGUIIndicator(String field, MMObjectNode node) {
        try {
            if (field.equals("dir")) {
                switch (node.getIntValue("dir")) {
                case DIR_BIDIRECTIONAL:
                    return "bidirectional";

                case DIR_UNIDIRECTIONAL:
                    return "unidirectional";

                default:
                    return "unknown";
                }
            } else if (field.equals("builder")) {
                int builder=node.getIntValue("builder");
                if (builder<=0) {
                    return "insrel";
                } else {
                    return mmb.getTypeDef().getValue(builder);
                }
            }
        } catch (Exception e) {}
        return null;
    }

    /**
     * Checks to see if a given relation definition is stored in the cache.
     * @param name A <code>String</code> of the relation definitions' name
     * @return a <code>boolean</code> indicating success if the relationname exists
     */

    public boolean isRelationTable(String name) {
        return relCache.containsKey(name);
    }

    // Retrieves the relationbuildercache (initializes a new cache if the old one is empty)
    private Map<Integer, MMObjectBuilder> getRelBuilderCache() {
        // first make sure the buildercache is loaded
        if (relBuilderCache == null) {
            relBuilderCache = new HashMap<Integer, MMObjectBuilder>();
            // add all builders that descend from InsRel
            for (MMObjectBuilder fbul : mmb.getBuilders()) {
                if (fbul instanceof InsRel) {
                    relBuilderCache.put(Integer.valueOf(fbul.getNumber()), fbul);
                }
            }
        }
        return relBuilderCache;
    }

    /**
     * Checks to see if a given builder (otype) is known to be a relation builder.
     * @param number The otype of the builder
     * @return a <code>boolean</code> indicating success if the builder exists in the cache
     */

    public boolean isRelationBuilder(int number) {
        return getRelBuilderCache().containsKey(Integer.valueOf(number));
    }

    /**
     * Returns a list of builders currently implementing a relation node.
     * @return an <code>Enumeration</code> containing the builders (as otype)
     */

    public Enumeration<MMObjectBuilder> getRelationBuilders() {
        return Collections.enumeration(getRelBuilderCache().values());
    }



    /**
     * Search the relation definition table for the identifying number of
     * a relation, by name of the relation to use
     * Similar to {@link #getGuessedByName} (but does not make use of dname)
     * Not very suitable to use, as success is dependent on the uniqueness of the builder in the table (not enforced, so unpredictable).
     * @param role The builder name on which to search for the relation
     * @return A <code>int</code> value indicating the relation's object number, or -1 if not found. If multiple relations use the
     *     indicated buildername, the first one found is returned.
     * @deprecated renamed to {@link #getNumberByName} which better explains its use
     */
    public int getGuessedNumber(String role) {
        return getNumberByName(role, false);
    }

    /**
     * Search the relation definition table for the identifying number of
     * a relationdefinition, by name of the role to use.
     * The name should be either the primary identifying role name (sname),
     * or a combination of sname and dname separated by a slash ("/").
     * @todo support for searching on dname
     * @param role The role name on which to search
     * @return A <code>int</code> value indicating the relation's object number, or -1 if not found.
     */
    public int getNumberByName(String role) {
        return getNumberByName(role, false);
     }

    /**
     * Search the relation definition table for the identifying number of
     * a relationdefinition, by name of the role to use.
     * Initially, this method seraches on either the primary identifying
     * role name (sname), or a combination of sname and dname separated by a slash ("/").
     * If this yields no result, and  searchBidirectional is true, the method then searches
     * on the secondary identifying role name.
     * The latter is not cached (to avoid conflict and is thus slower).
     *
     * @todo support for searching on dname
     * @param role The role name on which to search
     * @param searchBidirectional determines whether to also search in sname
     * @return A <code>int</code> value indicating the relation's object number, or -1 if not found.
     */
    public int getNumberByName(String role, boolean searchBidirectional) {
        Integer number = relCache.get(role);
        if (number != null) {
            return number.intValue();
        }
        if (searchBidirectional) {
            NodeSearchQuery query = new NodeSearchQuery(this);
            Constraint constraint = new BasicFieldValueConstraint(query.getField(getField("dname")), role);
            query.setConstraint(constraint);
            query.setMaxNumber(1);
            try {
                List<MMObjectNode> reldefs = getNodes(query);
                if (reldefs.size() != 0) {
                    MMObjectNode node = reldefs.get(0);
                    return node.getNumber();
                }
            } catch (SearchQueryException sqe) {
                // should never happen
                log.error(sqe);
            }
        }
        return -1;
     }

    /**
     * Search the relation definition table for the identifying number of
     * a relation, by name of the relation to use.
     * This function is used by descendants of Insrel to determine a default reference to a 'relation definition' (reldef entry).
     * The 'default' is the relation with the same name as the builder. If no such relation exists, there is no default.
     * @param role The role name on which to search for the relation
     * @return A <code>int</code> value indicating the relation's object number, or -1 if not found. If multiple relations use the
     *     indicated buildername, the first one found is returned.
     * @deprecated use {@link #getNumberByName} instead
     */

    public int getGuessedByName(String role) {
        return getNumberByName(role, true);
    }

    /**
     * Searches for the relation number on the combination of sname and dname.
     * When there's no match found in this order a search with a swapped sname and dname will be done.
     * Note that there is no real assurance that an sname/dname combination must be unique.
     * @param sname The first name on which to search for the relation (preferred as the source)
     * @param dname The second name on which to search for the relation (preferred as the destination)
     * @return A <code>int</code> value indicating the relation's object number, or -1 if not found. If multiple relations use the
     *     indicated names, the first one found is returned.
     * @deprecated use {@link #getNumberByName} instead
     */
    public int getRelsNrByName(String sname, String dname) {
        int res = getNumberByName(sname + "/" + dname);
        if (res < -1) {
            res = getNumberByName(dname + "/" + sname);
        }
        return res;
    }

    /**
     * {@inheritDoc}
     * Called when a remote node is changed.
     * If a node is changed or newly created, this adds the new or updated role (sname and dname) to the
     * cache.
     * @todo Old roles are cuerrently not cleared or removed - which means that they may remain
     * useable for some time after the actual role is deleted or renamed.
     * @todo Use 1.8 event mechanism in stead.
     *
     * This because old role information is no longer available when this call is made.
     * @since MMBase-1.7.1

     */
    @Override
    public boolean nodeRemoteChanged(String machine, String number, String builder, String ctype) {
        if (builder.equals(getTableName())) {
            if (ctype.equals("c") || ctype.equals("n")) {
                // should remove roles referencing this number from relCache here
                int rnumber = Integer.parseInt(number);
                removeFromCache(rnumber);
                addToCache(getNode(rnumber));
            } else if (ctype.equals("d")) {
                removeFromCache(Integer.parseInt(number));
            }
        }
        return super.nodeRemoteChanged(machine, number, builder, ctype);
    }
}




