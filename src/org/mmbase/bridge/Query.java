/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;
import org.mmbase.storage.search.*;
import java.util.SortedSet;

/**
 * Representation of a (database) query. It is modifiable for use by bridge-users.
 *
 * @author Michiel Meeuwissen
 * @version $Id: Query.java,v 1.14 2003-08-05 20:24:54 michiel Exp $
 * @since MMBase-1.7
 */
public interface Query extends SearchQuery, Cloneable {


    /**
     * Wheter this query is 'aggregating'. You can only use 'addAggregatedField' on aggregating querys.
     * @todo Should this not appear in SearchQuery itself? Or should there be an AggregatingQuery interface?
     * It is now used in BasicCloud.getList.
     */
    boolean isAggregating(); 

    /**
     * Adds a NodeManager to this Query. This can normally be done only once. After that you need
     * to use (one of the) 'addRelationStep'.
     *
     * @param nodeManager The nodeManager associated with the step.
     * @return The 'step' wrapping the NodeManager.
     * @throws IllegalArgumentException when an invalid argument is supplied.
     * @see #addRelationStep
     */
    Step addStep(NodeManager nodeManager);
    
    

    /**
     * Adds new RelationManager to the query.  Adds the next Step (containing the Destination
     * Manager) as well, it can be retrieved by calling <code> {@link
     * org.mmbase.storage.search.RelationStep#getNext getNext()} </code> on the relationstep, and
     * cast to {@link Step Step}.
     *
     * @param RelationManager the relation type associated with the step
     * @return The new relationstep.
     * @throws IllegalArgumentException when an invalid argument is supplied.
     * @throws IllegalStateException when there is no previous step.
     */
    RelationStep addRelationStep(RelationManager relationManager);

    /**
     * Also explicitely state the direction of the relation. This can be needed if the
     * RelationManager has two equals sides.
     * @param RelationManager the relation type associated with the step
     * @return The new relationstep.
     * @throws IllegalArgumentException when an invalid argument is supplied.
     * @throws IllegalStateException when there is no previous step.
     */
    RelationStep addRelationStep(RelationManager relationManager, int searchDir);

    /*
     * If you need to add a 'related' NodeManager without specifying an actual RelationManager, then
     * simply use these addRelationStep.
     */

    RelationStep addRelationStep(NodeManager otherManager);

    /**
     * Also explicitely state the direction of the relation. This can be needed if the
     * RelationManager has two equals sides.
     */
    RelationStep addRelationStep(NodeManager otherManager, int searchDir);

    /**
     * Adds a field to a step.
     */
    StepField addField(Step step, Field field);

    /**
     * Creates a StepField object withouth adding it (e.g. needed for aggregated queries).
     */
    StepField createStepField(Step step, Field field);

    StepField createStepField(Step step, String fieldName);


    /**
     * Creates the step field for the given name. For a NodeQuery the arguments is simply the name of the
     * field. For a 'normal' query, it should be prefixed by the (automatic) alias of the Step.
     */

    StepField createStepField(String fieldIdentifer);

    /**
     * Add an aggregated field to a step
     */
    AggregatedField addAggregatedField(Step step, Field field, int aggregationType);
    

    /**
     * Specifies wether the query result must contain only 'distinct' results.
     * @see org.mmbase.storage.search.implementation.BasicSearchQuery#setDistinct
     * @see #isDistinct
     */
    Query setDistinct(boolean distinct);

    /**
     * Limits the query-result to maxNumber records.
     * @see org.mmbase.storage.search.implementation.BasicSearchQuery#setMaxNumber
     * @see #getMaxNumber
     */
    Query setMaxNumber(int maxNumber);

    /**
     * Offsets the query-result with offset records.
     * @see org.mmbase.storage.search.implementation.BasicSearchQuery#setOffset
     * @see #getOffset
     */
    Query setOffset(int offset);


    // Constraints and so on..


    /**
     * Create a contraint (for use with this Query object). The argument is a string, as also can be
     * used as an argument of the 'non-query' getList. This should be considered legacy.
     * @see  Cloud.getList(String startNodes, String nodePath, String fields, String constraints, String orderby, String directions, String searchDir, boolean distinct)
     * @see  NodeManager.getList(String constraints, String orderby, String directions);    
     */
    LegacyConstraint           createConstraint(String s);


    /**
     * Create a contraint (for use with this Query object). The given field must be 'null'.
     */
    FieldNullConstraint         createConstraint(StepField f);

    /**
     * Create a contraint (for use with this Query object). The given field must equal the given
     * value 'v'.
     */
    FieldValueConstraint        createConstraint(StepField f, Object v);

    /**
     * Create a contraint (for use with this Query object). The given field and the given
     * value 'v', combined with given operator must evaluate to true.
     */
    FieldValueConstraint        createConstraint(StepField f, int op, Object v);

    /**
     * Create a contraint (for use with this Query object). The two given fields , combined with
     * given operator must evaluate to true.
     */
    CompareFieldsConstraint     createConstraint(StepField f, int op, StepField  v);

    /**
     * Create a contraint (for use with this Query object). The given field must lie between the
     * two given values.
     */
    FieldValueBetweenConstraint createConstraint(StepField f, Object o1, Object o2);

    /**
     * Create a contraint (for use with this Query object). The given field value must be contained
     * by the given set of values. 
     */
    FieldValueInConstraint      createConstraint(StepField f, SortedSet v);

    /**
     * Changes the given constraint's 'case sensitivity' (if applicable). Default it is true.
     */
    FieldConstraint             setCaseSensitive(FieldConstraint constraint, boolean sensitive);  

    /**
     * Changes the given constraint's 'inverse' (if applicable). Default it is (of course) false.
     */
    Constraint                  setInverse(Constraint c, boolean i);

   /**
     * Combines two Constraints to one new one, using a boolean operator (AND or OR). Every new
     * constraint must be combined with the ones you already have with such a new CompositeConstraint.
     *
     * If the first constraint is a composite constraint (with the same logical operator), then the
     * second one will simply be added.
     */
    CompositeConstraint         createConstraint(Constraint c1, int op, Constraint c2);

    /**
     * The (composite) constraint can actually be set into the query with this method.
     */
    void setConstraint(Constraint c);


    /**
     * Adds an order on a certain field.
     * @see org.mmbase.storage.search.implementation.BasicSearchQuery#addSortOrder
     * @see #getSortOrders
     */
    SortOrder addSortOrder(StepField f, int direction);

    /**
     * Whether this query was used or not. If is was used, then you cannot modify it anymore (would
     * kill caches, and references to 'original query' would get invalid)
     */
    boolean isUsed();

    /**
     * Mark this query 'used'. It has to be copied first, if you want to add things to it.
     */
    boolean markUsed();

    /**
     * Create an (unused) clone
     */
    Object clone();

    /**
     * Creates an unused aggregate clone of this query. If this query is not itself aggregated, all
     * fields are removed (but the contraints on them remain), and you can add aggregated fields
     * then.
     */
    Query aggregatingClone();


}
