<#macro correctId node><#if node.class.simpleName == "NodeWrapper">${node.getId()?c}<#else>_${node.matchedNodes?join("_")?replace(",","")}</#if></#macro>
<#macro graph node debug>
    <@correctId node/>[<#if debug><@debugLabel node /><#else><@label node /></#if>]
    <#list node.children as orderedRel>
        <@correctId node/>-><@correctId orderedRel.child />
        <@graph orderedRel.child debug />
    </#list>
</#macro>
<#macro nodeId node><@correctId node /><#list node.children as orderedRel>, <@nodeId orderedRel.child /></#list></#macro>
<#macro label node>label="${node.simplifiedType()}"</#macro>
<#macro debugLabel node>shape=none, margin=0, label=<
<table border="0" cellborder="1" cellspacing="0" cellpadding="4">
    <tr>
        <td colspan="3">${node.simplifiedType()}</td>
    </tr>
    <#list node.values as key, value>
    <tr>
        <td><#if key?contains(":")>${key[0..key?index_of(":")-1]}<#else>${key}</#if></td>
        <td><#if key?contains(":")>${key[key?index_of(":")+1..]}</#if></td>
        <td>${value}</td>
    </tr>
    </#list>
</table>
></#macro>
<#macro pattern trufflePattern debug containerStart="```dot" containerEnd="```">
<table class="tg" idList="<#list trufflePattern.nodeIds as node>${node?c}<#if node_has_next>,</#if></#list>">
    <tr>
        <td>Occurence / Trees</td>
        <td>${trufflePattern.count} / ${trufflePattern.getTreeCount()}</td>
    </tr>
    <#if debug?? && debug>
    <tr>
        <td>Trees</td>
        <td><#list trufflePattern.treeIds as tree>${tree?c}<#if tree_has_next>, </#if></#list></td>
    </tr>
    <tr>
        <td>Nodes</td>
        <td><#list trufflePattern.nodeIds as node>${node?c}<#if node_has_next>, </#if></#list></td>
    </tr>
    <tr>
        <td>Debug Queries:</td>
        <td>
            <table class="nested">
                <tr>
                    <td>Pattern in DB</td>
                    <td>MATCH (n) WHERE id(n) IN [<#list trufflePattern.treeIds as node>${node?c}<#if node_has_next>, </#if></#list>] RETURN (n)</td>
                </tr>
                <tr>
                    <td>All Nodes in DB</td>
                    <td>MATCH (n) WHERE id(n) IN [<#list trufflePattern.nodeIds as node>${node?c}<#if node_has_next>, </#if></#list>] RETURN (n)</td>
                </tr>
            </table>
        </td>
    </tr>
    </#if>
</table>
${containerStart?no_esc}
digraph G {
    <@graph trufflePattern.patternNode debug />
}
${containerEnd?no_esc}</#macro>
<#macro searchSpace seSp debug containerStart="```dot" containerEnd="```">
    <#if debug>MATCH (n) WHERE id(n) IN [<#list seSp as tree>${tree.key[0].id?c}<#if tree_has_next>, </#if></#list>] RETURN (n)</#if>
    <#list seSp as tree>
${containerStart?no_esc}
digraph G {
<#list tree.key as node>
    ${node.id?c}[label="${node.simplifiedType()}"]
</#list>
<#list tree.value as rel>
    ${rel.parent.id?c}->${rel.child.id?c}
</#list>
}
${containerEnd?no_esc}
    </#list>
</#macro>