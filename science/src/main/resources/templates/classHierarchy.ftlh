digraph G {
<#assign count = 0 />
<#macro hierarchy class sourceId>
    ${sourceId?c}[label="${class.getName()}"]
    <#if classHierarchy.getClassHierarchy()?api.get(class)??>
        <#list classHierarchy.getClassHierarchy()?api.get(class) as subclass>
            <#assign count = count + 1/>
            ${sourceId?c}->${count}
            <@hierarchy subclass count/></#list></#if></#macro>
<@hierarchy entryPoint 0/>
}