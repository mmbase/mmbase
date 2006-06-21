package com.finalist.cmsc.resources.forms;

import org.mmbase.bridge.NodeManager;
import org.mmbase.bridge.NodeQuery;

public class ImageAction extends SearchAction {

    public static final String TITLE_FIELD = "title";
    public static final String DESCRIPTION_FIELD = "description";
    
    protected void addConstraints(SearchForm searchForm, NodeManager nodeManager, QueryStringComposer queryStringComposer, NodeQuery query) {
        ImageForm form = (ImageForm) searchForm;
        addField(nodeManager, queryStringComposer, query, TITLE_FIELD, form.getTitle());
        addField(nodeManager, queryStringComposer, query, DESCRIPTION_FIELD, form.getDescription());
    }

}
