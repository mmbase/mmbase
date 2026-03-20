/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search;

import java.io.Serializable;
import java.util.List;

import org.mmbase.cache.Cacheable;
/**
 * Encapsulates a request for a search of the object cloud.
 * <p>
 * This corresponds to a SELECT query in SQL syntax.
 *
 * @author Rob van Maris
 * @version $Id$
 * @since MMBase-1.7
 */
public interface SearchQuery extends Cacheable, Serializable {
    /**
     * Default maxNumber value, corresponds to no maximum.
     * @see SearchQuery#getMaxNumber
     */
    int DEFAULT_MAX_NUMBER = -1;

    /**
     * Default offset value, corresponds to no offset.
     * @see SearchQuery#getOffset
     */
    int DEFAULT_OFFSET = 0;

    /**
     * Tests if the search request is to return distinct results. In that case duplicate nodes will be removed from the result.
     * <p>
     * This corresponds to the use of "DISTINCT" in SQL SELECT-syntax
     * .
     */
    boolean isDistinct();

    /**
     * Tests if this is an aggregating query, i.e. containing aggregated fields.
     */
    boolean isAggregating();

    /**
     * Gets the steps in the search request.
     * <p>
     * This corresponds to the tables in SQL SELECT-syntax.
     */
    List<Step> getSteps();

    /**
     * Gets the stepfields in the search request.
     * <p>
     * This corresponds to the fields in SQL SELECT-syntax.
     */
    List<StepField> getFields();

    /**
     * Gets the constraints on the search results.
     * <p>
     * This corresponds to (part of) the constraints in the WHERE-clause in SQL SELECT-syntax.
     */
    Constraint getConstraint();

    /**
     * Gets the maximum number of results to be returned, or -1 if the number of results to be returned is unlimited.
     * <p>
     * Note: limiting the number of results may not be supported by the database layer.
     */
    int getMaxNumber();

    /**
     * Gets the (zerobased) offset in the list of results, of the first result to return. Note that,
     * since it is zerobased, it is equal to the number of results that are skipped.<p> Note:
     * skipping results may not be supported by the database layer.
     */
    int getOffset();

    /**
     * Gets the SortOrder objects in the order they are to be applied.
     *  This specifies the sorting order of the search results.
     * <p>
     * This corresponds to the ORDER BY clause in SQL SELECT syntax.
     */
    List<SortOrder> getSortOrders();

    /**
     * Compares this query to the specified object. The result is
     * <code>true</code> if and only if the argument is a non-null
     * SearchQuery object representing the same query.
     *
     * @param obj The object to compare with.
     * @return <code>true</code> if the objects are equal,
     * <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj);

    // javadoc is inherited
    @Override
    public int hashCode();

    /**
     * Returns a string representation of this SearchQuery.
     * The string representation has the form
     * "SearchQuery(distinct:&lt;distinct&gt;,
     *  steps:&lt;steps&gt;, fields:&lt;fields&gt;,
     *  constraint:&lt;constraint&gt;, sortorders:&lt;sortorders&gt;,
     *  max:&lt;max&gt;, offset:&lt;offset&gt;)"
     * where
     * <ul>
     * <li><em>&lt;distinct&gt;</em> is value returned by
     *     {@link #isDistinct isDistinct()}
     * <li><em>&lt;steps&gt;</em> is the list returned by
     *     {@link #getSteps getSteps()}
     * <li><em>&lt;fields&gt;</em> is the list returned by
     *     {@link #getFields getFields()}
     * <li><em>&lt;constraint&gt;</em> is the constraint returned by
     *     {@link #getConstraint getConstraint()}
     * <li><em>&lt;sortorders&gt;</em> is the list returned by
     *     {@link #getSortOrders getSortOrders()}
     * <li><em>&lt;max&gt;</em> is value returned by
     *     {@link #getMaxNumber getMaxNumber()}
     * <li><em>&lt;offset&gt;</em> is value returned by
     *     {@link #getOffset getOffset()}
     * </ul>
     *
     * @return A string representation of this SearchQuery.
     */
    @Override
    public String toString();


    /**
     * @since MMBase-1.9.2
     */
    boolean markUsed();




}
