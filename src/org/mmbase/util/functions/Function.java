/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.functions;

import java.util.*;


/**
 * A representation of a piece of functionality (a 'function'). A function has a name, a
 * return type, and a parameter-definition (which is a {@link Parameter} array).
 *
 * The goal of a Function object is to call its {@link #getFunctionValue(Parameters)} method, which
 * executes it, given the specified parameters.
 *
 * @author Pierre van Rooden
 * @author Daniel Ockeloen
 * @author Michiel Meeuwissen
 * @version $Id: Function.java,v 1.4 2004-12-06 15:25:19 pierre Exp $
 * @since MMBase-1.7
 * @see Parameter
 * @see Parameters
 */
public interface Function {
    /**
     * Creates an empty 'Parameters'  object for you, which you have to fill and feed back to getFunctionValue
     * @see #getFunctionValue(Parameters)
     */
    public Parameters createParameters();

    /**
     * Executes the defined function supplying the given arguments.
     * @see #createParameters
     * @param parameters The parameters for the function. To specify an empty parameter list use {@link Parameters#VOID}.
     *                   Implementors are encouraged to support <code>null</code> too.
     * @return The function value, which can be of any type compatible to {@link #getReturnType}
     */
    public Object getFunctionValue(Parameters parameters);

    /**
     * Executes the defined function supplying the given List of arguments.
     * This is a convenience method, as the List is mapped to a Parameters type and passed to {@link  #getFunctionValue(Parameters)}.
     * @param parameters The parameters for the function. To specify an empty parameter list use {@link Parameters#VOID}.
     *
     * @return The function value, which can be of any type compatible to {@link #getReturnType}
     */
    public Object getFunctionValueWithList(List parameters);

    /**
     * For documentational  purposes a function object needs a description too.
     */
    public void setDescription(String description);

    /**
     * @see #setDescription(String)
     */
    public String getDescription();

    /**
     * A function <em>must</em> have a name. This is the name which was used to aquire the function object.
     * @return The function's name, never <code>null</code>
     */
    public String getName();

    /**
     * @return The currently set Parameter definition array, or <code>null</code> if not set already.
     */
    public DataType[] getParameterDefinition();

    /**
     * A function object is of no use, as long as it lacks a definition.
     * @param params An array of Parameter objects.
     * @throws IllegalStateException if there was already set a parameter definition for this function object.
     */
    public void setParameterDefinition(DataType[] params);

    /**
     * @return The DataType of the function's result value, or <code>null</code> if unknown.
     */
    public DataType getReturnType();

    /**
     * Sets the DataType of the function's result value.
     * @param type A DataType object. For void functions that could be {@link ReturnType#VOID}.
     * @throws IllegalStateException if there was already set a return type for this function object.
     */
    public void setReturnType(DataType type);

}
