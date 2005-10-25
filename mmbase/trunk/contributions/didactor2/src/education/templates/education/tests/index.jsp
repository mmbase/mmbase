<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm"%>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>

<%@ page import="java.text.*,java.util.*" %>

<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">


<mm:import id="testNo" externid="learnobject" required="true"/>


<%@include file="/shared/setImports.jsp" %>
<%@include file="/education/tests/definitions.jsp" %>


<%-- remember this page --%>
<mm:treeinclude page="/education/storebookmarks.jsp" objectlist="$includePath" referids="$referids">
   <mm:param name="learnobject"><mm:write referid="testNo"/></mm:param>
   <mm:param name="learnobjecttype">tests</mm:param>
</mm:treeinclude>


<%-- find user's copybook --%>
<mm:node number="$user">
   <%@include file="find_copybook.jsp"%>
</mm:node>

<mm:node number="$testNo">
  <mm:relatednodescontainer path="madetests,copybooks" element="madetests">

    <mm:constraint field="madetests.score" referid="TESTSCORE_INCOMPLETE" inverse="true"/>

    <mm:constraint field="copybooks.number" referid="copybookNo"/>

    <mm:relatednodes>

       <mm:field id="madetestNo" name="number" write="false"/>

       <mm:field id="madetestscore" name="score" write="false"/>

    </mm:relatednodes>

  </mm:relatednodescontainer>

    <mm:listnodescontainer type="tests"> 
       <mm:constraint operator="equal" field="number" referid="testNo" />
       <mm:listnodes >    
          <mm:first>  
            <mm:node>
               <mm:field id="testIsAlwaysOnline" name="always_online" write="false" />
               <mm:field id="testStartDate" name="online_date" write="false" />
               <mm:field id="testEndDate" name="offline_date" write="false" />
            </mm:node> 
          </mm:first>   
       </mm:listnodes>   
    </mm:listnodescontainer>

    <mm:present referid="testStartDate">
      <mm:import jspvar="testIsAlwaysOnline"><mm:write referid="testIsAlwaysOnline" /></mm:import>
      <mm:import jspvar="testStartDate"><mm:write referid="testStartDate" /></mm:import>
      <mm:import jspvar="testEndDate"><mm:write referid="testEndDate" /></mm:import>
      
      <%
        long startDateTimestamp = 0;
        try{
          startDateTimestamp = Long.parseLong(testStartDate) * 1000;
        }
        catch(Exception e){
          e.printStackTrace();
        }
        long endDateTimestamp = 0;
        try{
          endDateTimestamp = Long.parseLong(testEndDate) * 1000;
        }
        catch(Exception e){
          e.printStackTrace();
        }

        long alwaysShow = 1;
        try{
          alwaysShow = Long.parseLong(testIsAlwaysOnline);
        }
        catch(Exception e){
          e.printStackTrace();
        }

        if ((startDateTimestamp == 0 || startDateTimestamp < System.currentTimeMillis()) &&
            (endDateTimestamp == 0 || endDateTimestamp > System.currentTimeMillis())){
          %>
          <!-- Test can be showed -->
          <%
        }
        else if (alwaysShow != 1){
          Calendar cal = Calendar.getInstance();
          SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
          cal.setTime(new Date(startDateTimestamp));
          String myStartDate = sdf.format(cal.getTime());
          cal.setTime(new Date(endDateTimestamp));
          String myStopDate = sdf.format(cal.getTime());
          %>
          <mm:import id="testCantBeShowed"></mm:import>
          <html>
            <head>
              <title>Toets</title>
              <link rel="stylesheet" type="text/css" href="<mm:treefile page="/css/base.css" objectlist="$includePath" />" />
            </head>
            <body>
              <div class="learnenvironment">
                Sorry, maar de test is gepland op <%= myStartDate %> - <%= myStopDate %>.
              </div>
            </body>
          </html>
          <%
        }
      %>
    </mm:present>
    
  <mm:notpresent referid="testCantBeShowed">
      <mm:present referid="madetestNo">
        <html>
          <head>
            <title>Toets</title>
            <link rel="stylesheet" type="text/css" href="<mm:treefile page="/css/base.css" objectlist="$includePath" />" />
          </head>
          <body>
          <div class="learnenvironment">
            <mm:field name="showtitle">
              <mm:compare value="1">
                <h1><mm:field name="name"/></h1>
              </mm:compare>
            </mm:field>
            <mm:field id="maychange" name="maychange" write="false"/>
            <mm:field id="mayview" name="mayview" write="false"/>
            <mm:field id="feedback" name="feedbackpage" write="false"/>
            <mm:compare referid="madetestscore" referid2="TESTSCORE_TBS">
            De toets is reeds gemaakt. De antwoorden moeten nog worden nagekeken.<p/>
            </mm:compare>
    
            <mm:compare referid="madetestscore" referid2="TESTSCORE_TBS" inverse="true">
    
              <%-- if madestestscore larger or equal than requiredscore --%>
    
              <mm:field id="requiredscore" name="requiredscore" write="false"/>
    
    
    
             <mm:islessthan referid="feedback" value="1">
    
              <mm:islessthan referid="madetestscore" referid2="requiredscore" inverse="true">
    
                De toets is reeds gemaakt en behaald.<p/>
    
              </mm:islessthan>
    
    
    
              <mm:islessthan referid="madetestscore" referid2="requiredscore">
    
                De toets is reeds gemaakt en niet behaald.<p/>
    
              </mm:islessthan>
    
             </mm:islessthan>
    
    
    
              <mm:compare referid="feedback" value="1">
    
                Deze oefentoets is reeds gemaakt.<p/>
    
              </mm:compare>
    
    
    
    
    
             <mm:compare referid="mayview" value="1">
    
                  <table><tr>
    
                     <td><div class="button1">
    
                       <a href="<mm:treefile page="/education/tests/viewanswersframe.jsp" objectlist="$includePath" referids="$referids">
    
                       <mm:param name="testNo"><mm:write referid="testNo"/></mm:param>
    
                       <mm:param name="madetestNo"><mm:write referid="madetestNo"/></mm:param>
    
                       <mm:param name="userNo"><mm:write referid="user"/></mm:param>
    
                       </mm:treefile>"><di:translate key="education.view" /></a>
    
                   </div></td>
    
                </mm:compare>
    
                <mm:compare referid="maychange" value="1">
    
                   <td><div class="button1">
    
                        <a href="<mm:treefile page="/education/tests/buildtest.jsp" objectlist="$includePath" referids="$referids">
    
                     <mm:param name="learnobject"><mm:write referid="testNo"/></mm:param>
    
                    </mm:treefile>"><mm:compare referid="feedback" value="1"><di:translate key="education.again" /></mm:compare><mm:compare referid="feedback" value="0"><di:translate key="education.retry" /></a></mm:compare></a>
    
                   </div></td>
    
                   </tr></table>
    
                   </mm:compare>
    
                </mm:compare>
    
    </div>
    
          </body>
    
        </html>
    
      </mm:present>
  </mm:notpresent>
</mm:node>

  <mm:notpresent referid="testCantBeShowed">

    <mm:present referid="madetestNo" inverse="true">
      <mm:treeinclude page="/education/tests/buildtest.jsp" objectlist="$includePath" referids="$referids">
        <mm:param name="learnobject"><mm:write referid="testNo"/></mm:param>
       </mm:treeinclude>
    </mm:present>
  </mm:notpresent>


</mm:cloud>

</mm:content>

