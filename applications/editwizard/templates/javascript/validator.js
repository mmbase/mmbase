/**
 * validator.jsp
 * Routines for validating the edit wizard form
 *
 * @since    MMBase-1.6
 * @version  $Id: validator.js,v 1.14 2003-05-26 14:54:35 pierre Exp $
 * @author   Kars Veling
 * @author   Pierre van Rooden
 */

//constructor
function Validator() {
    //properties

    //methods
}

Validator.prototype.attach = attach_validator;
Validator.prototype.detach = detach_validator;
Validator.prototype.validate = validate_validator;
Validator.prototype.validateElement = validateElement_validator;
Validator.prototype.getValue = getValue_validator;
Validator.prototype.setValue = setValue_validator;

function attach_validator(element) {
    if (!element) return;
    this.validateElement(element);
}

function detach_validator(element) {
    if (!element) return;
    this.validateElement(element, true);
}

function validate_validator(event, el) {
    // called from html: when user pressed a key or leaves a field.
    if (!el) var el = event.srcElement || event.target;
    validateElement_validator(el);
    doValidateAndUpdateButtons();
}

function getToolTipValue(el,attribname,defaultvalue,param) {
    var value = el.getAttribute(attribname);
    if (value==null || value=="") value=defaultvalue;
    if (param) {
        return value.replace(/(\{0\})/g, param);
    }
    return value;
}

// Here some date-related code that we need top determine if we're living within Daylight Saving Time
//
function makeArray()    {
    this[0] = makeArray.arguments.length;
    for (i = 0; i<makeArray.arguments.length; i++)
        this[i+1] = makeArray.arguments[i];
}

var daysofmonth   = new makeArray( 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31);
var daysofmonthLY = new makeArray( 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31);

function LeapYear(year) {
    if ((year/4)   != Math.floor(year/4))   return false;
    if ((year/100) != Math.floor(year/100)) return true;
    if ((year/400) != Math.floor(year/400)) return false;
    return true;
}

function NthDay(nth,weekday,month,year) {
    if (nth > 0) return (nth-1)*7 + 1 + (7 + weekday - DayOfWeek((nth-1)*7 + 1,month,year))%7;
    if (LeapYear(year)) var days = daysofmonthLY[month];
    else                var days = daysofmonth[month];
    return days - (DayOfWeek(days,month,year) - weekday + 7)%7;
}

function DayOfWeek(day,month,year) {
    var a = Math.floor((14 - month)/12);
    var y = year - a;
    var m = month + 12*a - 2;
    var d = (day + y + Math.floor(y/4) - Math.floor(y/100) + Math.floor(y/400) + Math.floor((31*m)/12)) % 7;
    return d+1;
}

function validateElement_validator(el, silent) {
    var form = document.forms[0];
    var superId = el.getAttribute("super");
    if (superId != null) {
        el = form[superId];
    }
    var id = el.name;
    var v = getValue_validator(el);
    var err = "";

    if (el.dtpattern) {
        var re = new RegExp(el.dtpattern);
        if (!v.match(re)) err += getToolTipValue(form,'message_pattern', "the value {0} does not match the required pattern", v);
    }

    // determine datatype
    var dttype = el.getAttribute("dttype");

    switch (dttype) {
        case "string":
            minlength=el.getAttribute("dtminlength");
            if ((minlength!=null) && (minlength!="") && (v.length < 1*minlength))
                err += getToolTipValue(form,'message_minlength',
                           "value must be at least {0} characters", minlength);
            maxlength=el.getAttribute("dtmaxlength");
            if ((maxlength!=null) && (maxlength!="") && (v.length > 1*maxlength))
                err += getToolTipValue(form,'message_maxlength',
                           "value must be at most {0} characters", maxlength);
            break;
        case "int":
            if (isNaN(v) || parseInt(v)==null) err += "value '" + v + "' is not a valid integer number";
            else {
                if ((el.dtmin || (el.dtmin!=null)) && (parseInt(v) < el.dtmin))
                    err += getToolTipValue(form,'message_min',
                           "value must be at least {0}", el.dtmin);
                if ((el.dtmax || (el.dtmax!=null)) && (parseInt(v) > el.dtmax))
                    err += getToolTipValue(form,'message_max',
                           "value must be at most {0}", el.dtmax);
            }
            break;
        case "enum":
            if ((el.getAttribute("dtrequired")!=null) && (el.getAttribute("dtrequired")=="true")) {
                if (el.options[el.selectedIndex].value == "-")
                    err += getToolTipValue(form,'message_required',
                           "value is required; please select a value");
            }
            break;
        case "datetime":
            if ((el.getAttribute("ftype") == "datetime") || (el.getAttribute("ftype") == "date")) {
                var month =form.elements["internal_" + id + "_month"].selectedIndex;
                var day = form.elements["internal_" + id + "_day"].selectedIndex+1;
                var year = form.elements["internal_" + id + "_year"].value;
            } else {
                var month = 0;
                var day = 1;
                var year = 1970;
            }
            if ((el.getAttribute("ftype") == "datetime") || (el.getAttribute("ftype") == "time")) {
                var hours = form.elements["internal_" + id + "_hours"].selectedIndex;
                var minutes = form.elements["internal_" + id + "_minutes"].selectedIndex;
            } else {
                var hours = 0;
                var minutes = 0;
            }

            // We don't want -1 = 2 BC, 0 = 1 BC,  -1 = 2 BC but
            //               0 -> error, -1 = 1 BC   1 = 1 AC
            if (year == 0) {
                err += getToolTipValue(form,"message_dateformat", "date/time format is invalid");
            }
            if (year < 0 ) year++;

            var date = new Date();
            date.setMonth(month, day);
            date.setFullYear(year);
            date.setHours(hours, minutes);
			
            var ms = date.getTime();

            if (date.getDate() != day) {
                err += getToolTipValue(form,"message_dateformat", "date/time format is invalid");
            } else {
                // checks min/max. note: should use different way to determine outputformat (month)
                if ((err.length == 0) && (el.ftype != "time") && (el.dtmin != null) && (ms < 1000*el.dtmin)) {
                    var d = new Date();
                    d.setTime(1000*dateel.dtmin);
                    err += getToolTipValue(form,"message_datemin",
                           "date must be at least {0}",
                           d.getDate() + " " + months[d.getMonth()] + " " + d.getUTCFullYear());
                }
                if ((err.length == 0) && (el.dttype != "time") && (el.dtmax != null) && (ms > 1000*el.dtmax)) {
                    var d = new Date();
                    d.setTime(1000*dateel.dtmax);
                    err += getToolTipValue(form,"message_datemax",
                           "date must be at most {0}",
                           d.getDate() + " " + months[d.getMonth()] + " " + d.getUTCFullYear());
                }
            }

            // Here we'll  calculate the start and end of Daylight Saving Time
            // We need that in order to display correct date and times in IE on Macintosh
            var DSTstart = new Date(year,4-1,NthDay(1,1,4,year),2,0,0);
            var DSTend   = new Date(year,10-1,NthDay(-1,1,10,year),2,0,0);
            var DSTstartMS = Date.parse(DSTstart);
            var DSTendMS = Date.parse(DSTend);

            // If Daylight Saving Time is active and clientNavigator=MSIE/Mac, add 60 minutes 
            // 
            if ((navigator.appVersion.indexOf('MSIE')!=-1) && (navigator.appVersion.indexOf('Mac')!=-1) && (ms>DSTstartMS) && (ms<DSTendMS)) {
                if (err.length == 0) {
                    form.elements[id].value = Math.round(ms/1000-(60*60));
                    //alert(form.elements[id].value + " = " + day + " " + month + " " + year + " " + hours + ":" + minutes);
                }
            } else {
                if (err.length == 0) {
                    form.elements[id].value = Math.round(ms/1000); // - (60*d.getTimezoneOffset()));
                    //alert(form.elements[id].value + " = " + day + " " + month + " " + year + " " + hours + ":" + minutes);
                }
            }
            break;
    }

    var prompt = document.getElementById("prompt_" + id);
    if (prompt && !silent) {
        var orgprompt=prompt.getAttribute("prompt");
        var description=prompt.getAttribute("description");
        if (err.length > 0) {
            prompt.title = description+" \n\n"+getToolTipValue(form,"message_thisnotvalid",
                                  "This field is not valid")+":\n "+err;
            try {
                window.status = getToolTipValue(form,"message_notvalid",
                                "The {0} is not valid",orgprompt)+": "+err;
            } catch(e) {}

            prompt.className = "notvalid";
        } else {
            prompt.className = "valid";
            prompt.title = description;
            try {
                window.status = "";
            } catch(e) {}
        }
    }

    return err.length == 0; // true == valid, false == invalid
}

function doValidateForm() {
    var invalid=false;
    form = document.forms["form"];

        if (form.getAttribute("invalidlist")!="") {
           invalid = true;
    }

    for (var i=0; i<form.elements.length; i++) {
        var elem = form.elements[i];

        //handle complex data types
        var dttype = elem.getAttribute("dttype");
        invalid = invalid || (!validator.validateElement(elem, true));
    }
    return (!invalid);
}

function doValidateAndUpdateButtons() {
    // check if current form is valid.
    var valid = doValidateForm();
    var curform = document.forms[0].elements['curform'].value;
    var savebut = document.getElementById("bottombutton-save");
    var stepbut = document.getElementById("step-" + curform);
    var otherforms = savebut.getAttribute("otherforms");
    var allvalid = valid && otherforms == 'valid';

    if (valid) {
        if (stepbut) {
            stepbut.className = "valid";
            var usetext = getToolTipValue(stepbut,"titlevalid",
                              "The current form is valid.");
            stepbut.title = usetext;
        }
    } else {
        if (stepbut) {
            stepbut.className = "invalid";
            var usetext = getToolTipValue(stepbut,"titlenotvalid",
                              "The current form is NOT valid. Correct red-marked fields and try again.");
            stepbut.title = usetext;
        }
    }

    if (allvalid) {
        savebut.className = "bottombutton";
        var usetext = getToolTipValue(savebut,"titlesave",
                          "Stores all changes.");
        savebut.title = usetext;
    } else {
        savebut.className = "bottombutton-disabled";
        var usetext = getToolTipValue(savebut,"titlenosave",
                          "You cannot save because one or more forms are invalid.");
        savebut.title = usetext;
    }

    return allvalid;
}

function getValue_validator(el) {
    var tagname=el.tagName;
    if (!tagname) tagname = el.nodeName;
    var v;
    switch (tagname.toLowerCase()) {
        case "input":
            v = el.value;
            break;
        case "select":
            v = el.selectedIndex;
            break;
        case "textarea":
            v = el.value;
            break;
        default:
            v = el.innerHTML;
            break;
    }
    return v;
}

function setValue_validator(el, value) {
    var tagname=el.tagName;
    if (!tagname) tagname = el.nodeName;
    switch (tagname.toLowerCase()) {
        case "input","textarea":
            el.value = value;
            break;
        case "select":
            el.selectedIndex = value;
            break;
        default:
            el.innerHTML = value;
            break;
    }
}

