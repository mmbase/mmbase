/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import org.mmbase.bridge.Field;
import org.mmbase.module.corebuilders.RelDef;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.Casting;

/**
 * This class is severely underdocumented. Some remarks:

 <ul>
 <li>This is a singleton</li>
 <li>It does not itself store the 'temporary nodes'. This is for some reason done in static map in MMObjectBuilder.</li>
 <li>Most methods accept 'owner' and 'key' arguments. It is not entirely clear what those mean</li>
 </ul>

 * @javadoc
 *
 * @author Rico Jansen
 * @version $Id: TemporaryNodeManager.java,v 1.56 2008-12-02 16:38:31 michiel Exp $
 */
public class TemporaryNodeManager {

    private static final Logger log = Logging.getLoggerInstance(TemporaryNodeManager.class);

    /**
     * Return value for setObjectField
     */
    public static final String UNKNOWN = "unknown";
    /**
     * @since MMBase-1.8
     */
    public static final String INVALID_VALUE = "invalid value";

    private final MMBase mmbase;

    /**
     * @javadoc
     */
    TemporaryNodeManager(MMBase mmbase) {
        this.mmbase = mmbase;
    }

    /**
     * @javadoc
     */
    public String createTmpNode(String type, String owner, String key) {
        if (log.isDebugEnabled()) {
            log.debug("createTmpNode : type=" + type + " owner=" + owner + " key=" + key);
        }
        // WTF!?
        //        if (owner.length() > 12) owner = owner.substring(0, 12);
        MMObjectBuilder builder = mmbase.getBuilder(type);
        MMObjectNode node;
        if (builder != null) {
            node = builder.getNewTmpNode(owner, getTmpKey(owner, key));
            if (log.isDebugEnabled()) {
                log.debug("New tmpnode " + node);
            }
        } else {
            log.error("Can't find builder " + type);
        }
        return key;
    }

    /**
     * @javadoc
     */
    protected String createTmpRelationNode(String role, String owner, String key, String source, String destination) throws Exception {
        // decode type to a builder using reldef
        RelDef reldef = mmbase.getRelDef();
        int rnumber = reldef.getNumberByName(role, true);
        if(rnumber == -1) {
            throw new Exception("role " + role + " is not a proper relation");
        }
        MMObjectBuilder builder = reldef.getBuilder(reldef.getNode(rnumber));
        String bulname          = builder.getTableName();

        // Create node
        createTmpNode(bulname, owner, key);
        setObjectField(owner, key, "_snumber", getTmpKey(owner, source));
        setObjectField(owner, key, "_dnumber", getTmpKey(owner, destination));
        setObjectField(owner, key, "rnumber", "" + rnumber);
        return key;
    }

    /**
     * @javadoc
     */
    public String createTmpAlias(String name, String owner, String key, String destination) {
        MMObjectBuilder builder = mmbase.getOAlias();
        String bulname = builder.getTableName();

        // Create alias node
        createTmpNode(bulname, owner, key);
        setObjectField(owner, key, "_destination", getTmpKey(owner, destination));
        setObjectField(owner, key, "name", name);
        return key;
    }

    /**
     * @javadoc
     */
    public String deleteTmpNode(String owner, String key) {
        MMObjectBuilder b = mmbase.getBuilder("object");
        b.removeTmpNode(getTmpKey(owner, key));
        if (log.isDebugEnabled()) {
            log.debug("delete node " + getTmpKey(owner, key));
        }
        return key;
    }

    /**
     * Tries to get the node with number 'key'. If there was already a temporary node with this key,
     * for the given owner, it will return that node. The node will <em>not</em> be stored in the
     * temporory nodes map. For that use {@link #getObject}.
     */
    public MMObjectNode getNode(String owner, String key) {
        MMObjectBuilder bul = mmbase.getBuilder("object");
        String tmpKey = getTmpKey(owner, key);
        MMObjectNode node = bul.getTmpNode(tmpKey);
        // fallback to normal nodes
        if (node == null) {
            log.debug("getNode tmp not node found " + key);
            node = bul.getNode(key);
            if(node == null) throw new RuntimeException("Node not found !! (key = '" + key + "' nor tmpKey = " + tmpKey + ")");
        }
        return node;
    }

    /**
     * Makes sure that the node identify with 'key' is a temporary node for the given owner. Returns
     * the key if sucessfull, <code>null</code> otherwise.
     *
     */
    public String getObject(final String owner, final String key, final String dbkey) {
        String tmpKey = getTmpKey(owner, key);
        MMObjectNode node = MMObjectBuilder.getTmpNode(tmpKey);
        if (node == null) {
            MMObjectBuilder bul = mmbase.getBuilder("object");
            node = bul.getNode(dbkey, false);
            if (node == null) {
                log.warn("Node not found in database " + dbkey);
            } else {
                MMObjectBuilder.putTmpNode(tmpKey, node);
            }
        }
        if (node != null) {
            return key;
        } else {
            return null;
        }
    }
    /**
     * @since MMBase-1.9
     */
    public String getObject(String owner, String key) {
        return getObject(owner, key, key);
    }

    /**
     * @javadoc
     * @return An empty string if succesfull, the string {@link #UNKNOWN} if the field was not found in the node.
     *         The string {@link #INVALID_VALUE} if the value was not valid for the field's type.
     */
    public String setObjectField(String owner, String key, String field, Object value) {
        MMObjectNode node = getNode(owner, key);
        if (node != null) {
            int type = node.getDBType(field);
            if (type >= 0) {
                if (value instanceof String) {
                    String stringValue = (String)value;
                    switch(type) {
                    case Field.TYPE_XML:
                    case Field.TYPE_STRING:
                        node.setValue(field, stringValue);
                        break;
                    case Field.TYPE_NODE:
                    case Field.TYPE_INTEGER:
                        try {
                            int i = -1;
                            if (!stringValue.equals("")) i = Integer.parseInt(stringValue);
                            node.setValue(field, i);
                        } catch (NumberFormatException x) {
                            log.debug("Value for field " + field + " is not a number '" + stringValue + "'");
                            return INVALID_VALUE;
                        }
                        break;
                    case Field.TYPE_BINARY:
                        log.error("We don't support casts from String to Binary");
                        return INVALID_VALUE; // so, a String value is invalid for binaries.
                    case Field.TYPE_FLOAT:
                        try {
                            float f = -1;
                            if (!stringValue.equals("")) f = Float.parseFloat(stringValue);
                            node.setValue(field,f);
                        } catch (NumberFormatException x) {
                            log.debug("Value for field " + field + " is not a number " + stringValue);
                            return INVALID_VALUE;
                        }
                        break;
                    case Field.TYPE_DOUBLE:
                        try {
                            double d = -1;
                            if (!stringValue.equals("")) d = Double.parseDouble(stringValue);
                            node.setValue(field,d);
                        } catch (NumberFormatException x) {
                            log.debug("Value for field " + field + " is not a number " + stringValue);
                            return INVALID_VALUE;
                        }
                        break;
                    case Field.TYPE_LONG:
                        try {
                            long l = -1;
                            if (!stringValue.equals("")) l = Long.parseLong(stringValue);
                            node.setValue(field,l);
                        } catch (NumberFormatException x) {
                            log.debug("Value for field " + field + " is not a number " + stringValue);
                            return INVALID_VALUE;
                        }
                        break;
                    case Field.TYPE_DATETIME:
                        try {
                            node.setValue(field, Casting.toDate(value));
                        } catch (Exception e) {
                            return INVALID_VALUE;
                        }
                        break;
                    case Field.TYPE_BOOLEAN:
                        if (org.mmbase.datatypes.StringDataType.BOOLEAN_PATTERN.matcher(stringValue).matches()) {
                            node.setValue(field, Casting.toBoolean(value));
                        } else {
                            return INVALID_VALUE;
                        }
                        break;
                    default:
                        log.error("Unknown type for field " + field);
                        break;
                    }
                } else {
                    log.debug("Setting " + field + " to " + value + " of " + node + " " + owner + " " + key);
                    node.setValue(field, value);
                }
            } else {
                node.setValue(field, value);
                log.warn("Invalid type for field " + field);
                return UNKNOWN;
            }
        } else {
            log.error("setObjectField(): Can't find node : " + key);
        }
        return "";
    }


    /**
     * @javadoc
     */
    public String getObjectField(String owner, String key, String field) {
        String rtn;
        MMObjectNode node = getNode(owner, key);
        if (node == null) {
            log.error("Node " + key + " not found!");
            rtn = "";
        } else {
            rtn = node.getStringValue(field);
        }
        return rtn;
    }

    /**
     * Returns the unique key in given the number and owner of a node.
     * TemporaryNodeManager distinguishes nodes for different users.
     */
    private String getTmpKey(String owner, String key) {
        return owner + "_" + key;
    }
}
