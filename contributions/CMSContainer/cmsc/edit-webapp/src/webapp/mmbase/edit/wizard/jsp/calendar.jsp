<%@page language="java" contentType="text/html;charset=UTF-8"
%><%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"
%><%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" 
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" 
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<fmt:setBundle basename="cmsc-calendar" scope="request"/>
<mm:content type="text/html" encoding="UTF-8" expires="0">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
<title><fmt:message key="calendar.head.title"/></title>
<link href="../../../../editors/editwizards_new/style/layout/wizard.css" type="text/css" rel="stylesheet"/>
<link href="../../../../editors/editwizards_new/style/color/wizard.css" type="text/css" rel="stylesheet"/>
<link href="../../../../editors/editwizards_new/style/extra/wizard.css" type="text/css" rel="stylesheet"/>
<link href="../../../../editors/editwizards_new/style/color/searchlist.css" type="text/css" rel="stylesheet"/>
<link href="../../../../editors/editwizards_new/style/extra/searchlist.css" type="text/css" rel="stylesheet"/>
<style xml:space="preserve" type="text/css">
   input.calendar {
       width: 22px;
       height: 17px; 
       border:0px;
    }
   input.schedule {
      width: 18px;
      height: 17px;
      border:0px;
   }
</style>
<script src="../../../../js/prototype.js" type="text/javascript"></script>
<c:url var="actionUrl" value="/editors/newsletter/Schedule.do"/>
<c:set var="type" value="${param.type}"/>
<script type="text/javascript">
   var type =1; 
   var newsletterId = "";
   type = '${type}';
   if(type=='0') {
      alert('<fmt:message key="calendar.options.select"/>') ;
      window.close();
   }
   var gotoString = '<fmt:message key="calendar.goto"/>';
   var todayString = '<fmt:message key="calendar.today"/>';
   var weekString = '<fmt:message key="calendar.week.short"/>';
   var scrollLeftMessage = '<fmt:message key="calendar.scrollLeftMessage"/>';
   var scrollRightMessage = '<fmt:message key="calendar.scrollRightMessage"/>';
   var selectMonthMessage = '<fmt:message key="calendar.selectMonthMessage"/>';
   var selectYearMessage = '<fmt:message key="calendar.selectYearMessage"/>';
   var selectDateMessage = '<fmt:message key="calendar.selectDateMessage"/>'; // do not replace [date], it will be replaced by date.
   var monthName =   new   Array(
                  '<fmt:message key="calendar.month.january"/>','<fmt:message key="calendar.month.february"/>',
                  '<fmt:message key="calendar.month.march"/>','<fmt:message key="calendar.month.april"/>',
                  '<fmt:message key="calendar.month.may"/>','<fmt:message key="calendar.month.june"/>',
                  '<fmt:message key="calendar.month.july"/>','<fmt:message key="calendar.month.august"/>',
                  '<fmt:message key="calendar.month.september"/>','<fmt:message key="calendar.month.october"/>',
                  '<fmt:message key="calendar.month.november"/>','<fmt:message key="calendar.month.december"/>');
    var dayName = new Array   ('<fmt:message key="calendar.week.sunday.short"/>','<fmt:message key="calendar.week.monday.short"/>',
                '<fmt:message key="calendar.week.tuesday.short"/>','<fmt:message key="calendar.week.wednesday.short"/>',
                '<fmt:message key="calendar.week.thursday.short"/>','<fmt:message key="calendar.week.friday.short"/>',
                '<fmt:message key="calendar.week.saturday.short"/>');

   function initDatetime(){
      var now = new Date(); 
      if(type == '1' || type == '2'){
         document.getElementById("date").value = now.getDate()+"-"+eval(now.getMonth()+1)+"-"+now.getFullYear();
      }
      document.getElementById("hour").value = now.getHours();
      document.getElementById("minute").value = now.getMinutes();

     var hour = document.getElementById("hour");
     var minute = document.getElementById("minute");

     for(var i = 0 ; i <24 ;i++) {
      var opt = document.createElement('OPTION');
      if(i ==  now.getHours()) {
         opt.selected = true;
      }
      if(i <10) {
         opt.value ="0"+ i;
         opt.text = "0"+i;
      }
      else {
         opt.value =i;
         opt.text = i;
      }
      hour.options.add(opt);
     }
     for(var j = 0 ; j <60 ;j++) {
      var opt = document.createElement('OPTION');
      if(j ==  now.getMinutes()) {
         opt.selected = true;
      }
      if(j <10) {
         opt.value ="0"+ j;
         opt.text = "0"+j;
      }
      else {
         opt.value =j;
         opt.text = j;
      }
      minute.options.add(opt);
     }
   }
</script>
<script type="text/javascript">

   var message = ""; 
   var id='${param.id}';

   function createCalendar(arg) {
      type = arg;
      if(type == '1' || type == '2'){
        var date   = document.getElementById("date").value;
        if(!strDateTime(date)) {
            alert('<fmt:message key="calendar.validator.date"/>');
            return;
        }
      }
      if(type == '2'){
        var interval   = document.getElementById("interval");
           if(!checkNumber(interval.value)) {
              alert('<fmt:message key="calendar.validator.interval"/>');
              interval.focus();
               return;
           }
      }
      if(type == '3'){
        var interval   = document.getElementById("interval");
        if(!checkNumber(interval.value)) {
           alert('<fmt:message key="calendar.validator.interval"/>');
           interval.focus();
           return;
        }
      }

      if(type == '4'){
      var interval   = document.getElementById("day");
      var strategy = document.getElementsByName("strategy");
      for(var i = 0 ; i < strategy.length;i++) {
         if(strategy[i].checked && strategy[i].value == '0') {
             if(!checkNumber(interval.value)) {
                alert('<fmt:message key="calendar.validator.interval"/>');
                interval.focus();
                return;
             }
         }
         }
      }
      sendRequest();
   }
   //send request 
   function sendRequest() {
      var myAjax = new Ajax.Request(
         '${actionUrl}?method=transform',
          {
            parameters:getParameters(),
            onComplete: processResponse
          }
      );
   }

   function getParameters(){
      var parameters = "";
      parameters += "type="+type;
      var date = document.getElementById("date");
      var hour = document.getElementById("hour");
      var minute = document.getElementById("minute");
      parameters += "&hour="+hour.value+"&minute="+minute.value;

      if(type == '1') {
         parameters += "&date="+date.value;
         message += '<fmt:message key="calendar.once"/>,<fmt:message key="calendar.start.datetime"/>'+date.value+' '+hour.value+':'+minute.value;
      }
      else if(type == '2') {
         parameters += "&date="+date.value;
         var approach = "2";
         var interval = document.getElementById("interval");
         parameters += "&strategy=2&interval="+interval.value;
         message += '<fmt:message key="calendar.daily"/>,<fmt:message key="calendar.start.datetime"/>'+date.value+' '+hour.value+':'+minute.value;

         if(approach == "0") {
            message += '<br/>  <fmt:message key="calendar.approach.interval.pre"/> <fmt:message key="calendar.approach.interval.day"/>'; 
         }
         else if(approach == "1") {
            message += '<br/>  <fmt:message key="calendar.approach.interval.pre"/> <fmt:message key="calendar.approach.weekday"/>'; 
         }
          else if(approach == "2") {
            message += '<br/>  <fmt:message key="calendar.approach.interval.pre"/> '+interval.value+' <fmt:message key="calendar.approach.interval.day"/>'; 
         }
      }
      else if(type == '3') {

         var varWeek = "";
         var interval = document.getElementById("interval");
         parameters += "&interval="+interval.value;
         var weeks = document.getElementsByName("weeks");
         for(var i = 0 ; i < weeks.length;i++) {
            if(weeks[i].checked) {
               parameters += "&weeks="+weeks[i].value;
               if(weeks[i].value == "1") {
                  varWeek += '<fmt:message key="calendar.week.monday"/>,';
               }
               else if(weeks[i].value == "2") {
                  varWeek += '<fmt:message key="calendar.week.tuesday"/>,';
               }
               else if(weeks[i].value == "3") {
                  varWeek += '<fmt:message key="calendar.week.wednesday"/>,';
               }
               else if(weeks[i].value == "4") {
                  varWeek += '<fmt:message key="calendar.week.thursday"/>,';
               }
               else if(weeks[i].value == "5") {
                  varWeek += '<fmt:message key="calendar.week.friday"/>,';
               }
               else if(weeks[i].value == "6") {
                  varWeek += '<fmt:message key="calendar.week.saturday"/>,';
               }
               else if(weeks[i].value == "7") {
                  varWeek += '<fmt:message key="calendar.week.sunday"/>,';
               }
            }
         }
        if(varWeek != null && varWeek != ""){
           if(varWeek.substr(varWeek.length-1,1) ==","){
              varWeek = varWeek.substr(0,varWeek.length-1) ;
           }
        }
         message += '<fmt:message key="calendar.weekly"/>,<fmt:message key="calendar.start.datetime"/>'+hour.value+':'+minute.value;
         message += '<br/> <fmt:message key="calendar.approach.interval.pre"/>'+interval.value+' <fmt:message key="calendar.approach.interval.week"/>';
         message += '<br/> <fmt:message key="calendar.approach.interval.week"/>: '+varWeek;
      }
      else {
         var strategy = document.getElementsByName("strategy");
         var months = "";
         var approach = "0";
         for(var i = 0 ; i < strategy.length;i++) {
            if(strategy[i].checked) {
               parameters += "&strategy="+strategy[i].value;
               approach = strategy[i].value;
               break;
            }
         }
         var day = document.getElementById("day");
         parameters += "&day="+day.value;
         var whichweek = document.getElementById("whichweek");
         parameters += "&whichweek="+whichweek.value;

         var week = document.getElementById("week");
         parameters += "&week="+week.value;
         var month = document.getElementsByName("month");
         for(var i = 0 ; i < month.length;i++) {
            if(month[i].checked) {
               parameters += "&month="+month[i].value;

               if(month[i].value == "0") {
                  months+= '<fmt:message key="calendar.month.january"/>,'
               }
               else if(month[i].value == "1") {
                  months+='<fmt:message key="calendar.month.february"/>,'
               }
               else if(month[i].value == "2") {
                  months+='<fmt:message key="calendar.month.march"/>,'
               }
               else if(month[i].value == "3") {
                  months+='<fmt:message key="calendar.month.april"/>,'
               }
               else if(month[i].value == "4") {
                  months+='<fmt:message key="calendar.month.may"/>,'
               }
               else if(month[i].value == "5") {
                  months+='<fmt:message key="calendar.month.june"/>,'
               }
               else if(month[i].value == "6") {
                  months+='<fmt:message key="calendar.month.july"/>,'
               }
               else if(month[i].value == "7") {
                  months+='<fmt:message key="calendar.month.august"/>,'
               }
               else if(month[i].value == "8") {
                  months+='<fmt:message key="calendar.month.september"/>,'
               }
               else if(month[i].value == "9") {
                  months+='<fmt:message key="calendar.month.october"/>,'
               }
               else if(month[i].value == "a") {
                  months+='<fmt:message key="calendar.month.november"/>,'
               }
               else if(month[i].value == "b") {
                  months+='<fmt:message key="calendar.month.december"/>,'
               }
            }
         }
        if(months != null && months != ""){
           if(months.substr(months.length-1,1) ==","){
              months = months.substr(0,months.length-1) ;
           }
        }
         message += '<fmt:message key="calendar.monthly"/>,<fmt:message key="calendar.start.datetime"/>'+hour.value+':'+minute.value;
         if(approach == "0") {
            message += '<br/>  <fmt:message key="calendar.approach.interval.pre"/> '+day.value+'  <fmt:message key="calendar.approach.interval.day"/>';
         }
         else if(approach == "1") {
            message += '<br/> <fmt:message key="calendar.week"/> ';
            message += whichweek.options[whichweek.selectedIndex].text;
            message += " "+week.options[week.selectedIndex].text; 
         }
         message += '<br/> <fmt:message key="calendar.month"/> '+months; 
      }
      return parameters;
   }
   //deal with the response 

    function processResponse(response) {   
       var expression = response.responseXML.getElementsByTagName("expression")[0].firstChild.nodeValue;
       window.opener.document.getElementById(id).value = expression;
       window.opener.document.getElementById("calendar-expression").innerHTML = message;
       window.close();
   }
   function strDateTime(str){
      var reg = /^(\d{1,2})(-|\/)(\d{1,2})\2(\d{1,4})$/;
      var r = str.match(reg);
      if(r==null)return false;
      var d= new Date(r[4], r[3]-1,r[1]);
      var newStr =d.getDate()+r[2]+(d.getMonth()+1)+r[2]+d.getFullYear();
      return newStr==str;
  }
  function check(reg,str){
     if( reg.test(str)) {
        return true;
     }
     return false;
  }
  function checkNumber(s){
     var reg = /^[0-9]*[1-9][0-9]*$/;
     return check(reg,s);
  }

  function showTab(element,type) {
     if(type == "add") {
        $('scheduleAdd').show(); 
        $('addDiv').className = "tab_active";
        $('listDiv').className = "tab";
        $('scheduleList').hide();
     }
     else if(type == "list") {
        $('addDiv').className = "tab";
        $('listDiv').className = "tab_active";
        $('scheduleAdd').hide(); 
  
        $('scheduleList').show();
        createSchedules();
     }
  }

  function createSchedules() {
      $('scheduleList').innerHTML = "";
     var myAjax = new Ajax.Request(
        '${actionUrl}?method=getSchedules',
         {
            parameters:'newsletterid='+newsletterId,
            onComplete: createScheduleList
         }
     );
  }

  function createScheduleList(response){
     var numbers = response.responseXML.getElementsByTagName("number");
     var expressions = response.responseXML.getElementsByTagName("expression");
     var descriptions = response.responseXML.getElementsByTagName("description");

     var divElement =  document.createElement("div");
     divElement.className = "searchresult";
     var table = document.createElement("table");
     var tbody = document.createElement("tbody");
      table.className = "searchresult";
      table.setAttribute("cellspacing","0");
     for(i = 0 ; i < numbers.length;i++) {

        var aDeleteElement=document.createElement("a");
        aDeleteElement.href="javascript:deleteSchedule('"+numbers[i].childNodes[0].nodeValue+"')";
        
        var deleteImg=new Image();
        deleteImg.src="../../../../editors/gfx/icons/delete.png";
        deleteImg.style.border="none";
        deleteImg.title = '<fmt:message key="calendar.delete"/>';
        aDeleteElement.appendChild(deleteImg) 

        var aRestoreElement=document.createElement("a");
        aRestoreElement.href="javascript:restoreSchedule('"+expressions[i].childNodes[0].nodeValue+"','"+descriptions[i].childNodes[0].nodeValue+"')";
        
        var restoreImg=new Image();
        restoreImg.src="../../../../editors/gfx/icons/restore.png";
        restoreImg.style.border="none";
        restoreImg.title = '<fmt:message key="calendar.restore"/>';
        aRestoreElement.appendChild(restoreImg) 

        var tr = document.createElement("tr");
        if(i% 2 ==0){
           tr.className = "even";
        }
        else {
           tr.className = "odd";
        }
        var td = document.createElement("td");
        td.innerHTML = descriptions[i].childNodes[0].nodeValue;
        tr.appendChild(td);

        var td1 = document.createElement("td");
        td1.appendChild(aRestoreElement);
        td1.appendChild(aDeleteElement);
        tr.appendChild(td1);

        tr.appendChild(td);

        tbody.appendChild(tr);

     }
     table.appendChild(tbody);
    divElement.appendChild(table);
    $('scheduleList').appendChild(divElement);
  }

  function deleteSchedule(number) {

     var myAjax = new Ajax.Request(
        '${actionUrl}?method=deleteSchedule',
         {
            parameters:'scheduleid='+number
         }
     );
     alert('<fmt:message key="calendar.version.delete"/>')
    createSchedules();
  }

  function restoreSchedule(expression,scheduleDescription){

     window.opener.document.getElementById(id).value = expression;
     window.opener.document.getElementById("calendar-expression").innerHTML = scheduleDescription;
     window.close();
  }

  function initPage(){
      $('scheduleList').hide(); 

      var inputs = window.opener.document.getElementsByTagName("input");
      for( i = 0 ; i < inputs.length ; i++) {
         if(inputs[i].getAttribute("fdatapath") && inputs[i].getAttribute("fdatapath") == "field[@name='title']") {
            newsletterId = inputs[i].getAttribute("number");
         }
      }
      $('newsletterid').value = newsletterId;
  }
</script>
<script src="../javascript/datepicker-new.js" type="text/javascript"></script>
</head>
<mm:cloud jspvar="cloud" rank="basic user" loginpage="../login.jsp">
<body onload=" initPopCalendar();initDatetime();initPage()">
<div id="stepsbar">
<div class="tabs">
<div class="tab_active" id="addDiv">
<div class="body">
<a href="javascript:showTab(this,'add');"  class="valid">
<fmt:message key="calendar.tab.add"/>
</a>
</div></div>
<div class="tab" id="listDiv"><div class="body"><a href="javascript:showTab(this,'list');" class="valid"><fmt:message key="calendar.tab.list"/></a></div></div></div></div>
<input type="hidden" value="" name="newsletterid" id="newsletterid"/>
<form method="post" name="form1" action="">
<div class="editor" style="height:260px">
<p></p>
<div id="scheduleAdd">
<div class="body">
<table>
<c:choose>
    <c:when test="${type == '1'}">         
        <tr class="fieldcanvas"><td  class="fieldprompt"><span  class="valid" ><fmt:message key="calendar.startdate"/></span> </td><td ><input type="text" name="date" id="date" size="12" maxlength="12" class="date"/> <input type="image" class="calendar" src="../media/datepicker/calendar.gif" border="0" onClick="popUpCalendar(this, 'dd-mm-yyyy', -105 , -30 ,this.form, 'date',event);return false;"/></td></tr>
        <tr class="fieldcanvas"><td class="fieldprompt">
       <span  class="valid" ><fmt:message key="calendar.starttime"/></span> </td><td> <select id="hour" name="hour"></select>:<select id="minute" name="minute"></select></td></tr>
       </table>
       <table  width="50%"><tr class="fieldcanvas"><td width="40%"  ><a href="#"  onclick="createCalendar('1')"/><span  class="valid" > <fmt:message key="calendar.ok"/></span></a></td> <td><a href="#"  onclick="javascript:window.close()"/><span  class="valid" > <fmt:message key="calendar.cancel"/></span> </a></td></tr></table>
    </c:when>
    <c:when test="${type == '2'}">
      <tr class="fieldcanvas">
        <td class="fieldprompt"><span  class="valid" >
            <fmt:message key="calendar.startdate"/></span>
        </td>
        <td > <input type="text" name="date" id="date" size="12" class="date"/><input type="image" class="calendar" src="../media/datepicker/calendar.gif" border="0" onClick="popUpCalendar(this, 'dd-mm-yyyy', -105 , -30  , document.forms[0], 'date',event);return false;"/>
        </td></tr>
        <tr><td colspan="2"></td></tr>
        <tr class="fieldcanvas"><td class="fieldprompt"><span  class="valid" >
        <fmt:message key="calendar.starttime"/></span></td><td ><select id="hour" name="hour"></select>:<select id="minute" name="minute"></select></td>
        </tr>
            <tr><td colspan="2"></td></tr>
        <tr class="fieldcanvas"><td  class="fieldprompt" >
        <span  class="valid"  ><fmt:message key="calendar.approach"/></span></td><td>                

       <span  class="valid" ><fmt:message key="calendar.approach.interval.pre"/></span><input type="text" size="4" name="interval" id="interval" value="1" class="calendar"/><span  class="valid" > <fmt:message key="calendar.approach.interval.day"/></span></td>         
        </tr> </table>  
        <br/>
        <table  width="50%"><tr class="fieldcanvas"><td width="40%"  ><a href="#"  onclick="createCalendar('2')"/><span  class="valid" ><fmt:message key="calendar.ok"/></span></a></td><td>  <a href="#"  onclick="javascript:window.close()"/><span  class="valid" ><fmt:message key="calendar.cancel"/></span></a></td></tr></table>
    </c:when>
    <c:when test="${type == '3'}">
        <tr class="fieldcanvas"><td  class="fieldprompt">
        <span  class="valid" ><fmt:message key="calendar.starttime"/></span></td><td> <select id="hour" name="hour"></select>:<select id="minute" name="minute"></select></td>
        </tr>
        <tr class="fieldcanvas"><td class="fieldprompt"></td> <td >
        <span  class="valid" ><fmt:message key="calendar.approach.interval.pre"/></span><input  class="calendar" type="text" name="interval" size="2"  id="interval" value="1"/><span  class="valid" > <fmt:message key="calendar.approach.interval.week"/>  </span></td></tr>
       <tr class="fieldcanvas"><td  class="fieldprompt"> <span  class="valid" ><fmt:message key="calendar.week"/></span></td><td>
       <table><tr  class="fieldcanvas"><td  >
        <input type="checkbox" name="weeks" id="weeks"  value="7" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.week.sunday"/> </span>
        </td><td  >
        <input type="checkbox" name="weeks" id="weeks"  value="1" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.week.monday"/></span>
        </td><td>
        <input type="checkbox" name="weeks" id="weeks"  value="2" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.week.tuesday"/> </span>
         </td></tr><tr class="fieldcanvas"><td  >
         <input type="checkbox" name="weeks" id="weeks"  value="3" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.week.wednesday"/></span>
         </td><td  >
         <input type="checkbox" name="weeks" id="weeks"  value="4" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.week.thursday"/></span>
         </td><td  >
         <input type="checkbox" name="weeks" id="weeks"  value="5" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.week.friday"/></span>
        </td></tr><tr class="fieldcanvas"><td colspan="3" >
        <input type="checkbox" name="weeks" id="weeks"  value="6" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.week.saturday"/></span>

        </td></tr></table>
        </td></tr></table>
       <table  width="50%" ><tr class="fieldcanvas"><td width="40%"  ><a href="#"  onclick="createCalendar('3')"/><span  class="valid" ><fmt:message key="calendar.ok"/></span></a>
        </td><td> <a href="#"  onclick="javascript:window.close()"/><span  class="valid" ><fmt:message key="calendar.cancel"/></span></a></td></tr></table>
    </c:when>
    <c:otherwise>
      <tr class="fieldcanvas"><td  class="fieldprompt">
       <span  class="valid" ><fmt:message key="calendar.starttime"/></span></td><td> <select id="hour" name="hour"></select>:<select id="minute" name="minute"></select></td></tr>
       <tr class="fieldcanvas"><td class="fieldprompt"></td><td>
       <input type="radio" checked name="strategy" id="strategy" value="0"  class="calendar"/> <span  class="valid" ><fmt:message key="calendar.dayofmonth"/></span> <input size="2" value="1"  type="text" name="day" id="day"  class="calendar"></td></tr>
       <tr><td></td><td>
       <input type="radio" name="strategy" id="strategy" value="1"  class="calendar"/> 
       <select name="whichweek" id="whichweek">
          <option value="1"><fmt:message key="calendar.which.week.first"/> </option>
          <option value="2"><fmt:message key="calendar.which.week.second"/> </option>
          <option value="3"><fmt:message key="calendar.which.week.third"/> </option>
          <option value="4"><fmt:message key="calendar.which.week.forth"/> </option>
          <option value="5"><fmt:message key="calendar.which.week.last"/> </option>
       </select>
       <select name="week" id="week">
          <option value="1"> <fmt:message key="calendar.week.monday"/> </option>
          <option value="2"> <fmt:message key="calendar.week.tuesday"/> </option>
          <option value="3"> <fmt:message key="calendar.week.wednesday"/> </option>
          <option value="4"> <fmt:message key="calendar.week.thursday"/> </option>
          <option value="5"> <fmt:message key="calendar.week.friday"/> </option>
          <option value="6"> <fmt:message key="calendar.week.saturday"/> </option>
          <option value="7"> <fmt:message key="calendar.week.sunday"/></option>
      </select>
      </td></tr>
      <tr class="fieldcanvas"><td  class="fieldprompt">
      <span  class="valid" ><fmt:message key="calendar.month"/></span></td><td>
      <table><tr class="fieldcanvas"><td>
      <input type="checkbox" name="month" id="month" value="0" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.january"/></span>
      </td><td  class="fieldprompt">
      <input type="checkbox" name="month" id="month" value="1" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.february"/></span>
      </td><td>
      <input type="checkbox" name="month" id="month" value="2" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.march"/></span>
      </td><td>
      <input type="checkbox" name="month" id="month" value="3" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.april"/></span></td></tr>
      <tr class="fieldcanvas"><td>
      <input type="checkbox" name="month" id="month" value="4" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.may"/></span>
      </td><td>
      <input type="checkbox" name="month" id="month" value="5" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.june"/></span>
      </td><td>
      <input type="checkbox" name="month" id="month" value="6" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.july"/>
      </span></td><td>
      <input type="checkbox" name="month" id="month" value="7" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.august"/></span></td></tr>
      <tr class="fieldcanvas"><td>
      <input type="checkbox" name="month" id="month" value="8" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.september"/></span>
      </td><td>
      <input type="checkbox" name="month" id="month" value="9" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.october"/></span>
      </td><td>
      <input type="checkbox" name="month" id="month" value="a" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.november"/></span>
      </td><td>
      <input type="checkbox" name="month" id="month" value="b" checked class="schedule"/><span  class="valid" ><fmt:message key="calendar.month.december"/></span></td></tr>
      </table>
      </td></tr>
      </table>
      <table  width="50%" ><tr class="fieldcanvas"><td width="40%"  >
      <a href="#"  onclick="createCalendar('4')"/><span  class="valid" ><fmt:message key="calendar.ok"/></span></a>
      </td><td>  <a href="#"  onclick="javascript:window.close()"/><span  class="valid" ><fmt:message key="calendar.cancel"/></span></a>
      </td></tr></table>
   </c:otherwise>
</c:choose>

</div>
</div>
<div id="scheduleList">
</div>
</form>
</body>
</mm:cloud>
</html>
</mm:content>