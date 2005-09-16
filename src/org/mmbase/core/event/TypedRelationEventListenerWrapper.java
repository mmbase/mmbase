/*
 * Created on 9-sep-2005
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative. The
 * license (Mozilla version 1.0) can be read at the MMBase site. See
 * http://www.MMBase.org/license
 */
package org.mmbase.core.event;

import java.util.Properties;

/**
 * This class is a wrapper for relation event listeners that only want to listen
 * to events from a specific builder.
 * 
 * @author Ernst Bunders
 * @since MMBase-1.8
 * 
 */
public class TypedRelationEventListenerWrapper implements RelationEventListener {
    private String nodeType;

    private RelationEventListener wrappedListener;

    /**
     * @param nodeType should be a valid builder name
     * @param wrappedListener the relation event listener you want to wrap
     */
    public TypedRelationEventListenerWrapper(String nodeType,
            RelationEventListener wrappedListener) {
        this.nodeType = nodeType;
        this.wrappedListener = wrappedListener;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mmbase.core.event.RelationEventListener#fire(org.mmbase.core.event.RelationEvent)
     */
    public void notify(RelationEvent event) {
        wrappedListener.notify(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mmbase.core.event.EventListener#getConstraintsForEvent(org.mmbase.core.event.Event)
     */
    public Properties getConstraintsForEvent(Event event) {
        Properties p = new Properties();
        p.setProperty(RelationEventBroker.PROPERTY_NODETYPE, nodeType);
        return p;
    }

    public String toString() {
        return "TypedRelationEventListenerWrapper(" + wrappedListener + ")";
    }

}
