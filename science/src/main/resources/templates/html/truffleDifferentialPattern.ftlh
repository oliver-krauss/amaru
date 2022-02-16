<#include "../nodeWrapperMacro.ftlh">
<#include "vizHtml.ftlh">
<#include "plotlyHtml.ftlh">
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
<head>
    <meta http-equiv="content-type" content="text/html; charset=utf-8">
    <title>Differential Pattern Analysis</title>
    <@dependencies />
    <@dependenciesPlotly />
    <#include "css.ftlh">
</head>
<body>

<!-- Tab links -->
<div class="tab">
    <button class="tablinks-0" onclick="openTab(event, 'ProblemDef', 0)">Problem Definitions</button>
    <button class="tablinks-0" onclick="openTab(event, 'Patterns', 0)">Patterns per Problem</button>
    <button id="defaultTab" class="tablinks-0" onclick="openTab(event, 'Diff', 0)">Differential between Problems</button>
</div>

<div id="ProblemDef" class="tabcontent-0">

    <div class="tab">
        <#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem>
            <button class="tablinks-1" onclick="openTab(event, 'problemdef-${problem.name}', 1)">${problem.name}</button>
        </#list>
    </div>

<h1>Problem Definitions</h1>
<#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem>
<div id="problemdef-${problem.name}" class="tabcontent-1">
<h2>${problem.name} - (${problem.searchSpace?api.getSearchSpace()?size} trees)</h2>
    <#if !omitSearchSpace?? || !omitSearchSpace>
    <@searchSpace problem.searchSpace?api.getSearchSpace() debug "<div class=\"graphviz\">" "</div>" />
    </#if>
</div>
</#list>
</div>

<div id="Patterns" class="tabcontent-0">
<h1>Patterns per Problem</h1>
<#assign ppp = truffleDifferentialPattern.patternsPerProblem />
<#assign radarChartWidth = 500/>
<#assign radarChartHeight = 500/>

<div class="wrapper">
    <div id="overlapRadarChart"></div>
    <#assign overlapCount = (truffleDifferentialPattern?api.overlapCount() - 1)/>
    <#if overlapCount <= 0><#assign overlapCount = 1 /></#if>
    <#assign degreePerPattern = 360 / overlapCount />
    <#assign theta><#list 0..truffleDifferentialPattern?api.overlapCount() as pos>${degreePerPattern * pos}<#if pos_has_next>,</#if></#list></#assign>
    <script>
        data = [
        <#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem><#assign prev = false/>
            {
            type: 'scatterpolar',
            r: [<#list truffleDifferentialPattern.differential?keys?sort_by("count") as pattern><#if truffleDifferentialPattern?api.contained(pattern)><#if prev>,</#if>${truffleDifferentialPattern?api.getPatternForProblem(problem, pattern)?api.getCount()}<#assign prev = true/></#if></#list>],
            theta: [${theta}],
            thetaunit: "degrees",
            fill: 'toself',
            name: "${problem.name}"
        }<#if problem_has_next>,</#if>
        </#list>
        ]
        layout = {
            width: ${radarChartWidth},
            height: ${radarChartHeight},
            polar: {
                radialaxis: {
                    visible: true,
                    range: [0, ${truffleDifferentialPattern?api.maxPatternCountPerProblem()}]
                }
            }
        }

        Plotly.newPlot('overlapRadarChart', data, layout);
    </script>
    <div id="fullOverlapRadarChart"></div>

    <#if 1 < truffleDifferentialPattern.differential?size>
        <#assign degreePerPattern = 360 / (truffleDifferentialPattern.differential?size - 1) />
    <#else>
        <#assign degreePerPattern = 360 />
    </#if>
    <#assign theta><#list 0..truffleDifferentialPattern.differential?size as pos>${degreePerPattern * pos}<#if pos_has_next>,</#if></#list></#assign>
    <script>
        data = [
        <#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem>
            {
                type: 'scatterpolar',
                r: [<#list truffleDifferentialPattern?api.patternSorted() as pattern><#if truffleDifferentialPattern?api.getPatternForProblem(problem, pattern)??>${truffleDifferentialPattern?api.getPatternForProblem(problem, pattern)?api.getCount()}<#else>0</#if><#if pattern_has_next>,</#if></#list>],
                theta: [${theta}],
                thetaunit: "degrees",
                fill: 'toself',
                name: "${problem.name}"
            }<#if problem_has_next>,</#if>
        </#list>
        ]
        layout = {
            width: ${radarChartWidth},
            height: ${radarChartHeight},
            polar: {
                radialaxis: {
                    visible: true,
                    range: [0, ${truffleDifferentialPattern?api.maxPatternCountPerProblem()}]
                }
            }
        }

        Plotly.newPlot('fullOverlapRadarChart', data, layout);
    </script>
    <div id="patternGroupings"></div>
    <script>
        var data = [
            {
                x: [<#list patternGroupings as pair>'${pair.key}'<#if pair_has_next>,</#if></#list>],
                y: [<#list patternGroupings as pair>${pair.value}<#if pair_has_next>,</#if></#list>],
                type: 'bar'
            }
        ];

        Plotly.newPlot('patternGroupings', data);
    </script>
</div>

    <#if !omitPatternsPerProblem?? || !omitPatternsPerProblem>
    <div class="tab">
        <#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem>
            <button class="tablinks-2" onclick="openTab(event, 'patterns-${problem.name}', 2)">${problem.name}</button>
        </#list>
    </div>

    <#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem>
    <div id="patterns-${problem.name}" class="tabcontent-2">
    <h2>${problem.name} - (${ppp?api.get(problem)?size} patterns)</h2>
     <div class="wrapper">
        <#list ppp?api.get(problem) as trufflePattern>
            <div>
            <@pattern trufflePattern debug "<div class=\"graphviz\">" "</div>" />
            </div>
        </#list>
     </div>
    </div>
    </#list>
    </#if>
</div>

<div id="Diff" class="tabcontent-0">
<h1>Differential between Problems</h1>

<#assign diff = truffleDifferentialPattern.differential />
<div class="wrapper">
<#assign theta><#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem>"${problem.name}"<#if problem_has_next>, </#if></#list></#assign>
<#assign divCnt = 0>
<#list truffleDifferentialPattern.differential?keys?sort_by("size") as trufflePattern>
    <div style="border-style: dashed">
    <@pattern trufflePattern debug "<div class=\"graphviz\">" "</div>" />
        <div id="patternChart${divCnt}"></div>
        <script>
            data = [{
                type: 'scatterpolar',
                r: [<#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem><#if truffleDifferentialPattern?api.getPatternForProblem(problem, trufflePattern)??>${truffleDifferentialPattern?api.getPatternForProblem(problem, trufflePattern)?api.getCount()}<#else>0</#if>,</#list>],
                theta: [${theta}],
                thetaunit: "degrees",
                fill: 'toself',
                name: "amount of occurences"
            },{
                type: 'scatterpolar',
                r: [<#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem><#if truffleDifferentialPattern?api.getPatternForProblem(problem, trufflePattern)??>${truffleDifferentialPattern?api.getPatternForProblem(problem, trufflePattern)?api.getTreeCount()}<#else>0</#if>,</#list>],
                theta: [${theta}],
                thetaunit: "degrees",
                fill: 'toself',
                name: "amount of trees"
            }]
            layout = {
                width: ${radarChartWidth},
                height: ${radarChartHeight},
                polar: {
                    radialaxis: {
                        visible: true,
                        range: [0, ${truffleDifferentialPattern?api.maxPatternCountPerProblem(trufflePattern)}]
                    }
                }
            }

            Plotly.newPlot('patternChart${divCnt}', data, layout); <#assign divCnt = divCnt + 1 />
        </script>
        <#if printDiff?? && printDiff>
    <#list diff?api.get(trufflePattern) as keypair, diffvalue>
${keypair.key.name} - ${keypair.value.name}: ${diffvalue}
    </#list>
        </#if>
    </div>
</#list>
</div>
</div>
    <@vizscript />
    <@tabscript />
</body>
</html>