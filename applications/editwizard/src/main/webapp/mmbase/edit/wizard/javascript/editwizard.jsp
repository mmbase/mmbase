/** -*- mode: java -*-
 * editwizard.jsp
 * Routines for refreshing the edit wizard,
 * interaction between form elements, navigation,
 * and validation (in validator.js)
 *
 * @since    MMBase-1.6
 * @version  $Id$
 * @author   Kars Veling
 * @author   Pierre van Rooden
 * @author   Nico Klasens
 */
<% response.setContentType("text/javascript"); %>
var form = null;

function doOnLoad_ew() {
    //signal the form hasn't been submitted yet
    document.forms[0].hasBeenSubmitted = false;

    //set variables
    form = document.forms["form"];

    //scan form fields
    for (var i=0; i<form.elements.length; i++) {

        var elem = form.elements[i];


        // set date fields to clientside now time if a new wizard.
        var superId = elem.getAttribute("super");
        if (superId != null) {
          var superElement = form[superId];

          var dttype = superElement.getAttribute("dttype");
          var ftype = superElement.getAttribute("ftype");
          var id = superElement.name;

          if (dttype == "datetime") {
            if (superElement.getAttribute("new") == "true") {
              var d = new Date();

              if (elem.name == "internal_" + id + "_day") {
                if (elem.selectedIndex) {
                  elem.selectedIndex = d.getDate() - 1;
                } else {
                  elem.value = d.getDate();
                }
              }
              if (elem.name == "internal_" + id + "_month") {
                elem.selectedIndex = d.getMonth();
              }
              if (elem.name == "internal_" + id + "_year") {
                var y = d.getFullYear();
                if (y <= 0) y--;
                elem.value = y;
              }

              if (elem.name == "internal_" + id + "_hours") {
                elem.selectedIndex = d.getHours();
              }
              if (elem.name == "internal_" + id + "_minutes") {
                elem.selectedIndex = d.getMinutes();
              }
              if (elem.name == "internal_" + id + "_seconds") {
                elem.selectedIndex = d.getSeconds();
              }
            }
          }
        }

        //handle complex data types
        var dttype = elem.getAttribute("dttype");
        var ftype = elem.getAttribute("ftype");
        if (dttype != "" && ftype != "") {
          initializeElement(elem, dttype, ftype);
        }
    }

    setTimeout('heartbeat()',60*1000);

    resizeEditTable();
    restoreScroll();
}


// function to initialize a custom element
function initializeElement(elem, dttype, ftype) {
}

function doOnUnLoad_ew() {
// This code is still here to document not to use the onunload event handler
// Since 1.7 a new wysiwyg editor (HTMLArea) is installed which replaces the
// onunload handler with one of his own. It is hard to override that one,
// because a timer is used to wait a while before attaching it.
// In short, DON'T USE OR OVERRIDE THIS FUNCTION.
}


//********************************
// COMMAND STUFF
//********************************

function doHelp() {
    var w=window.open("","Help", "width=350 height=400 scrollbars=yes toolbar=no statusbar=no resizable=yes");
    try {
        var str=document.getElementById("help_text").innerHTML;

        w.document.writeln('<html><head>');
        w.document.writeln('<link rel="stylesheet" href="../style/layout/help.css">');
        w.document.writeln('<link rel="stylesheet" href="../style/color/help.css">');
        w.document.writeln('</head><body>');
        w.document.writeln(str);
        w.document.writeln('</body></html>');
    } catch (e) {
        w.close();
    }
}

function doSearch(el, cmd, sessionkey) {
    doCheckHtml();
    // most of this is probably better to just pass to list.jsp...
    var searchfields = document.forms[0].elements["searchfields_" + cmd].value;
    var searchtype = document.forms[0].elements["searchtype_" + cmd].value;
    if (searchtype=="") searchtype="like";
    var searchage = -1;
    if (document.forms[0].elements["searchage_" + cmd]) {
       searchage = new Number(document.forms[0].elements["searchage_" + cmd].value);
    }
    var searchterm = document.forms[0].elements["searchterm_" + cmd].value+"";

    if (searchtype=="like") searchterm = searchterm.toLowerCase();
    if (searchtype=="string" || searchtype=="like") {
        searchterm = searchterm.replace("'","''");
    }

    var filterrequired = el.getAttribute("filterrequired");
    if (filterrequired=="true" && searchterm=="") {
        var form = document.forms["form"];
        var errmsg=form.getAttribute("filter_required")
        if (errmsg==null || errmsg=="") {
            errmsg="Entering a search term is required";
        }
        alert(errmsg);
        return;
    } // 11948878

    // recalculate age
    if (searchage == -1){
        searchage = 99999;
    }

    var startnodes = el.getAttribute("startnodes");
    var nodepath   = el.getAttribute("nodepath");
    var fields     = el.getAttribute("fields");
    var constraints= el.getAttribute("constraints");
    var orderby    = el.getAttribute("orderby");
    var directions = el.getAttribute("directions");
    var searchdir = el.getAttribute("searchdir");
    var distinct   = el.getAttribute("distinct");
    var main  = el.getAttribute("main");
    var pagelength = el.getAttribute("pagelength");
    if (!pagelength) {
        pagelength = 10;
    }

    // lastobject is generally the last builder in the nodepath or the builder in 'main'.
    // however, if the first field is a "<buildername>.number" field, that buildername is used

    var lastobject = main;
    if (lastobject == null) lastobject = "";
    if (lastobject == "") {
        var tmp = nodepath.split(",");
        if (tmp.length > 1) {
            lastobject = tmp[tmp.length-1];
            tmp = fields.split(",");
            if (tmp.length > 1 && tmp[0].indexOf(".number") != -1) {
                lastobject = tmp[0].split(".")[0];
            }
        }
    }

    // check constraints
    var cs = searchfields.split("|");
    if (constraints!="" && constraints) var constraints = "("+constraints+") AND (";
    else constraints = "(";
    for (var i=0; i<cs.length; i++) {
        if (i>0) constraints += " OR ";
        var fieldname=cs[i];
        if (fieldname.indexOf(".")==-1 && lastobject!="") fieldname = lastobject+"."+fieldname;

        if (searchtype=="string") {
            constraints += fieldname+" = '%"+searchterm+"%'";
        } else if (searchtype=="like") {
            var commaloc = fieldname.indexOf(',');
            while (commaloc > -1) {
               var tmpfield = fieldname.substring(0, commaloc);
               fieldname = fieldname.substring(commaloc + 1, fieldname.length);
               constraints += "LOWER(["+tmpfield+"]) LIKE '%"+searchterm+"%' OR ";
               commaloc = fieldname.indexOf(',');
            }
            constraints += "LOWER(["+fieldname+"]) LIKE '%"+searchterm+"%'";
        } else {
            if (searchterm=="") searchterm="0";
            if (searchtype=="greaterthan") {
                constraints += fieldname + " > " + searchterm;
            } else if (searchtype=="lessthan") {
                constraints += fieldname + " < " + searchterm;
            } else if (searchtype=="notgreaterthan") {
                constraints += fieldname + " <= "+searchterm;
            } else if (searchtype=="notlessthan") {
                constraints += fieldname + " >= "+searchterm;
            } else if (searchtype=="notequals") {
                constraints += fieldname+" != "+searchterm;
            } else { // equals
                constraints += fieldname+" = "+searchterm;
            }
        }
        // make sure these fields are added to the fields-param, but not if its the number field
        //
        //if (fields.indexOf(fieldname)==-1 && fieldname.indexOf("number")==-1) {
        //    fields += "," + fieldname;
        //}
    }
    constraints += ")";

    // build url
    var url="<%= response.encodeURL("list.jsp")%>?proceed=true&popupid=search&replace=true&referrer=<%=java.net.URLEncoder.encode(request.getParameter("referrer"),"UTF-8")%>&listtype=search&nodepath="+nodepath+"&fields="+fields+"&pagelength="+pagelength+"&language=<%=request.getParameter("language")%>&country=<%=request.getParameter("country")%>&timezone=<%=request.getParameter("timezone")%>";
    url += setParam("searchvalue", searchterm);
    url += setParam("sessionkey", sessionkey);
    url += setParam("startnodes", startnodes);
    url += setParam("constraints", escape(constraints));
    url += setParam("orderby", orderby);
    url += setParam("directions", directions);
    url += setParam("searchdir", searchdir);
    url += setParam("distinct", distinct);
    url += setParam("main", main);
    url += setParam("age", searchage+"");
    url += setParam("type", el.getAttribute("type"));

    url += setParam("relationOriginNode", el.getAttribute("relationOriginNode"));
    url += setParam("relationRole", el.getAttribute("relationRole"));
    url += setParam("relationCreateDir", el.getAttribute("relationCreateDir"));

    url += "&cmd=" + cmd;

    showSearchScreen(cmd, url);
}

function showSearchScreen(cmd, url) {
    alert("Add the searchwindow.js or searchiframe.js to the wizard.xsl");
}

function doStartWizard(fieldid,dataid,wizardname,objectnumber,origin) {
    doCheckHtml();
    saveScroll();

    var fld = document.getElementById("hiddencmdfield");
    fld.name = "cmd/start-wizard/"+fieldid+"/"+dataid+"/"+objectnumber+"/"+origin+"/";
    fld.value = wizardname;
    document.forms[0].submit();
}

function doGotoForm(formid) {
    doCheckHtml();
    saveScroll();

    var fld = document.getElementById("hiddencmdfield");
    fld.name = "cmd/goto-form//"+formid+"//";
    fld.value = "";
    document.forms[0].submit();
}

/**
 * The delaying of commands is here, because without the delay the browser wont have the cycles to hide the buttons
 */
function doSendDelayedCommand(command) {
	setTimeout("doSendCommand(\""+command+"\")", 10);
}

function doSendCommand(cmd, value) {
    doCheckHtml();
    var fld = document.getElementById("hiddencmdfield");
    fld.name = cmd;
    fld.value = "";
    if (value) fld.value = value;
    document.forms[0].submit();
}

function doAdd(s, cmd) {
    saveScroll();
    if (!s || (s == "")) return;
    doSendCommand(cmd, s);
}

function doAddInline(cmd) {
    saveScroll();
    doSendCommand(cmd);
}

function doRemove(cmd) {
    saveScroll();
    doSendCommand(cmd);
}

function doMoveUp(cmd) {
    saveScroll();
    doSendCommand(cmd);
}

function doMoveDown(cmd) {
    saveScroll();
    doSendCommand(cmd);
}

function doCancel() {
    clearScroll();
    setButtonsInactive();
    doSendDelayedCommand("cmd/cancel////");
}

function doSave() {
    doCheckHtml();
    if (!isSaveInactive()) {
        clearScroll();
        setButtonsInactive();
        doSendDelayedCommand("cmd/commit////");
    }
}

function doSaveOnly() {
    doCheckHtml();
    if (!isSaveInactive()) {
        saveScroll();
        setButtonsInactive();
        doSendDelayedCommand("cmd/save////");
    }
}

function isSaveInactive() {
    var savebut = document.getElementById("bottombutton-save");
    if(savebut != null) {
    	return (savebut.getAttribute("inactive") == 'true');
	}
	else {
		return false;
	}
}

function setSaveInactive(inactive) {
    var savebut = document.getElementById("bottombutton-save");
    if(savebut != null) {
    	savebut.setAttribute("inactive", inactive);
	}
}

function doRefresh() {
    saveScroll();
    doSendDelayedCommand("","");
}

function doStartUpload(el) {
    var href = el.getAttribute("href");
    window.open(href,null,"width=300,height=300,status=yes,toolbar=no,titlebar=no,scrollbars=no,resizable=no,menubar=no,top=100,left=100");

    return false;
}

//********************************
// MISC STUFF
//********************************

function resizeEditTable() {
    var divButtonsHeight = document.getElementById("commandbuttonbar").offsetHeight;
    var divTop = findPosY(document.getElementById("editform"));

    var isIE = (navigator.appVersion.indexOf('MSIE')!=-1);

    if (isIE && (navigator.appVersion.indexOf('Mac')!=-1)) {

      // IE on the Mac has some overflow problems.
      // These statements will move the button div to the right position and
      // resizes the editform div.
      var docHeight = getDimensions().documentHeight;
      document.getElementById("editform").style.height = docHeight - (divTop + divButtonsHeight);
      // The div is relative positioned to the surrounding table.
      // +10, because we have a padding of 10 in the css.
      document.getElementById("commandbuttonbar").style.top = docHeight - (2*divButtonsHeight + 10);
    }
    else {
       var docHeight = getDimensions().windowHeight;
       document.getElementById("editform").style.height = docHeight - (divTop + divButtonsHeight);
    }
    var docWidth = getDimensions().windowWidth;
    document.getElementById("editform").style.width = docWidth-((isIE)?5:0);

    var textareas = document.getElementsByTagName("textarea");
    for (var i = 0 ; i < textareas.length ; i++) {
        if(isSubEditElement(textareas[i])) {
	        textareas[i].style.width = docWidth - 405;
	     }
	     else {
        	textareas[i].style.width = docWidth -150;
	     }
    }
}

function isSubEditElement(element) {
	if(element.className == "itemcanvas") {
		return true;
	}
	else if(element.parentNode == undefined) {
		return false;
	}
	else {
		return isSubEditElement(element.parentNode);
    }
}

function setParam(name, value) {
    if (value!="" && value!=null) return "&"+name+"="+value;
    return "";
}

function setButtonsInactive() {
   for (i = 0; i < document.links.length; i++) {
      if (document.links[i].id.substr(0,"bottombutton-".length)=="bottombutton-") {
         var button = document.links[i];
         button.style.visibility = "hidden";
      }
   }
}

function doCheckHtml() {
}

//********************************
// ITEMROW STUFF
//********************************
function objMouseOver(el) {
   el.className="itemrow-hover";
}

function objMouseOut(el) {
   el.className="itemrow";
}

function objClick(el) {
   var href = el.getAttribute("href")+"";
   var target = el.getAttribute("target")+"";

   if (href.length<10) return;
   if (target == "_blank") {
      window.open(href,"");
   }
   else {
      document.location=href;
   }
}

//********************************
// SCROLLBAR STUFF
//********************************
function restoreScroll() {
    var wizardinstance = document.forms[0].getAttribute("wizardinstance");
    var id = document.forms[0].getAttribute("id");
    var st = readCookie_general(wizardinstance, id, 0);
    if (st != 0) {
        document.getElementById("editform").scrollTop = st;
    } else {
        setFocusOnFirstInput();
    }
}

function saveScroll() {
    var wizardinstance = document.forms[0].getAttribute("wizardinstance");
    var id = document.forms[0].getAttribute("id");
    var scrollTop = document.getElementById("editform").scrollTop;
        writeCookie_general(wizardinstance, id, scrollTop);
}

function clearScroll() {
    var wizardinstance = document.forms[0].getAttribute("wizardinstance");
    clearCookie_general(wizardinstance);
}

function setFocusOnFirstInput() {
    var form = document.forms["form"];
    for (var i=0; i < form.elements.length; i++) {
        var elem = form.elements[i];
        // find first editable field
        var hidden = elem.getAttribute("type"); //.toLowerCase();
        if (hidden != "hidden") {
            // It is very annoying when you want to scroll with a wheel mouse
            // when you open a wizard and the selectbox is the first field.
            if (elem.getAttribute('ftype') != 'enum') {
                elem.focus();
                break;
            }
        }
    }
}

//********************************
// UNUSED STUFF AT THE MOMENT
//********************************
// debug method
function showAllProperties(el, values) {
    var s = "";
    for (e in el) {
        s += e;
        if (values) s += "["+el[e]+"]";
        s += ", ";
    }
    alert(s);
}
