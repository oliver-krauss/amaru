<#macro solutionQuality solution debug>
    <table border="1" cellborder="1" cellspacing="0" cellpadding="4">
        <tr>
            <td colspan="2">${solution.quality}<#if debug> (${solution.id!"null"})</#if></td>
        </tr>
        <#list solution.cachets as cachet>
            <#if debug>
                <tr>
                    <td colspan="2">${cachet.id}</td>
                </tr>
            </#if>
            <tr>
                <td>${cachet.name}</td>
                <td>${cachet.quality}</td>
            </tr>
        </#list>
    </table>
</#macro>