/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search.implementation;

import java.util.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.StringSearchConstraint;

/**
 * Basic implementation.
 *
 * @author Rob van Maris
 * @version $Revision: 1.4 $
 * @since MMBase-1.7
 */
public class BasicStringSearchConstraint extends BasicFieldConstraint implements StringSearchConstraint {
    
    /** The search type. */
    private int searchType = 0;
    
    /** The match type. */
    private int matchType = 0;
    
    /** Map storing additional parameters. */
    private Map parameters = new HashMap(3);
    
    /** List of searchterms. */
    private List searchTerms = null;
    
    /** 
     * Creates a new instance of BasicStringSearchConstraint.
     *
     * @param field The associated field.
     * @param searchType The search type.
     * @param matchType The match type.
     * @param searchTerms the searchterms
     * @throws IllegalArgumentValue when an invalid argument is supplied.
     * @see #getSearchType
     * @see #getMatchType
     */
    public BasicStringSearchConstraint(StepField field, int searchType, 
    int matchType, List searchTerms) {
        this(field, searchType, matchType);
        setSearchTerms(searchTerms);
    }
        
    /** 
     * Creates a new instance of BasicStringSearchConstraint.
     *
     * @param field The associated field.
     * @param searchType The search type.
     * @param matchType The match type.
     * @param searchTerms String containing searchterms as words separated 
     *        by white space.
     * @throws IllegalArgumentValue when an invalid argument is supplied.
     * @see #getSearchType
     * @see #getMatchType
     */
    public BasicStringSearchConstraint(StepField field, int searchType, 
    int matchType, String searchTerms) {
        this(field, searchType, matchType);
        setSearchTerms(searchTerms);
    }
    
    /** 
     * Creates a new instance of BasicStringSearchConstraint.
     * Private, is to be called from all other creators. 
     *
     * @param field The associated field.
     * @param searchType The search type.
     * @param matchType The match type.
     * @throws IllegalArgumentValue when an invalid argument is supplied.
     * @see #getSearchType
     * @see #getMatchType
     */
    private BasicStringSearchConstraint(StepField field, int searchType, 
    int matchType) {
        super(field);
        if (field.getType() != FieldDefs.TYPE_STRING
        && field.getType() != FieldDefs.TYPE_XML) {
            throw new IllegalArgumentException(
            "StringSearchConstraint not allowed for this field type: "
            + getField().getType());
        }
        setSearchType(searchType);
        setMatchType(matchType);
    }
    
    /**
     * Sets the match type. 
     *
     * @param matchType The matchtype.
     * @return This <code>BasicStringSearchConstraint</code> instance.
     * @throws IllegalArgumentValue when an invalid argument is supplied.
     * @see #getMatchType
     */
    public BasicStringSearchConstraint setMatchType(int matchType) {
        if (matchType != StringSearchConstraint.MATCH_TYPE_LITERAL
        && matchType != StringSearchConstraint.MATCH_TYPE_FUZZY
        && matchType != StringSearchConstraint.MATCH_TYPE_SYNONYM) {
            throw new IllegalArgumentException(
            "Invalid match type value: " + matchType);
        }
        this.matchType = matchType;
        if (matchType != StringSearchConstraint.MATCH_TYPE_FUZZY) {
            parameters.remove(StringSearchConstraint.PARAM_FUZZINESS);
        }
        return this;
    }
    
    /**
     * Sets the search type.
     *
     * @param searchType The searchType.
     * @return This <code>BasicStringSearchConstraint</code> instance.
     * @throws IllegalArgumentValue when an invalid argument is supplied.
     * @see #getSearchType
     */
    public BasicStringSearchConstraint setSearchType(int searchType) {
        if (searchType != StringSearchConstraint.SEARCH_TYPE_WORD_ORIENTED
        && searchType != StringSearchConstraint.SEARCH_TYPE_PHRASE_ORIENTED
        && searchType != StringSearchConstraint.SEARCH_TYPE_PROXIMITY_ORIENTED) {
            throw new IllegalArgumentException(
            "Invalid search type value: " + searchType);
        }
        this.searchType = searchType;
        if (searchType != StringSearchConstraint.SEARCH_TYPE_PROXIMITY_ORIENTED) {
            parameters.remove(StringSearchConstraint.PARAM_PROXIMITY_LIMIT);
        }
        return this;
    }
    
    /**
     * Adds searchterm to list of searchterms.
     *
     * @param searchTerm the searchterms
     * @return This <code>BasicStringSearchConstraint</code> instance.
     * @throws IllegalArgumentException when an invalid argument is supplied.
     */
    public BasicStringSearchConstraint addSearchTerm(String searchTerm) {
        if (searchTerm.trim().length() == 0) {
            throw new IllegalArgumentException(
            "Invalid search term value: \"" + searchTerm + "\"");
        }
        searchTerms.add(searchTerm);
        return this;
    }
    
    /**
     * Sets searchterms to elements in specified list.
     *
     * @param searchTerms the searchterms
     * @return This <code>BasicStringSearchConstraint</code> instance.
     * @throws IllegalArgumentException when an invalid argument is supplied.
     */
    public BasicStringSearchConstraint setSearchTerms(List searchTerms) {
        if (searchTerms.size() == 0) {
            throw new IllegalArgumentException(
            "Invalid search terms value: " + searchTerms);
        }
        List newSearchTerms = new ArrayList();
        Iterator iSearchTerms = searchTerms.iterator();
        while (iSearchTerms.hasNext()) {
            Object searchTerm = iSearchTerms.next();
            if (!(searchTerm instanceof String)) {
                throw new IllegalArgumentException(
                "Invalid search term value: " + searchTerm);
            }
            newSearchTerms.add(searchTerm);
        }
        this.searchTerms = newSearchTerms;
        return this;
    }
    
    /**
     * Sets searchterms to searchterms in string.
     *
     * @param searchTerms String containing searchterms as words separated 
     *        by white space.
     * @return This <code>BasicStringSearchConstraint</code> instance.
     * @throws IllegalArgumentException when an invalid argument is supplied.
     */
    public BasicStringSearchConstraint setSearchTerms(String searchTerms) {
        if (searchTerms.trim().length() == 0) {
            throw new IllegalArgumentException(
            "Invalid search terms value: \"" + searchTerms + "\"");
        }
        List newSearchTerms = new ArrayList();
        StringTokenizer st = new StringTokenizer(searchTerms);
        while (st.hasMoreTokens()) {
            newSearchTerms.add(st.nextToken());
        }
        this.searchTerms = newSearchTerms;
        return this;
    }
    
    /**
     * Sets parameter. Ignored if parameter is not relavant to the present
     * search- and matchtype.
     *
     * @param name The parameter name.
     * @param value The parameter value.
     * @return This <code>BasicStringSearchConstraint</code> instance.
     * @throws IllegalArgumentValue when an invalid argument is supplied.
     * @see #getParameters
     */
    public BasicStringSearchConstraint setParameter(String name, Object value) {
        if (name.equals(StringSearchConstraint.PARAM_FUZZINESS)
        && matchType == StringSearchConstraint.MATCH_TYPE_FUZZY) {
            if (!(value instanceof Float)) {
                throw new IllegalArgumentException(
                "Invalid type for parameter \"" + name + "\": " 
                + value.getClass().getName());
            }
            float floatValue = ((Float) value).floatValue();
            if (floatValue < 0 || floatValue > 1) {
                throw new IllegalArgumentException(
                "Invalid fuzziness value: " + floatValue);
            }
        } else if (name.equals(StringSearchConstraint.PARAM_PROXIMITY_LIMIT)
        && searchType == StringSearchConstraint.SEARCH_TYPE_PROXIMITY_ORIENTED) {
            if (!(value instanceof Integer)) {
                throw new IllegalArgumentException(
                "Invalid type for parameter \"" + name + "\": " 
                + value.getClass().getName());
            }
            int intValue = ((Integer) value).intValue();
            if (intValue < 1) {
                throw new IllegalArgumentException(
                "Invalid proximity limit value: " + intValue);
            }
        } else {
            throw new IllegalArgumentException(
            "Invalid parameter name: \"" + name + "\"");
        }
        parameters.put(name, value);
        return this;
    }

    // javadoc is inherited
    public Map getParameters() {
        return Collections.unmodifiableMap(parameters);
    }
    
    // javadoc is inherited
    public int getMatchType() {
        return matchType;
    }
    
    // javadoc is inherited
    public int getSearchType() {
        return searchType;
    }
    
    // javadoc is inherited
    public List getSearchTerms() {
        return Collections.unmodifiableList(searchTerms);
    }
    
    // javadoc is inherited
    public int getBasicSupportLevel() {
        // no basic support
        return SearchQueryHandler.SUPPORT_NONE;
    }
    
    // javadoc is inherited
    public boolean equals(Object obj) {
        // Must be same class (subclasses should override this)!
        if (obj != null && obj.getClass() == getClass()) {
            BasicStringSearchConstraint constraint = (BasicStringSearchConstraint) obj;
            return isInverse() == constraint.isInverse()
                && isCaseSensitive() == constraint.isCaseSensitive()
                && getField().getFieldName().equals(constraint.getField().getFieldName())
                && getField().getStep().getAlias().equals(
                    constraint.getField().getStep().getAlias())
                && searchType == constraint.getSearchType()
                && matchType == constraint.getMatchType()
                && parameters.equals(constraint.parameters)
                && searchTerms.equals(constraint.searchTerms);
        } else {
            return false;
        }
    }
    
    // javadoc is inherited
    public int hashCode() {
        return super.hashCode()
        + 117 * searchType
        + 127 * matchType
        + 131 * parameters.hashCode()
        + 137 + searchTerms.hashCode();
    }
    
    // javadoc is inherited
    public String toString() {
        return
        "StringSearchConstraint(inverse:" + isInverse()
        + ", field:" + getField()
        + ", casesensitive:" + isCaseSensitive()
        + ", searchtype:" + searchType
        + ", matchtype:" + matchType
        + ", parameters:" + parameters
        + ", searchterms:" + searchTerms + ")";
    }
}
