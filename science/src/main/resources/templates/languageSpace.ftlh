digraph G {
<#assign classCnt = 0>
<#list languageSpace as key, classInformation>
    ${classCnt}[shape=octagon, label="${classInformation.getClazz().getName()}"]
    <#assign initializerCnt = classCnt + 1/>
    <#list classInformation.getInitializers() as initializer>
        ${classCnt}->${initializerCnt}
        ${initializerCnt}[shape=<#if initializer.isMethod()>triangle<#else>invtriangle</#if>, label="${initializer.getName()}"]
        <#assign parameterCnt = initializerCnt + 1/>
        <#list initializer.getParameters() as parameter>
            ${initializerCnt}->${parameterCnt}
            ${parameterCnt}[shape=<#if parameter.isArray()>invhosue<#else>house</#if>, label="${parameter.getName()} : ${parameter.getType().getName()}"]
            <#assign parameterCnt = parameterCnt + 1/>
        </#list>
        <#assign initializerCnt = parameterCnt/>
    </#list>
    <#assign classCnt = initializerCnt/>
</#list>
}