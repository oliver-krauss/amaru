<#include "plotlyHtml.ftlh">
<#include "vizHtml.ftlh">
<#include "../nodeWrapperMacro.ftlh">
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
    <head>
        <meta http-equiv="content-type" content="text/html; charset=utf-8">
        <title>Experiment Log for ${name}</title>
        <@dependencies />
        <@dependenciesPlotly />
        <#include "css.ftlh">
    </head>
    <body>
        <h1>Experiment Log for ${name}</h1>

        <div id='populationProgress'></div>
        <div id='histogram'></div>

        <table class="tg">
            <thead>
                <tr>
                    <th class="tg-0lax">Parameter</th>
                    <th class="tg-0lax">Setting</th>
                </tr>
            </thead>
                <tbody>
                <#list parameters?keys as pKey>
                    <tr>
                        <td class="tg-0lax">${pKey}</td>
                        <td class="tg-0lax">${parameters[pKey]}</td>
                    </tr>
                </#list>
            </tbody>
        </table>

        <table class="tg">
            <thead>
            <tr>
                <th class="tg-0lax">Statistic</th>
                <th class="tg-0lax">Value</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td class="tg-0lax">Initial AST size</td>
                <td class="tg-0lax">${statistics["initialAST"]}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Best Found AST size</td>
                <td class="tg-0lax">${statistics["bestAST"]}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Max AST size</td>
                <td class="tg-0lax">${statistics["maxAST"]}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Min AST size</td>
                <td class="tg-0lax">${statistics["minAST"]}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Avg AST size</td>
                <td class="tg-0lax">${statistics["avgAST"]}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Median AST size</td>
                <td class="tg-0lax">${statistics["medianAST"]}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Number of ASTs evaluated</td>
                <td class="tg-0lax">${statistics["numberASTs"]}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Total Number of Nodes</td>
                <td class="tg-0lax">${statistics["sumNodes"]}</td>
            </tr>
            </tbody>
        </table>


        <h3>Original AST</h3>
        <div class="graphviz">
            digraph G {
            <@graph originalAst false />
            }
        </div>

        <h3>Best Found AST</h3>
        <div class="graphviz">
            digraph G {
            <@graph bestAst false />
            }
        </div>


        <script type="application/x-javascript">

            <#assign keys = steps[0]?keys>

            var data = [
                <#list keys as key>
                {
                    y: [<#list steps as step>${step[key]}<#if step_has_next>,</#if></#list>],
                    name: "${key}",
                    type: 'scatter'
                },
                </#list>
            ];

            var layout = {
                title:'Solution Quality in Experiment',
                xaxis: {
                    title: {
                        text: 'Quality (lower is better)',
                    }
                },
                yaxis: {
                    title: {
                        text: 'Generations',
                    }
                }
            };
            Plotly.newPlot('populationProgress', data, layout);

            var dataHist = [
                {
                    y: [<#list stepsCount as step>${step["solved"]}<#if step_has_next>,</#if></#list>],
                    name: "solved",
                    type: 'bar'
                },
                {
                    y: [<#list stepsCount as step>${step["success"]}<#if step_has_next>,</#if></#list>],
                    name: "success",
                    type: 'bar'
                },
                {
                    y: [<#list stepsCount as step>${step["type"]}<#if step_has_next>,</#if></#list>],
                    name: "type",
                    type: 'bar'
                },
                {
                    y: [<#list stepsCount as step>${step["fail"]}<#if step_has_next>,</#if></#list>],
                    name: "fail",
                    type: 'bar'
                }
            ];
            var layout = {
                title:'Histogram of ASTs in Experiment',
                xaxis: {
                    title: {
                        text: 'Quality (lower is better)',
                    }
                },
                yaxis: {
                    title: {
                        text: 'Generations',
                    }
                }
            };
            Plotly.newPlot('histogram', dataHist, layout);

        </script>
        <@vizscript />
    </body>
</html>

