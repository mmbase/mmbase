var validator;

(function () {
    document.addEventListener("DOMContentLoaded", function() {

        Widgets.instance.enumerationSuggestion("body.config select[name=mmjspeditors_uri]");

        validator = new MMBaseValidator();
        validator.logEnabled = false;
        validator.traceEnabled = false;
        validator.validateHook = function () {
            const okbutton = document.getElementById("okbutton");
            if (okbutton != null) {
                okbutton.disabled = this.invalidElements !== 0;
            }
            const savebutton = document.getElementById("savebutton");
            if (savebutton != null) {
                savebutton.disabled = this.invalidElements !== 0;
            }
        };

        validator.lang = document.querySelector("html head meta[name='MMBase-Language']")?.getAttribute("content") || "en";
        validator.sessionName = document.querySelector("html head meta[name='MMBase-SessionName']")?.getAttribute("content");
        const nt = document.querySelector("html head meta[name='MMBase-NodeType']")?.getAttribute("content");

        if (nt != null && nt.length > 0) {
            validator.prefetchNodeManager(nt);
        }

        const forms = document.querySelectorAll("form[name=change], form[name=create]");
        forms.forEach((form) => {
            validator.addValidation(form);
        });
    });
})();
