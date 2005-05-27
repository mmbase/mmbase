              <td class="listItem">
                <a href="<mm:treefile page="/pop/index.jsp" objectlist="$includePath" referids="$referids,currentprofile">
                    <mm:param name="currentcomp"><%= thisCompetencie %></mm:param>
                    <mm:param name="command">editcomp</mm:param>
                   </mm:treefile>"><mm:field name="name"/>
                </a>
              </td>
              <% isEmpty = true; %>
              <mm:import id="thisfeedback" reset="true">-1</mm:import>
              <mm:list nodes="$currentpop" path="pop,popfeedback,people" constraints="people.number='$user'">
                <mm:field name="popfeedback.number" jspvar="thisFeedback" vartype="String">
                  <mm:list nodes="<%= thisCompetencie %>" path="competencies,popfeedback"
                      constraints="<%= "popfeedback.number='" + thisFeedback +"'" %>">
                    <% isEmpty = false; %>
                    <mm:import id="thisfeedback" reset="true"><%= thisFeedback %></mm:import>
                    <td class="listItem"><mm:field name="popfeedback.rank"/></td>
                    <td class="listItem"><mm:field name="popfeedback.text"/></td>
                  </mm:list>
                </mm:field>
              </mm:list>
              <% if (isEmpty) { %>
                <td class="listItem">&nbsp;</td>
                <td class="listItem">&nbsp;</td>
              <% } %>
              <% int sumOfRatings = 0;
                 int ratingsNum = 0; 
                 String sRating = "";
              %>
              <mm:list nodes="$currentpop" path="pop,popfeedback,people"
                  constraints="NOT people.number='$user'">
                <mm:field name="popfeedback.number" jspvar="thisFeedback" vartype="String">
                  <mm:list nodes="<%= thisCompetencie %>" path="competencies,popfeedback,ratings"
                      constraints="<%= "popfeedback.number='" + thisFeedback +"'" %>">
                    <mm:field name="ratings.pos" jspvar="dummy" vartype="String">
                      <% ratingsNum++;
                         sumOfRatings += (new Integer(dummy)).intValue();
                      %>
                    </mm:field>
                  </mm:list>
                </mm:field>
              </mm:list>
              <% if (ratingsNum == 0) { %>
                 <td class="listItem">&nbsp;</td>
              <% } else {
                   int averageRating = Math.round((float)sumOfRatings / (float)ratingsNum);
                   %>
              <mm:list nodes="<%= thisCompetencie %>" path="competencies,insrel,profiles,related,pop" constraints="pop.number='$currentpop'">
                <mm:node element="insrel">
                  <mm:related path="posrel,ratings">
                    <mm:field name="ratings.pos" jspvar="dummy" vartype="String">
                      <% int compRating = (new Integer(dummy)).intValue();
                         if (averageRating < compRating) {
                           sRating = "Goed (+)";
                         } else {
                           sRating = "Voldoende (-)";
                         }
                      %>
                    </mm:field>
                  </mm:related>
                </mm:node>
              </mm:list>
              <td class="listItem"><%= sRating %></td>
              <% } %>
              <td class="listItem">
                <mm:list nodes="<%= thisCompetencie %>" path="competencies,todoitems,pop" 
                    constraints="pop.number='$currentpop'" orderby="todoitems.number" directions="UP">
                  &bull; <a href="<mm:treefile page="/pop/index.jsp" objectlist="$includePath"
                             referids="$referids,currentfolder,currentprofile">
                           <mm:param name="currentcomp"><%= thisCompetencie %></mm:param>
                           <mm:param name="todonumber"><mm:field name="todoitems.number"/></mm:param>
                           <mm:param name="command">addtodo</mm:param>
                           <mm:param name="returnto">no</mm:param>
                         </mm:treefile>"><mm:field name="todoitems.name" jspvar="todoName" vartype="String"
                         ><% if (todoName.length()>0) { %><%= todoName %><% } else { %>...<% } %></mm:field></a><br/>
                </mm:list>
              </td>
              <% if (isEmpty) { %>
                 <td class="listItem">&nbsp;</td>
              <% } else { %>
              <td class="listItem">
                <mm:node number="$thisfeedback">
                  <mm:relatednodes type="attachments">
                    <mm:related path="folders,portfolios,people" constraints="people.number='$user'">
                      &bull; <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$referids">
                               <mm:param name="typeof"><mm:field name="portfolios.type"/></mm:param>
                               <mm:param name="currentfolder"><mm:field name="folders.number"/></mm:param>
                             </mm:treefile>"
                    ></mm:related><mm:field name="title"/></a><br/>
                  </mm:relatednodes>
                  <mm:relatednodes type="folders">
                    <mm:import id="foldernumber" reset="true"><mm:field name="number"/></mm:import>
                    <mm:related path="portfolios,people" constraints="people.number='$user'">
                      &bull; <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$referids">
                               <mm:param name="typeof"><mm:field name="portfolios.type"/></mm:param>
                               <mm:param name="currentfolder"><mm:write referid="foldernumber"/></mm:param>
                             </mm:treefile>"
                    ></mm:related><mm:field name="name"/></a><br/>
                  </mm:relatednodes>
                  <mm:relatednodes type="urls">
                    <mm:related path="folders,portfolios,people" constraints="people.number='$user'">
                      &bull; <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$referids">
                               <mm:param name="typeof"><mm:field name="portfolios.type"/></mm:param>
                               <mm:param name="currentfolder"><mm:field name="folders.number"/></mm:param>
                             </mm:treefile>"
                    ></mm:related><mm:field name="name"/></a><br/>
                  </mm:relatednodes>
                  <mm:relatednodes type="pages">
                    <mm:related path="folders,portfolios,people" constraints="people.number='$user'">
                      &bull; <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$referids">
                               <mm:param name="typeof"><mm:field name="portfolios.type"/></mm:param>
                               <mm:param name="currentfolder"><mm:field name="folders.number"/></mm:param>
                             </mm:treefile>"
                    ></mm:related><mm:field name="name"/></a><br/>
                  </mm:relatednodes>
                  <mm:relatednodes type="chatlogs">
                    <mm:related path="folders,portfolios,people" constraints="people.number='$user'">
                      &bull; <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$referids">
                               <mm:param name="typeof"><mm:field name="portfolios.type"/></mm:param>
                               <mm:param name="currentfolder"><mm:field name="folders.number"/></mm:param>
                             </mm:treefile>"
                    ></mm:related><mm:field name="date"/></a><br/>
                  </mm:relatednodes>
                </mm:node>
              </td>
              <% } %>