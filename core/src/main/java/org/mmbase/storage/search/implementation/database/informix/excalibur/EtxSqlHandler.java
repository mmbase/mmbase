/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search.implementation.database.informix.excalibur;

import java.io.*;
import java.util.*;

import org.mmbase.bridge.Field;
import org.mmbase.module.core.*;
import org.mmbase.storage.StorageManagerFactory;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.database.*;
import org.mmbase.util.logging.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * The Etx query handler adds support for Excalibur Text Search constraints,
 * when used with an Informix database and an Excalibur Text Search datablade.
 * This class is provided as a coding example of a ChainedSqlHandler.
 * <p>
 * On initialization, the handler reads a list of etx-indices from a
 * configuration file.
 * This configurationfile must be named <em>etxindices.xml</em> and located
 * inside the <em>databases</em> configuration directory.
 * It's dtd is located in the directory
 * <code>org.mmbase.storage.search.implementation.database.informix.excalibur.resources</code>
 * in the MMBase source tree and
 * <a href="https://www.mmbase.org/dtd/etxindices.dtd">here</a> online.
 *
 * @author Rob van Maris
 * @version $Id$
 * @since MMBase-1.7
 */
// TODO RvM: (later) add javadoc, elaborate on overwritten methods.
public class EtxSqlHandler extends ChainedSqlHandler implements SqlHandler {

    private static final Logger log = Logging.getLoggerInstance(EtxSqlHandler.class);

    /**
     * The indexed fields, stored as {@link #BuilderField BuilderField}
     *  instances.
     */
    private Set<String> indexedFields = new HashSet<String>();

    /**
     * Creates a new instance of EtxueryHandler.
     *
     * @param successor Successor in chain or responsibility.
     */
    public EtxSqlHandler(SqlHandler successor) throws IOException {
        super(successor);
        init();
    }

    // javadoc is inherited
    public void appendConstraintToSql(StringBuilder sb, Constraint constraint,
    SearchQuery query, boolean inverse, boolean inComposite)
    throws SearchQueryException {
        // Net effect of inverse setting with constraint inverse property.
        boolean overallInverse = inverse ^ constraint.isInverse();

        if (constraint instanceof StringSearchConstraint) {
            // TODO: test for support, else throw exception
            // TODO: support maxNumber for query with etx constraint.
            StringSearchConstraint stringSearchConstraint
                = (StringSearchConstraint) constraint;
            StepField field = stringSearchConstraint.getField();
            Map<String,Object> parameters = stringSearchConstraint.getParameters();

            // TODO: how to implement inverse,
            // it is actually more complicated than this:
            if (overallInverse) {
                sb.append("NOT ");
            }
            sb.append("etx_contains(").
            append(getAllowedValue(field.getStep().getAlias())).
            append(".").
            append(getAllowedValue(field.getFieldName())).
            append(", Row('");

            Iterator<String> iSearchTerms
                = stringSearchConstraint.getSearchTerms().iterator();
            while (iSearchTerms.hasNext()) {
                String searchTerm = iSearchTerms.next();
                sb.append(searchTerm);
                if (iSearchTerms.hasNext()) {
                    sb.append(" ");
                }
            }
            sb.append("', '");
            switch (stringSearchConstraint.getSearchType()) {
                case StringSearchConstraint.SEARCH_TYPE_WORD_ORIENTED:
                    sb.append("SEARCH_TYPE = WORD");
                    break;

                case StringSearchConstraint.SEARCH_TYPE_PHRASE_ORIENTED:
                    sb.append("SEARCH_TYPE = PHRASE_EXACT");
                    break;

                case StringSearchConstraint.SEARCH_TYPE_PROXIMITY_ORIENTED:
                    Integer proximityLimit
                        = (Integer) parameters.
                            get(StringSearchConstraint.PARAM_PROXIMITY_LIMIT);
                    if (proximityLimit == null) {
                        throw new IllegalStateException(
                        "Parameter PARAM_PROXIMITY_LIMIT not set " +
                        "while trying to perform proximity oriented search.");
                    }
                    sb.append("SEARCH_TYPE = PROX_SEARCH(").append(proximityLimit).append(")");
                    break;

                default:
                    throw new IllegalStateException("Invalid searchtype value: "
                        + stringSearchConstraint.getSearchType());
            }

            switch(stringSearchConstraint.getMatchType()) {
                case StringSearchConstraint.MATCH_TYPE_FUZZY:
                    Float fuzziness =
                        (Float) parameters.get(StringSearchConstraint.PARAM_FUZZINESS);
                    int wordScore = Math.round(100 * fuzziness);
                    sb.append(" & PATTERN_ALL & WORD_SCORE = ").append(wordScore);
                    break;

                case StringSearchConstraint.MATCH_TYPE_LITERAL:
                    break;

                case StringSearchConstraint.MATCH_TYPE_SYNONYM:
                    log.warn("Synonym matching not supported. Executing this query with literal matching instead: " + query);
                    break;

                default:
                    throw new IllegalStateException("Invalid matchtype value: "
                        + stringSearchConstraint.getMatchType());
            }

            sb.append("'))");

        } else {
            getSuccessor().appendConstraintToSql(sb, constraint, query,
            inverse, inComposite);
        }
    }

    // javadoc is inherited
    public int getSupportLevel(int feature, SearchQuery query) throws SearchQueryException {
        int support;
        switch (feature) {
            case SearchQueryHandler.FEATURE_MAX_NUMBER:
                // optimal with etx index on field, and constraint is
                // StringSearchConstraint, with no additonal constraints.
                Constraint constraint = query.getConstraint();
                if (constraint != null
                        && constraint instanceof StringSearchConstraint
                        && hasEtxIndex(((StringSearchConstraint) constraint).getField())
                        && !hasAdditionalConstraints(query)) {
                    support=SearchQueryHandler.SUPPORT_OPTIMAL;
                } else {
                    support = getSuccessor().getSupportLevel(feature, query);
                }
                break;
            default:
                support = getSuccessor().getSupportLevel(feature, query);
        }
        return support;
    }

    // javadoc is inherited
    public int getSupportLevel(Constraint constraint, SearchQuery query)
    throws SearchQueryException {
        int support;

        if (constraint instanceof StringSearchConstraint
                && hasEtxIndex(((StringSearchConstraint) constraint).getField())) {
            StringSearchConstraint stringSearchConstraint =
                (StringSearchConstraint) constraint;
            // StringSearchConstraint on field with etx index:
            // - none if matchtype = MATCH_TYPE_SYNONYM
            // - otherwise: weak support if other stringsearch constraints are present
            // - otherwise: optimal support
            if (stringSearchConstraint.getMatchType()
                    == StringSearchConstraint.MATCH_TYPE_SYNONYM) {
                support = SearchQueryHandler.SUPPORT_NONE;
            } else if (containsOtherStringSearchConstraints(
                    query.getConstraint(), stringSearchConstraint)) {
                support = SearchQueryHandler.SUPPORT_WEAK;
            } else  {
                support = SearchQueryHandler.SUPPORT_OPTIMAL;
            }
        } else {
            support = getSuccessor().getSupportLevel(constraint, query);
        }
        return support;
    }

    /**
     * Tests if an Excelibur Text Search index has been made for this field.
     *
     * @param field the field.
     * @return true if an Excelibur Text Search index has been made for this field,
     *         false otherwise.
     */
    public boolean hasEtxIndex(StepField field) {
        boolean result = false;
        if (field.getType() == Field.TYPE_STRING
        || field.getType() == Field.TYPE_XML) {
            result = indexedFields.contains(
            field.getStep().getTableName() + "." + field.getFieldName());
        }
        return result;
    }

    /**
     * Tests if the query contains additional constraints, i.e. on relations
     * or nodes.
     *
     * @param query the query.
     * @return true if the query containts additional constraints,
     *         false otherwise.
     */
    protected boolean hasAdditionalConstraints(SearchQuery query) {
        for (Step step1 : query.getSteps()) {
            Step step = step1;
            if (step instanceof RelationStep || step.getNodes() != null) {
                // Additional constraints on relations or nodes.
                return true;
            }
        }
        // No additonal constraints:
        return false;
    }

    /**
     * Tests if a constaint is/contains another stringsearch constraint than
     * the specified one. Recursively seaches through all childs of composite
     * constraints.
     *
     * @param constraint the constraint.
     * @param searchConstraint the stringsearch constraint.
     * @return true if the constraint is/contains another stringsearch constraint
     *             than the given one, false otherwise.
     */
    protected boolean containsOtherStringSearchConstraints(
    Constraint constraint,
    StringSearchConstraint searchConstraint) {
        if (constraint instanceof CompositeConstraint) {
            // Composite constraint.
            for (Constraint constraint1 : ((CompositeConstraint) constraint).getChilds()) {
                Constraint childConstraint = constraint1;
                if (containsOtherStringSearchConstraints(childConstraint, searchConstraint)) {
                    // Another stringsearch constraint found in childs.
                    return true;
                }
            }
            // No other stringsearch constraint found in childs.
            return false;

        } else return constraint instanceof StringSearchConstraint
                && constraint != searchConstraint;
    }

    /**
     * Initializes the handler by reading the etxindices configuration file
     * to determine which fields have a etx index.
     * <p>
     * The configurationfile must be named <em>etxindices.xml</em> and located
     * inside the <em>databases</em> configuration directory.
     *
     * @throw IOException When a failure occurred while trying to read the
     *        configuration file.
     */
    private void init() throws IOException {
        File etxConfigFile = new File(
            MMBaseContext.getConfigPath() + "/databases/etxindices.xml");
        XmlEtxIndicesReader configReader =
            new XmlEtxIndicesReader(
                new InputSource(
                    new BufferedReader(
                        new FileReader(etxConfigFile))));

        for (Iterator<Element> eSbspaces = configReader.getSbspaceElements(); eSbspaces.hasNext();) {
            Element sbspace = eSbspaces.next();

            for (Iterator<Element> eEtxIndices = configReader.getEtxindexElements(sbspace); eEtxIndices.hasNext();) {
                Element etxIndex = eEtxIndices.next();
                String table = configReader.getEtxindexTable(etxIndex);
                String field = configReader.getEtxindexField(etxIndex);
                String index = configReader.getEtxindexValue(etxIndex);
                try {
                    String builderField = toBuilderField(table, field);
                    indexedFields.add(builderField);
                    log.service("Registered etx index \"" + index +
                    "\" for builderfield " + builderField);
                } catch (IllegalArgumentException e) {
                    log.error("Failed to register etx index \"" +
                    index + "\": " + e);
                }
            }
        }
    }

    /**
     * Finds builderfield corresponding to the database table and field names.
     *
     * @param dbTable The tablename used in the database.
     * @param dbField The fieldname used in the database.
     * @return The corresponding builderfield represented by a string of the
     *         form &lt;buildername&gt;.&lt;fieldname&gt;.
     * @throws IllegalArgumentException when an invalid argument is supplied.
     */
    static String toBuilderField(String dbTable, String dbField) {
        // package visibility!
        MMBase mmbase = MMBase.getMMBase();
        StorageManagerFactory factory = mmbase.getStorageManagerFactory();
        String tablePrefix = mmbase.getBaseName() + "_";

        if (!dbTable.startsWith(tablePrefix)) {
            throw new IllegalArgumentException(
            "Invalid tablename: \"" + dbTable + "\". " +
            "It should start with the prefix \"" + tablePrefix + "\".");
        }

        String builderName = dbTable.substring(tablePrefix.length());
        MMObjectBuilder builder;
        try {
            builder = mmbase.getBuilder(builderName);
        } catch (BuilderConfigurationException e){
            // Unknown builder.
            builder = null;
        }

        if (builder == null) {
            throw new IllegalArgumentException(
            "Unknown builder: \"" + builderName + "\".");
        }

        for (String s : builder.getFieldNames()) {
            String fieldName = s;
            if (factory.getStorageIdentifier(fieldName).equals(dbField)) {
                return builderName + "." + fieldName;
            }
        }

        throw new IllegalArgumentException(
        "No field corresponding to database field \"" + dbField
        + "\" found in builder \"" + builderName + "\".");
    }

}
