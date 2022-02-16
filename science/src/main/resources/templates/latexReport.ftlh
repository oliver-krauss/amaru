\begin{table}[]
\begin{center}
<#if report.getChildReports()?size != 0 >
    <#assign keys = report.getChildReports()?keys />
    <#assign values = report.getReport(0).getValues()?keys />
    <#assign valuePad = report.longestChildKey()/>
    <#assign keyPad = report.longestChildString()/>
\begin{tabular}{|l|<#list keys as key>l|</#list>}
\hline
  ${""?right_pad(valuePad)} & <#list keys as key>\textbf{${key}}${""?right_pad(keyPad - key?length - 9)} <#if key_has_next>& </#if></#list> \\ \hline
    <#list values as value>
        <#if value != "index">
  ${value?left_pad(valuePad)} & <#list keys as key>${report.getReport(key).getValue(value)?right_pad(keyPad + 3)} <#if key_has_next>& </#if></#list>  \\ \hline
        </#if>
    </#list>
\end{tabular}
</#if>

<#if report.getValues()?size != 0 >
    <#assign longest = report.longestKey()/>
\begin{tabular}{|l|l|}
\hline
    <#list report.values as key, value>
        <#if key != "label">
  ${key?left_pad(longest)} & ${value.value} \\ \hline
        </#if>
    </#list>
\end{tabular}
</#if>
\caption{${caption}}
\label{${label}}
\end{center}
\end{table}