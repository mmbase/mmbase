/*<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@ taglib uri="http://www.opensymphony.com/oscache" prefix="os"
%><jsp:directive.page session="false" />
*///<mm:content type="text/javascript" expires="3600" postprocessor="none"><os:cache
time="3600" key="<%=request.getServletPath()%>"
refresh="${param.flush eq 'true' ? true : false}" ><mm:escape escape="javascript-compress">
/*
 * See test.jspx for example usage.

 * new MMBaseValidator(el): attaches events to all elements in el when ready.
 * new MMBaseValidator():       attaches no events yet. You could replace some functions, add hooks, set settings first or so.
 *                              then call validator.setup(el).
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
function MMBaseValidator(root, id) {
    this.renderTime = '<mm:time time="now" />';
    this.uniqueId     = id == null ? new Date().getTime() : id;


    this.logEnabled   = false;
    this.traceEnabled = false;

    //<mm:cloud jspvar="cloud">
      this.uri = '<%=cloud.getCloudContext().getUri()%>';
      this.cloud = '<%=cloud.getName()%>';
    //</mm:cloud>
    this.invalidElements = 0;
    //this.changedElements  = 0;
    this.elements        = [];

    this.validateHook    = null; // deprecated

    this.root = root;
    this.setup();

    this.lang          = document.querySelector("html")?.getAttribute("lang");
    this.sessionName   = null;
    this.activeElement = null;
    this.checkAfter    = 600;
    this.logArea       = "logarea";
    this.id = MMBaseValidator.validators.push(this);
    if (MMBaseValidator.validators.length === 1) {
        setTimeout(MMBaseValidator.watcher, 500);
    }
    this.saveToForm    = null;
}

// Caches
MMBaseValidator.dataTypeCache          = {};
MMBaseValidator.prefetchedNodeManagers = {};

// All instances
MMBaseValidator.validators = [];
MMBaseValidator.hasJavaClassesCache = [];

MMBaseValidator.watcher = function() {
  for (let i = 0; i < MMBaseValidator.validators.length; i++) {
    const validator = MMBaseValidator.validators[i];
    const el = validator.activeElement;
    const now = new Date().getTime();
    if (el != null) {
      if (! el.serverValidated) {
        if (el.lastChange == null) {
          el.lastChange = new Date(0);
        }
        if (new Date(validator.checkAfter + el.lastChange.getTime()) < now) {
          validator.validateElement(validator.activeElement, true, true);
        } else {
        }
      }
    }
  }
  setTimeout(MMBaseValidator.watcher, 150);
};



MMBaseValidator.prototype.setup = function(el) {
    var docReady = (fn) => {
        if (document.readyState === "complete" || document.readyState === "interactive") {
            setTimeout(fn, 0);
        } else {
            document.addEventListener("DOMContentLoaded", fn);
        }
    };

    if (el != null) {
        this.root = el;
        if (this.root === window) {
            this.root = this.root.document;
        }
    }

    if (this.root != null) {
        var self = this;
        docReady(function(event) {
            self.onLoad(event);
        });
    }
};


MMBaseValidator.prototype.onLoad = function(event) {
    if (this.root == null && event != null) {
        this.root = event.target;
    }
    //console.log("Root" + this.root);
    this.addValidation(this.root);
    //validatePage(target);
};



MMBaseValidator.prototype.log = function (msg) {
    if (this.logEnabled) {
        const errorTextArea = document.getElementById(this.logarea);
        if (errorTextArea) {
            errorTextArea.value = "LOG: " + msg + "\n" + errorTextArea.value;
        } else {
            // firebug console
            if (typeof(console) != "undefined") {
                console.log(msg);
            }
        }
    }
};

MMBaseValidator.prototype.trace = function (msg) {
    if (this.traceEnabled && this.logEnabled) {
        const errorTextArea = document.getElementById(this.logarea);
        if (errorTextArea) {
            errorTextArea.value = "TRACE: " + msg + "\n" + errorTextArea.value;
        } else {
            // firebug console
            if (typeof(console) != "undefined") {
                console.log(msg);
            }
        }
    }
};

/**
* Returns the mmbase node number associated with the given input element. Or null, if there is
 * no such node, or the node is not yet created.
*/
MMBaseValidator.prototype.getNode = function(el) {
    return this.getDataTypeKey(el).node;
};

/**
 * Whether a restriction a certain input element mus be enforced.
 */
MMBaseValidator.prototype.enforce = function(el, enf) {
    this.trace("Enforce " + enf);
    if (enf === 'never') return false;
    if (enf === 'always') return true;
    if (enf === 'absolute') return true;
    if (enf === 'onvalidate') return true;
    if (enf === 'oncreate') return  this.getNode(el) == null;
    if (enf === 'onchange') return  this.getNode(el) == null || this.isChanged(el);
};

MMBaseValidator.prototype.isChanged = function(el) {
    if (el != null) {
        return this.getValue(el) !== el.originalValue;
    } else {
        const els = this.elements;
        for (let  i = 0; i < els.length; i++) {
            const entry = els[i];
            if (this.isChanged(entry)) return true;
        }
        return false;
    }
};

/**
 * Work around http://dev.jquery.com/ticket/155
 * Actually, or course, it's a bug in that horrible browser IE.
*/
MMBaseValidator.prototype.find = function(el, path, r) {
    if (r == null) r = [];
    if (typeof(path) === "string") path = path.split(/\s+/);

    var tagName = path.shift();
    var tag = el == null ? null : el.firstChild;
    while (tag != null) {
	    if (tag.nodeType === 1) {
	        var name = tag.nodeName.replace(/^.*:/,'');
	        if (name === tagName) {
		        if (path.length === 0) {
		            r.push(tag);
		        } else {
		            this.find(tag, path, r);
		        }
	        }
	    }
	    tag = tag.nextSibling;
    }
    return r;
};


/**
 * Whether the element is a 'required' form input
 */
MMBaseValidator.prototype.isRequired = function(el) {
    if (el.mm_isrequired != null) return el.mm_isrequired;
    const re = this.find(this.getDataTypeXml(el), 'datatype required')[0];
    el.mm_isrequired = re != null && ("true" === "" + re.getAttribute("value"));
    el.mm_isrequired_enforce = re != null && re.getAttribute("enforce");
    return el.mm_isrequired;
};

MMBaseValidator.prototype.warnedUpload = false;

MMBaseValidator.prototype.showWarning = function(e) {
    if (! MMBaseValidator.prototype.warnedUpload) {
      const warning = "<div style='background-color: yellow; color: black; position: absolute; right: 0; top: 0; z-index: 2000;'>I'm sorry, your browser cannot determine the size of the upload. Please use e.g. FireFox, Safari or Chromium. Please DO NOT USE Internet Explorer. That is an unusable, crappy, sorry excuse for a browser. (" + e + ")</div>";
      const div = document.createElement("div");
      div.innerHTML = warning;
      document.body.appendChild(div);

      MMBaseValidator.prototype.warnedUpload = true;
      setTimeout(function() {
        div.classList.add("slow");
        }, 3000);
    }

};

MMBaseValidator.prototype.getLength = function(el) {
    let length;
    if (el.type === "file") {
        if (el.value === "") {
            this.getDataTypeKey(el); // set also mm_length
            length = el.mm_initial_length;
        } else {
            if (el.files == null) {
                try {
                    // We can always try.
                    // According to
                    // http://msdn.microsoft.com/en-us/library/z9ty6h50%28VS.85%29.aspx
                    // this should work.
                    // I have never seen that it actually does.
                    // IE probably just sucks too much.
                    const oas = new ActiveXObject("Scripting.FileSystemObject");
                    const file = oas.getFile(el.value);
                    length = file.length;
                } catch (e) {
                    // Out of luck, both el.files  and the silly activexobject are not working.
                    this.showWarning(e);
                    length = null;
                }
            } else {
                // most other browsers simply support the following (Note the incredible ease and simplicity, compared to the horrible shit of IE).
                if (el.files.length > 0) {
                    length = el.files.item(0).fileSize;
                } else {
                    length = 0;
                }
            }
        }
    } else {
        const value = this.getValue(el);
        if (value == null) {
            length = 0;
        } else {
            length = value.length;
        }
       }
    return length;
};

/**
 * Returns the mimetype as reported by the browser for the given file
 * upload input.
 * Probably won't work in IE.
 * If it can't be determined, this method returns the empty string.
 */
MMBaseValidator.prototype.getMimeType = function(el) {
    let type;
    if (el.type === "file") {
        if (el.value === "") {
            this.getDataTypeKey(el); // set also mm_length
            type = el.mm_initial_mimetype;
        } else {
            if (el.files == null) {
                try {
                    // We can always try.
                    // According to
                    // http://msdn.microsoft.com/en-us/library/z9ty6h50%28VS.85%29.aspx
                    // this should work.
                    // I have never seen that it actually does.
                    // IE probably just sucks too much.
                    const oas = new ActiveXObject("Scripting.FileSystemObject");
                    const file = oas.getFile(el.value);
                    type = file.type;
                } catch (e) {
                    // Out of luck, both el.files  and the silly activexobject are not working.
                    this.showWarning(e);
                    type = "";
                }
            } else {
                // most other browsers simply support the following (Note the incredible ease and simplicity, compared to the horrible shit of IE).
                if (el.files.length > 0) {
                    type = el.files.item(0).type;
                    if (type == null) {
                        // This happens sometimes in some Windows, I don't understand that.
                        type = "";
                    }
                    //type = "";
                } else {
                    //alert("Did not recognize type of " + el.files);
                    type = "";
                }
            }
        }
   } else {
        const value = this.getValue(el);
        if (value == null) {
            type = null;
        } else {
            type = "";
            //
        }
    }
    return type;
};

/**
 * Whether the value in the form element obeys the restrictions on length (minLength, maxLength, length)
 */
MMBaseValidator.prototype.lengthValid = function(el) {
    const length = this.getLength(el);
    if (this.isRequired(el) && this.enforce(el, el.mm_isrequired_enforce) && length == 0) {
        return false;
    }
    const xml = this.getDataTypeXml(el);

    if (el.mm_minLength_set == null) {
        const ml =  this.find(xml, 'datatype minLength')[0];
        if (ml != null) {
            el.mm_minLength = parseInt(ml.getAttribute("value"));
            el.mm_minLength_enforce = ml.getAttribute("enforce");
        }
        el.mm_minLength_set = true;
    }
    if (this.enforce(el, el.mm_minLength_enforce)) {
        if (el.mm_minLength != null && length < el.mm_minLength) {
            return false;
        }
    }

    if (el.mm_maxLength_set == null) {
        const ml =  this.find(xml, 'datatype maxLength')[0];
        if (ml != null) {
            el.mm_maxLength = parseInt(ml.getAttribute("value"));
            el.mm_maxLength_enforce = ml.getAttribute("enforce");
        }
        el.mm_maxLength_set = true;
    }

    if (this.enforce(el, el.mm_maxLength_enforce)) {
        if (el.mm_maxLength != null && length > el.mm_maxLength) {
            return false;
        }
    }

    if (el.mm_length_set == null) {
        const l =  this.find(xml, 'datatype length')[0];
        if (l != null) {
            el.mm_length = parseInt(l.getAttribute("value"));
            el.mm_length_enforce = l.getAttribute("enforce");
        }
        el.mm_length_set = true;
    }

    if (this.enforce(el, el.mm_length_enforce)) {
        if (el.mm_length != null && length != el.mm_length) {
            return false;
        }
    }
    return true;
};

// much much, too simple
MMBaseValidator.prototype.javaScriptPattern = function(javaPattern) {
    try {

        // This code tries to translate a java regexp to a javascript regexp.
        var flags = "";
        if (javaPattern.indexOf("(?i)") === 0) {
            flags += "i";
            javaPattern = javaPattern.substring(4);
        }
        if (javaPattern.indexOf("(?s)") === 0) {
            //this.log("dotall, not supported");
            javaPattern = javaPattern.substring(4);
            // I only hope this is always right....
            javaPattern = javaPattern.replace(/\./g, "(.|\\n)");
        }
        javaPattern = javaPattern.replace(/\\A/g, "\^");
        javaPattern = javaPattern.replace(/\\z/g, "\$");
        javaPattern = javaPattern.replace(/\\p\{L\}/, "a-zA-Z");

        const reg = new RegExp(javaPattern, flags);
        return reg;
    } catch (ex) {
        this.log(ex);
        return null;
    }
};

MMBaseValidator.prototype.patternValid = function(el) {
    if (this.isString(el)) {
      if (! this.isRequired(el)) {
        if (el.value === "" || el.value == null) return true;
      }
        const xml = this.getDataTypeXml(el);
        if (el.mm_pattern == null) {
            const javaPatternXml = this.find(xml, 'datatype pattern')[0];
            if (javaPatternXml == null) {
                alert("No pattern found for " + el.id);
                return true;
            }

            const javaPattern = javaPatternXml.getAttribute("value");
            el.mm_pattern = this.javaScriptPattern(javaPattern);
            if (el.mm_pattern == null) return true;
            this.trace("pattern : " + el.mm_pattern + " " + el.value);
        }
        return el.mm_pattern.test(el.value);
    } else {
        return true;
    }
};

MMBaseValidator.prototype.hasJavaClass = function(el, javaClass) {
    const key = this.getDataTypeKey(el).string() + ":" + javaClass;
    if (MMBaseValidator.hasJavaClassesCache[key] == null) {
        const pattern = new RegExp(javaClass);
        const xml = this.getDataTypeXml(el);
        const javaClassElement = this.find(xml, 'datatype class')[0];
        if (! javaClassElement) {
            MMBaseValidator.hasJavaClassesCache[key] = false;
            return false;
        }
        const name = javaClassElement.getAttribute("name");
        if (pattern.test(name)) {
            MMBaseValidator.hasJavaClassesCache[key] = true;
            return true;
        }
        const ex = javaClassElement.getAttribute("extends");
        const javaClasses = ex.split(",");
        for (let i = 0; i < javaClasses.length; i++) {
            if (pattern.test(javaClasses[i])) {
                MMBaseValidator.hasJavaClassesCache[key] = true;
                return true;
            }
        }
        MMBaseValidator.hasJavaClassesCache[key] = false;
        return false;
    } else {
        return MMBaseValidator.hasJavaClassesCache[key];

    }
};

/**
 * Whether the form element represents a numeric value. There is made no difference between float,
 * double, integer and long. This means that we don't care about loss of precision only.
 */
MMBaseValidator.prototype.isNumeric = function(el) {
    if (el.mm_isnumeric != null) return el.mm_isnumeric;
    el.mm_isnumeric = this.hasJavaClass(el, "org\.mmbase\.datatypes\.NumberDataType");
    return el.isnumeric;
};

MMBaseValidator.prototype.isBoolean = function(el) {
    if (el.mm_isboolean != null) return el.mm_isboolean;
    el.mm_isboolean = this.hasJavaClass(el, "org\.mmbase\.datatypes\.BooleanDataType");
    return el.isboolean;
};

MMBaseValidator.prototype.isInteger = function(el) {
    if (el.mm_isinteger != null) return el.mm_isinteger;
    el.mm_isinteger = this.hasJavaClass(el, "(org\.mmbase\.datatypes\.IntegerDataType|org\.mmbase\.datatypes\.LongDataType)");
    return el.mm_isinteger;
};

MMBaseValidator.prototype.isFloat = function(el) {
    if (el.mm_isfloat != null) return el.mm_isfloat;
    el.mm_isfloat = this.hasJavaClass(el, "(org\.mmbase\.datatypes\.FloatDataType|org\.mmbase\.datatypes\.DoubleDataType|org\.mmbase\.datatypes\.DecimalDataType)");
    return el.mm_isfloat;
};

MMBaseValidator.prototype.isString = function(el) {
    if (el.mm_isstring != null) {
        return el.mm_isstring;
    }
    el.mm_isstring =  this.hasJavaClass(el, "org\.mmbase\.datatypes\.StringDataType");
    return el.mm_isstring;
};

MMBaseValidator.prototype.isDateTime = function(el) {
    if (el.mm_isdatetime != null) return el.mm_isdatetime;
    el.mm_isdatetime = this.hasJavaClass(el, "org\.mmbase\.datatypes\.DateTimeDataType");
    return el.mm_isdatetime;
};

MMBaseValidator.prototype.isBinary = function(el) {
    if (el.mm_isbinary != null) return el.mm_isbinary;
    el.mm_isbinary = this.hasJavaClass(el, "org\.mmbase\.datatypes\.BinaryDataType");
    return el.mm_isbinary;
};

MMBaseValidator.prototype.isCheckEquality = function(el) {
    if (el.mm_ischeckequality != null) return el.mm_ischeckequality;
    el.mm_ischeckequality = this.hasJavaClass(el, "org\.mmbase\.datatypes\.CheckEqualityDataType");
    return el.ischeckequality;
};

MMBaseValidator.prototype.isXml = function(el) {
    if (el.mm_isxml != null) return el.mm_isxml;
    el.mm_isxml= this.hasJavaClass(el, "org\.mmbase\.datatypes\.XmlDataType");
    return el.mm_isxml;
};

MMBaseValidator.prototype.INTEGER = /^[+-]?\d+$/;

MMBaseValidator.prototype.FLOAT   = /^[+-]?(\d+|\d+\.\d*|\d*\.\d+)(e[+-]?\d+|)$/i;

MMBaseValidator.prototype.typeValid = function(el) {
    const value = this.getValue(el);
    if (value === "" || value == null) return true;

    if (this.isInteger(el)) {
        if (! this.INTEGER.test(el.value)) return false;
    }
    if (this.isFloat(el)) {
        if (! this.FLOAT.test(el.value)) return false;
    }
    return true;

};



/**
 * Small utility to just get the dom attribute 'value', but also parse to float, if 'numeric' is true.
 */
MMBaseValidator.prototype.getValueAttribute = function(numeric, el) {
    if (el == null) return null;

    let value = el.getAttribute("value");
    const evalled = el.getAttribute("eval");
    if (evalled && evalled !== "") value = evalled;

    if (numeric) {
        if (value === "") return null;
        return parseFloat(value);
    } else {
        return value;
    }
};

/**
 * Whether the value of the given form element satisfies possible restrictions on minimal and
 * maximal values. This takes into account whether it is a numeric value, which is quite important
 * for this.
 */
MMBaseValidator.prototype.minMaxValid  = function(el) {
    this.trace("validating : " + el);
    try {
        const xml   = this.getDataTypeXml(el);
        const value = this.getValue(el);
        const numeric = this.isNumeric(el);
        {
            if (el.mm_minInc_set == null) {
                const minInclusive = this.find(xml, 'datatype minInclusive')[0];
                el.mm_minInc = this.getValueAttribute(numeric, minInclusive);
                el.mm_minInc_enforce = minInclusive != null ? minInclusive.getAttribute("enforce") : null;
                el.mm_minInc_set = true;
            }
            this.trace("" + value + " < " + el.mm_minInc  + " " + this.enforce(el, el.mm_minInc_enforce));
            if (el.mm_minInc != null && this.enforce(el, el.mm_minInc_enforce) && value <  el.mm_minInc) {
                return false;
            }
        }

        {
            if (el.mm_minExcl_set == null) {
                const minExclusive = this.find(xml, 'datatype/ minExclusive')[0];
                el.mm_minExcl = this.getValueAttribute(numeric, minExclusive);
                el.mm_minExcl_enforce = minExclusive != null ? minExclusive.getAttribute("enforce") : null;
                el.mm_minExcl_set = true;
            }
            if (el.mm_minExcl != null && this.enforce(el, el.mm_minExcl_enforce) && value <=  el.mm_minExcl) {
                this.trace("" + value + " <= " + el.mm_minInc);
                return false;
            }
        }
        {
            if (el.mm_maxInc_set == null) {
                const maxInclusive = this.find(xml, 'datatype maxInclusive')[0];
                el.mm_maxInc = this.getValueAttribute(numeric, maxInclusive);
                el.mm_maxInc_enforce = maxInclusive != null ? maxInclusive.getAttribute("enforce") : null;
                el.mm_maxInc_set = true;
            }
            if (el.mm_maxInc != null && this.enforce(el, el.mm_maxInc_enforce) && value >  el.mm_maxInc) {
                this.trace("" + value + " > " + el.mm_maxInc);
                return false;
            }
        }

        {
            if (el.mm_maxExcl_set == null) {
                const maxExclusive = this.find(xml, 'datatype maxExclusive')[0];
                el.mm_maxExcl = this.getValueAttribute(numeric, maxExclusive);
                el.mm_maxExcl_enforce = maxExclusive != null ? maxExclusive.getAttribute("enforce") : null;
                el.mm_maxExcl_set = true;
            }
            if (el.mm_maxExcl != null && this.enforce(el, el.mm_maxExcl_enforce) && value >=  el.mm_maxExcl) {
                this.trace("" + value + " >= " + el.mm_maxExcl);
                return false;
            }
        }
    } catch (ex) {
        this.log(ex);
        throw ex;
    }
    return true;

};


/**
 * Given a certain form element, this returns an XML representing its mmbase Data Type.
 * This will do a request to MMBase, unless this XML was cached already.
 */
MMBaseValidator.prototype.getDataTypeXml = async function(el) {
    this.checkPrefetch();
    const key = this.getDataTypeKey(el);
    if (el.mm_key == null) {
        el.mm_key = key.string();
    }

    var dataType = MMBaseValidator.dataTypeCache[el.mm_key];
    if (dataType == null) {
        let url = '<mm:url page="/mmbase/validation/datatype.jspx" />';
        const params = this.getDataTypeArguments(key);

        let needsAmp = false;
        for (let p in params) {
            if (needsAmp) {
                url += "&";
            } else {
                url += "?";
            }
            url += p + "=" + params[p];
            needsAmp = true;
        }

        const self = this;
        await fetch(url)
            .then(function(response) {
                if (response.ok || response.status === 404) {
                    return response.text();
                }
                throw new Error('Network response was not ok');
            })
            .then(function(html) {
                const parser = new DOMParser();
                dataType = parser.parseFromString(html, "application/xml");
                MMBaseValidator.dataTypeCache[el.mm_key] = dataType;
                self.log("Found " + dataType);

                return dataType;
            })
            .catch(function(error) {
                console.error('Error fetching datatype XML -' + error);
            });
    } else {
        this.trace("Found in cache " + dataType);
    }
    return dataType;
};


function Key() {
    this.uri = null;
    this.cloud = null;
    this.node = null;
    this.nodeManager = null;
    this.field = null;
    this.datatype = null;
    this.origin = "field";
}
Key.prototype.string = function() {
    return this.cloud + "@" + this.uri + "," + this.dataType + "," + this.field + "," + this.nodeManager + "," + this.origin;
};

/**
 * Given an element, returns the associated MMBase DataType as a structutre. This structure has three fields:
 * field, nodeManager and dataType. Either dataType is null or field and nodeManager are null. They
 * are all null if the given element does not contain the necessary information to identify an
 * MMBase DataType.
 */
MMBaseValidator.prototype.getDataTypeKey = function(el) {
    if (el == null) return null;

    if (el.mm_dataTypeStructure == null) {
        const classNames = el.className.split(" ");
        const result = new Key();
        result.uri   = this.uri;
        result.cloud = this.cloud;
        for (var i = 0; i < classNames.length; i++) {
            var className = classNames[i];
            if (className.indexOf("mm_dt_") === 0) {
                result.dataType = className.substring(6);
	    } else if (className.indexOf("mm_dto_") === 0) {
                result.origin = className.substring(7);
            } else if (className.indexOf("mm_f_") === 0) {
                result.field = className.substring(5);
            } else if (className.indexOf("mm_nm_") === 0) {
                result.nodeManager = className.substring(6);
            } else if (className.indexOf("mm_n_") === 0) {
                result.node = className.substring(5);
            } else if (className.indexOf("mm_length_") === 0) {
                el.mm_initial_length = parseInt(className.substring(10));
            } else if (className.indexOf("mm_mimetype_") === 0) {
                el.mm_initial_mimetype = className.substring(12);
            }

        }
        if (result.field == null && result.datatype == null) {
            //console.log("FOOOOOUUTTTT");
            //console.log(el);
        }
        this.trace("got " + result.string());
        el.mm_dataTypeStructure = result;
    }
    return el.mm_dataTypeStructure;
};


/**
 * Fetches all fields of a certain nodemanager at once (with one http request), and fills the cache
 * of 'getDataTypeXml'. The intention is that you call this method if you're sure that all (or a lot
 * of) the fields of a certain nodemanager will be on the page.  Otherwise a new http request will
 * be done for every field.
 *
 */
MMBaseValidator.prototype.prefetchNodeManager = function(nodemanager) {
    if (nodemanager != null) {
        const nm = nodemanager.split(",");
        for (let i=0; i < nm.length; i++) {
            if (nm[i].length > 0) {
                const n = nm[i];
                if (MMBaseValidator.prefetchedNodeManagers[n] === "success") {
                } else {
                    MMBaseValidator.prefetchedNodeManagers[n] = "requested";
                }
            }
        }
    }
};

MMBaseValidator.prototype.checkPrefetch = async function() {
    const nodemanagers = [];

    // MMBaseValidator.prefetchedNodeManagers
    for (var k in MMBaseValidator.prefetchedNodeManagers) {
        if (MMBaseValidator.prefetchedNodeManagers[k] === "requested") {
            nodemanagers.push(k);
        }
    }

    if (nodemanagers.length > 0) {
        this.log("prefetching " + nodemanagers);
        const url = '<mm:url page="/mmbase/validation/datatypes.jspx" />';

        const params = new URLSearchParams();
        params.append("nodemanager", nodemanagers.toString());
        if (this.uri) {
            params.append("uri", this.uri);
        } else {
            params.append("uri", "local");
        }
        if (this.cloud) {
            params.append("cloud", this.cloud);
        } else {
            params.append("cloud", "mmbase");
        }

        await fetch(url + '?' + params.toString())
            .then((res) => res.text())
            .then((html) => {
                const parser = new DOMParser();
                const dataTypes = parser.parseFromString(html, "application/xml");

                const fields = dataTypes.documentElement.childNodes;
                for (let i = 0; i < fields.length; i++) {
                    const key = new Key();
                    key.uri = params.get("uri");
                    key.cloud = params.get("cloud");
                    key.nodeManager = fields[i].getAttribute("nodemanager");
                    key.field = fields[i].getAttribute("name");
                    MMBaseValidator.dataTypeCache[key.string()] = fields[i];
                    MMBaseValidator.prefetchedNodeManagers[key.nodeManager] = "success";
                }
            })
            .catch((err) => {
                console.error('Error prefetching datatypes -', err);
            });
    }
};


/**
 * All server side JSP's with which this javascript talks, can run in 2 modes. They either accept the
 * one 'datatype' parameter, or a 'field' and a 'nodemanager' parameters.
 * The result of {@link #getDataTypeKey} serves as input, and returned is a query string which can
 * be appended to the servlet path.
 */
MMBaseValidator.prototype.getDataTypeArguments = function(key) {
    if (key.dataType != null) {
        return {datatype: key.dataType, uri: key.uri, cloud: key.cloud, origin: key.origin};
    } else {
        return {field: key.field, nodemanager: key.nodeManager, uri: key.uri, cloud: key.cloud};
    }
};


/**
 * If it was determined that a certain form element was or was not valid, this function
 * can be used to set an appropriate css class, so that this status also can be indicated to the
 * user using CSS.
 */
MMBaseValidator.prototype.setClassName = function(valid, el) {
    this.trace("Setting classname on " + el);
    if (valid) {
        el.classList.remove("invalid");
        el.classList.add("valid");
    } else {
        el.classList.remove("valid");
        el.classList.add("invalid");
    }
};


MMBaseValidator.prototype.getValue = function(el) {
    if (this.isDateTime(el)) {
        return  this.getDateValue(el);
    } else {
        if (el.tagName === "div") {
            return el.innerText;
        }
        let value = el.value;

        if( this.isNumeric(el)) {
            if (value === "") {
            } else {
                value = parseFloat(value);
            }
        } else if (this.isBoolean(el)) {
            if ("checkbox" === el.type) {
                value = el.checked;
            }
        }
        return value;
    }
};


MMBaseValidator.prototype.getDateValue = function(el) {
    if (el.classList.contains("mm_datetime")) {
        let year = 0;
        let month = 0;
        let day = 0;
        let hour = 0;
        let minute = 0;
        let second = 0;
        const els = el.childNodes;
        for (let  i = 0; i < els.length; i++) {
            var entry = els[i];
            if (entry.type == null) continue;
            if (entry.classList.contains("mm_datetime_year")) {
                year = entry.value;
            } else if (entry.classList.contains("mm_datetime_month")) {
                month = entry.value;
            } else if (entry.classList.contains("mm_datetime_day")) {
                day = entry.value;
            } else if (entry.classList.contains("mm_datetime_hour")) {
                hour = entry.value;
            } else if (entry.classList.contains("mm_datetime_minute")) {
                minute = entry.value;
            } else if (entry.classList.contains("mm_datetime_second")) {
                second = entry.value;
            }

        }
        const date = new Date(year, month - 1, day, hour , minute, second, 0);
        this.trace("date " + date);
        return date.getTime() / 1000;
    } else {
        return el.value;
    }
};

/**
 * Returns whether a form element contains a valid value. I.e. in a fast way, validation is done in
 * javascript, and therefore cannot be absolute.
 */
MMBaseValidator.prototype.valid = function(el) {
    const value = this.getValue(el);

    if (typeof(value) == 'undefined') {
        this.log("Unsupported element " + el);
        return true; // not yet supported
    }
    if (this.isCheckEquality(el)) {
        return true; // not yet supported
    }

    if (! this.isString(el)) { // For Strings, you cannot enter 'null'. The empty string is interpreted as "" in
                               // stead. So skip the 'required' checks.
        if (this.isRequired(el) && this.enforce(el, el.mm_isrequired_enforce)) {
            if (this.getLength(el) <= 0 && (value === "" || value == null)) {
                return false;
            }
        } else {
            if (value === "" || value == null) {
                return true;
            }
        }
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
};



/**
 * Validation of binaries is a bit more complex, so we do it seperately here.
 */
MMBaseValidator.prototype.binaryServerValidation = function(el) {
    const key = this.getDataTypeKey(el);
    const value = this.getValue(el);

    let validationUrl = '<mm:url page="/mmbase/validation/binaryValid.jspx" />?';
    const self = this;
    const params = this.getDataTypeArguments(key);
    if (this.lang != null) {
        params.lang = this.lang;
    }
    if (this.sessionName != null) {
        params.sessionname = this.sessionName;
    }
    if (key.node != null && key.node > 0) {
        params.node = key.node;
    }

    params.fieldname = el.getAttribute("name");
    params.changed = this.isChanged(el);
    params.length = this.getLength(el);
    params.type = this.getMimeType(el);
    if (params.length == null) {
        delete params.length;
    }
    var needsAmp = false;
    for (var p in params) {
        if (needsAmp) {
            validationUrl += "&";
        }
        validationUrl += p + "=" + params[p];
        needsAmp = true;
        //form.append('<input type="hidden" name="' + p + '" value="' + params[p] + '" />');
    }
    if (params.length != null) {
        console.log('binaryServerValidation -', validationUrl);
        fetch(validationUrl)
            .then(function(response) {
                if (response.ok) {
                    return response.text();
                }
                throw new Error('Network response was not ok');
            })
            .then(function(html) {
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, "application/xml");
                el.serverValidated = true;
                self.showServerErrors(el, doc, el.initialId);
            })
            .catch(function(error) {
                self.log('binaryServerValidation error - ' + error.message);
            });
    }
};

/**
 * Determines whether a form element contains a valid value, according to the server.
 * Returns an XML containing the reasons why it would not be valid.
 */
MMBaseValidator.prototype.serverValidation = function(el) {
    if (el == null) {
        return;
    }
    try {
        if (this.isBinary(el)) {
            this.binaryServerValidation(el);
            el.serverValidated = true;
            return;
        }
        if (this.isCheckEquality(el)) { // Not yet supported
            el.serverValidated = true;
            const msg = document.querySelector('result[valid="true"].implicit_checkequality');
            this.showServerErrors(el, msg);
            return;
        }

        const self = this;
        const key = this.getDataTypeKey(el);
        const value = this.getValue(el);
        const params = this.getDataTypeArguments(key);

        let validationUrl = '<mm:url page="/mmbase/validation/valid.jspx" />';

        if (this.saveToForm != null) {
            params.form = this.saveToForm;
        }

        if (this.lang != null) params.lang = this.lang;
        if (this.sessionName != null) params.sessionname = this.sessionName;
        params.value = value;
        if (key.node != null && key.node > 0) params.node = key.node;
        params.changed = this.isChanged(el);

        const fetchUrl = validationUrl + "?" + new URLSearchParams(params);
        fetch(fetchUrl)
            .then(function(resp){ return resp.text()})
            .then(function(html) {
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, "application/xml");
                el.serverValidated = true;
                self.showServerErrors(el, doc);
            })
            .catch(function(error) {
                self.log('There has been a problem with your fetch operation: ' + error);
            });

    } catch (ex) {
        console.info("Exception in serverValidation: ", ex);
        this.log(ex);
        throw ex;
    }
};

/**
 * The result of {@link #serverValidation} is parsed, and converted to a simple boolean
 */
MMBaseValidator.prototype.validResult = function(xml) {
    try {
        if (xml.documentElement != null) {
            return "true" === xml.documentElement.getAttribute("valid");
        } else {
            return "true" === "" + xml.getAttribute("valid");
        }
    } catch (ex) {
        this.log(ex);
        throw ex;
    }
};

/**
 * Cross browser hack. We hate all browsers. Especially IE.
 */
MMBaseValidator.prototype.target = function(event) {
    return event.target || event.srcElement;
};

MMBaseValidator.prototype.getElement = function(event) {
    let target = this.target(event);
    if (target == null) target = event;
    //this.log("event " + event.type + " on " + target.id);
    if (target.classList.contains("mm_validate")) {
      return target;
    } else if (target.parentNode.classList.contains("mm_validate")) {
      return target.parentNode;
    } else {
      return null;
    }
};

/**
 * The event handler which is linked to form elements
 * A 'validateHook' is called in this function, which you may want to set, in stead of
 * overriding this function.
 */
MMBaseValidator.prototype.validate = function(event, server) {
    // this.log("event " + event.type + " on " + target.id);
    const element = this.getElement(event);
    return this.validateElement(element, server);
};

MMBaseValidator.prototype.serverValidate = function(event) {
    this.validate(event, true);
};

MMBaseValidator.prototype.showServerErrors = function(element, serverXml, id) {
    const valid = this.validResult(serverXml);
    if (!id) {
      id = element.id;
    }

    if (id) {
        const errorDiv = document.getElementById("mm_check_" + id.substring(3));
        if (errorDiv) {
            errorDiv.className = valid ? "mm_check_noerror mm_check_updated" : "mm_check_error mm_check_updated";
            if (errorDiv) {
                errorDiv.innerHTML = "";
                const errors = serverXml.documentElement ? serverXml.documentElement.childNodes : [];
                this.log("errors for " + element.id + " " +  serverXml + " " + errors.length);

                for (let i = 0; i < errors.length; i++) {
                  if (errors[i].tagName.toLowerCase() === "error") {
                    const span = document.createElement("span");
                    span.innerHTML = errors[i].innerHTML;
                    span.setAttribute("class", errors[i].getAttribute("class"));
                    errorDiv.append(span);
                  }
                }
            }
        } else {
            this.log("No error div " + "mm_check_" + id.substring(3));
        }
    } else {
      this.log("No element " + id);
    }
    this.updateValidity(element, valid, true);
};

MMBaseValidator.prototype.updateValidity = function(element, valid, server) {
    if (valid !== element.prevValid) {
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

    element.dispatchEvent(new CustomEvent("mmValidate", { detail: { validator: this, valid: valid, server: server } }));
    return valid;
};

MMBaseValidator.prototype.validateElement = function(element, server) {
    let valid;
    if (server) {
        const prevValue = element.prevValue;
        const curValue  = "" + this.getValue(element);
        if (curValue === prevValue) {
            server = false;
            element.serverValidated = true;
            // already validated, so nothing to do.
            return false;
        }
        element.prevValue = "" + curValue;
    }

    this.activeElement = element;
    if (server) {
        this.serverValidation(element);
        return null; // don't know yet.
    } else {
        valid = this.valid(element);
        this.updateValidity(element, valid, false);

        return valid;
    }
};

/**
 * Validates al mm_validate form entries which were marked for
 * validation with addValidation int this validator.
 */
MMBaseValidator.prototype.validatePage = function(server) {
    const els = this.elements;
    this.log("Validating page " + server + " (" + els.length + " elements)");
    for (let  i = 0; i < els.length; i++) {
        const entry = els[i];
        this.validateElement(entry, server);
    }
    return this.invalidElements === 0;
};

MMBaseValidator.prototype.hasElement = function(el) {
    const els = this.elements;
    for (let  i = 0; i < els.length; i++) {
        const e = els[i];
        if (el.initialId === e.initialId) {
            return true;
        }
    }
    return false;
};

MMBaseValidator.prototype.getElementsWithClass = function(className) {
    const result = [];
    const els = this.elements;
    for (let  i = 0; i < els.length; i++) {
        const e = els[i];
        if (e.classList.contains(className)) {
            result[result.length] = e;
        }
    }
    return result;
};

MMBaseValidator.prototype.getInvalidElements = function() {
    return this.getElementsWithClass("invalid");
};

MMBaseValidator.prototype.getValidElements = function() {
    return this.getElementsWithClass("valid");
};

MMBaseValidator.prototype.removeValidation = function(el) {
    if (el == null) {
        el = document.documentElement;
    }
    const self = this;
    el.querySelectorAll(".mm_validate").forEach(function(entry) {
      self.removeValidationFromElement(entry);
    });
};

MMBaseValidator.prototype.removeValidationFromElement = function(el) {
    const self = this;
    if (self.hasElement(el)) {
        if (! el.prevValid) {
            self.invalidElements--;
        }
        el.removeEventListener("change", function() {});
        const newElements = [];
        self.elements.forEach(function(elem) {
            if (el.initialId !== elem.initialId) {
                newElements.push(elem);
            }
        });
        self.elements = newElements;
    }
};



MMBaseValidator.prototype.setLastChange = function(event) {
    var target = this.getElement(event);
    target.lastChange = new Date();
    target.serverValidated = false;
};

MMBaseValidator.prototype.addValidationForElements = function(els) {
    for (let i = 0; i < els.length; i++) {
        const entry = els[i];
        if (!entry.classList.contains("mm_validate")) {
            continue;
        }

        if (entry.type === "textarea") {
            entry.value = entry.value.replace(/^\s+|\s+$/g, "");
        }
        // Store the original ID, especially for binaries, because jquery-upload may temporary change it sometimes, which would make the error div unfindable
        entry.initialId = entry.id;

        const self = this;
        // switch stolen from editwizards, not all cases are actually supported already here.
        switch(entry.type) {
        case "text":
        case "password":
        case "textarea":
            entry.addEventListener("keyup", function(ev) { self.setLastChange(ev); self.validate(ev); }, false);
            entry.addEventListener("change", function(ev) { self.serverValidate(ev); }, false);
            entry.addEventListener("blur", function(ev) { self.serverValidate(ev); }, false);
            // IE calls this when the user does a right-click paste
            entry.addEventListener("paste", function(ev) { self.setLastChange(ev); self.validate(ev); }, false);
            // FireFox calls this when the user does a right-click paste
            break;

        case "radio":
        case "checkbox":
            entry.addEventListener("click", function(ev) { self.setLastChange(ev); self.validate(ev); }, false);
            entry.addEventListener("blur", function(ev) { self.serverValidate(ev); }, false);
            entry.addEventListener("change", function(ev) { self.serverValidate(ev); }, false);
            break;
        case "file":
            entry.addEventListener("change", function(ev) { self.setLastChange(ev); self.validate(ev); }, false);
            break;
        case "select-one":
        case "select-multiple":
        default:
            this.log("Adding eventhandler to " + entry + " (" + entry.type + ")");
            this.log(entry);
            entry.addEventListener("change", function(ev) { self.setLastChange(ev); self.validate(ev); }, false);
            entry.addEventListener("blur", function(ev) { self.serverValidate(ev); }, false);
        }
        entry.serverValidated = true; // It starts out server-validated
        entry.originalValue = this.getValue(entry);
        var valid = this.valid(entry);
        entry.prevValid = valid;
        this.elements.push(entry);
        this.setClassName(valid, entry);
        if (!valid) {
            this.invalidElements++;
        }
        if (this.validateHook) {
            this.validateHook(valid, entry);
        }
    }

    if (els.length === 0) {
        if (this.validateHook) {
            this.validateHook(this.invalidElements === 0);
        }
    }
};

/**
 * Adds event handlers to all mm_validate form entries
 */
MMBaseValidator.prototype.addValidation = function(el) {
    if (el == null) {
        el = document.documentElement;
    }
    var els = el.querySelectorAll(".mm_validate");

    this.log("Will validate elements in " + el + " (" + els.length + " elements)");
    this.addValidationForElements(els);
    el = null;
};
//</mm:escape></os:cache></mm:content>
