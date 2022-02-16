<#include "../nodeWrapperMacro.ftlh">
<#include "vizHtml.ftlh">
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
<head>
    <meta http-equiv="content-type" content="text/html; charset=utf-8">
    <title>AST Visualization</title>
    <@dependencies />
    <#include "css.ftlh">
</head>
<body>
<h1>AST Visualization</h1>

<#list nodeWrapperGroup as nodeWrapper>
    <div class="graphviz">
        digraph G {
        <@graph nodeWrapper debug />
        }
    </div>
</#list>


<@vizscript />
</body>
</html>

