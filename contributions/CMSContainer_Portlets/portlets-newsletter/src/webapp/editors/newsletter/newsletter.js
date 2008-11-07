function massDelete(confirmmessage) {
   if (confirmmessage) {
      if (confirm(confirmmessage)) {
          var checkboxs = document.getElementsByTagName("input");
          var objectnumbers = '';
          for(i = 0; i < checkboxs.length; i++) {
            if(checkboxs[i].type == 'checkbox' && checkboxs[i].name.indexOf('chk_') == 0 && checkboxs[i].checked) {
               objectnumbers += checkboxs[i].value+",";
            }
          }
          if(objectnumbers == ''){
            return ;
          }
          objectnumbers = objectnumbers.substr(0,objectnumbers.length - 1);
          document.forms[0].deleteRequest.value = objectnumbers;
          document.forms[0].method.value = "delete";
          document.forms[0].submit();
      }
   }   
}

function setOffset(offset) {
   document.forms[0].offset.value = offset;
   document.forms[0].submit();
}
function showItem(objectnumber) {
   openPopupWindow("showItem", 500, 500, 'NewsletterBounceAction.do?method=getItem&objectnumber=' + objectnumber);
}
function sortBy(orderColumn){
   var offset = document.forms[0].offset.value;
   var oldOrderColumn = document.forms[0].order.value; 

   if (oldOrderColumn == orderColumn) {
   // order column is not changed so change direction
      var oldDirection = document.forms[0].direction.value;

      if (oldDirection == 'down') {
         document.forms[0].direction.value = 'up';
      }
      else {
         document.forms[0].direction.value = 'down';
      }
   }
   else {
      document.forms[0].order.value = orderColumn;
      document.forms[0].direction.value = 'down';
   }
   document.forms[0].submit();
}
function resets(){
   document.forms[0].reset();
   var startDate = document.getElementsByName("startDate");
   var endDate = document.getElementsByName("endDate");
   startDate[0].value = "";
   endDate[0].value = "";
}
function strDateTime(str){
   var reg = /^(\d{1,4})(-|\/)(\d{1,2})\2(\d{1,2})$/;
   var r = str.match(reg);
   if(r==null)return false;
   var datenum = parseInt(r[4],10);
   if(datenum>=1 && datenum<=9){
      datenum = '0'+datenum;
   }
   var monthnum = parseInt(r[3],10);
   if(monthnum >=1 && monthnum<=9){
      monthnum = '0' +monthnum;
   }
   str = r[1]+r[2]+monthnum+r[2]+datenum;
   var d= new Date(r[1], r[3]-1,r[4]);
   var dateRel = parseInt(d.getDate(),10);
   if(dateRel >=1 && dateRel <=9){
      dateRel = '0'+dateRel;
   }
   var monthRel = parseInt(d.getMonth()+1,10);
   if(monthRel >=1 && monthRel <=9){
      monthRel = '0' +monthRel;
   }
   var newStr =d.getFullYear()+r[2]+monthRel+r[2]+dateRel;
   return newStr==str;
}