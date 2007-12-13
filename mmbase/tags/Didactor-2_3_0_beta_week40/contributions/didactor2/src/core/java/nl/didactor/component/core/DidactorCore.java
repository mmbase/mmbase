/**
 * Component description interface.
 */
package nl.didactor.component.core;
import nl.didactor.component.Component;
import nl.didactor.builders.*;
import org.mmbase.bridge.Cloud;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.module.core.MMBase;
import java.util.Map;

public class DidactorCore extends Component {
    /**
     * Returns the version of the component
     */
    public String getVersion() {
        return "2.0";
    }

    /**
     * Returns the name of the component
     */
    public String getName() {
        return "core";
    }

    /**
     * Returns an array of components this component depends on.
     * This should always contain at least one component: 'DidactorCore'
     */
    public Component[] dependsOn() {
        return new Component[0];
    }


    public void init() {
        super.init();
        MMBase mmbase = MMBase.getMMBase();
        DidactorRel classrel = (DidactorRel)mmbase.getBuilder("classrel");
        classrel.registerPostInsertComponent(this, 10);
    }

    public boolean postInsert(MMObjectNode node) {
        if (node.getBuilder().getTableName().equals("classrel")) {
            MMBase mmbase = MMBase.getMMBase();
            DidactorRel classrel = (DidactorRel)mmbase.getBuilder("classrel");
            MMObjectNode source = classrel.getSource(node);
            MMObjectNode destination = classrel.getDestination(node);
            // adding a copybook to the relation between people and classes
            if (source.getBuilder().getTableName().equals("classes") && destination.getBuilder().getTableName().equals("people")) {
                return insertCopybook(node, destination);
            }
            if (source.getBuilder().getTableName().equals("people") && destination.getBuilder().getTableName().equals("classes")) {
                return insertCopybook(node, source);
            }
            // if people are directly connected to educations they should also have a copybook
            if (source.getBuilder().getTableName().equals("educations") && destination.getBuilder().getTableName().equals("people")) {
                return insertCopybook(node, destination);
            }
            if (source.getBuilder().getTableName().equals("people") && destination.getBuilder().getTableName().equals("educations")) {
                return insertCopybook(node, source);
            }
        }

        return true;
    }
 
    /**
     * When inserting a new classrel, we need to add a copybook.
     * @param classrel The new object
     */
    private boolean insertCopybook(MMObjectNode classrel, MMObjectNode person) {
        String owner = classrel.getStringValue("owner");

        MMObjectNode copybook = MMBase.getMMBase().getBuilder("copybooks").getNewNode(owner);
        copybook.setValue("name", "copybook for " + person.getGUIIndicator());
        copybook.insert(owner);

        MMObjectNode relnode = MMBase.getMMBase().getInsRel().getNewNode(owner);
        int rnumber = MMBase.getMMBase().getRelDef().getNumberByName("related");
        relnode.setValue("snumber", classrel.getNumber());
        relnode.setValue("dnumber", copybook.getNumber());
        relnode.setValue("rnumber", rnumber);
        relnode.insert(owner);

        return true;
    }
}
