<#include "../nodeWrapperMacro.ftlh">
# Problem Definitions
<#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem>
## ${problem.name} - (${problem.searchSpace?api.getSearchSpace()?size} trees)
    <#if !omitSearchSpace?? || !omitSearchSpace>
    <@searchSpace problem.searchSpace?api.getSearchSpace() debug "```dot" "```" />
    </#if>
</#list>
# Patterns per Problem

```latex{cmd=true hide=true}
\documentclass{standalone}
\usepackage{tikz}
\usepackage{pgfplots}
\begin{document}
\begin{tikzpicture}
\begin{axis}[
  xtick={<#list patternGroupings?reverse?reverse as pair>${pair.key}<#if pair_has_next>,</#if></#list>},
  symbolic x coords = {<#list patternGroupings?reverse?reverse as pair>${pair.key}<#if pair_has_next>,</#if></#list>},
	ylabel=Patterns,
  xlabel=Problems,
  legend pos=outer north east,
	ybar,
]
\addplot
	coordinates {<#list patternGroupings?reverse?reverse as pair>(${pair.key}, ${pair.value})<#if pair_has_next> </#if></#list>};
\end{axis}
\end{tikzpicture}
\end{document}
```

<#assign ppp = truffleDifferentialPattern.patternsPerProblem />
<#list truffleDifferentialPattern.patternsPerProblem?keys?sort_by("name") as problem>
## ${problem.name} - (${ppp?api.get(problem)?size} patterns)
    <#list ppp?api.get(problem) as trufflePattern>
        <@pattern trufflePattern false/>

    </#list>

</#list>

# Differential between Problems

<#assign diff = truffleDifferentialPattern.differential />
<#list truffleDifferentialPattern.differential?keys?sort_by("size") as trufflePattern>
    <@pattern trufflePattern false/>

    <#list diff?api.get(trufflePattern) as keypair, diffvalue>
${keypair.key.name} - ${keypair.value.name}: ${diffvalue}
    </#list>
</#list>