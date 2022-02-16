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

        <table class="tg">
            <thead>
            <tr>
                <th class="tg-0lax">Identifier</th>
                <th class="tg-0lax">Test (input -> output)</th>
            </tr>
            </thead>
            <tbody>
            <#list keys as key>
                <tr>
                    <td class="tg-0lax">${key}</td>
                    <td class="tg-0lax">${testInfo[key]}</td>
                </tr>
        </#list>
            </tbody>
        </table>

        <div id='boxplot'></div>
        <div id='performanceIterations'></div>

        <table class="tg">
            <thead>
            <tr>
                <th class="tg-0lax">Value</th>
                <#list keys as key>
                    <th class="tg-0lax">${key}</th>
                </#list>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td class="tg-0lax">Minimum</td>
                <#list keys as key>
                    <td class="tg-0lax">${runtimeTestProfile[key].key.minimum?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">1st Quartile</td>
                <#list keys as key>
                    <td class="tg-0lax">${runtimeTestProfile[key].key.firstQuartile?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">Median</td>
                <#list keys as key>
                    <td class="tg-0lax">${runtimeTestProfile[key].key.median?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">Average</td>
                <#list keys as key>
                    <td class="tg-0lax">${runtimeTestProfile[key].key.mean?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">3rd Quartile</td>
                <#list keys as key>
                    <td class="tg-0lax">${runtimeTestProfile[key].key.thirdQuartile?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">Maximum</td>
                <#list keys as key>
                    <td class="tg-0lax">${runtimeTestProfile[key].key.maximum?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">Standard Deviation excl. Upper Outliers</td>
                <#list keys as key>
                    <td class="tg-0lax">${runtimeTestProfile[key].key.standardDeviationNoOutliers?c}</td>
                </#list>
            </tr>
            <tr>
                <td class="tg-0lax">Standard Deviation</td>
                <#list keys as key>
                    <td class="tg-0lax">${runtimeTestProfile[key].key.standardDeviation?c}</td>
                </#list>
            </tr>
            </tbody>
        </table>
    </body>

    <div id='histogram'></div>
    <div id='boxplotFull'></div>

    <script type="application/x-javascript">

        var data = [
            <#list keys as key>
            {
                x: ['lower outliers', 'lower suspected outliers', 'lower inner fence', 'lower quarter', 'upper quarter', 'uppper inner fence', 'upper supsected outliers', 'upper outliers'],
                y: [${runtimeTestProfile[key].key.groups[0]?c}, ${runtimeTestProfile[key].key.groups[1]?c}, ${runtimeTestProfile[key].key.groups[2]?c}, ${runtimeTestProfile[key].key.groups[3]?c}, ${runtimeTestProfile[key].key.groups[4]?c}, ${runtimeTestProfile[key].key.groups[5]?c}, ${runtimeTestProfile[key].key.groups[6]?c}, ${runtimeTestProfile[key].key.groups[7]?c}],
                name: "${key}",
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
            <#list keys as key>
            {
                y: [<#list runtimeTestProfile[key].value as val>${val?c}<#if val_has_next>,</#if></#list>],
                mode: 'lines',
                name: '${key}'
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
                "q1": [ ${runtimeTestProfile[key].key.firstQuartile?c} ],
                "median": [ ${runtimeTestProfile[key].key.median?c} ],
                "q3": [ ${runtimeTestProfile[key].key.thirdQuartile?c} ],
                "lowerfence": [ ${runtimeTestProfile[key].key.minimum?c} ],
                // "upperfence": [ ${runtimeTestProfile[key].key.maximum?c} ],
                "mean": [ ${runtimeTestProfile[key].key.mean?c} ],
                // "sd": [ ${runtimeTestProfile[key].key.standardDeviationNoOutliers?c} ]
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

        var xVal = [
            <#list keys as key>key<#if key_has_next>,</#if></#list>
        ]
        var boxdata = [
            <#list keys as key>
            {
                "type": "box",
                x: ["${key}"],
                "name": "${key}",
                "q1": [ ${runtimeTestProfile[key].key.firstQuartile?c} ],
                "median": [ ${runtimeTestProfile[key].key.median?c} ],
                "q3": [ ${runtimeTestProfile[key].key.thirdQuartile?c} ],
                "lowerfence": [ ${runtimeTestProfile[key].key.minimum?c} ],
                "upperfence": [ ${runtimeTestProfile[key].key.maximum?c} ],
                "mean": [ ${runtimeTestProfile[key].key.mean?c} ],
                "sd": [ ${runtimeTestProfile[key].key.standardDeviation?c} ]
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
