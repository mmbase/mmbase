package nl.didactor.builders;
import nl.didactor.component.Component;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import java.util.SortedSet;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * This class provides a default framework for Didactor builders, where components
 * can register themselves for events on the builders.
 * @author Johannes Verelst &lt;johannes.verelst@eo.nl&gt;
 */
public class DidactorBuilder extends MMObjectBuilder {
    private org.mmbase.util.Encode encoder = null;
    private static Logger log = Logging.getLoggerInstance(DidactorBuilder.class.getName());

    private SortedSet preInsertComponents = new TreeSet();
    private SortedSet postInsertComponents = new TreeSet();

    private SortedSet preCommitComponents = new TreeSet();
    private SortedSet postCommitComponents = new TreeSet();

    public boolean init() {
        return super.init();
    }

    public void registerPreInsertComponent(Component c, int priority) {
        EventInstance event = new EventInstance(c, priority);
        preInsertComponents.add(event);
        log.info("Added listener on " + getTableName() + ".preInsert(): '" + c.getName() + "' with priority " + priority);
    }

    public void registerPostInsertComponent(Component c, int priority) {
        EventInstance event = new EventInstance(c, priority);
        postInsertComponents.add(event);
        log.info("Added listener on " + getTableName() + ".postInsert(): '" + c.getName() + "' with priority " + priority);
    }

    public void registerPreCommitComponent(Component c, int priority) {
        EventInstance event = new EventInstance(c, priority);
        preCommitComponents.add(event);
        log.info("Added listener on " + getTableName() + ".preCommit(): '" + c.getName() + "' with priority " + priority);
    }

    public void registerPostCommitComponent(Component c, int priority) {
        EventInstance event = new EventInstance(c, priority);
        postCommitComponents.add(event);
        log.info("Added listener on " + getTableName() + ".postCommit(): '" + c.getName() + "' with priority " + priority);
    }

    /**
     * Overridden 'insert' from MMObjectBuilder. It will call the 'preInsert()' 
     * method for all registered components just before inserting the node. It
     * calls the 'postInsert()' for all registered components after inserting the node.
     */
    public int insert(String owner, MMObjectNode node) {
        Iterator i = preInsertComponents.iterator();
        while (i.hasNext()) {
            Component c = ((EventInstance)i.next()).component;
            log.info("Firing " + c.getName() + ".preInsert() on object of type '" + node.getBuilder().getTableName() + "'");
            c.preInsert(node);
        }
        int res = super.insert(owner, node);
        i = postInsertComponents.iterator();
        while (i.hasNext()) {
            Component c = ((EventInstance)i.next()).component;
            log.info("Firing " + c.getName() + ".postInsert() on object of type '" + node.getBuilder().getTableName() + "'");
            c.postInsert(node);
        }
        return res;
    }


    /**
     * Overridden 'preCommit' from MMObjectBuilder. It will call the 'preCommit()' 
     * method for all registered components.
     */
    public MMObjectNode preCommit(MMObjectNode node) {
        Iterator i = preCommitComponents.iterator();
        while (i.hasNext()) {
            Component c = ((EventInstance)i.next()).component;
            log.info("Firing " + c.getName() + ".preCommit() on object of type '" + node.getBuilder().getTableName() + "'");
            c.preCommit(node);
        }
        super.preCommit(node);
        return node;
    }

    /**
     * Overridden 'commit' from MMObjectBuilder. It will call the 'postCommit()' .
     * method for all registered components
     */
    public boolean commit(MMObjectNode node) {
        boolean bSuperCommit = super.commit(node);
        Iterator i = postCommitComponents.iterator();
        while (i.hasNext()) {
            Component c = ((EventInstance)i.next()).component;
            log.info("Firing " + c.getName() + ".postCommit() on object of type '" + node.getBuilder().getTableName() + "'");
            c.postCommit(node);
        }
        return bSuperCommit;
    }

    /**
     * This small innerclass represents an event-instance. It mainly is just a wrapper
     * around the component, but also has a priority that allows the components to be
     * sorted when they are fired.
     */
    private class EventInstance implements Comparable {
        protected Component component;
        protected int priority;
      
        /**
         * Public constructor
         */
        public EventInstance(Component component, int priority) {
            this.component = component;
            this.priority = priority;
        }

        /**
         * @returns a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
         */
        public int compareTo(Object o) {
            if (o instanceof EventInstance) {
                EventInstance other = (EventInstance)o;
                if (this.priority == other.priority) {
                    return this.component.getName().compareTo(other.component.getName());
                }
                return this.priority - other.priority;
            }
            return -1;
        }
    }
}
