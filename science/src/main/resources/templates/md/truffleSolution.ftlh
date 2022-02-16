<#include "../nodeWrapperMacro.ftlh">
<#include "../solutionMacro.ftlh">

<@solutionQuality truffleSolution debug/>

# Problem Definitions
<#list problems?sort_by("name") as problem>
## ${problem.name}
    <@searchSpace problem.searchSpace debug "```dot" "```" />

</#list>
# Patterns per Problem

<#list problems?sort_by("name") as problem>
## ${problem.name}
    <#list truffleSolution.solutionGenes as gene>
        <#if helper.containsProblem(gene, problem)>
            <@pattern gene.gene false/>
        </#if>
    </#list>

</#list>