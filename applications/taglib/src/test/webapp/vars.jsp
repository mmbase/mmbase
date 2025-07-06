<jsp:root
    xmlns:jsp="http://java.sun.com/JSP/Page" version="2.0"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:c="http://java.sun.com/jsp/jstl/core"
    xmlns:mm="http://www.mmbase.org/mmbase-taglib-2.0">
  <html  xmlns="http://www.w3.org/1999/xhtml">
    <head>
      <title>Testing MMBase/taglib</title>
      <script src="https://code.jquery.com/jquery-3.7.1.min.js"
        integrity="sha256-CSXorXvZcTkaix6Yvo6HppcZGetbYMGWSFlBw8HfCJo="
        crossorigin="anonymous"></script>
      <style>
        html {
        background-color: #ccc;
        color: black;
        padding: 10px;
        }
        body {
        background-color: white;

        }
        table {
        width: 100%;
        border: solid 1px;
        }
        th.id {
        width: 4%;
        }
        th {
        width: 32%;
        border-bottom: 1px dotted #444;

        }
        dt {
          font-style: italic;
        }
        tr.todo {
        background-color: #faa;
        }

      </style>

    </head>
    <body>
      <h1>Testing MMBase/taglib - Contexts nesting and vars</h1>

      <jsp:directive.include file="/mmbase/components/taglib/test/context_vars.jspx" />
    </body>
  </html>
</jsp:root>
