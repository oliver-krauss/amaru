\begin{table}[]
\begin{center}
\begin{tabular}{|<#list dataset.titles as title>l|</#list>}
\hline
  <#list dataset.titles as title>${title}<#if title_has_next>&</#if></#list> \\ \hline
<#assign rows = dataset.data[0]?size - 1 />
<#assign cols = dataset.data?size - 1 />
<#list 0..rows as i>
  <#list 0..cols as j>${dataset.data[j][i]}<#if j_has_next>&</#if></#list> \\ \hline
</#list>
\end{tabular}
\caption{${caption}}
\label{${label}}
\end{center}
\end{table}