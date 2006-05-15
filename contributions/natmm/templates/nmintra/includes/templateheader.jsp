<base href="<%= javax.servlet.http.HttpUtils.getRequestURL(request) %>" />
<%@page import="java.text.*,java.io.*,org.mmbase.bridge.*" %><%! 

public String getParameter(String parameterStr, String queryStr) {
    String parameterValue = null;
    int qpos = queryStr.indexOf("&" + parameterStr + "=");
    if(qpos==-1) { qpos = queryStr.indexOf("?" + parameterStr + "="); }
    if(qpos>-1) {
        int vstart = queryStr.indexOf("=",qpos)+1;
        int vend = queryStr.indexOf("&",vstart);
        parameterValue = queryStr.substring(vstart,vend);
    }
    return parameterValue;
}
%><%@include file="../globals.jsp" 
%><%

String rubriekId = "";
String pageId = request.getParameter("p"); if(pageId==null){ pageId = ""; }
String refererId = request.getParameter("referer"); if(refererId==null){ refererId = ""; }
String articleId = request.getParameter("article"); if(articleId==null){ articleId = ""; }
PaginaHelper ph = new PaginaHelper(cloud);
String rootId = ph.getRootRubriek(cloud, pageId);

//String visitorGroup = request.getParameter("vg"); if(visitorGroup==null){ visitorGroup = ""; }

// IntraShop
String imageId = request.getParameter("i"); 
String shop_itemId = request.getParameter("u"); if((shop_itemId==null) || (shop_itemId.equals(""))) { shop_itemId="-1"; }
String actionId = request.getParameter("t"); if (actionId==null) {actionId=""; }
String pageUrl = "";
String totalitemsId = (String) session.getAttribute("totalitems");
if(totalitemsId==null) totalitemsId = "0";
String shop_itemHref = "";
String extendedHref = "";
NumberFormat nf = NumberFormat.getInstance(Locale.FRENCH);
nf.setMaximumFractionDigits(2);
nf.setMinimumFractionDigits(2);
boolean bShowPrices = true;
boolean bMemberDiscount = false;
boolean bExtraCosts = false;

// IntraProject
String projectId = request.getParameter("project"); if(projectId==null){ projectId = ""; }
String typeId = request.getParameter("type"); if(typeId==null) { typeId="-1"; }
String groupId = request.getParameter("group"); if(groupId==null) { groupId="-1"; }
String medewerkerId = request.getParameter("medewerker"); if(medewerkerId==null) { medewerkerId=""; }
String projectNameId = request.getParameter("projectname"); if(projectNameId==null) { projectNameId=""; }
String abcId = request.getParameter("abc"); if(abcId==null) { abcId=""; }
String termSearchId = request.getParameter("termsearch"); if(termSearchId==null) { termSearchId=""; }

// item and info
String offsetId = request.getParameter("offset"); if(offsetId==null){ offsetId=""; }
String periodId = request.getParameter("d"); if(periodId==null){ periodId =""; }

// info & producttypes
String poolId = request.getParameter("pool"); if(poolId==null){ poolId=""; }
String productId = request.getParameter("product"); if(productId==null){ productId=""; }
String locationId = request.getParameter("location"); if(locationId==null){ locationId=""; }

// smoelenboek
String employeeId = request.getParameter("employee"); if(employeeId==null) { employeeId=""; }
String departmentId = request.getParameter("department"); if(departmentId==null){ departmentId="default"; }
String programId = request.getParameter("program"); if(programId==null){ programId="default"; }
String descriptionId = request.getParameter("description"); if(descriptionId==null) { descriptionId=""; }

// smoelenboek update
String nameId = request.getParameter("name"); if(nameId==null) { nameId=""; }
String nameEntry = "voor- en/of achternaam";
String firstnameId = request.getParameter("firstname"); if(firstnameId==null) { firstnameId=""; }
String initialsId = request.getParameter("initials"); if(initialsId==null) { initialsId=""; }
String suffixId = request.getParameter("suffix"); if(suffixId==null) { suffixId=""; }
String lastnameId = request.getParameter("lastname"); if(lastnameId==null) { lastnameId=""; }
String companyphoneId = request.getParameter("companyphone"); if(companyphoneId==null) { companyphoneId=""; }
String cellularphoneId = request.getParameter("cellularphone"); if(cellularphoneId==null) { cellularphoneId=""; }
String faxId = request.getParameter("fax"); if(faxId==null) { faxId=""; }
String emailId = request.getParameter("email"); if(emailId==null) { emailId=""; }
String deptdescrId = request.getParameter("deptdescr"); if(deptdescrId==null) { deptdescrId=""; }
String progdescrId = request.getParameter("progdescr"); if(progdescrId==null) { progdescrId=""; }
String posdescrId = request.getParameter("posdescr"); if(posdescrId==null) { posdescrId=""; }
String descrupdateId = request.getParameter("descrupdate"); if(descrupdateId==null) { descrupdateId=""; }
String introupdateId= request.getParameter("introupdate"); if(introupdateId==null) { introupdateId=""; }

// prikbord (including email from smoelenboek)
String queryString = request.getQueryString()+"&";
String titleId = request.getParameter("title"); if(titleId==null) { titleId=""; }
String textId = getParameter("text",queryString); if(textId==null) { textId=""; }

// searchresults
String searchId = request.getParameter("search"); if(searchId==null) { searchId=""; }
String categoryId = request.getParameter("category"); if(categoryId==null){ categoryId = ""; }

// formulier
String postingStr = request.getParameter("pst"); if(postingStr==null) { postingStr=""; }

//IntraEducations
String keywordId = request.getParameter("k"); if(keywordId==null){ keywordId = ""; }
String providerId = request.getParameter("pr"); if(providerId==null){ providerId = ""; }
String competenceId = request.getParameter("c"); if(competenceId==null){ competenceId = ""; }
String educationId = request.getParameter("e"); if(educationId==null){ educationId = ""; }
String educationNameId = request.getParameter("en"); if(educationNameId==null) { educationNameId = ""; }

//IntraEvents
String eventId = request.getParameter("ev"); if(eventId==null){ eventId = ""; }
String eTypeId = request.getParameter("evt"); if(eTypeId==null){ eTypeId = ""; }
String pCategorieId = request.getParameter("pc"); if(pCategorieId==null){ pCategorieId = ""; }
String pAgeId = request.getParameter("pa"); if(pAgeId==null){ pAgeId = ""; }
String nReserveId = request.getParameter("nr"); if(nReserveId==null){ nReserveId = ""; }
String eDistanceId = request.getParameter("evl"); if(eDistanceId==null){ eDistanceId = ""; }
String eDurationId = request.getParameter("evd"); if(eDurationId==null){ eDurationId = ""; }

// globals
int expireTime = 0;
String cacheKey = "";
String templateTitle = "";
String uri = request.getRequestURI();
boolean isPreview = ((uri.indexOf("/dev/")!=-1)||(uri.indexOf("/editors/")!=-1));
String infopageClass = "infopage";
boolean isIPage = (uri.indexOf("ipage.jsp")!=-1);
if(isIPage) { infopageClass = "ipage"; }

// email addresses
String defaultFromAddress = "intranet@natuurmonumenten.nl";
String defaultPZAddress = "A.deBeer@Natuurmonumenten.nl";
String defaultFZAddress = "C.Koumans@natuurmonumenten.nl";
if(!isProduction) {
    defaultPZAddress = "hangyi@xs4all.nl";
    defaultFZAddress = "hangyi@xs4all.nl";
}
String emailHelpText = "<br><br>N.B. Op sommige computers binnen Natuurmonumenten is het niet mogelijk om direct op een link in de email te klikken."
                    + "<br>Als dit bij jou het geval is moet je de volgende handelingen uitvoeren:"
                    + "<br>1.open het programma Internet Explorer"
                    + "<br>2.kopieer de bovenstaande link uit deze email naar de adres balk van Internet Explorer"
                    + "<br>3.druk op de \"Enter\" toets";

%><%@include file="../includes/templatesettings.jsp"  %>
