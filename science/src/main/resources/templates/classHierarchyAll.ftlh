digraph G {
<#assign count = classHierarchyAll.getClassHierarchy()?size>
<#list classHierarchyAll.getClassHierarchy() as key, value>
    <#assign idx = key?index />
    ${idx}[label="${key.getName()}"]
    <#list value as relation>
        <#assign found = false/>
        ${idx}-><#list classHierarchyAll.getClassHierarchy() as subkey, subvalue><#if subkey.getName() == relation.getName()>${subkey?index}<#assign found = true></#if></#list><#if !found>${count}
        ${count}[label="${relation.getName()}"]<#assign count = count + 1 /></#if>
    </#list>
</#list>
}