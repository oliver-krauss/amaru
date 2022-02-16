-----------
${report.name}
<#if report.getChildReports()?size != 0 >
    <#assign keys = report.getChildReports()?keys />
    <#assign values = report.getReport(0).getValues()?keys />
    <#assign valuePad = report.longestChildKey()/>
    <#assign keyPad = report.longestChildString()/>
${""?right_pad(valuePad)} <#list keys as key>${key?right_pad(keyPad)} </#list>
    <#list values as value>
        <#if value != "index">
${value?left_pad(valuePad)} <#list keys as key>${report.getReport(key).getValue(value)?right_pad(keyPad)} </#list>
        </#if>
    </#list>
</#if>
<#if report.getValues()?size != 0 >
    <#assign longest = report.longestKey()/>
Results:
    <#list report.values as key, value>
${key?left_pad(longest)} = ${value.value}
    </#list>
</#if>
-----------