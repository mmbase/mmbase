/*
 * Created on 6-sep-2005
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative. The
 * license (Mozilla version 1.0) can be read at the MMBase site. See
 * http://www.MMBase.org/license
 */
package org.mmbase.core.event;

import java.io.Serializable;

import org.mmbase.module.core.MMBase;

/**
 * This class is the base class for all mmbase events
 * 
 * @author  Ernst Bunders
 * @since   MMBase-1.8
 * @version $Id: Event.java,v 1.8 2006-07-06 09:52:30 michiel Exp $
 */
public abstract class Event implements Serializable, org.mmbase.util.PublicCloneable {


    public static final int TYPE_UNSPECIFIED = -1;
    public static final int TYPE_NEW      = 0;
    public static final int TYPE_CHANGE   = 1;
    public static final int TYPE_DELETE   = 2;

    protected int    eventType = TYPE_UNSPECIFIED;
    protected String machine;

    /**
     * Every event originates from a certain machine, which is identified by a String.
     * If this equals {@link MMBase#getMachineName()} then this is a local event.
     */
    public String getMachine() {
        return machine;
    }
    public boolean isLocal() {
        return MMBase.getMMBase().getMachineName().equals(machine);
    }


    /**
     * Most events will come in certain 'types', default contants which are provided are {@link
     * #TYPE_NEW}, {@link #TYPE_CHANGE} and {@link #TYPE_DELETE}.
     */
    public int getType() {
        return eventType;
    }

    /**
     * @param machine The machine name. If <code>null</code> the local machine name is extracted from MMBase, using
     *                {@link MMBase#getMachineName()}
     */
    public Event(String machine, int type) {
        this.machine =  machine == null ?
            MMBase.getMMBase().getMachineName() :
            machine;
        this.eventType    = type;
    }

    public Event(String machine) {
        this(machine, TYPE_UNSPECIFIED);
    }

    public Object clone(){
        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException e) {}
        return clone;
    }

}
