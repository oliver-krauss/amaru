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

        <div id='boxplot'></div>
        <div id='performanceIterations'></div>

        <table class="tg">
            <thead>
            <tr>
                <td class="tg-0lax">Minimum</td>
                <td class="tg-0lax">${runtimeProfile.minimum?c}</td>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td class="tg-0lax">1st Quartile</td>
                <td class="tg-0lax">${runtimeProfile.firstQuartile?c}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Median</td>
                <td class="tg-0lax">${runtimeProfile.median?c}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Average</td>
                <td class="tg-0lax">${runtimeProfile.mean?c}</td>
            </tr>
            <tr>
                <td class="tg-0lax">3rd Quartile</td>
                <td class="tg-0lax">${runtimeProfile.thirdQuartile?c}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Maximum</td>
                <td class="tg-0lax">${runtimeProfile.maximum?c}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Standard Deviation excl. Upper Outliers</td>
                <td class="tg-0lax">${runtimeProfile.standardDeviationNoOutliers?c}</td>
            </tr>
            <tr>
                <td class="tg-0lax">Standard Deviation</td>
                <td class="tg-0lax">${runtimeProfile.standardDeviation?c}</td>
            </tr>
            </tbody>
        </table>

        <div id='histogram'></div>
        <div id='histogramAlt'></div>

        <script type="application/x-javascript">

            var data = [
                {
                    x: ['lower outliers', 'lower suspected outliers', 'lower inner fence', 'lower quarter', 'upper quarter', 'uppper inner fence', 'upper supsected outliers', 'upper outliers'],
                    y: [${runtimeProfile.groups[0]?c}, ${runtimeProfile.groups[1]?c}, ${runtimeProfile.groups[2]?c}, ${runtimeProfile.groups[3]?c}, ${runtimeProfile.groups[4]?c}, ${runtimeProfile.groups[5]?c}, ${runtimeProfile.groups[6]?c}, ${runtimeProfile.groups[7]?c}],
                    type: 'bar'
                }
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

            var runtimeData = [<#list perfMeasurements as val>${val?c}<#if val_has_next>,</#if></#list>];

            var ddd = [
                {
                    y: runtimeData,
                    type: 'histogram'
                }
            ];
            var layout = {
                title:'Count of Executons in Quartiles',
                xaxis: {
                    title: {
                        text: 'Number of ASTs',
                    }
                },
                yaxis: {
                    title: {
                        text: 'Median run time (ns)',
                    }
                }
            };
            Plotly.newPlot('histogramAlt', ddd, layout);

            var trace = {
                y: runtimeData,
                mode: 'lines',
                name: 'Lines'
            };

            var data = [ trace ];

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

            var boxtrace = {
                y: runtimeData,
                type: 'box',
                name: 'Mean and Standard Deviation',
                marker: {
                    color: 'rgb(10,140,208)'
                },
                boxmean: 'sd'
            };

            var boxdata = [ boxtrace ];


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

            Plotly.newPlot('boxplot', boxdata, layout);
        </script>
    </body>
</html>

