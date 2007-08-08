    /**
     * Render een node van de tree.
     * 
     * @see com.finalist.tree.TreeCellRenderer#getElement(TreeModel, Object, String)
     */
    public TreeElement getElement(TreeModel model, Object node, String id) {
        Node parentNode = (Node) node;
        if (id == null) {
            id = String.valueOf(parentNode.getNumber());
        }
        
        UserRole role = null;
        boolean isPage = false;
        boolean isSite = false;
        boolean isRssFeed = false;
        boolean secure = false;
        String name = parentNode.getStringValue("title");
        String fragmentFieldName = null;
        Node parentParentNode = null;
        if(ModuleUtil.checkFeature(FEATURE_RSSFEED) && RssFeedUtil.isRssFeedType(parentNode)) {
        	isRssFeed = true;
        	parentParentNode = parentNode.getRelatedNodes("page").getNode(0);
        	role = NavigationUtil.getRole(parentNode.getCloud(), parentParentNode, false);
        	fragmentFieldName = RssFeedUtil.FRAGMENT_FIELD;
        }
        else {
        	secure = parentNode.getBooleanValue(PagesUtil.SECURE_FIELD);
        	role = NavigationUtil.getRole(parentNode.getCloud(), parentNode, false);
        	isPage = PagesUtil.isPage(parentNode);
        	isSite = !isPage;
        
        	fragmentFieldName = isPage ? PagesUtil.TITLE_FIELD : SiteUtil.TITLE_FIELD;
        }
        String fragment = parentNode.getStringValue( fragmentFieldName);
        
        
        String action = null;
        if (ServerUtil.useServerName()) {
            String[] pathElements = NavigationUtil.getPathElementsToRoot(parentNode, true);

            action = HttpUtil.getWebappUri(request, pathElements[0], secure);
            for (int i = 1; i < pathElements.length; i++) {
                action += pathElements[i] + "/";
            }
            if (!request.getServerName().equals(pathElements[0])) {
                action = HttpUtil.addSessionId(request, action);
            }
            else {
                action = response.encodeURL(action);
            }
        }
        else {
            String path = null;

           	path = NavigationUtil.getPathToRootString(parentNode, true); 
            String webappuri = HttpUtil.getWebappUri(request, secure);
            action = response.encodeURL(webappuri + path);
        }
        
        TreeElement element = createElement(getIcon(node, role), id, name, fragment, action, target);
        
        if (role != null && SecurityUtil.isWriter(role)) {
            if (isPage) {
                if (SecurityUtil.isEditor(role)) {
                    String labelPageEdit = JstlUtil.getMessage(request, "site.page.edit");
                    element.addOption(createOption("edit_defaults.png", labelPageEdit,
                        getUrl("PageEdit.do?number=" + parentNode.getNumber()), target));
                    
                    if ((model.getChildCount(parentNode) == 0) || SecurityUtil.isWebmaster(role)) {
                        String labelPageRemove = JstlUtil.getMessage(request, "site.page.remove");
                        element.addOption(createOption("delete.png", labelPageRemove,
                                getUrl("PageDelete.do?number=" + parentNode.getNumber()), target));
                    }
                }
            }
            if (isSite) {
                if (SecurityUtil.isChiefEditor(role)) {
                    String labelSiteEdit = JstlUtil.getMessage(request, "site.site.edit");
                    element.addOption(createOption("edit_defaults.png", labelSiteEdit,
                            getUrl("SiteEdit.do?number=" + parentNode.getNumber()), target));

                    if ((model.getChildCount(parentNode) == 0) || SecurityUtil.isWebmaster(role)) {
                        String labelSiteRemove = JstlUtil.getMessage(request, "site.site.remove");
                        element.addOption(createOption("delete.png", labelSiteRemove,
                            getUrl("SiteDelete.do?number=" + parentNode.getNumber()), target));
                    }
                }
            }
            if (isRssFeed) {
                if (SecurityUtil.isEditor(role)) {
                    String labelSiteEdit = JstlUtil.getMessage(request, "site.rss.edit");
                    element.addOption(createOption("edit_defaults.png", labelSiteEdit,
                            getUrl("../rssfeed/RssFeedEdit.do?number=" + parentNode.getNumber()), target));

                    if ((model.getChildCount(parentNode) == 0) || SecurityUtil.isWebmaster(role)) {
                        String labelSiteRemove = JstlUtil.getMessage(request, "site.rss.remove");
                        element.addOption(createOption("delete.png", labelSiteRemove,
                            getUrl("../rssfeed/RssFeedDelete.do?number=" + parentNode.getNumber()), target));
                    }
                }
            }
            if (SecurityUtil.isEditor(role)) {
            	if (isPage || isSite) {
	                String labelPageNew = JstlUtil.getMessage(request, "site.page.new");
	                element.addOption(createOption("new.png", labelPageNew,
	                        getUrl("PageCreate.do?parentpage=" + parentNode.getNumber()), target));
	
	                if(ModuleUtil.checkFeature(FEATURE_RSSFEED)) {
	                   String labelRssNew = JstlUtil.getMessage(request, "site.rss.new");
	                   element.addOption(createOption("rss_new.png", labelRssNew,
	                         getUrl("../rssfeed/RssFeedCreate.do?parentpage=" + parentNode.getNumber()), target));
	                }
	                
	                if (NavigationUtil.getChildCount(parentNode) >= 2) {
	                    String labelPageReorder = JstlUtil.getMessage(request, "site.page.reorder");
	                    element.addOption(createOption("reorder.png", labelPageReorder, 
	                            getUrl("reorder.jsp?parent=" + parentNode.getNumber()), target));
	                }

	                if(ModuleUtil.checkFeature(FEATURE_PAGEWIZARD)) {
				        String labelPageWizard = JstlUtil.getMessage(request, "site.page.wizard");
				        element.addOption(createOption("wizard.png", labelPageWizard,
				            getUrl("../pagewizard/StartPageWizardAction.do?number=" + parentNode.getNumber()), target));
			        }
            	}
                
                if (SecurityUtil.isChiefEditor(role)) {
                    if (isPage || isRssFeed) {
                        String labelPageCut = JstlUtil.getMessage(request, "site.page.cut");
                        element.addOption(createOption("cut.png", labelPageCut, "javascript:cut('"
                                + parentNode.getNumber() + "');", null));
                        String labelPageCopy = JstlUtil.getMessage(request, "site.page.copy");
                        element.addOption(createOption("copy.png", labelPageCopy, "javascript:copy('"
                                + parentNode.getNumber() + "');", null));
                    }
                    if (isPage || isSite) {
	                    String labelPagePaste = JstlUtil.getMessage(request, "site.page.paste");
	                    element.addOption(createOption("paste.png", labelPagePaste, "javascript:paste('"
	                            + parentNode.getNumber() + "');", null));
                    }
                }
            }

            if (SecurityUtil.isWebmaster(role)) {
		        if(ModuleUtil.checkFeature(FEATURE_WORKFLOW)) {
                   String labelPublish = JstlUtil.getMessage(request, "site.page.publish");
                   element.addOption(createOption("publish.png", labelPublish,
                       getUrl("../workflow/publish.jsp?number=" + parentNode.getNumber()), target));
                   if (isPage || isSite) {
                	   String labelMassPublish = JstlUtil.getMessage(request, "site.page.masspublish");
                	   element.addOption(createOption("masspublish.png", labelMassPublish,
                			   getUrl("../workflow/masspublish.jsp?number=" + parentNode.getNumber()), target));
                   }
		        }
            }
            
            if (isPage || isSite) {
            	String labelPageRights = JstlUtil.getMessage(request, "site.page.rights");
            	element.addOption(createOption("rights.png", labelPageRights,
            			getUrl("../usermanagement/pagerights.jsp?number=" + parentNode.getNumber()), target));
            }
        }

        return element;
    }