<#include "plotlyHtml.ftlh">
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
    <head>
        <meta http-equiv="content-type" content="text/html; charset=utf-8">
        <title>Runtime Profile for ${name}</title>
        <@dependenciesPlotly />
    </head>
    <body>
        <h1>Runtime Profile for ${name}</h1>

        <#assign keys = sortedKeys>

        <div id='boxplot'></div>
        <div id='performanceIterations'></div>

        <table class="tg">
            <thead>
            <tr>
                <th class="tg-0lax">Value</th>
                <#list keys as rpKey>
                    <th class="tg-0lax">${rpKey}</th>
                </#list>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td class="tg-0lax">Minimum</td>
                <#list keys as rpKey>
                    <td class="tg-0lax">${runtimeProfileGroup[rpKey].key.minimum?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">1st Quartile</td>
                <#list keys as rpKey>
                    <td class="tg-0lax">${runtimeProfileGroup[rpKey].key.firstQuartile?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">Median</td>
                <#list keys as rpKey>
                    <td class="tg-0lax">${runtimeProfileGroup[rpKey].key.median?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">Average</td>
                <#list keys as rpKey>
                    <td class="tg-0lax">${runtimeProfileGroup[rpKey].key.mean?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">3rd Quartile</td>
                <#list keys as rpKey>
                    <td class="tg-0lax">${runtimeProfileGroup[rpKey].key.thirdQuartile?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">Maximum</td>
                <#list keys as rpKey>
                    <td class="tg-0lax">${runtimeProfileGroup[rpKey].key.maximum?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">Standard Deviation excl. Upper Outliers</td>
                <#list keys as rpKey>
                    <td class="tg-0lax">${runtimeProfileGroup[rpKey].key.standardDeviationNoOutliers?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">Standard Deviation</td>
                <#list keys as rpKey>
                    <td class="tg-0lax">${runtimeProfileGroup[rpKey].key.standardDeviation?c}</td>
                </#list>
            </tr>
            </tbody>
        </table>

        <div id='histogram'></div>
        <div id='boxplotFull'></div>
    </body>

    <script type="application/x-javascript">

        var data = [
            <#list keys as rpKey>
            {
                x: ['lower outliers', 'lower suspected outliers', 'lower inner fence', 'lower quarter', 'upper quarter', 'uppper inner fence', 'upper supsected outliers', 'upper outliers'],
                y: [${runtimeProfileGroup[rpKey].key.groups[0]?c}, ${runtimeProfileGroup[rpKey].key.groups[1]?c}, ${runtimeProfileGroup[rpKey].key.groups[2]?c}, ${runtimeProfileGroup[rpKey].key.groups[3]?c}, ${runtimeProfileGroup[rpKey].key.groups[4]?c}, ${runtimeProfileGroup[rpKey].key.groups[5]?c}, ${runtimeProfileGroup[rpKey].key.groups[6]?c}, ${runtimeProfileGroup[rpKey].key.groups[7]?c}],
                name: "${rpKey}",
                type: 'bar'
            },
            </#list>
        ];
        var layout = {
            title:'Count of Executons in Quartiles',
            yaxis: {
                title: {
                    text: 'Amount of Executions',
                }
            }
        };
        Plotly.newPlot('histogram', data, layout);

        var data = [
            <#list keys as rpKey>
            {
                y: [<#list runtimeProfileGroup[rpKey].value as val>${val?c}<#if val_has_next>,</#if></#list>],
                mode: 'lines',
                name: '${rpKey}'
            },
            </#list>
        ];

        var layout = {
            title:'Performance over executions',
            xaxis: {
                title: {
                    text: 'Execution (repetition)',
                },
            },
            yaxis: {
                title: {
                    text: 'Run time per execution (ns)',
                }
            }
        };

        Plotly.newPlot('performanceIterations', data, layout);

        var boxdata = [
            <#list keys as key>
            {
                "type": "box",
                x: ["${key}"],
                "name": "${key}",
                "q1": [ ${runtimeProfileGroup[key].key.firstQuartile?c} ],
                "median": [ ${runtimeProfileGroup[key].key.median?c} ],
                "q3": [ ${runtimeProfileGroup[key].key.thirdQuartile?c} ],
                "lowerfence": [ ${runtimeProfileGroup[key].key.minimum?c} ],
                // "upperfence": [ ${runtimeProfileGroup[key].key.maximum?c} ],
                "mean": [ ${runtimeProfileGroup[key].key.mean?c} ],
                // "sd": [ ${runtimeProfileGroup[key].key.standardDeviationNoOutliers?c} ]
            },
            </#list>
        ];


        var layout = {
            title: 'Run time plot of executions (values above third quartile excluded)',
            yaxis: {
                title: {
                    text: 'Run time (ns)',
                }
            },
            boxmode: 'overlay',
            boxgroupgap: 0.2,
            boxgap: 0.2
        };

        Plotly.newPlot('boxplot', boxdata, layout);

        var boxdata = [
            <#list keys as key>
            {
                "type": "box",
                x: ["${key}"],
                "name": "${key}",
                "q1": [ ${runtimeProfileGroup[key].key.firstQuartile?c} ],
                "median": [ ${runtimeProfileGroup[key].key.median?c} ],
                "q3": [ ${runtimeProfileGroup[key].key.thirdQuartile?c} ],
                "lowerfence": [ ${runtimeProfileGroup[key].key.minimum?c} ],
                "upperfence": [ ${runtimeProfileGroup[key].key.maximum?c} ],
                "mean": [ ${runtimeProfileGroup[key].key.mean?c} ],
                "sd": [ ${runtimeProfileGroup[key].key.standardDeviation?c} ]
            },
            </#list>
        ];


        var layout = {
            title: 'Run time plot of executions',
            yaxis: {
                title: {
                    text: 'Run time (ns)',
                }
            },
            boxmode: 'overlay',
            boxgroupgap: 0.2,
            boxgap: 0.2
        };

        Plotly.newPlot('boxplotFull', boxdata, layout);
    </script>
</html>

