/**
 * Javascript to mold default html input widgets (e.g. as made by <mm:fieldinfo type="input" />) to things regularly requested by customers.
 *
 * @TODO It would be nice if some of these methods (like 'enumerationSuggestion' for non-enforces enumeration) be called automatically.
 *        Currently the molding process must be bootstrapped manually, per input box.

 * Supported are
 *  -  Widgets.instance.enumerationSuggestion(selector):  Makes single selection only a suggestion, meaning that the value 'OTHER' gives the user the possibility to type a value herself
 *  -  Widgets.instance.boxes(selector):  Makes select into a list of checkboxes (multiple) or radio boxes (single)
 *  -  Widgets.instance.twoMultiples(selector):  Splits up multiple selection into 2 boxes, the left one containing the selected values, the right one the optiosn which are not selected.

 *  -  Widgets.instance.labelsToInputs(selector):  Select a bunch of 'labels'. The text of the label will be put as value of the associated text-input, and removed on focus. The label itself will be hidden.
 *
 * @version $Id$   BETA
 * @author Michiel Meeuwissen

 */




function Widgets() {
}

Widgets.instance = new Widgets();

/**
 * This function is used by {@link $enumerationSuggestion}.
 */

Widgets.prototype.switchEnumerationSuggestion = function(ev) {
    var target = ev.target;
    if ('OTHER' == target.value) {
        var text = $("<input type='text'> </input>");
        var t = $(target);
        t.after(text);
        text.attr('class', t.attr('class'));
        text.attr('id', t.attr('id'));
        text.attr('name', t.attr('name'));
        text.attr('value', $(target.options[target.selectedIndex]).text());
        t.remove();
        text[0].original = target;
        text.keyup(function(ev) {
            if (ev.target.value == '') {
                var t = ev.target;
                setTimeout(function() {
                    if (t.value == '') {
                        t.original.selectedIndex = 0;
                        $(t).after(t.original);
                        $(t).remove();
                        $(t.original).change(Widgets.prototype.swichEnumerationSuggestion);
                    }
                }, 2000);
            }
        });
    }
};


/**
 * Makes a select only a suggestion. If the user selects the option with value 'OTHER', the select is
 * automatically changed into a text input box. (and back if this input box is made empty and left that way for 2 seconds).
 */
Widgets.prototype.enumerationSuggestion = function(selector) {
    $(document).ready(function() {
        $(selector).change(Widgets.prototype.switchEnumerationSuggestion);
    });
};


/**
 * Utility function to just convert an Object to a comma separated list
 */
Widgets.prototype.setToString = function(set) {
    var v = "";
    for (var i in set) {
        if (set[i] == true) {
            if (v.length > 0) v += ",";
            v += i;
        }
    }
    return v;
};

Widgets.prototype.singleBoxes = function(select, min, max) {
    var t = $(select);
    var text = document.createElement("div");
    text.className = "mm_boxes";
    text.setAttribute("id", t.attr("id"));
    if (min) {
        text.appendChild(document.createTextNode(min));
    }
    var first = true;
    for (var i = 0; i < select.options.length; i++) {
        var option = select.options[i];
        if (! $(option).hasClass("head")) {
            var nobr = document.createElement('nobr');
            var input;
            try {
                // This is just for IE. IE sucks incredibly, since it does not support basic DOM manipulation,
                // and we have to use this convoluted trick, which would even throw an exception in other browsers.
                // JQuery doesn't help either, with this.
                input = document.createElement("<input type='radio'  name='" + t.attr('name') + "' " +
                                               (option.selected ? "checked='checked'" : "") +
                                               " value='" +   option.value + "' />");
            } catch(err) {
                input = document.createElement("input");
                input.setAttribute("type",  "radio");
                input.setAttribute("name",  t.attr('name'));
                if (option.selected) {
                    input.setAttribute("checked", option.selected);
                }
                input.setAttribute("value",  option.value);
            }

            nobr.appendChild(input);
            $(nobr).addClass("index" + i);
            if (! min) {
                nobr.appendChild(document.createTextNode($(option).text()));
            }
            text.appendChild(nobr);
            first = false;
        } else if ($(option).text() == "--") {
            if (! first) {
                text.append("<br />");
            }
        } else {
            var span = $("<span class='head' />");
            text.append(span);
            span.text($(option).text());
            first = false;

        }
    }
    if (max) {
        text.appendChild(document.createTextNode(max));
    }
    t.after(text);
    t.remove();
};

Widgets.prototype.multipleBoxes = function(select) {
    var t = $(select);
    var text = $("<div class='mm_boxes' />");
    text.attr("id", t.attr("id"));
    text.addClass(t.attr("class"));
    var hidden = $("<input type='hidden' />");
    text.append(hidden);
    hidden.attr("name", t.attr("name"));
    hidden[0].values = new Object();
    var first = true;
    var div = $("<div />");
    text.append(div);
    var options = select.options;
    for (var i = 0; i < options.length; i++) {
        var opt = options[i];
        try {
            if (! $(opt).hasClass("head")) {
                var nobr = $("<nobr />");
                nobr.addClass(t.attr('name'));
                nobr.addClass($(opt).attr('class'));
                var input = $("<input type='checkbox' value='" + opt.value + "' " + (opt.selected ? "checked='checked'" : "") + " />");
                input.attr('name', t.attr('name') + "___" + opt.value);
                if (opt.selected) {
                    hidden[0].values[opt.value] = true;
                }
                nobr.append(input);
                nobr.append($(opt).text());
                div.append(nobr);
                input.change(function() {
                    hidden[0].values[this.value] = this.checked;
                    hidden[0].value = Widgets.prototype.setToString(hidden[0].values);

                });
                first = false;
            } else if ($(opt).text() == "--") {
                if (! first) {
                    div.append("<br />");
                }
            } else {
                if (! first) {
                    div = $("<div />");
                    text.append(div);
                }
                var span = $("<span class='head' />");
                div.append(span);
                span.text($(opt).text());
                first = false;
            }
        } catch(err) {
            //console.log(err);
        }
    }
    hidden.attr("value", Widgets.prototype.setToString(hidden[0].values));
    t.after(text);
    t.remove();
};

/**
 * Molds a select input to a list of checkboxes (for multiple selections) or radiobuttons (for single selections).
 */
Widgets.prototype.boxes = function(selector, multiple, min, max) {
    $(document).ready(function() {
        $(selector).each(function() {
            if (multiple || this.multiple) {
                Widgets.prototype.multipleBoxes(this);
            } else {
                Widgets.prototype.singleBoxes(this, min, max);
            }

        });
    });
};


Widgets.prototype.moveFromAToB = function(option, a, b) {
    var options = b[0].options;
    var appended = false;
    for(var i = 0; i < options.length; i++) {
        var o = options[i];
        if (o.originalPosition > option.originalPosition) {
            $(o).before(option);
            appended = true;
            break;
        }
    }
    if (! appended) {
        b.append(option);
    }
};


Widgets.prototype.twoMultiples = function(selector) {
    $(document).ready(function() {
        $(selector).each(function() {
            var select = this;
            var t = $(this);
            var text  = $("<div class='mm_twomultiples'></div>");
            var left  = $("<select multiple='multiple' />");
            left.attr("name", t.attr("name"));
            left.attr("id", t.attr("id"));
            var right = $("<select multiple='multiple' />");

            t.parents("form").submit(function() {
                for (var i = 0; i < left[0].options.length; i++) {
                    left[0].options[i].selected = true;
                }
            });
            var opts = [];
            for (var i = 0; i < select.options.length; i++) {
                var option = select.options[i];
                opts[i] = option;
                option.originalPosition = option.index;
            }
            for (var i = 0; i < opts.length; i++) {
                var option = opts[i];
                if (option.value == null || option.value == '') {
                } else if (option.selected) {
                    left.append(option);
                } else {
                    right.append(option);
                }
            }
            var nobr = $("<nobr />");
            var buttonToLeft  = $("<input type='button' value=' &lt; ' />");
            buttonToLeft.click(function() {
                for (var i = right[0].options.length - 1; i >= 0; i--) {
                    var o = right[0].options[i];
                    if (o.selected) {
                        Widgets.prototype.moveFromAToB(o, right, left);
                    }
                }
            });
            var buttonToRight = $("<input type='button' value=' &gt; ' />");
            buttonToRight.click(function() {
                for (var i = left[0].options.length - 1; i >= 0; i--) {
                    var o = left[0].options[i];
                    if (o.selected) {
                        Widgets.prototype.moveFromAToB(o, left, right);
                    }
                }
            });
            right.dblclick(function(ev) {
                var option;
                if (ev.target.tagName.toUpperCase() == 'SELECT') {
                    // Happens in ***** IE
                    option = $(ev.target).find("option[value=" + ev.target.value + "]")[0];
                } else {
                    option = ev.target;
                }
                Widgets.prototype.moveFromAToB(option, right, left);
            });
            left.dblclick(function(ev) {
                var option;
                if (ev.target.tagName.toUpperCase() == 'SELECT') {
                    // Happens in ***** IE
                    option = $(ev.target).find("option[value=" + ev.target.value + "]")[0];
                } else {
                    option = ev.target;
                }
                Widgets.prototype.moveFromAToB(option, left, right);
            });

            text.append(left).append(buttonToLeft).append(buttonToRight).append(right);
            t.after(text);
            t.remove();
        });

    });
};




Widgets.prototype.labelsToInputs = function(selector, options) {
    var emptyisuntouched = options && options['emptyisuntouched'];
    //var ignornon         = options && options['emptyisuntouched'];

    $(document).ready(
	function() {
	    $(selector).each(
		function() {
		    var labelText = $(this).text();
		    var labelFor = $(this).attr("for");
		    var input = $("#" + labelFor);
		    if ($.trim(input.val()) == "") {
			if (input.attr("type") == 'password') {
			    try {
				input.attr("type", "text");
			    } catch (e) {
				// happens in text/html FF, never mind...
				var i = $("<input type='text' value='' id='" + input.attr("id") + " name='" + input.attr('name') + " class='" + input.attr("class") + "' />");
				input.before(i);
				input.hide();
				i[0].realInput = input;
				input = i;

			    }
			    input.addClass("password");
			}
			input.val(labelText);
			input.addClass("untouched");
			$(this).css("display", "none");
			var focus = function() {
			    // if entered for the first time, remove the label value
			    if ($(this).hasClass("untouched")) {
				if (emptyisuntouched) {
				    $(this).removeClass("untouched");
				}
				this.value = "";
				if ($(this).hasClass("password")) {
				    try {
					$(this).attr("type", "password");
				    } catch (e) {
					$(this.realInput).show().focus();
					$(this).hide();
					// happens in text/html FF, never mind...

				    }
				}
			    }
			};
			input.focus(focus);
			input.select(focus);
			input.blur(
			    function() {
				// if leaving, the value is empty, and empty is equivalent to 'untouched', put the label back in.
				if ($.trim($(this).val()) == "") {
				    if (emptyisuntouched) {
					$(this).addClass("untouched");
				    }
				    if ($(this).hasClass("untouched")) {
					$(this).val(labelText);
					if ($(this).hasClass("password")) {
					    try {
						$(this).attr("type", "text");
					    } catch (e) {
						// happens in text/html FF, never mind...
					    }
					}
				    }
				}
			    });
			if (! emptyisuntouched) {
			    input.keyup(
				function() {
				    $(this).removeClass("untouched");
				});
			}
		    } else {
			// value is not empty, so cant use it for the label
		    }

		});
	});
};


