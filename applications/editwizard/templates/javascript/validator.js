/**
 * validator.jsp
 * Routines for validating the edit wizard form
 *
 * @since    MMBase-1.6
 * @version  $Id: validator.js,v 1.10 2002-11-06 10:26:31 michiel Exp $
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

function validateElement_validator(el, silent) {
    var form = document.forms[0];
    var superId = el.getAttribute("super");
    if (superId != null) {
        el = form[superId];
    }
    var id = el.name;
    // form.elements["debug"].value = "validating: " + id + "\n" + form.elements["debug"].value;
    var v = getValue_validator(el);
    var err = "";

    if (el.dtpattern) {
        var re = new RegExp(el.dtpattern);
        if (!v.match(re)) err += getToolTipValue(form,'message_pattern', "the value {0} does not match the required pattern", v);
    }

    // determine datatype
    var dttype = el.getAttribute("dttype");
    //form.elements["debug"].value = "dttype: " + dttype + "\n" + form.elements["debug"].value;
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

			var date = new Date();
			date.setMonth(month, day);
			date.setFullYear(year);
			date.setHours(hours, minutes);
			
            var ms = date.getTime();
			
            //form.elements["debug"].value = ms + "\n" + form.elements["debug"].value;;

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
            if (err.length == 0) {
                form.elements[id].value = Math.round(ms/1000);
				 // - (60*d.getTimezoneOffset()));
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

