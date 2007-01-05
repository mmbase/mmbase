<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<html>
  <head>
  </head>
  <body>
    <mm:cloud  rank="administrator" >
            
      <!--  create the email node -->
        <mm:createnode id="mail1" type="email">
          <mm:setfield name="from">daniel@mmbase.org</mm:setfield>
          <mm:setfield name="subject">MMBase group mail</mm:setfield>
          <mm:setfield name="body">Hi this mail went to all our group members !</mm:setfield>
        </mm:createnode>
        
        <!-- get the group node we want to mail -->
        <mm:node id="group1" number="groups.testgroep" />
        
        <!-- create a relation to the group -->
        <mm:createrelation source="mail1" destination="group1" role="related" />
        
        <!-- start the mailer and wait for it to finish, default type is oneshot -->	
        <mm:node referid="mail1">
          <mm:function name="mail" />
        </mm:node>
        
        
    </mm:cloud>
  </body>
</html>
