<%@ taglib prefix='c' uri='http://java.sun.com/jstl/core' %>

<c:url var="readUrl" value="test">
  <c:param name="inputFile" value="testFile.txt"/>
</c:url>

<c:url var="writeUrl" value="test">
  <c:param name="outputFile" value="testFile.txt"/>
</c:url>

<html>
  <head>
    <title>jndi-appconfig Demo</title>
  </head>

  <body>
    <h1>jndi-appconfig Demo</h1>

    <a href="${writeUrl}">Write</a> a configuration file.<br>
    <a href="${readUrl}">Read</a> a configuration file.<br>

  </body>
</html>