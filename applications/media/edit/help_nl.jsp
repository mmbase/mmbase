<DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd">
<%@page language="java" contentType="text/html;charset=UTF-8" 
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" 
%><%@include file="config/read.jsp" %><mm:content language="$config.lang" postprocessor="reducespace">
<mm:cloud jspvar="cloud" loginpage="login.jsp" method="asis">
<html>
<head>
  <title><mm:write id="title" value="<%=m.getString("title")%>" /></title>
   <link href="style/streammanager.css" type="text/css" rel="stylesheet" />
   <script src="<mm:url page="style/streammanager.js.jsp?dir=&amp;fragment=" />" language="javascript"><!--help IE--></script>
<head>
<body class="help">
  <h1>Help</h1>
  <h2>Inhoudsopgave</h2>
  <p>
    De streammanager bestaat uit 2 onderdelen.
    <ul>
      <li><a href="#intro">Introductie</a></li>
      <li><a href="#search">Zoek-mogelijkheid</a></li>
      <li><a href="#editor">Editor</a></li>
      <li><a href="#security">Security</a></li>
    </ul>
  </p>  
  <h2>Introductie</h2>  
  <a name="intro"> </a>
  <p>
    De streammanager bestaat uit 2 onderdelen.
  </p>
  <h2>Een zoek-mogelijkheid</h2>  
  <a name="search"> </a>
  <p>
    Het mogelijk om de streammanager alleen als een zoek-tool te gebruiken. Hij kan dan
    geldige URL's genereren die je eventueel elders kunt gebruiken.
  </p>
  <p>
    Als de streammanager is geïntegreerd in uw eigen (mmbase) site dan is deze optie verder niet zo
    interessant, want de de URL's kunnen dan automatisch op de juiste wijze op de juiste plek worden
    getoond.
  </p>
  <h2>De streammanager editor</h2>
  <a name="editor"> </a>
  <p>
    De editor van de de streammanager kan gebruikt worden om nieuwe media fragment in te voeren en
    te wijzigen. Deze optie is alleen beschikbaar als u bent ingelogged. Als u dit wilt doen is het
    goed om te weten hoe de ondeliggend objecten-structuur in elkaar zit.
  </p>
  <p>
    We onderscheiden 'mediaproviders', 'mediabronnen' en 'mediafragmenten'. Voor
    'mediaproviders' kan men het beste maar gewoon 'streamservers' lezen, maar het is in feite wat
    abstracter, omdat het bijvoorbeeld ook een bepaald scriptje kan zijn op zo'n
    server. Mediabronnen zijn dan weinig meer dan een URL naar de de gebruikte stream en zo'n
    mediabron is dan dus altijd geässocieerd met eeen mediaprovider. Bij een mediabron kan in
    principe ook extra technische meta-informatie over de stream ingevoerd worden, zoals de grootte
    van het bestand, het type en de encodering ('bitrate' 'codec'). Dit soort informatie is meestal
    niet essentieel of kan ook automatisch bepaald worden en in het algemeen zal u dit niet hoeven invoeren.
  </p>
  <p>
    Inhouds-gerelateerde metainformatie zoals titels en beschrijvingen worden ingevoerd in apart
    objecten. De zogenaamde 'mediafragmenten'. Een mediafragment bevat ook een start- en een
    stoptijd en kan dus ook gebruikt worden slechts een gedeelte van de stream te beschrijven. Een
    mediafragment wordt ook wel een simpelweg 'fragment' of 'clip' genoemd.    
  </p>
  <p>
    De streammanager is voornamelijk bedoeld voor het creëeren van dit soort
    mediafragment-objecten.
  </p>
  <p>
    Verder kunnen aan mediafragmenten dan nog andere objecten gekoppeld zoals introductie en
    outroductie fragmenten, geldigheidsperiodes, default-sjablonen (voor de presentatie) en
    sub-mediafragmenten ('items').
  </p>
  <p>
    'Mediabronnen' en 'mediafragmenten' zijn in feite ook abstracties en in praktijk zult u of
    'audiobronnen' en 'audiofragmenten' of 'videobronnen' en 'videofragmenten'
    bewerken. Vanzelfsprekend kunnen ook de 'items' van audiofragmenten alleen audiofragmenten zijn
    en de items van videofragmenten alleen videofragmenten.
  </p>
  <p>
    Tenslotten zijn mediafragmenten nog gegroepeerd ('pools'). Dit maakt het mogelijk om je 'eigen'
    mediafragmenten makkelijk terug te vinden.
  </p>
  <p>
    Het 'object-model' kan dan grafisch bijvoorbeeld als volgt worden weergegeven. De 'rechte' lijnen
    en circels geven de mogelijke relaties aan, en de vloeiende lijnen indiceren slechts een 'specialisatie'.
    <img src="images/Media.jpg" />
  </p>
  <h2>Security</h2>
  <a name="security"> </a>
  <p>
    Er zijn 3 soorten gebruikers van de streammanager.
    <dl>
      <dt>Bezoekers</dt>
      <dd>
        Bezoekers hebben slechts toegang tot de resultaten. Bezoekers hoeven geen account te hebben.
      </dd>
      <dt>Gebruikers</dt>
      <dd>
        Gebruikers hebben ook toegang tot de edit-tools, en kunnen dus nieuwe streams opknippen en
        zo, en hebben dus toegang tot het 'Bewerken' tabje. Zij krijgen een account van een beheerder.
      </dd>
      <dt>Beheerders</dt>
      <dd>
        Beheerders kunnen niewe gebruikers en nieuwe categorie&euml;n maken. Zij hebben ook toegang tot een
        'security' tabje. Ze kunnen hier ook categorieën 'weggeven' aan een andere security contex,en
         nieuwe categori&euml; cre&euml;eren. In de 'account' editor valt er meer informatie te
        vinden over wat een 'security context' is.
      </dd>
    </dl>
  </p>

</body>
</html>
</mm:cloud>
</mm:content>