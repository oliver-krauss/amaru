<#include "../nodeWrapperMacro.ftlh">
<#include "vizHtml.ftlh">
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
    <head>
        <meta http-equiv="content-type" content="text/html; charset=utf-8">
        <title>Search Space (${truffleSearchSpace?size} trees)</title>
        <@dependencies />
    </head>
    <body>
    <@searchSpace truffleSearchSpace debug "<div class=\"graphviz\">" "</div>" />
    <@vizscript />
    </body>
</html>

