<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"%>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">

<mm:import externid="testNo" required="true"/>
<mm:import externid="madetestNo" required="true"/>
<mm:import externid="userNo" required="true"/>
<mm:import externid="feedback" required="false"/>

<%@include file="/shared/setImports.jsp" %>
<%@include file="/education/tests/definitions.jsp" %>

<di:may component="education" action="isSelfOrTeacherOf" arguments="userNo">

<mm:node referid="madetestNo">
 
  <mm:relatednodes type="givenanswers">
    <mm:relatednodes type="questions">
      <mm:import id="questiontype"><mm:nodeinfo type="type"/></mm:import>

      <mm:field id="questiontext" name="text" write="false"/>
      Vraag: <mm:write referid="questiontext" escape="none"/><br/>
      <mm:remove referid="questiontext"/>
      <mm:field id="questionNo" name="number" write="false"/>
    </mm:relatednodes>

    <mm:notpresent referid="questiontype">
      questiontype not found 
      <mm:import id="questiontype">none</mm:import>
    </mm:notpresent>

    <mm:compare referid="questiontype" value="openquestions">
      Gegeven antwoord: <mm:field name="text"/><br/>
      <mm:relatednodescontainer type="feedback">
        <mm:relatednodes>
          <%-- Open Question Feedback (from teacher) --%>
          Feedback: <mm:field name="text" escape="none"/>
          <p/>
        </mm:relatednodes>
      </mm:relatednodescontainer>
    </mm:compare>

    <mm:compare referid="questiontype" value="mcquestions">
      <mm:node referid="questionNo">
        <mm:field name="type" id="type" write="false"/>
      </mm:node>

      <mm:compare referid="type" value="0">
        <mm:relatednodes type="answers">
          Gegeven antwoord:
          <mm:field name="text"/>
        </mm:relatednodes>
      </mm:compare>
      <mm:compare referid="type" value="1">
        Gegeven antwoorden:
        <mm:relatednodes type="answers">
          <mm:field name="text"/>
        </mm:relatednodes>
        <br/>
      </mm:compare>
    </mm:compare>

    <mm:compare referid="questiontype" value="couplingquestions">
      <% int counter;%>
      <mm:import id="size" jspvar="size" vartype="Integer">
        <mm:relatednodescontainer type="answers" role="leftanswer">
        <mm:size/></mm:relatednodescontainer>
      </mm:import>
        <% for (int i= 0;i < size.intValue();i++) { %>
        <%counter=0;%>
        <mm:relatednodes type="answers" role="leftanswer" orderby="posrel.pos">
	  <% counter++;if (counter-1 == i) { %>
            <mm:field id="leftanswer" name="number" write="false"/>
	  <% } %>
        </mm:relatednodes>

        <%counter=0;%>
        <mm:relatednodes type="answers" role="rightanswer" orderby="posrel.pos">
	  <% counter++;if (counter-1 == i) { %>
            <mm:field id="rightanswer" name="number" write="false"/>
	  <% } %>
        </mm:relatednodes>
        <mm:node referid="leftanswer"><mm:field name="text1"/></mm:node> gekoppeld aan
        <mm:node referid="rightanswer"><mm:field name="text2"/></mm:node><br/>
        <mm:remove referid="leftanswer"/>
        <mm:remove referid="rightanswer"/>
      <% } %>

      <mm:remove referid="size"/>
    </mm:compare>


    <mm:compare referid="questiontype" value="rankingquestions">

      <mm:related path="posrel,rankinganswers">
        <mm:field id="rankinganswersnumber" name="rankinganswers.number" write="false"/>
        <div class="images">
          
          <mm:node referid="rankinganswersnumber">
            <mm:relatednodes type="images">
              <mm:field name="title"/><br/>
              <img src="<mm:image />" width="200" border="0" /><br/>
              <mm:field name="description" escape="none"/> 
              <mm:remove referid="hasimage"/>
              <mm:import id="hasimage"/>
            </mm:relatednodes>
          </mm:node>
        </div>

        <mm:notpresent referid="hasimage">
          <mm:field name="rankinganswers.text" escape="none"/>
        </mm:notpresent>
         Gegeven volgorde: <mm:field name="posrel.pos"/><br>
         <mm:remove referid="rankinganswersnumber"/>
      </mm:related>
    </mm:compare>

    <mm:compare referid="questiontype" value="valuequestions">
    </mm:compare>

    <mm:compare referid="questiontype" value="reusequestions">
    </mm:compare>

    <mm:field id="questionscore" name="score" write="false"/>
    <mm:compare referid="questionscore" value="1">
      <b>Goed</b>
    </mm:compare>
    <mm:compare referid="questionscore" value="0">
       <b>Fout</b>
    </mm:compare>
    <mm:compare referid="questionscore" referid2="TESTSCORE_TBS">
       Moet nog nagekeken worden.
    </mm:compare>

    <%-- Feedback (from the question) --%>
    <mm:node number="$questionNo">
    <mm:relatednodescontainer type="feedback">
      <mm:constraint field="maximalscore" referid="questionscore" operator=">="/>
      <mm:constraint field="minimalscore" referid="questionscore" operator="<="/>
      <mm:relatednodes>
	<h1><mm:field name="name"/></h1>
	<mm:relatednodes type="images">
	    <img src="<mm:image template="s(150x150)"/>" alt="<mm:field name="title"/>">
	</mm:relatednodes>
<p><mm:field name="text" escape="none"/></p>
      </mm:relatednodes>
    </mm:relatednodescontainer>
    </mm:node>
    <p/>

    <mm:remove referid="questionscore"/>
    <mm:remove referid="questiontype"/>
    <mm:remove referid="questionNo"/>
  </mm:relatednodes>

</mm:node>
</di:may>

</mm:cloud>
</mm:content>
