/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib.containers;

import org.mmbase.bridge.jsp.taglib.util.Attribute;
import org.mmbase.bridge.jsp.taglib.*;

import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;

import java.util.*;
import javax.servlet.jsp.JspTagException;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.7
 * @version $Id: NodeListNextBatchesTag.java,v 1.3 2003-08-08 09:22:55 michiel Exp $
 */
public class NodeListNextBatchesTag extends StringListTag implements NodeListContainerReferrer {
    private static Logger log = Logging.getLoggerInstance(NodeListNextBatchesTag.class);

    protected Attribute container  = Attribute.NULL;

    protected int indexOffSet = 0;

    public void setContainer(String c) throws JspTagException {
        container = getAttribute(c);
    }

    public int getIndexOffset() {
        return indexOffSet;
    }


    protected List getList() throws JspTagException {
        NodeListContainer c = (NodeListContainer) findParentTag(NodeListContainer.class, (String) container.getValue(this));
        Query query = c.getQuery();
        int offset = query.getOffset();
        int maxNumber = query.getMaxNumber();
        if (maxNumber == Query.DEFAULT_MAX_NUMBER) {
            throw new JspTagException("No max-number set. Cannot batch results (use mm:maxnumber first)");
        }
        if (offset % maxNumber != 0) { // be paranoid, perhaps not necessary, but guarantees less queries in case of url-hacking (if 'offset' is used on url)
            throw new JspTagException("Offset (" + offset + ") is not a multipible of max-number (" + maxNumber + "): Cannot batch results.");
        }
        Query count = query.aggregatingClone();
        
        Cloud cloud = getCloud();

        Step step = (Step) (count.getSteps().get(0));
        count.addAggregatedField(step, cloud.getNodeManager(step.getTableName()).getField("number"), AggregatedField.AGGREGATION_TYPE_COUNT);
        Node result = (Node) cloud.getList(count).get(0);
        int totalSize = result.getIntValue("number");

        indexOffSet = offset / maxNumber + 1;
        List resultList = new ArrayList();
        int maxSize = getMaxNumber();

        while (offset + maxNumber < totalSize) {
            offset += maxNumber;
            resultList.add("" + offset);
            if (maxSize > 0 && resultList.size() == maxSize) break;
        }
        return resultList;
    }

}
