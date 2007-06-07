<%@page language="java" contentType="text/html;charset=utf-8"%>
<%@include file="globals.jsp" %>
<mm:content type="text/html" encoding="UTF-8" expires="0">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html:html xhtml="true">
<head>
  <title><fmt:message key="recyclebin.restore.title" /></title>
  <link href="../css/main.css" type="text/css" rel="stylesheet" />
</head>
<body>

<mm:cloud jspvar="cloud" rank="basic user" method='http'>

<mm:import externid="content" vartype="Node"/>
<mm:import externid="contentchannels" vartype="List"/>

<mm:node referid="content">
	<mm:import id="contentnumber"><mm:field name="number"/></mm:import>
</mm:node>

    <div class="tabs">
        <div class="tab_active">
            <div class="body">
                <div>
                    <a href="#"><fmt:message key="recyclebin.restore.title" /></a>
                </div>
            </div>
        </div>
    </div>

	<div class="editor">
		<div class="body">
		
			<p><fmt:message key="recyclebin.restore.selectchannel" /></p>
			<ul>
			<mm:list referid="contentchannels">
				<mm:import id="channelnumber"><mm:field name="number"/></mm:import>
				
				<mm:url page="RestoreAction.do" id="url" write="false" >
				   <mm:param name="channelnumber" value="$channelnumber"/>
				   <mm:param name="objectnumber" value="$contentnumber"/>
				</mm:url>
				<li>
					<a href="<mm:write referid="url"/>">
						<mm:field name="path"/>
					</a>
				</li>
			</mm:list>
			</ul>
			
			<ul class="shortcuts">
               <li class="close">
	               <a href="#" onClick="history.back()"><fmt:message key="recyclebin.restore.back" /></a>
				</li>
			</ul>
			
		</div>
		<div class="side_block_end"></div>
	</div>	

</mm:cloud>
</body>
</html:html>
</mm:content>