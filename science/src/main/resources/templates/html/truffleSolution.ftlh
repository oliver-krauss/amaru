<#include "../nodeWrapperMacro.ftlh">
<#include "../solutionMacro.ftlh">
<#include "vizHtml.ftlh">
<#include "plotlyHtml.ftlh">
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
<head>
    <meta http-equiv="content-type" content="text/html; charset=utf-8">
    <title>Pattern Solution</title>
    <@dependencies />
    <@dependenciesPlotly />
    <#include "css.ftlh">
</head>
<body>

<@solutionQuality truffleSolution debug/>

<!-- Tab links -->
<div class="tab">
    <button class="tablinks-0" onclick="openTab(event, 'ProblemDef', 0)">Problem Definitions</button>
    <button id="defaultTab" class="tablinks-0" onclick="openTab(event, 'Patterns', 0)">Patterns</button>
</div>

<div id="ProblemDef" class="tabcontent-0">

    <div class="tab">
        <#list problems?sort_by("name") as problem>
            <button class="tablinks-1" onclick="openTab(event, 'problemdef-${problem.name}', 1)">${problem.name}</button>
        </#list>
    </div>

<h1>Problem Definitions</h1>
<#list problems?sort_by("name") as problem>
<div id="problemdef-${problem.name}" class="tabcontent-1">
<h2>${problem.name} - (${problem.searchSpace?api.getSearchSpace()?size} trees)</h2>
    <@searchSpace problem.searchSpace?api.getSearchSpace() debug "<div class=\"graphviz\">" "</div>" />
</div>
</#list>
</div>

<div id="Patterns" class="tabcontent-0">
<h1>Patterns</h1>

    <div class="tab">
        <#list problems?sort_by("name") as problem>
            <button class="tablinks-2" onclick="openTab(event, 'patterns-${problem.name}', 2)">${problem.name}</button>
        </#list>
    </div>

<#list problems?sort_by("name") as problem>
<div id="patterns-${problem.name}" class="tabcontent-2">
<h2>${problem.name}</h2>
 <div class="wrapper">
    <#list truffleSolution.solutionGenes as gene>
        <#if helper.containsProblem(gene, problem)>
        <div>
        <@pattern gene.gene debug "<div class=\"graphviz\">" "</div>" />
        </div>
        </#if>
    </#list>
 </div>
</div>
</#list>
</div>

    <@vizscript />
    <@tabscript />
</body>
</html>