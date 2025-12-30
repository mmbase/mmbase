$(document).ready(
    function() {
	var els = $("form.mm_form .mm_validate")
            .filter(function() {  // but not to the entries in the mm-sr:relatednodes. It is itself responsible for that
                        return $(this).closest("div.list").length == 0;
                    });

        var validator = new MMBaseValidator();
	if (els.length > 0) {
	    const form = $(els[0]).closest("form");
	    validator.saveToForm = $(form).find("input[name=mm_form_name]").val();
        validator.addValidationForElements(els);
	}

	$("form.mm_form").
	    bind("mmsrValidateHook",
		 function(ev, list, valid, reason) {
		     var formInvalid = $(ev.target).find("div.list.invalid");
		     if (valid) {
			 $(".info").text("Form is valid");
                     } else {
			 $(".info").text("Form is invalid: " + reason);
                     }
                     if (formInvalid.length == 0) {
			 $("input[name=submit]").removeAttr("disabled");
                     } else {
			 $("input[name=submit]").attr("disabled", "disable");
                     }
		 });
    });
