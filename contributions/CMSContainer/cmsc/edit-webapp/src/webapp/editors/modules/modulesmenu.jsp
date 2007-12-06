<%@page language="java" contentType="text/html;charset=UTF-8"%>
<%@include file="../globals.jsp"%>
<mm:content type="text/html" encoding="UTF-8" expires="0">
	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
	<html:html xhtml="true">
		<cmscedit:head title="modules.title" />
		<body>
		<mm:cloud jspvar="cloud" loginpage="login.jsp">
			<mm:haspage page="/editors/admin/">
				<mm:hasrank minvalue="siteadmin">
					<cmscedit:sideblock title="modules.title">
						<ul class="shortcuts">
							<mm:haspage page="/editors/modules/customermenu.jsp">
								<jsp:include page="/editors/modules/customermenu.jsp"/>
							</mm:haspage>
							<mm:haspage page="/editors/messageoftheday">
								<li class="messageoftheday">
									<c:url var="messageofthedayUrl" value="/editors/messageoftheday/index.jsp"/>
									<a href="${messageofthedayUrl}" target="rightpane">
									<fmt:message key="modules.messageoftheday" />
									</a> </li>
							</mm:haspage>
							<mm:haspage page="/editors/resources/reactionsearch.jsp">
								<li class="reactions"> <a href="<mm:url page="../resources/ReactionInitAction.do"/>" target="rightpane">
									<fmt:message key="modules.reactions" />
									</a> </li>
							</mm:haspage>
							<mm:haspage page="/editors/versioning/modules.jsp">
								<li class="versioning"> <a href="<mm:url page="../versioning/modules.jsp"/>" target="rightpane">
									<fmt:message key="modules.versioning" />
									</a> </li>
							</mm:haspage>
							<mm:haspage page="/editors/newsletter/index.jsp">
								<li class="versioning"><a href="<mm:url page="../newsletter/index.jsp"/>" target="rightpane">
									<fmt:message key="modules.newsletter" />
									</a> </li>
							</mm:haspage>
							<mm:haspage page="/editors/community/admin.jsp">
								<li class="versioning"><a href="<mm:url page="../community/admin.jsp"/>" target="rightpane">
									<fmt:message key="modules.community" />
									</a> </li>
							</mm:haspage>
							<mm:haspage page="/editors/egemmail">
								<li class="egem"> <a href="../egemmail/EgemSearchInitAction.do" target="rightpane">
									<fmt:message key="modules.egemmail.export" />
									</a> </li>
								<li class="egem"> <a href="<cmsc:property key="egemmail.beheer.path"/>" target="_blank">
									<fmt:message key="modules.egemmail.admin" />
									</a> </li>
							</mm:haspage>
							<mm:haspage page="/editors/language-redirect">
								<li class="guestbook"> <a href="../language-redirect/test.jsp" target="rightpane">Taal afhankelijkheden</a> </li>
							</mm:haspage>
						</ul>
					</cmscedit:sideblock>
				</mm:hasrank>
			</mm:haspage>
		</mm:cloud>
		</body>
	</html:html>
</mm:content>
