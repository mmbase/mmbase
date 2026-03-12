<%@tag import="org.mmbase.framework.ErrorRenderer"
%><%@attribute name="exception"   required="true" type="java.lang.Throwable"
%><%@attribute name="showversion" type="java.lang.Boolean"
%><%@attribute name="showsession" type="java.lang.Boolean"
%><%@attribute name="requestignore" type="java.lang.String"
%><%@attribute name="sessionignore" type="java.lang.String"
%><%
Throwable exception = (Throwable) jspContext.getAttribute("exception");
int errorCode = exception instanceof org.mmbase.bridge.NotFoundException ? 404 : 500;
response.setStatus(errorCode);
ErrorRenderer.Error error = new ErrorRenderer.Error(errorCode , exception);
error.setShowVersion((Boolean) jspContext.getAttribute("showversion"));
error.setShowSession((Boolean) jspContext.getAttribute("showsession"));
error.setRequestIgnore((String) jspContext.getAttribute("requestignore"));
error.setSessionIgnore((String) jspContext.getAttribute("sessionignore"));
error.getErrorReport(out, request, new org.mmbase.util.transformers.Xml());
%>
