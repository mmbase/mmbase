<% response.setContentType("text/javascript"); %>
/**
 * list.jsp
 * Routines for NewFromList
 * 
 * @since    MMBase-1.9
 * @version  $Id: newfromlist.jsp,v 1.1 2007-04-23 14:08:32 michiel Exp $
 */

function doMySearch(el) {
    var searchfields = document.forms[0].elements["realsearchfield"].value;
    var searchtype = "like";

    if (searchfields=="number") searchtype = "equal";

    var searchage = document.forms[0].elements["age"].value;

    var searchterm = document.forms[0].elements["searchvalue"].value+"";

    if (searchtype=="like") searchterm = searchterm.toLowerCase();

    // recalculate age
    if (searchage == -1){
        searchage = 99999;
    }

    var newfromlist = "<%=request.getParameter("newfromlist")%>";

    var tmp=newfromlist.split(",");

    var nodepath   = tmp[2];
    var lastobject = tmp[2];

    var fields = "";
    var cs = searchfields.split(",");
    var constraints = "(";
    for (var i=0; i<cs.length; i++) {
        if (i>0) {
            constraints += " OR ";
            fields += ",";
        }
        var fieldname=cs[i];
        if (fieldname.indexOf(".")!=-1 ) fieldname = fieldname.substring(fieldname.indexOf(".")+1,fieldname.length);

        if (searchtype=="like") {
            constraints += "LOWER("+fieldname+") LIKE '%25"+searchterm+"%25'";
        } else {
            if (searchterm=="") searchterm="0";
            constraints += fieldname+" = '"+searchterm +"'";
        }
        fields += fieldname;
    }
    constraints += ")";



    // build url
    var url="<%= response.encodeURL("list.jsp")%>?proceed=true&popupid=search&replace=true&referrer=<%=java.net.URLEncoder.encode(request.getParameter("referrer"),"UTF-8")%>&template=xsl/newfromlist.xsl&nodepath="+nodepath+"&fields="+fields+"&pagelength=10&language=<%=request.getParameter("language")%>&country=<%=request.getParameter("country")%>&timezone=<%=request.getParameter("timezone")%>";
    url += setParam("newfromlist",newfromlist);
    url += setParam("constraints", constraints);
    url += setParam("age", searchage+"");

    showPopup(url);
}

function setParam(name, value) {
    if (value!="" && value!=null) return "&"+name+"="+value;
    return "";
}

function showPopup(url) {
	var left = (screen.width - 400)/2;
	var top = (screen.height - 400)/2;
	var windowPopup = window.open("","Search", "width=400,height=400,left=" + left + ",top=" + top + "scrollbars=yes,toolbar=no,status=yes,resizable=yes");
	try {
		windowPopup.document.writeln('<span>...searching...</span>');
	} catch (e) {
		windowPopup.close();
	}
   	windowPopup.document.location.replace(url);
}


function buildSelectedList() {
    var s = selected + ",";
    var f = document.forms[0];
    for (var i=0; i<f.elements.length; i++) {
        var e = f.elements[i];
        if (e.type.toLowerCase() == "checkbox") {
            if (e.checked) {
                // Add it if it's not already in the list.
                if (s.indexOf("," + e.getAttribute("did") + ",") == -1) s += e.getAttribute("did") + ",";
            } else {
                // Remove it if it's not selected anymore.
                var pos = s.indexOf("," + e.did);
                if (pos > -1) s = s.substring(0,pos) + s.substring(pos + e.did.length + 1);
            }
        }
    }
    return s.substring(0, s.length - 1);;
}

