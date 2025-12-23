/* Generic javascripts to check forms */

/*
 * Return true if the input field has had input (a radio was selected, text has been entered etc.)
 * throw in the formitem itself, not it's value.
 * TODO: only input type="text", input type="checkbox", input type="radio", select and textarea
 * have been implemented so far.
 *
 * @param   formfield   the form field to check
 * @return  true when there is a value, false when not
 */
function hasInput(formfield) {
  if (formfield) {
    if (formfield.type) {
      if (formfield.type.toUpperCase()==="TEXT" || formfield.type.toUpperCase()==="TEXTAREA") {
        if (formfield.value&&formfield.value.length>=1) {
          return true;
        }
      } else if (formfield.nodeName.toUpperCase() === "SELECT") {
        switch (formfield.selectedIndex) {
          case -1:
            return false;
          case 0:
            if (formfield.options[formfield.selectedIndex].value) {
              return true;
            }
            break;
          default:
            return true;
        }
      }
    } else {
        return checkRadio(formfield);
    }
  }
  return false;
}

/*
 * This one is called by hasInput(), checks a group of radiobuttons for a value.
 *
 * @param   radioGroup  id of group of radiobuttons to check
 * @return  true when there is value, false when not
 */
function checkRadio(radioGroup) {
  // Return true if one of the radiobuttons is checked
  if (radioGroup.length>0) {
      for (i=0;i<radioGroup.length; i++) {
          if (radioGroup[i].checked) {
              return true;
          }
      }
  } else {
      alert("checkRadio: wrong formfield sent to function.");
  }
  return false;
}

/*
 * Checks a Dutch postcode '1234 AB'.
 * Allows a space between '1234' and 'AB', checks case-insensitive
 *
 * @param   str String containing a postcode
 * @return  true when postcode seams OK, false when not
 */
function checkPostcode(str) {
    reg = /^[0-9]{4} ?[A-Z]{2}$/i;
    return reg.test(str);
}

/*
 * Checks an email address, this method follows these rules:
 *
 * The address should start with at least 1 character before the at sign (@).
 * A dot (.) and a hyphen (_) can't be next to each other and not next to an at sign.
 * The address should contain an at sign.
 * There have to be at least two characters after the at sign.
 * The address has to end with one of the Top Level Domains (TLD's) menthioned below
 * and the TLD should be prefixed with a dot.
 *
 * @param   str an email address
 * @return  true when the address seems valid, false if otherwise
 */
function emailCheck(str) {
    var domStr = "";
    // Well known official top level domains
    domStr += "com|edu|gov|int|mil|net|org|arpa|nato|info|aero|biz|coop|museum|name|pro|";
    // Alphabetically split top level domains
    domStr += "ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|az|";
    domStr += "ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|";
    domStr += "ca|cc|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|";
    domStr += "de|dj|dk|dm|do|dz|";
    domStr += "ec|ee|eg|eh|er|es|et|eu|";
    domStr += "fi|fj|fk|fm|fo|fr|fx|";
    domStr += "ga|gb|gd|ge|gf|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|";
    domStr += "hk|hm|hn|hr|ht|hu|";
    domStr += "id|ie|il|in|io|iq|ir|is|it|";
    domStr += "jm|jo|jp|";
    domStr += "ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|";
    domStr += "la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|";
    domStr += "ma|mc|md|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|";
    domStr += "na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|";
    domStr += "om|";
    domStr += "pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|pt|pw|py|";
    domStr += "qa|";
    domStr += "re|ro|ru|rw|";
    domStr += "sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|st|sv|sy|sz|";
    domStr += "tc|td|tf|tg|th|tj|tk|tm|tn|to|tp|tr|tt|tv|tw|tz|";
    domStr += "ua|ug|uk|um|us|uy|uz|";
    domStr += "va|vc|ve|vg|vi|vn|vu|";
    domStr += "wf|ws|";
    domStr += "ye|yt|yu|";
    domStr += "za|zm|zr|zw";
    // custom domains for testing purposes
    //domStr += "mad|local";

    var re = new RegExp("^[A-Z0-9_]+([\.-]?[A-Z0-9_])*@([A-Z0-9_-]{2,}[\.]{1})+("+ domStr +")$","i");

    return re.test(str);
}
