<html:hidden property="contenttypes" value="newsletterpublication" />
<table border="0">
   <tr>
      <td style="width: 110px"><fmt:message key="newspubform.title" /></td>
      <td><html:text style="width: 150px" property="title"/></td>
   </tr>
   <tr>
      <td><fmt:message key="newspubform.description" /></td>
      <td><html:text style="width: 150px" property="description"/></td>
   </tr>
   <tr>
      <td><fmt:message key="newspubform.subject" /></td>
      <td><html:text style="width: 150px" property="subject"/></td>
   </tr>
   <tr>
      <td><fmt:message key="newspubform.intro" /></td>
      <td><html:text style="width: 150px" property="intro"/></td>
   </tr>
   <tr>
      <td></td>
      <td>
         <input type="submit" name="submitButton" onclick="setOffset(0);" 
         value="<fmt:message key="newspubform.submit" />"/>
      </td>
   </tr>
</table>