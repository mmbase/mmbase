<%@include file="globals.jsp"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html"
%><%@ taglib uri="http://finalist.com/cmsc" prefix="cmsc"
%><%@ taglib uri="http://jsptags.com/tags/navigation/pager" prefix="pg"
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
   <title>
      <fmt:message key="datafile.import.success"/>
   </title>
   <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
   <link rel="icon" href="<c:url value='/favicon.ico'/>" type="image/x-icon"/>
   <link rel="shortcut icon" href="<c:url value='/favicon.ico'>" type="image/x-icon"/>
   <link href="<c:url value='/editors/css/main.css'/>" type="text/css" rel="stylesheet"/>
   <!--[if IE]>
   <style type="text/css" xml:space="preserve">
      body { behavior: url(<c:url value='/editors/css/hover.htc'/>);}
   </style>
   <![endif]-->
   <script src="<c:url value='/editors/utils/rowhover.js'/>" type="text/javascript"></script>

   <script src="<c:url value='/js/window.js'/>" type="text/javascript"></script>
   <script src="<c:url value='/js/transparent_png.js'/>" type="text/javascript"></script>
   <style type="text/css">
      input {
         width: 100px;
      }
   </style>
</head>
<body>
<div class="side_block_green">
   <div class="header">
      <div class="title">
         <fmt:message key="datafile.import.success"/>
      </div>

      <div class="header_end"></div>
   </div>
   <div class="body">

      <p>
         <fmt:message key="datafile.import.success"/>
      </p>

   </div>
   <div class="side_block_end"></div>
</div>
</body>
</html>