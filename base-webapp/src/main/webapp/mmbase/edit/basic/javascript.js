var validator;

(function () {
    console.log('do we run?!');
    Widgets.instance.enumerationSuggestion("body.config select[name=mmjspeditors_uri]");
    domReady(function () {
        validator = new MMBaseValidator();
        validator.logEnabled = false;
        validator.traceEnabled = false;
        validator.validateHook = function () {
            var okbutton = document.getElementById("okbutton");
            if (okbutton != null) okbutton.disabled = this.invalidElements != 0;
            var savebutton = document.getElementById("savebutton");
            if (savebutton != null) savebutton.disabled = this.invalidElements != 0;
        };

        // validator.lang = $("html head meta[name='MMBase-Language']").attr("content");
        // validator.sessionName = $("html head meta[name='MMBase-SessionName']").attr("content");
        // var nt = $("html head meta[name='MMBase-NodeType']").attr("content");
        validator.lang =
            document.querySelector("html head meta[name='MMBase-Language']").getAttribute("content") || "en";
        validator.sessionName = document
            .querySelector("html head meta[name='MMBase-SessionName']")
            .getAttribute("content");
        var nt = document.querySelector("html head meta[name='MMBase-NodeType']").getAttribute("content");

        if (nt != null && nt.length > 0) {
            validator.prefetchNodeManager(nt);
        }
        const forms = document.querySelectorAll("form[name=change], form[name=create]");
        forms.forEach((form) => {
            validator.addValidation(form);
        });
        // validator.addValidation($("form[name=change]"));
        // validator.addValidation($("form[name=create]"));
    });
})();
