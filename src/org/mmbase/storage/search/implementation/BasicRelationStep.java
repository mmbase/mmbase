/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search.implementation;

import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.storage.search.*;

/**
 * Basic implementation.
 *
 * @author Rob van Maris
 * @version $Revision: 1.1 $
 * @since MMBase-1.7
 */
public class BasicRelationStep extends BasicStep implements RelationStep {
    
    /** Directionality property. */
    private int directionality = RelationStep.DIRECTIONS_BOTH;
    
    /** Previous step. */
    private Step previous = null;
    
    /** Next step. */
    private Step next = null;
    
    /**
     * Creator.
     *
     * @param builder The relation builder.
     * @param previous The previous step.
     * @param next The next step.
     * @throws IllegalArgumentException when an invalid argument is supplied.
     */
    // package visibility!
    BasicRelationStep(InsRel builder, 
        Step previous, Step next) {
        super(builder);
        if (previous == null) {
            throw new IllegalArgumentException(
            "Invalid previous value: " + previous);
        }
        this.previous = previous;
        if (next == null) {
            throw new IllegalArgumentException(
            "Invalid next value: " + next);
        }
        this.next = next;
    }
    
    /**
     * Sets directionality property.
     * 
     * @param directionality The directionality.
     * Must be one of the values defined in <code>
     * {@link org.mmbase.storage.search.RelationStep RelationStep}.</code>
     * @throws IllegalArgumentException when an invalid argument is supplied.
     */
    public void setDirectionality(int directionality) {
        if (directionality != RelationStep.DIRECTIONS_SOURCE
        && directionality != RelationStep.DIRECTIONS_DESTINATION
        && directionality != RelationStep.DIRECTIONS_BOTH) {
            throw new IllegalArgumentException(
            "Invalid directionality value: " + directionality);
        }
        this.directionality = directionality;
    }
    
    // javadoc is inherited
    public int getDirectionality() {
        return directionality;
    }

    // javadoc is inherited
    public Step getPrevious() {
        return previous;
    }

    // javadoc is inherited
    public Step getNext() {
        return next;
    }

    // javadoc is inherited
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof RelationStep) {
            RelationStep step = (RelationStep) obj;
            return getTableName().equals(step.getTableName())
                && getAlias().equals(step.getAlias())
                && getNodes().equals(step.getNodes())
                && step.getDirectionality() == directionality;
        } else {
            return false;
        }
    }
    
    // javadoc is inherited
    public int hashCode() {
        return 41 * getTableName().hashCode()
        + 43 * getAlias().hashCode() 
        + 47 * getNodes().hashCode()
        + 113 * directionality;
    }
    
    // javadoc is inherited
    public String toString() {
        StringBuffer sb = new StringBuffer("RelationStep(tablename:");
        sb.append(getTableName()).
        append(", alias:").
        append(getAlias()).
        append(", nodes:").
        append(getNodes()).
        append(", dir:").
        append(RelationStep.DIRECTIONALITY_NAMES[getDirectionality()]).
        append(")");
        return sb.toString();
    }
    
}
