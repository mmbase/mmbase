// -*- mode: javascript; -*-
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"  %>
<mm:content type="text/javascript" expires="3600">
/**
 * See test.jspx for example usage.

 * new MMBaseValidator(window, root): attaches events to all elements in root when loading window.
 * new MMBaseValidator(window): attaches events to all elements in window when loading window.
 * new MMBaseValidator():       attaches no events yet. You could replace some functions, add hooks, set settings first or so.
 *                              then call validator.setup(window[,root]).
 *
 * @author Michiel Meeuwissen
 * @version $Id: validation.js.jsp,v 1.39 2007-10-03 16:40:16 michiel Exp $
 */
var validators = new Array();


function watcher() {
    for (var i = 0; i < validators.length; i++) {
	var validator = validators[i];
	var el = validator.activeElement;
	var now = new Date().getTime();
        if (el != null) {
            if (! el.serverValidated) {
		if (new Date(validator.checkAfter + el.lastChange.getTime()) < now) {
                    validators[i].validateElement(validators[i].activeElement, true);
		}
            }
        }
    }
    setTimeout("watcher()", 150);

}
setTimeout("watcher()", 500);

function MMBaseValidator(w, root) {

    this.logEnabled   = false;
    this.traceEnabled = false;

    this.dataTypeCache   = new Object();
    this.invalidElements = 0;
    this.elements        = new Array();
    this.validateHook;

    this.setup(w);
    this.root = root;
    this.lang;
    this.id = validators.push(this);
    this.activeElement = null;
    this.checkAfter    = 600;
}

MMBaseValidator.prototype.setup = function(w) {
    if (w != null) {
        addEventHandler(w, "load", this.onLoad, this);
    }
}


MMBaseValidator.prototype.onLoad = function(event) {
    if (this.root == null) {
        this.root = event.target || event.srcElement;
    }
    this.addValidation(this.root);
    //validatePage(target);
}



MMBaseValidator.prototype.log = function (msg) {
    if (this.logEnabled) {
        // firebug console"
        console.log(msg);
    }
}

MMBaseValidator.prototype.trace = function (msg) {
    if (this.traceEnabled && this.logEnabled) {
        console.log(msg);
    }
}

/**
* Returns the mmbase node number associated with the given input element. Or null, if there is
 * no such node, or the node is not yet created.
*/
MMBaseValidator.prototype.getNode = function(el) {
    return this.getDataTypeKey(el).node;
}

/**
 * Whether a restriction on a certain input element mus be enforced.
 */
MMBaseValidator.prototype.enforce = function(el, enf) {
    this.log("ENformce " + enf);
    if (enf == 'never') return false;
    if (enf == 'always') return true;
    if (enf == 'absolute') return true;
    if (enf == 'oncreate') return  this.getNode(el) == null;
    if (enf == 'onchange') return  this.getNode(el) == null || this.isChanged(el);
}

MMBaseValidator.prototype.isChanged = function(el) {
    return this.getValue(el) != el.originalValue;
}


/**
 * Whether the element is a 'required' form input
 */
MMBaseValidator.prototype.isRequired = function(el) {
    if (el.mm_isrequired != null) return el.mm_isrequired;
    var re = this.getDataTypeXml(el).selectSingleNode('//dt:datatype/dt:required');
    el.mm_isrequired = "true" == "" + re.getAttribute("value");
    el.mm_isrequired_enforce = re.getAttribute("enforce");
    return el.mm_isrequired;
}

/**
 * Whether the value in the form element obeys the restrictions on length (minLength, maxLength, length)
 */
MMBaseValidator.prototype.lengthValid = function(el) {
    if (! this.isRequired(el) && this.enforce(el, el.mm_isrequired_enforce) && this.getValue(el).length == 0) return true;
    var xml = this.getDataTypeXml(el);

    if (el.mm_minLength_set == null) {
        var ml =  xml.selectSingleNode('//dt:datatype/dt:minLength');
        if (ml != null) {
            el.mm_minLength = ml.getAttribute("value");
            el.mm_minLength_enforce = ml.getAttribute("enforce");
        }
        el.mm_minLength_set = true;
    }
    if (el.mm_minLength != null && el.value.length < el.mm_minLength) {
        return false;
    }

    if (el.mm_maxLength_set == null) {
        var ml =  xml.selectSingleNode('//dt:datatype/dt:maxLength');
        if (ml != null) {
            el.mm_maxLength = ml.getAttribute("value");
            el.mm_maxLength_enforce = ml.getAttribute("enforce");
        }
        el.mm_maxLength_set = true;
    }

    if (el.mm_maxLength != null && el.value.length > el.mm_maxLength) {
        return false;
    }

    if (el.mm_length_set == null) {
        var l =  xml.selectSingleNode('//dt:datatype/dt:length');
        if (l != null) {
            el.mm_length = l.getAttribute("value");
            el.mm_length_enforce = l.getAttribute("enforce");
        }
        el.mm_length_set = true;
    }

    if (el.mm_length != null && el.value.length != el.mm_length) {
        return false;
    }
    return true;
}

// much much, too simple
MMBaseValidator.prototype.javaScriptPattern = function(javaPattern) {
    try {
        var flags = "";
        if (javaPattern.indexOf("(?i)") == 0) {
            flags += "i";
            javaPattern = javaPattern.substring(4);
        }
        if (javaPattern.indexOf("(?s)") == 0) {
            //this.log("dotall, not supported");
            javaPattern = javaPattern.substring(4);
            // I only hope this is always right....
            javaPattern = javaPattern.replace(/\./g, "(.|\\n)");
        }
        javaPattern = javaPattern.replace(/\\A/g, "\^");
        javaPattern = javaPattern.replace(/\\z/g, "\$");

        var reg = new RegExp(javaPattern, flags);
        return reg;
    } catch (ex) {
        this.log(ex);
        return null;
    }
}

MMBaseValidator.prototype.patternValid = function(el) {
    if (this.isString(el)) {
        var xml = this.getDataTypeXml(el);
        if (el.mm_pattern == null) {
            var javaPattern = xml.selectSingleNode('//dt:datatype/dt:pattern').getAttribute("value");
            el.mm_pattern = this.javaScriptPattern(javaPattern);
            if (el.mm_pattern == null) return true;
            this.log("pattern : " + el.mm_pattern + " " + el.value);
        }
        return el.mm_pattern.test(el.value);
    } else {
        return true;
    }
}

MMBaseValidator.prototype.hasJavaClass = function(el, javaClass) {
    var pattern = new RegExp(javaClass);
    var xml = this.getDataTypeXml(el);
    var javaClassElement = xml.selectSingleNode('//dt:datatype/dt:class');
    var name = javaClassElement.getAttribute("name");
    if (pattern.test(name)) {
        return true;
    }
    var ex = javaClassElement.getAttribute("extends");
    var javaClasses = ex.split(",");
    for (i = 0; i < javaClasses.length; i++) {
        if (pattern.test(javaClasses[i])) {
            return true;
        }
    }
    //this.log("" + el + " is not numeric");
    return false;
}

/**
 * Whether the form element represents a numeric value. There is made no difference between float,
 * double, integer and long. This means that we don't care about loss of precision only.
 */
MMBaseValidator.prototype.isNumeric = function(el) {
    if (el.mm_isnumeric != null) return el.mm_isnumeric;
    el.mm_isnumeric = this.hasJavaClass(el, "org\.mmbase\.datatypes\.NumberDataType");
    return el.isnumeric;
}
MMBaseValidator.prototype.isInteger = function(el) {
    if (el.mm_isinteger != null) return el.mm_isinteger;
    el.mm_isinteger = this.hasJavaClass(el, "(org\.mmbase\.datatypes\.IntegerDataType|org\.mmbase\.datatypes\.LongDataType)");
    return el.mm_isinteger;
}
MMBaseValidator.prototype.isFloat = function(el) {
    if (el.mm_isfloat != null) return el.mm_isfloat;
    el.mm_isfloat = this.hasJavaClass(el, "(org\.mmbase\.datatypes\.FloatDataType|org\.mmbase\.datatypes\.DoubleDataType)");
    return el.mm_isfloat;
}
MMBaseValidator.prototype.isString = function(el) {
    if (el.mm_isstring != null) return el.mm_isstring;
    el.mm_isstring =  this.hasJavaClass(el, "org\.mmbase\.datatypes\.StringDataType");
    return el.mm_isstring;
}

MMBaseValidator.prototype.isDateTime = function(el) {
    if (el.mm_isdatetime != null) return el.mm_isdatetime;
    el.mm_isdatetime = this.hasJavaClass(el, "org\.mmbase\.datatypes\.DateTimeDataType");
    return el.mm_isdatetime;
}

MMBaseValidator.prototype.INTEGER = /^[+-]?\d+$/;

MMBaseValidator.prototype.FLOAT   = /^[+-]?(\d+|\d+\.\d*|\d*\.\d+)(e[+-]?\d+|)$/i;

MMBaseValidator.prototype.typeValid = function(el) {
    if (el.value == "") return true;

    if (this.isInteger(el)) {
        if (! this.INTEGER.test(el.value)) return false;
    }
    if (this.isFloat(el)) {
        if (! this.FLOAT.test(el.value)) return false;
    }
    return true;

}



/**
 * Small utility to just get the dom attribute 'value', but also parse to float, if 'numeric' is true.
 */
MMBaseValidator.prototype.getValueAttribute = function(numeric, el) {
    if (el == null) return null;
    var value = el.getAttribute("value");
    var eval = el.getAttribute("eval");
    if (! eval == "") value = eval;

    if (numeric) {
        if (value == "") return null;
        return parseFloat(value);
    } else {
        return value;
    }
}

/**
 * Whether the value of the given form element satisfies possible restrictions on minimal and
 * maximal values. This takes into account whether it is a numeric value, which is quite important
 * for this.
 */
MMBaseValidator.prototype.minMaxValid  = function(el) {
    this.trace("validating : " + el);
    try {
        var xml   = this.getDataTypeXml(el);
        var value = this.getValue(el);
        var numeric = this.isNumeric(el);
        {
            if (el.mm_minInc_set == null) {
                var minInclusive = xml.selectSingleNode('//dt:datatype/dt:minInclusive');
                el.mm_minInc = this.getValueAttribute(numeric, minInclusive);
                el.mm_minInc_enforce = minInclusive != null ? minInclusive.getAttribute("enforce") : null;
                el.mm_minInc_set = true;
            }
            this.log("" + value + " < " + el.mm_minInc  + " " + this.enforce(el, el.mm_minInc_enforce));
            if (el.mm_minInc != null && this.enforce(el, el.mm_minInc_enforce) && value <  el.mm_minInc) {

                return false;
            }
        }

        {
            if (el.mm_minExcl_set == null) {
                var minExclusive = xml.selectSingleNode('//dt:datatype/dt:minExclusive');
                el.mm_minExcl = this.getValueAttribute(numeric, minExclusive);
                el.mm_minExcl_enforce = minExclusive != null ? minExclusive.getAttribute("enforce") : null;
                el.mm_minExcl_set = true;
            }
            if (el.mm_minExcl != null && this.enforce(el, el.mm_minExcl_enforce) && value <=  el.mm_minExcl) {
                this.log("" + value + " <= " + el.mm_minInc);
                return false;
            }
        }
        {
            if (el.mm_maxInc_set == null) {
                var maxInclusive = xml.selectSingleNode('//dt:datatype/dt:maxInclusive');
                el.mm_maxInc = this.getValueAttribute(numeric, maxInclusive);
                el.mm_maxInc_enforce = maxInclusive != null ? maxInclusive.getAttribute("enforce") : null;
                el.mm_maxInc_set = true;
            }
            if (el.mm_maxInc != null && this.enforce(el, el.mm_maxInc_enforce) && value >  el.mm_maxInc) {
                this.log("" + value + " > " + el.mm_maxInc);
                return false;
            }
        }

        {
            if (el.mm_maxExcl_set == null) {
                var maxExclusive = xml.selectSingleNode('//dt:datatype/dt:maxExclusive');
                el.mm_maxExcl = this.getValueAttribute(numeric, maxExclusive);
                el.mm_maxExcl_enforce = maxExclusive != null ? maxExclusive.getAttribute("enforce") : null;
                el.mm_maxExcl_set = true;
            }
            if (el.mm_maxExcl != null && this.enforce(el, el.mm_maxExcl_enforce) && value >=  el.mm_maxExcl) {
                this.log("" + value + " >= " + el.mm_maxExcl);
                return false;
            }
        }
    } catch (ex) {
        this.log(ex);
        throw exsdf
    }
    return true;

}


/**
 * Given a certain form element, this returns an XML representing its mmbase Data Type.
 * This will do a request to MMBase, unless this XML was cached already.
 */
MMBaseValidator.prototype.getDataTypeXml = function(el) {
    var key = this.getDataTypeKey(el);
    if (el.mm_key == null) {
        el.mm_key = key.string();
    }
    var dataType = this.dataTypeCache[el.mm_key];
    if (dataType == null) {

        var xmlhttp = new XMLHttpRequest();
        xmlhttp.open("GET", '<mm:url page="/mmbase/validation/datatype.jspx" />' + this.getDataTypeArguments(key), false);
        xmlhttp.send(null);
        dataType = xmlhttp.responseXML;
        try {
            dataType.setProperty("SelectionNamespaces", "xmlns:dt='http://www.mmbase.org/xmlns/datatypes'");
            dataType.setProperty("SelectionLanguage", "XPath");
        } catch (ex) {
            // happens in safari
        }
        this.dataTypeCache[el.mm_key] = dataType;
    }
    return dataType;
}


function Key() {
    this.node = null;
    this.nodeManager = null;
    this.field = null;
    this.datatype = null;
}
Key.prototype.string = function() {
    return this.dataType + "," + this.field + "," + this.nodeManager;
}

/**
 * Given an element, returns the associated MMBase DataType as a structutre. This structure has three fields:
 * field, nodeManager and dataType. Either dataType is null or field and nodeManager are null. They
 * are all null if the given element does not contain the necessary information to identify an
 * MMBase DataType.
 */
MMBaseValidator.prototype.getDataTypeKey = function(el) {
    if (el == null) return;
    if (el.mm_dataTypeStructure == null) {
        var classNames = el.className.split(" ");
        var result = new Key();
        for (var i = 0; i < classNames.length; i++) {
            var className = classNames[i];
            if (className.indexOf("mm_dt_") == 0) {
                result.dataType = className.substring(6);
            } else if (className.indexOf("mm_f_") == 0) {
                result.field = className.substring(5);
            } else if (className.indexOf("mm_nm_") == 0) {
                result.nodeManager = className.substring(6);
            } else if (className.indexOf("mm_n_") == 0) {
                result.node = className.substring(5);
            }

        }
        this.log("got " + result);
        el.mm_dataTypeStructure = result;
    }
    return el.mm_dataTypeStructure;
}


/**
 * Fetches all fields of a certain nodemanager at once (with one http request), and fills the cache
 * of 'getDataTypeXml'. The intention is that you call this method if you're sure that all (or a lot
 * of) the fields of a certain nodemanager will be on the page.  Otherwise a new http request will
 * be done for every field.
 *
 */
MMBaseValidator.prototype.prefetchNodeManager = function(nodemanager) {
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.open("GET", '<mm:url page="/mmbase/validation/datatypes.jspx" />?nodemanager=' + nodemanager, false);
    xmlhttp.send(null);
    var dataTypes = xmlhttp.responseXML;

    var fields = dataTypes.documentElement.childNodes;
    for (var i = 0; i < fields.length; i++) {
        var key = new Key();
        key.nodeManager = nodemanager;
        key.field = fields[i].getAttribute("name");
        var xml = Sarissa.getDomDocument();

        try {
            xml.setProperty("SelectionNamespaces", "xmlns:dt='http://www.mmbase.org/xmlns/datatypes'");
            xml.setProperty("SelectionLanguage", "XPath");
        } catch (ex) {
            // happens in safari
        }
        xml.appendChild(fields[i].firstChild.cloneNode(true));

        this.dataTypeCache[key.string()] = xml;
    }

}


/**
 * All server side JSP's with which this javascript talks, can run in 2 modes. They either accept the
 * one 'datatype' parameter, or a 'field' and a 'nodemanager' parameters.
 * The result of {@link #getDataTypeKey} serves as input, and returned is a query string which can
 * be appended to the servlet path.
 */
MMBaseValidator.prototype.getDataTypeArguments = function(key) {
    if (key.dataType != null) {
        return "?datatype=" + key.dataType;
    } else {
        return "?field=" + key.field + "&nodemanager=" + key.nodeManager;
    }
}


/**
 * If it was determined that a certain form element was or was not valid, this function
 * can be used to set an appropriate css class, so that this status also can be indicated to the
 * user using CSS.
 */
MMBaseValidator.prototype.setClassName = function(valid, el) {
    this.trace("Setting classname on " + el);
    if (el.originalClass == null) el.originalClass = el.className;
    el.className = el.originalClass + (valid ? " valid" : " invalid");
}

MMBaseValidator.prototype.hasClass = function(el, searchClass) {
    var pattern = new RegExp("(^|\\s)" + searchClass + "(\\s|$)");
    return pattern.test(el.className);
}

MMBaseValidator.prototype.getValue = function(el) {
    if (this.isDateTime(el)) {
        return  this.getDateValue(el);
    } else {
        var value = el.value;
        if( this.isNumeric(el)) {
            value = parseFloat(value);
        }

        return el.value;
    }
}

MMBaseValidator.prototype.getDateValue = function(el) {
    if (this.hasClass(el, "mm_datetime")) {
        var year = 0;
        var month = 0;
        var day = 0;
        var hour = 0;
        var minute = 0;
        var second = 0;
        var els = el.childNodes;
        for (var  i = 0; i < els.length; i++) {
            var entry = els[i];
            if (this.hasClass(entry, "mm_datetime_year")) {
                year = entry.value;
            } else if (this.hasClass(entry, "mm_datetime_month")) {
                month = entry.value;
            } else if (this.hasClass(entry, "mm_datetime_day")) {
                day = entry.value;
            } else if (this.hasClass(entry, "mm_datetime_hour")) {
                hour = entry.value;
            } else if (this.hasClass(entry, "mm_datetime_minute")) {
                minute = entry.value;
            } else if (this.hasClass(entry, "mm_datetime_second")) {
                second = entry.value;
            }

        }
        var date = new Date(year, month - 1, day, hour , minute, second, 0);
        this.log("date " + date);
        return date.getTime() / 1000;
    } else {
        return el.value;
    }

}

/**
 * Returns whether a form element contains a valid value. I.e. in a fast way, validation is done in
 * javascript, and therefore cannot be absolute.
 */
MMBaseValidator.prototype.valid = function(el) {
    var value = this.getValue(el);

    if (typeof(value) == 'undefined') {
        this.log("Unsupported element " + el);
        return true; // not yet supported
    }

    if (this.isRequired(el) && this.enforce(el, el.mm_isrequired_enforce)) {
        if (value == "") {
            return false;
        }
    } else {
        if (value == "") return true;
    }
    if (! this.typeValid(el)) return false;
    if (! this.lengthValid(el)) return false;
    if (! this.minMaxValid(el)) return false;
    if (! this.patternValid(el)) return false; // not perfect yet
    // @todo of course we can go a bit further here.

    // datetime validation is still broken. (those can have more fields and so on)

    // enumerations: but must of the time those would have given dropdowns and such, so it's hardly
    // possible to enter wrongly.
    //


    return true;
}

/**
 * Determins whether a form element contains a valid value, according to the server.
 * Returns an XML containing the reasons why it would not be valid.
 * @todo make asynchronous.
 */
MMBaseValidator.prototype.serverValidation = function(el) {
    if (el == null) return;
    try {
        var key = this.getDataTypeKey(el);
        var xmlhttp = new XMLHttpRequest();
        var value = this.getDateValue(el);
        xmlhttp.open("GET",
                     '<mm:url page="/mmbase/validation/valid.jspx" />' +
                     this.getDataTypeArguments(key) +
                     (this.lang != null ? "&lang=" + this.lang : "") +
                     "&value=" + value +
                     (key.node != null ? ("&node=" + key.node) : "") +
                     "&changed=" + this.isChanged(el),
                     false);
        xmlhttp.send(null);
        el.serverValidated = true;
        return xmlhttp.responseXML;
    } catch (ex) {
        this.log(ex);
        throw ex;
    }
}

/**
 * The result of {@link #serverValidation} is parsed, and converted to a simple boolean
 */
MMBaseValidator.prototype.validResult = function(xml) {
    try {
        return "true" == "" + xml.selectSingleNode('/result/@valid').nodeValue;
    } catch (ex) {
        this.log(ex);
        throw ex;
    }
}

/**
 * Cross browser hack.
 */
MMBaseValidator.prototype.target = function(event) {
    return event.target || event.srcElement;
}
/**
 * The event handler which is linked to form elements
 * A 'validateHook' is called in this function, which you may want to set, in stead of
 * overriding this function.
 */
MMBaseValidator.prototype.validate = function(event, server) {
    this.log("event" + event + " on " + this.target(event));
    var target = this.target(event);
    if (this.hasClass(target, "mm_validate")) {
        this.validateElement(target, server);
    } else if (this.hasClass(target.parentNode, "mm_validate")) {
        this.validateElement(target.parentNode, server);
    }
}

MMBaseValidator.prototype.serverValidate = function(event) {
    this.validate(event, true);
}


MMBaseValidator.prototype.validateElement = function(element, server) {
    var valid;
    this.log("Validating " + element);
    this.activeElement = element;
    if (server) {
        var serverXml = this.serverValidation(element);
        valid = this.validResult(serverXml);
        if (element.id) {
            var errorDiv = document.getElementById("mm_check_" + element.id.substring(3));
            errorDiv.className = valid ? "mm_check_noerror" : "mm_check_error";
            if (errorDiv) {
                Sarissa.clearChildNodes(errorDiv);
                var errors = serverXml.documentElement.childNodes;
                for (var  i = 0; i < errors.length; i++) {
                    var span = document.createElement("span");
                    span.textContent = errors[i].textContent;
                    errorDiv.appendChild(span);
                }
            }
        }
    } else {
        element.serverValidated = false;
	element.lastChange = new Date();
        valid = this.valid(element);
    }
    if (valid != element.prevValid) {
        if (valid) {
            this.invalidElements--;
        } else {
            this.invalidElements++;
        }
    }
    element.prevValid = valid;
    this.setClassName(valid, element);
    if (this.validateHook) {
        this.validateHook(valid, element);
    }
}

/**
 * Validates al mm_validate form entries which were marked for validation with addValidation.
 */
MMBaseValidator.prototype.validatePage = function(server) {
    var els = this.elements;
    for (var  i = 0; i < els.length; i++) {
        var entry = els[i];
        this.validateElement(entry, server);
    }
    return this.invalidElements == 0;
}

/**
 * Adds event handlers to all mm_validate form entries
 */
MMBaseValidator.prototype.addValidation = function(el) {
    if (el == null) {
        el = document.documentElement;
    }
    this.log("Will validate " + el);

    var els = getElementsByClass(el, "mm_validate");
    for (var i = 0; i < els.length; i++) {
        var entry = els[i];
        if (entry.type == "textarea") {
            entry.value = entry.value.replace(/^\s+|\s+$/g, "");
        }
        // switch stolen from editwizards, not all cases are actually supported already here.
        switch(entry.type) {
        case "text":
        case "textarea":
            addEventHandler(entry, "keyup", this.validate, this);
            addEventHandler(entry, "change", this.validate, this);
            addEventHandler(entry, "blur", this.serverValidate, this);
            // IE calls this when the user does a right-click paste
            addEventHandler(entry, "paste", this.validate, this);
            // FireFox calls this when the user does a right-click paste
            addEventHandler(entry, "input", this.validate, this);
            break;
        case "radio":
        case "checkbox":
            addEventHandler(entry, "click", this.validate, this);
            addEventHandler(entry, "blur", this.serverValidate, this);
            break;
        case "select-one":
        case "select-multiple":
        default:
            this.log("Adding eventhandler to " + entry);
            addEventHandler(entry, "change", this.validate, this);
            addEventHandler(entry, "blur", this.serverValidate, this);
        }

        entry.originalValue = this.getValue(entry);
        var valid = this.valid(entry);
        entry.prevValid = valid;
        this.elements.push(entry);
        this.setClassName(this.valid(entry), entry);
        if (!valid) {
            this.invalidElements++;
        }
        if (this.validateHook) {
            this.validateHook(valid, entry);
        }

    }
    el = null;
}


</mm:content>
