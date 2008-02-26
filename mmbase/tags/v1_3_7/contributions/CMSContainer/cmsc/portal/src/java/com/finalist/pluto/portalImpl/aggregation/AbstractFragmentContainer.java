/*
 * Copyright 2003,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* 

 */

package com.finalist.pluto.portalImpl.aggregation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.finalist.pluto.portalImpl.core.PortalURL;

public abstract class AbstractFragmentContainer extends AbstractFragment {

	private ArrayList<Fragment> children = new ArrayList<Fragment>();

	public AbstractFragmentContainer(String id, ServletConfig config, Fragment parent) throws Exception {
		super(id, config, parent);
	}

	public void preService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

	public void postService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

	public Collection<Fragment> getChildFragments() {
		return children;
	}

	public void addChild(Fragment child) {
		children.add(child);
	}

	abstract public void createURL(PortalURL url);

}
