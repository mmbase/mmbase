<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0"   prefix="mm"
%><%@page language="java" contentType="text/html; charset=UTF-8"
%><mm:content postprocessor="reducespace">
<%@include file="import.jsp" %><%@include file="settings.jsp" %>
<mm:import id="url">help.jsp</mm:import>
<mm:cloud method="loginpage" loginpage="login.jsp" jspvar="cloud" rank="$rank">
 <%@include file="you.div.jsp" %>
 <mm:import id="current">help</mm:import>
 <%@include file="navigate.div.jsp" %>

  <div class="help">
  <p>
  
  </p>
  <h2>Beschikbare rechten</h2>
  <p>
    Rechten worden verdeeld door middel van 'security contexten', 'groepen' en 'gebruikers'. Aan elk
   object zit dan zo'n security context vast, en daarin zitten 'groepen' en 'gebruikers' vast en die
   bepalen wie wat mag met zo'n object.
   </p>
   <p>
    Er zijn de volgende rechten.
  </p>
  <table class="rights">
   <mm:import id="operations" vartype="list">create,read,write,delete,change context</mm:import>
    <tr><mm:stringlist referid="operations"><th><mm:write /></th></mm:stringlist></tr>
    <tr>
    <td class="text">Creëer-rechten zijn alleen maar relevant voor objecten die zelf een 'object type' beschrijven. Als je het 'creëer' recht hebt op dat type object, dan mag je nieuwe objecten van dat type maken.</td>
    <td class="text">Leesrechten: Als je dit recht op de context van het object hebt, dan mag je hem zien.</td>
    <td class="text">Schrijfrechten: Als je dit recht op de context van het object hebt, dan mag je hem veranderen.</td>
    <td class="text">Verwijderrechten: Als je dit recht op de context van het object  hebt, dan mag je hem verwijderen.</td>
    <td class="text">Contextrechten: Als je dit recht op de context van het object  hebt, dan mag je de 'context' er van wijzigen (en dus de rechten veranderen).</td>
  </table>

  <h2>De status van een recht</h2>
  <p>
    Rechten worden dus in feite aan groepen of gebruikers toegekend. Groepen en gebruikers zitten in
    een soort boom-structuur, wat betekent dat gebruikers in groepen kunnen zitten, en groepen weer in
    andere groepen. Een groep of een gebruiker krijgt automatisch alle rechten die de groepen waar
    hij inzit ook al heeft.
  </p>
  <p>
    In deze editors kun je zien dat een recht er al is doordat de groep of gebruiker in een andere
    groep zit, doordat de achtergrond van het vink-boxje groen is. Zo'n recht is dus niet
    rechtstreeks te verwijderen (daarvoor moet je bij de groep zijn waar deze gebruiker/groep inzit).
  </p>
  <table class="rights">
    <tr><td><input type="checkbox" /></td><td class="text">Mag niet, maar u kunt het toestaan.</td></tr>
    <tr><td><input type="checkbox" checked="checked" /></td><td class="text">Mag, maar u kunt het verbieden.</td></tr>
    <tr><td class="parent"><input type="checkbox" /></td><td class="text">Mag van groep waarin deze gebruiker/groep zit, u kunt het ook hier toestaan (als het recht dan verwijderd wordt van de bevattende groep, dan zal het voor deze groep/gebruiker toch blijven mogen)</td></tr>
    <tr><td class="parent"><input type="checkbox" checked="checked" /></td><td class="text">Mag van de bevattende groep, en ook van de groep/gebruiker zelf (en dat laatste mag u weghalen)</td></tr>
    <tr><td></td><td class="text">Mag niet, en u kunt het niet toestaan</td></tr>
    <tr><td>X</td><td class="text">Mag, en u kunt het niet verbieden</td></tr>
    <tr><td class="parent"></td><td class="text">Mag van de bevattende groep, u mag het niet toestaan voor deze gebruiker/groep</td></tr>
    <tr><td class="parent">X</td><td class="text">Mag van de gevattende grope, en ook voor deze gebruiker/groep, en u kunt het niet verbieden.</td></tr>
  </table>
  <h2>Manier van werken</h2>
  <p>
    Het komt er in feite op neer dat alle gebruikersgroepen van uw website overheenkomen met een
    groep in het security systeem, en ook tenminste 1 eigen security context. Als er dus een gebruiker moet worden aangemaakt voor een nog niet bestaande groep,
    dan kunt u het beste beginnen met het creëren van de groep en/of context. Deze groep en context zullen vaak 1 op 1 bij elkaar horen. De leden van de groep kunnen 
    nodes van deze context editen, en anderen niet. De leden van de groep zullen iha ook de default context hebben die bij deze groep hoort, dat betekent dat als ze 
    nieuwe nodes maken, deze bij default van deze groep worden, en alle leden ze dus weer kunnen editen. Omdat gebruikers ook lid kunnen zijn van meerdere groepen, zal het 
    vaak zo zijn dat de edit-pagina's van een site er voor zorgen dat deze 'default context' niet gebruikt wordt, maar in plaats van de context die bestemd is voor het 
    gedeelte van de site waar de nieuwe node 'bij' zal horen.    
  </p>
  <p>
    Omdat het vaak dus zo zal zijn dat contexten en groepen bij elkaar horen, geven deze editors te mogelijkheid ze tegelijkertijd te creëren. Als u er voor kiest om 
    deze optie te gebruiken zal na het creëren van de groep de bijbehorende context meteen bestaan, en ook de voor de hand liggende rechten relaties tussen die twee 
    (de nieuwe groep heeft alle rechten op de nieuwe context)
  </p>
  <p>
    Natuurlijk zijn er meestal ook nog groepen waar geen context bij hoort. In de meeste systemen
    zullen er groepen zijn als 'Users', de groep die alle 'gebruikersgroepen' bevat, en dus rechten
    kan verdelen aan mensen die zich hebben geauthenticeerd. Samen met de 'anonieme' gebruiker zit deze groep 
    dan bijv. weer in de 'Everybody' groep. Voor extranetten bijv. zal de groep Everybody geen
    leesrechten hebben op de meeste objecten, wat er dan zorg voor draagt dat je moet zijn ingelogged om 
    iets aan de site te hebben (anders mag je niks zien). Dan zal er waarschijnlijk nog een groep
    'Administrators' zijn. Deze groep bevat de gebruikers die heel veel mogen. Elke 'gebruikersgroep' zal meestal
    ook 'Administrators' bevatten. Als u dus een nieuwe groep creëert zult u waarschijnlijk deze
    rechten laten ontvangen van "Users" en rechten laten geven aan "Administrators".
  </p>
  <p>
    Gebruikers hebben ook nog een rang. Rangen worden gebruikt om toegang tot gedeeltes van de site te regelen. Verder is er 
    nog de 'Administrator' rang, als je die rang hebt dan mag je sowieso alles, ook al zit je niet eens in de groep 'Administrators'. 
    Als je die rang hebt moet je dus goed weten wat je doet. De 'anonieme' (oningeloggede) gebruiker
    is meestal de enige met laagste rang 'anonymous'. Dat wordt gebruikt om zulke gebruikers
    bijv. toegang tot de editors te ontzeggen, want ze mogen daar waarschijnlijk toch niks. Ook de
    andere rangen hebben verder geen speciale betekenis.
  </p>
  <h2>Object model</h2>
  <p>
    De volgende object-types zijn belangrijk voor deze security-implementatie.
    <dl>
      <dt>mmbaseranks</dt>
      <dd>
        Beschrijven de beschikbare rangen in het systeem. Er is hiervoor geen gespecialiseerde
        editor beschikbaar. De administrator zal generieke editors gebruiken, als het nodig is.
      </dd>
      <dt>mmbaseusers</dt>
      <dd>
        Elke gebruikers-account wordt opgeslagen in een mmbaseusers object, wat bijv. de username,
        het wachtwoord (gecodeerd) en de 'default security context' bevat (de security context die nieuwe
        objecten van deze gebruiker hebben).
      </dd>
      <dt>mmbasegroups</dt>
      <dd>
        Gebruikers en andere groepen kunnen worden gegroepeerd door ze te relateren aan een mmbasegroups object.
      </dd>
      <dt>mmbasecontexts</dt>
      <dd>
        Rechten worden gecreëeerd door het leggen van relaties tussen 'security contexten' en
        gebruikers en/of groepen. Elk mmbase object is geassocieerd met zo'n security context. De
        security contexten kunnen gezien worden als de 'eigenaars'.
      </dd>
    </dl>
    <img src="<mm:url page="${location}images/Security.jpg" />" />
  </p>

</div>
</mm:cloud>
</mm:content>