// -*- mode: javascript; -*-
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"  %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:bundle basename="org.mmbase.searchrelate.resources.searchrelate">
<mm:content type="text/javascript" expires="0">

/**
 * This javascript binds to a div.list.
 *
 * This div is suppose to contain an <ol> with <a class="delete" />, and a <a class="create" />
 *
 * Items in the list can be added and deleted. They can also be edited (with validation).
 * The user does not need to push a commit button. All data is implicitely committed (after a few second of inactivity, or before unload).
 *
 * Custom events (called on the associated div)
 * -  mmsrRelatedNodesReady
 * -  mmsrCreated
 *
 * @author Michiel Meeuwissen
 * @version $Id: List.js.jsp,v 1.31 2008-10-30 12:50:22 michiel Exp $
 */


$(document).ready(function() {
    var l = List; // hoping to make IE a bit faster
    $(document).find("div.list").each(function() {
        if (this.list == null) {
            this.list = new l(this);
        }
    });
    $(document).find("div.list:last").each(function() {
        l.seq = $(this).find("input[name = 'seq']")[0].value;
    });
});




function List(d) {
    this.div = d;
    var self = this;

    this.callBack = null; // called on delete and create


    var listinfos  = this.findByClass(this.div, "listinfo");

    this.type      = listinfos.find("input[name = 'type']")[0].value;
    this.item      = listinfos.find("input[name = 'item']")[0].value;
    this.source    = listinfos.find("input[name = 'source']")[0].value;
    this.icondir   = listinfos.find("input[name = 'icondir']")[0].value;
    this.createpos = listinfos.find("input[name = 'createpos']")[0].value;

    this.lastCommit = null;

    this.defaultStale = 1000;

    this.valid = true;
    this.validator = new MMBaseValidator();

    this.validator.prefetchNodeManager(this.type);
    this.validator.setup(this.div);
    this.validator.validateHook =  function(valid, element) {
        self.valid = valid;
        self.lastChange = new Date();
        if (self.lastCommit == null && element == null) {
            self.lastCommit = self.lastChange;
        }
    };
    $.timer(1000, function(timer) {
        self.commit();
    });

    this.find(this.div, "a.create").each(function() {
        self.bindCreate(this);
    });
    this.find(this.div, "a.delete").each(function() {
        self.bindDelete(this);
    });


    $(window).bind("beforeunload",
                   function(ev) {
                       var result = self.commit(0, true);
                       if (!result) {
                           ev.returnValue = '<fmt:message key="invalid" />';
                       }
                       return result;
                   });
    // automaticly make the entries empty on focus if they evidently contain the default value only
    $(this.div).find("input[type='text']").filter(function() {
        return this.value.match(/^<.*>$/); }).one("focus", function() {
            this.value = "";
            self.validator.validateElement(this);
        });
    this.setTabIndices();
    $(this.div).trigger("mmsrRelatedNodesReady", [self]);
}

List.prototype.leftPage = false;

/**
 * Finds all elements with given node name and class, but ignores everything in a child div.list.
 */
List.prototype.find = function(el, selector, result) {
    if (result == null) {
        result = [];
    }
    var self = this;
    var childNodes = el.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        var childNode = childNodes[i];
        var cn = childNode.nodeName.toUpperCase();
        if (cn == '#TEXT' || cn == 'OPTION' || (cn == 'DIV' && $(childNode).hasClass("list"))) {

        } else {
            if ($(childNode).filter(selector).length > 0) {
                result[result.length] = childNode;
            }
            self.find(childNode, selector, result);
        }
    }
    return $(result);
}
List.prototype.findByClass = function(el, clazz, result) {
    if (result == null) {
        result = [];
    }
    var self = this;
    var childNodes = el.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        var childNode = childNodes[i];
        var cn = childNode.nodeName;
        if (cn == '#text' || cn== 'option' || (cn == 'div' && $(childNode).hasClass("list"))) {

        } else {
            if ($(childNode).hasClass(clazz)) {
                result[result.length] = childNode;
            }
            self.findByClass(childNode, clazz, result);
        }
    }
    return $(result);
}




/**
 * Effort to get the browsers tab-indices on a logical order
 * Not sure that this works nice.
 */
List.prototype.setTabIndices = function() {
    var i = 0;
    $(this.div).find("input").each(function() {
        this.tabIndex = i;
        i++;
    });
    $(this.div).find("a").each(function() {
        this.tabIndex = i;
        i++;
    });
}

List.prototype.bindCreate = function(a) {
    a.list = this;
    $(a).click(function(ev) {
        var url = a.href;
        var params = {};
        if (this.item != undefined) {
            params.item   = this.item;
        }
        if (this.source != undefined) {
            params.source = this.source;
        }
        params.createpos = this.parentNode.list.createpos;

        $.ajax({async: false, url: url, type: "GET", dataType: "xml", data: params,
                complete: function(res, status){
                    try {
                        if ( status == "success" || status == "notmodified" ) {
                            var r = $(res.responseText)[0];
                            // remove default value on focus
                            $(r).find("input").one("focus", function() {
                                this.value = "";
                                a.list.validator.validateElement(this);
                            });
                            if (params.createpos == 'top') {
                                a.list.find(a.list.div, "ol").prepend(r);
                            } else {
                                a.list.find(a.list.div, "ol").append(r);
                            }
                            a.list.validator.addValidation(r);
                            a.list.find(r, "a.delete").each(function() {
                                a.list.bindDelete(this);
                            });
                            $(r).find("* div.list").each(function() {
                                var div = this;
                                if (div.list == null) {
                                    div.list = new List(div);
                                }
                            });
                            a.list.executeCallBack("create", r); // I think this may be deprecated. Custom events are nicer
                            $(a.list.div).trigger("mmsrCreated", [r]);

                        } else {
                            alert(status + " with " + url);

                        }
                    } catch (ex) {
                        alert(ex);
                    }

                }
               });
        return false;
    });
}

List.prototype.bindDelete = function(a) {
    a.list = this;
    $(a).click(function(ev) {
        var really = true;
        if ($(a).hasClass("confirm")) {
            $($(a).parents("li")[0]).addClass("highlight");
            really = confirm('<fmt:message key="really" />');
            $($(a).parents("li")[0]).removeClass("highlight");
        }
        if (really) {
            var url = a.href;
            var params = {};
            $.ajax({async: true, url: url, type: "GET", dataType: "xml", data: params,
                    complete: function(res, status){
                        if ( status == "success" || status == "notmodified" ) {
                            var li = $(a).parents("li")[0];
                            a.list.validator.removeValidation(li);
                            var ol = $(a).parents("ol")[0];
                            ol.removeChild(li);
                            a.list.executeCallBack("delete", li);
                        }
                    }
                   });
        }
        return false;
    });

}

List.prototype.executeCallBack = function(type, element) {
    if (this.callBack != null) {
        this.callBack(self, type, element);
    } else {
    }

}

List.prototype.needsCommit = function() {
    return this.lastChange != null &&
        (this.lastCommit == null || this.lastCommit.getTime() < this.lastChange.getTime());
}

List.prototype.status = function(message, fadeout) {
    this.find(this.div, "span.status").each(function() {
        if (this.originalTextContent == null) this.originalTextContent = this.textContent;
        $(this).fadeTo("fast", 1);
        $(this).empty();
        $(this).append(message);
        if (fadeout) {
            var p = this;
            $(this).fadeTo(4000, 0.1, function() { $(p).empty(); $(p).append(p.originalTextContent); } );
        }
    });
}

/**
 * @param stale Number of millisecond the content may be aut of date. Defaults to 5 s. But on unload it is set to 0.
 */
List.prototype.commit = function(stale, leavePage) {
    var result;
    if(this.needsCommit()) {
        if (this.valid) {
            var now = new Date();
            if (stale == null) stale = this.defaultStale; //
            if (now.getTime() - this.lastChange.getTime() > stale) {
                this.lastCommit = now;
                var params = {};
                params.item   = this.item;
                params.seq    = this.seq;
                params.source = this.source;
                params.icondir = this.icondir;
                params.createpos = this.createpos;
                params.leavePage = leavePage ? true : false;

                this.find(this.div, "input[checked], input[type='text'], input[type='hidden'], input[type='password'], option[selected], textarea")
                .each(function() {
                    params[this.name || this.id || this.parentNode.name || this.parentNode.id ] = this.value;
                });

                var self = this;
                this.status("<img src='${mm:link('/mmbase/style/ajax-loader.gif')}' />");
                $.ajax({ type: "POST",
                         async: leavePage == null ? true : !leavePage,
                         url: "${mm:link('/mmbase/searchrelate/list/save.jspx')}",
                         data: params,
                         complete: function(req, textStatus) {
                             self.status('<fmt:message key="saved" />', true);
                         }
                       });

                result = true;
            } else {
                // not stale enough
                result = false;
            }
        } else {
            result = false;
        }
    } else {
        result = true;
    }
    if (leavePage && ! List.prototype.leftPage) {
        List.prototype.leftPage = true;
        $.ajax({ type: "GET", async: false, url: "${mm:link('/mmbase/searchrelate/list/leavePage.jspx')}" });
    }
    return result;
}




</mm:content>
</fmt:bundle>
