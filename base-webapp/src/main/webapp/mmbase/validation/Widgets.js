/**
 * Javascript to mold default html input widgets (e.g. as made by <mm:fieldinfo type="input" />) to things regularly requested by customers.
 *
 * @TODO It would be nice if some of these methods (like 'enumerationSuggestion' for non-enforces enumeration) be called automaticly.
 *        Currently the moldingprocess must be bootstrapped manually, per input box.

 * Supported are
 *  -  Widgets.instance.enumerationSuggestion(selector):  Makes single selection only a suggestion, meaning that the value 'OTHER' gives the user the possibility to type a value herself
 *  -  Widgets.instance.boxes(selector):  Makes select into a list of checkboxes (multiple) or radioboxes (single)
 *  -  Widgets.instance.twoMultiples(selector):  Splits up multiple selection into 2 boxes, the left one containing the selected values, the right one the optiosn which are not selected.

 *  -  Widgets.instance.labelsToInputs(selector):  Select a bunch of 'labels'. The text of the label will be put as value of the associated text-input, and removed on focus. The label itself will be hidden.
 *
 * @version $Id$   BETA
 * @author Michiel Meeuwissen

 */


function Widgets() {
}

function domReady(fn) {
	if (document.readyState === "complete" || document.readyState === "interactive") {
		setTimeout(fn, 0);
	} else {
		document.addEventListener("DOMContentLoaded", fn);
	}
};
Widgets.instance = new Widgets();


/**
 * This function is used by {@link $enumerationSuggestion}.
 */
Widgets.prototype.switchEnumerationSuggestion = function (ev) {
	var target = ev.target;
	if ('OTHER' == target.value) {
		var textInput = document.createElement('input');
		textInput.type = "text";

		target.after(textInput);
		textInput.className = target.className;
		textInput.id = target.id;
		textInput.name = target.name;
		textInput.value = target.options[target.selectedIndex].innerText;

		textInput.original = target;
		target.remove();

		textInput.addEventListener('keyup', function (ev) {
			if (ev.target.value == "") {
				var t = ev.target;
				setTimeout(function () {
					if (t.value == "") {
						t.original.selectedIndex = 0;

						t.after(t.original);
						t.remove();
						t.original.addEventListener("change", Widgets.prototype.swichEnumerationSuggestion);
					}
				}, 2000);
			}
		});
	}
};


/**
 * Makes a select only a suggestion. If the user selects the option with value 'OTHER', the select is
 * automaticly changed into a text input box. (and back if this input box is made empty and left that way for 2 seconds).
 */
Widgets.prototype.enumerationSuggestion = function (selector) {
	const element = document.querySelector(selector);
	if (element) {
		domReady(() => element.addEventListener("change", Widgets.prototype.switchEnumerationSuggestion));
	}
};


/**
 * Utility function to just convert an Object to a comma separated list
 */
Widgets.prototype.setToString = function (set) {
	var v = "";
	for (var i in set) {
		if (set[i] == true) {
			if (v.length > 0) v += ",";
			v += i;
		}
	}
	return v;
};

Widgets.prototype.singleBoxes = function (select, min, max) {
	var text = document.createElement("div");
	text.className = "mm_boxes";
	text.setAttribute("id", select.id);

	if (min) {
		text.appendChild(document.createTextNode(min));
	}

	var first = true;
	for (var i = 0; i < select.options.length; i++) {
		var option = select.options[i];

		if (!option.classList.contains("head")) {
			var nobr = document.createElement("span");
			var input;
			try {
				// This is just for IE. IE sucks incredibly, since it does not support basic DOM manipulation,
				// and we have to use this convulated trick, which would even throw an exception in other browers.
				// JQuery doesn't help either, with this.
				input = document.createElement("input");
				input.setAttribute("type", "radio");
				input.setAttribute("name", t.attr("name"));
				if (option.selected) {
					input.setAttribute("checked", option.selected);
				}
				input.setAttribute("value", option.value);
			} catch (err) {
				input = document.createElement("input");
				input.setAttribute("type", "radio");
				input.setAttribute("name", t.attr("name"));
				if (option.selected) {
					input.setAttribute("checked", option.selected);
				}
				input.setAttribute("value", option.value);
			}

			nobr.appendChild(input);
			nobr.classList.add("index" + i);

			if (!min) {
				nobr.appendChild(document.createTextNode(option.innerText));
			}
			text.appendChild(nobr);
			first = false;
		} else if (option.innerText == "--") {
			if (!first) {
				text.append(document.createElement("br"));
			}
		} else {
			var span = document.createElement("span");
			span.className = "head";

			text.append(span);
			span.innerText = option.innerText;
			first = false;
		}
	}
	if (max) {
		text.appendChild(document.createTextNode(max));
	}
	t.after(text);
	t.remove();
};

Widgets.prototype.multipleBoxes = function (select) {
	var text = document.createElement("div");
	text.className = "mm_boxes" + ` ${select.className}`;
	text.id = select.id;

	var hidden = document.createElement("input");
	hidden.setAttribute("type", "hidden");
	hidden.setAttribute("name", select.name);
	hidden[0].values = new Object();
	text.append(hidden);

	var first = true;
	var div = document.createElement("div");
	text.append(div);

	var options = select.options;
	for (var i = 0; i < options.length; i++) {
		var opt = options[i];
		try {
			if (opt.classList.contains("head")) {
				var nobr = document.createElement("span");
				nobr.classList.add(select.getAttribute("name"), opt.getAttribute("class"));

				var input = document.createElement("input");
				input.setAttribute("type", "checkbox");
				input.setAttribute("value", opt.value);
				input.setAttribute("name", select.name + "___" + opt.value);
				if (opt.selected) {
					input.setAttribute("checked", "checked");
					hidden[0].values[opt.value] = true;
				}

				nobr.append(input);
				nobr.append(opt.innerText);
				div.append(nobr);

				input.addEventListener("change", function () {
					hidden[0].values[this.value] = this.checked;
					hidden[0].value = Widgets.prototype.setToString(hidden[0].values);
				});
				first = false;
			} else if (opt.innerText == "--") {
				if (!first) {
					var br = document.createElement("br");
					div.append(br);
				}
			} else {
				if (!first) {
					var dv = document.createElement("div");
					text.append(dv);
				}
				var span = document.createElement("span");
				span.className = "head";
				div.append(span);
				span.innerText = opt.innerText;
				first = false;
			}
		} catch (err) {
			console.log("some error occured - ", err);
		}
	}
	hidden.setAttribute("value", Widgets.prototype.setToString(hidden[0].values));
	select.after(text);
	select.remove();
};

/**
 * Molds a select input to a list of checkboxes (for multiple selections) or radiobuttons (for single selections).
 */
Widgets.prototype.boxes = function (selector, multiple, min, max) {
	domReady(function () {
		const elems = document.querySelectorAll(selector);
		elems.forEach(function (select) {
			if (multiple || select.multiple) {
				Widgets.prototype.multipleBoxes(select);
			} else {
				Widgets.prototype.singleBoxes(select, min, max);
			}
		});
	});
};


Widgets.prototype.moveFromAToB = function (option, a, b) {
	var options = b[0].options;
	var appended = false;
	for (var i = 0; i < options.length; i++) {
		var o = options[i];
		if (o.originalPosition > option.originalPosition) {
			o.before(option);
			appended = true;
			break;
		}
	}
	if (!appended) {
		b.append(option);
	}
};


Widgets.prototype.twoMultiples = function (selector) {
	domReady(function () {
		const elems = document.querySelectorAll(selector);
		elems.forEach(function (select) {
			var text = document.createElement("div");
			text.className = "mm_twomultiples";

			var left = document.createElement("select");
			left.setAttribute("multiple", "multiple");
			left.setAttribute("name", select.getAttribute("name"));
			left.setAttribute("id", select.getAttribute("id"));

			var right = document.createElement("select");
			right.setAttribute("multiple", "multiple");

			var parent = select.parentElement;
			if (parent.tagName.toLowerCase() === "form") {
				parent.addEventListener("submit", function () {
					for (var i = 0; i < left[0].options.length; i++) {
						left[0].options[i].selected = true;
					}
				});
			}

			var opts = [];
			for (var i = 0; i < select.options.length; i++) {
				var option = select.options[i];
				opts[i] = option;
				option.originalPosition = option.index;
			}
			for (var i = 0; i < opts.length; i++) {
				var option = opts[i];
				if (option.value === null || option.value === "") {
				} else if (option.selected) {
					left.append(option);
				} else {
					right.append(option);
				}
			}

			var buttonToLeft = document.createElement("input");
			buttonToLeft.type = "button";
			buttonToLeft.value = " &lt; ";
			buttonToLeft.addEventListener("click", function (ev) {
				ev.preventDefault();
				for (var i = right[0].options.length - 1; i >= 0; i--) {
					var o = right[0].options[i];
					if (o.selected) {
						Widgets.prototype.moveFromAToB(o, right, left);
					}
				}
			});

			var buttonToRight = document.createElement("input");
			buttonToRight.type = "button";
			buttonToRight.value = " &gt; ";
			buttonToRight.addEventListener("click", function () {
				for (var i = left[0].options.length - 1; i >= 0; i--) {
					var o = left[0].options[i];
					if (o.selected) {
						Widgets.prototype.moveFromAToB(o, left, right);
					}
				}
			});

			right.addEventListener("dblclick", function (ev) {
				var option;
				if (ev.target.tagName.toUpperCase() === "SELECT") {
					// Happens in ***** IE
					option = ev.target.querySelector("option[value=" + ev.target.value + "]");
				} else {
					option = ev.target;
				}
				Widgets.prototype.moveFromAToB(option, right, left);
			});

			left.addEventListener("dblclick", function (ev) {
				var option;
				if (ev.target.tagName.toUpperCase() === "SELECT") {
					// Happens in ***** IE
					option = ev.target.querySelector("option[value=" + ev.target.value + "]");
				} else {
					option = ev.target;
				}
				Widgets.prototype.moveFromAToB(option, left, right);
			});

			text.append(left);
			text.append(buttonToLeft);
			text.append(buttonToRight);
			text.append(right);
			select.after(text);
			select.remove();
		});
	});
};


Widgets.prototype.labelsToInputs = function (selector, options) {
	var emptyisuntouched = options && options['emptyisuntouched'];
	//var ignornon         = options && options['emptyisuntouched'];

	domReady(function () {
		const elems = document.querySelectorAll(selector);
		elems.forEach(function (label) {
			var labelText = label.innerText;
			var inputId = label.getAttribute("for");
			var input = document.getElementById(inputId);

			if (input.value.toString().trim() == "") {
				if (input.getAttribute("type") == 'password') {
					try {
						input.setAttribute("type", "text");
					} catch (err) {
						// happens in text/html FF, never mind...
						var i = document.createElement("input");
						i.setAttribute("type", "text");
						i.setAttribute("value", "");
						i.setAttribute("id", input.getAttribute("id"));
						i.setAttribute("name", input.getAttribute("name"));
						i.setAttribute("class", input.getAttribute("class"));
						input.parentNode.insertBefore(i, input);
						input.style.display = "none";
						i[0].realInput = input;
						input = i;
					}
					input.classList.add("password");
				}

				input.value = labelText;
				input.classList.add("untouched");
				label.style.display = "none";

				var focus = function () {
					// if entered for the first time, remove the label value
					if (label.classList.contains("untouched")) {
						if (emptyisuntouched) {
							label.classList.remove("untouched");
						}
						label.value = "";
						if (label.classList.contains("password")) {
							try {
								label.setAttribute("type", "password");
							} catch (err) {
								label.realInput.style.display = "block";
								label.realInput.focus();
								label.style.display = "none";
								// happens in text/html FF, never mind...
							}
						}
					}
				};
			}

			input.addEventListener("focus", focus);
			input.addEventListener("select", focus);
			input.addEventListener("blur", function (ev) {
				// if leaving, the value is empty, and empty is equivalent to 'untouched', put the label back in.
				if (input.value.toString().trim() == "") {
					if (emptyisuntouched) {
						input.classList.add("untouched");
					}
					if (input.classList.contains("untouched")) {
						input.value = labelText;
						if (input.classList.contains("password")) {
							try {
								input.type = "text";
							} catch (e) {
								// happens in text/html FF, never mind...
							}
						}
					}
				}
			});

			if (!emptyisuntouched) {
				input.addEventListener("keyup", function () {
					input.classList.remove("untouched");
				});
			} else {
				// value is not empty, so cant use it for the label
			}
		});
	});
};
