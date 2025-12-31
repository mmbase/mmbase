<%--
  Reuseable generic login-page.
  Perhaps this could be placed on a more generic location like /mmbase
--%>
<%@ page import="org.mmbase.security.AuthenticationData,org.mmbase.bridge.*,org.mmbase.util.functions.*,java.util.*,org.mmbase.datatypes.*"
%><%@  taglib uri="http://www.mmbase.org/mmbase-taglib-2.0"  prefix="mm"
%><%!
   String getPrompt(String key, Locale locale) {
     try {
       return ResourceBundle.getBundle(AuthenticationData.STRINGS, locale).getString(key);
     } catch (MissingResourceException mre) {
       return key;
     }
   }
%>
<mm:import from="request" externid="language">en</mm:import>
<mm:import from="request" externid="country"></mm:import>
<mm:import from="request" externid="sessionname">cloud_mmbase</mm:import>
<mm:import from="request" externid="uri" jspvar="uri">local</mm:import>

<mm:content type="text/html" language="$language" country="$country" expires="0" jspvar="locale">

<mm:import from="request"  externid="reason">please</mm:import>
  <mm:import from="request" externid="exactreason" />
  <mm:import from="request" externid="usernames" />
  <mm:import from="request" externid="referrer">.</mm:import>
  <mm:compare referid="reason" value="failed">
    <p class="failed">
      <%=getPrompt("failed", locale)%> <mm:write referid="exactreason"><mm:isnotempty>(<mm:write />)</mm:isnotempty></mm:write>.
    </p>
  </mm:compare>
  <mm:compare referid="reason" value="rank">
    <p class="failed">
      <%=getPrompt("failed_rank", locale)%>
    </p>
  </mm:compare>
  <table class="login">
    <%
      AuthenticationData authentication = ContextProvider.getCloudContext(uri).getAuthentication();
      String[] authenticationTypes = authentication.getTypes(authentication.getDefaultMethod(request.getProtocol()));
      StringList cloudNames = ContextProvider.getCloudContext(uri).getCloudNames();
    %>
    <mm:import externid="authenticate" jspvar="currentType" vartype="string" ><%=authenticationTypes[0]%></mm:import>
    <form method="post" action="<mm:write referid="referrer" />" >

    <% if (cloudNames.size() > 1) { %>
    <tr><td>Cloud</td>
    <td>
       <select name="cloud">
         <% for(String cloudName : cloudNames) { %>
          <option value="<%=cloudName%>"><%=cloudName%></option>
         <% } %>
       </select>
    <% } %>
    <% Parameter[] params = authentication.createParameters(currentType).getDefinition();
       for (int j = 0; j < params.length ; j++) {
         Parameter param = params[j];
         Class type = param.getTypeAsClass();
         if (type.isAssignableFrom(String.class) && param.isRequired()) {
    %>
    <tr>
      <td><%=param.getLocalizedGUIName().get(locale)%> <mm:write value="<%=param.getLocalizedDescription().get(locale)%>"><mm:isnotempty>(<mm:write />)</mm:isnotempty></mm:write>
      </td>
      <td>
        <input type="<%= ((StringDataType) param.getDataType()).isPassword() ? "password" : "text" %>" name="<%=param.getName()%>" autocomplete="off" />
       </td>
     </tr>
     <%  }
        }
     %>
     <input type="hidden" name="usernames" value="<mm:write referid="usernames" />" />
    <tr><td /><td><input type="hidden" name="command" value="login" /><input type="submit" name="__submit" value="<%=getPrompt("login", locale)%>" /></td></tr>
  </form>
    <tr>
      <td><%=getPrompt("authenticate", locale)%>:</td>
      <td>
        <form method="post" name="auth">
        <select name="authenticate" onChange="document.forms['auth'].submit();">
          <% for (int i = 0 ; i < authenticationTypes.length; i++) { %>
          <option value="<%=authenticationTypes[i]%>" <%= currentType.equals(authenticationTypes[i])? " selected='selected'" : ""%>><%=authenticationTypes[i]%></option>
          <% } %>
          <input type="hidden" name="referrer" value="<mm:write referid="referrer" />" />
          <input type="hidden" name="usernames" value="<mm:write referid="usernames" />" />
         </select>
       </form>
    </tr>
</table>
</mm:content>
