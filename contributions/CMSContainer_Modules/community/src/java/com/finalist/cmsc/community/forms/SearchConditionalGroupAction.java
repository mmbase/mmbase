package com.finalist.cmsc.community.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.mmapps.commons.util.StringUtil;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.finalist.cmsc.paging.PagingStatusHolder;
import com.finalist.cmsc.paging.PagingUtils;
import com.finalist.cmsc.services.community.domain.GroupForShowVO;
import com.finalist.cmsc.services.community.person.Person;
import com.finalist.cmsc.services.community.security.Authentication;
import com.finalist.cmsc.services.community.security.Authority;

public class SearchConditionalGroupAction extends AbstractCommunityAction {

	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		SearchGroupForm searchform = (SearchGroupForm) form;
		PagingStatusHolder holder = setPagingInformation(request);
		HashMap map = getParameterMap(searchform);
		List<Authority> authorities = getAuthorityService().getAssociatedAuthorities(map,holder);
		int totalCount = getAuthorityService().getAssociatedAuthoritiesNum(map,holder);
		setSharedAttributes(request, authorities, totalCount);
		return mapping.findForward("success");
	}

	private void setSharedAttributes(HttpServletRequest request, List<Authority> authorities, int totalCount) {
		request.setAttribute("totalCount", totalCount);
		request.setAttribute("results", convertAuthrityTOVO(authorities));
	}

	private PagingStatusHolder setPagingInformation(HttpServletRequest request) {
		PagingUtils.initStatusHolder(request);
	        PagingStatusHolder holder = PagingUtils.getStatusHolder();
	        holder.setDefaultSort("asn.id","desc");
		return holder;
	}

	private HashMap getParameterMap(SearchGroupForm searchform) {
		HashMap map = new HashMap();

		if (!StringUtil.isEmptyOrWhitespace(searchform.getMember())) {
			map.put("username", searchform.processNames(searchform.getMember()));
		}

		if (!StringUtil.isEmptyOrWhitespace(searchform.getGroupname())) {
			map.put("group", searchform.getGroupname());
		}
		return map;
	}

	private List<GroupForShowVO> convertAuthrityTOVO(List<Authority> authorities){
		List<GroupForShowVO> groupForShow = new ArrayList<GroupForShowVO>();
		for(Authority authority : authorities){
			if (null!=authority) {
				GroupForShowVO group = new GroupForShowVO();
				group.setGroupName(authority.getName());
				group.setGroupId(authority.getId().toString());
				StringBuffer userNames = new StringBuffer();
				Set<Authentication> authentications = authority.getAuthentications();
				if (!authentications.isEmpty()) {
					for (Authentication au : authentications) {
						Person person = getPersonService()
								.getPersonByAuthenticationId(au.getId());
						if (person != null) {
							userNames.append(person.getFirstName() + " "+ person.getLastName() + ", ");
						}
					}
					group.setUsers(userNames.substring(0,
							userNames.length() - 2));
				} else {
					group.setUsers("");
				}
				groupForShow.add(group);
			}			
		}
		return groupForShow;
   }

}