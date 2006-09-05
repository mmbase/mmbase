<%@ page isErrorPage="true"%>
<%@ include file="../globals.jsp"%>
<%@ page import="java.io.*,java.text.*"%>
<%!
    Logger log = Logging.getLoggerInstance("ERROR-JSP");

   /** the date + time long format */
   private static final SimpleDateFormat DATE_TIME_FORMAT =
      new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

   /**
    * Creates String.from given long according to dd-MM-yyyy HH:mm:ss
    * @param date the date to format
    * @return Datestring
    */
   public static String getDateTimeString(long date) {
      return DATE_TIME_FORMAT.format(new Date(date));
   }
%>
<%
	// $Name: not supported by cvs2svn $ will be expanded when checked out with an explicit tagname. For Example "cvs co -r first"
	String version = com.finalist.util.version.VersionUtil.getVersion(this.getServletConfig().getServletContext());
	// prepare error ticket
	long ticket = System.currentTimeMillis();

	String msg = net.sf.mmapps.commons.util.HttpUtil.getErrorInfo(request, exception, ticket, version);
	
	String message = "";
	if (exception != null) {
		message = exception.getMessage();
		if (message == null) {
			message = exception.toString();
		}
	}
	
    // write errors to mmbase log
    log.error(ticket+":\n" + msg);
%>
<% request.setAttribute("title", "500: internal server error"); %>
<%@include file="header.jsp"%>
<p>
<fmt:message key="exception.500.message" /><br />
<fmt:message key="exception.500.description" /><br />
<fmt:message key="exception.500.actions" /><br />
</p>
<hr />
<form action="<c:url value='/error/senderror.jsp' />" method="post">
	<input type="hidden" name="messagetype" value="500" />
	<input type="hidden" name="ticket" value="<%= ticket %>" />
	<input type="hidden" name="message" value="<%= msg %>" />
</form>
<p><a href="javascript:document.forms[0].submit();"><fmt:message key="exception.500.send" /></a></p>

<script type="text/javascript">
      function showError() {
         var errorVisible = document.getElementById('errordiv').style.visibility;
         if (errorVisible=='hidden') {
            document.getElementById('errordiv').style.visibility='visible';
         }
         else {
            document.getElementById('errordiv').style.visibility='hidden';
         }
      }
</script>
<P>
   <a href="javascript:showError();"><fmt:message key="exception.500.showerror" /></a>
</P>
<div id="errordiv" style="visibility:hidden">
<pre><%= getDateTimeString(ticket) %> - <%= message %></pre>
<br /><br />
<pre><%=msg%></pre>
</div>
<%@include file="footer.jsp"%>