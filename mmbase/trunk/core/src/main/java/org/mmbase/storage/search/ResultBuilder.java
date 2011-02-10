/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search;

import java.util.List;

import org.mmbase.bridge.Field;
import org.mmbase.cache.AggregatedResultCache;
import org.mmbase.cache.Cache;
import org.mmbase.module.core.*;
import org.mmbase.core.CoreField;
import org.mmbase.storage.StorageException;

/**
 * A <code>ResultBuilder</code> is a builder for
 * {@link ResultNode ResultNodes}, that represent the results of executing
 * an arbitrary search query.
 * <p>
 * This builder contains info on the fields of the resultnodes.
 *
 * @author  Rob van Maris
 * @version $Id$
 * @since MMBase-1.7
 */
public class ResultBuilder extends VirtualBuilder {

    private final SearchQuery query;
    protected static final Cache<String, CoreField> fieldCache = new Cache<String, CoreField>(200) {
        @Override
        public String getName() {
            return "ResultFieldCache";
        }
        @Override
        public String getDescription() {
            return "Caches the field objects which are needed to present aggregated results";
        }
    };
    static {
        fieldCache.putCache();
    }


    /**
     * Creator.
     * Creates new <code>ResultBuilder</code> instance, used to represent
     * the results of executing a search query.
     *
     * @param mmbase MMBase instance.
     * @param query The search query that defines the search.
     */
    public ResultBuilder(MMBase mmbase, SearchQuery q) {
        super(mmbase);

        // The ResultBuilder will be back referenced by nodes in the result of an aggregate query.
        // And this result may be cached.
        // We don't want to cache 'Wrapped' queries (e.g. bridge.BasicQuery, because they reference a Cloud).
        while (q instanceof SearchQueryWrapper) {
            q = ((SearchQueryWrapper)q).unwrap();
        }
        this.query = q;

        // Create fieldsByAlias map.
        for (StepField field : query.getFields()) {
            String fieldAlias = field.getAlias();
            if (fieldAlias == null) {
                fieldAlias = field.getFieldName();
            }
            String key = fieldAlias + "-" + field.getType();
            CoreField coreField = fieldCache.get(key);
            if (coreField == null) {
                coreField = org.mmbase.core.util.Fields.createField(fieldAlias, field.getType(), -1, Field.STATE_VIRTUAL, null);
                fieldCache.put(key, coreField);
            }
            fields.put(fieldAlias, coreField);
        }
    }

    /**
     * @see org.mmbase.module.core.VirtualBuilder#getNewNode(java.lang.String)
     */
    @Override
    public MMObjectNode getNewNode(String owner) {
        return new ResultNode(this);
    }

    public List<MMObjectNode> getResult() throws StorageException, SearchQueryException {
        AggregatedResultCache cache = AggregatedResultCache.getCache();

        List<MMObjectNode> resultList = cache.get(query);
        if (resultList == null) {
            resultList = this.mmb.getSearchQueryHandler().getNodes(query, this);
            cache.put(query, resultList);
        }
        return resultList;
    }

}
