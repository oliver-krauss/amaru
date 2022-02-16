# Implementation Report

## KEY groupings
<#assign lastKey = "">
<#list exception?sort_by("key") as pair>
<#if lastKey != pair.key>
### Key ${pair.key}
| Source | Exception |
| --- | --------- |
</#if>
| ${source(pair.value.getStackTrace())} | <@stacktrace pair.value.getStackTrace() /> | <#assign lastKey = pair.key>
</#list>

## LOCATION groupings
<#assign lastSource = "">
<#list exception as pair>
<#assign src = source(pair.value.getStackTrace()) />
    <#if !lastSource?contains(src)>
<#assign lastSource = lastSource + src>
### Location ${src}
| key | Exception |
| --- | --------- |
        <#list exception as subPair>
            <#assign subSrc = source(subPair.value.getStackTrace()) />
            <#if src == subSrc>
| ${subPair.key} | <@stacktrace subPair.value.getStackTrace() /> |
            </#if>
        </#list>
    </#if>
</#list>

## EXCEPTION groupings
<#assign lastException = "">
<#list exception as pair>
    <#assign exc = pair.value.class.name />
    <#if !lastException?contains(exc)>
        <#assign lastException = lastException + exc>
### Exception ${exc}
| key | Source | Exception |
| --- | ------ | --------- |
        <#list exception as subPair>
            <#assign subExc = subPair.value.class.name />
            <#if exc == subExc>
| ${subPair.key} | ${source(subPair.value.getStackTrace())} /> | <@stacktrace subPair.value.getStackTrace() /> |
            </#if>
        </#list>
    </#if>
</#list>

<#function source trace><#return trace[0][trace[0]?index_of("(")+1..trace[0]?index_of(".java")-1] /></#function>
<#macro stacktrace trace><#list trace as line>${line}<br></#list></#macro>