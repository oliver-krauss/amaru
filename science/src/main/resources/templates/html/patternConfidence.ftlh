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
        <h1>Pattern Confidence for ${name}</h1>

        <table class="tg">
            <thead>
                <tr>
                    <th class="tg-0lax">Confidence</th>
                    <th class="tg-0lax">Soft Confidence</th>
                </tr>
            </thead>
                <tbody>
                    <tr>
                        <td class="tg-0lax">${confidence}</td>
                        <td class="tg-0lax">${softConfidence}</td>
                    </tr>
            </tbody>
        </table>

        <div id='exceptions'></div>

        <h3>Patterns</h3>
        <#list patternConfidence as pattern>
            <div class="graphviz">
                digraph G {
                <@graph pattern false />
                }
            </div>
        </#list>

        <h3>Anti Patterns</h3>
        <#list antiPatterns as pattern>
            <div class="graphviz">
                digraph G {
                <@graph pattern false />
                }
            </div>
        </#list>

        <script type="application/x-javascript">
            var data = [{
                values: [<#list exceptions?keys as eKey>${exceptions[eKey]}<#if eKey_has_next>,</#if></#list>],
                labels: [<#list exceptions?keys as eKey>"${eKey}"<#if eKey_has_next>,</#if></#list>],
                type: 'pie'
            }];

            Plotly.newPlot('exceptions', data);

        </script>
        <@vizscript />
    </body>
</html>

