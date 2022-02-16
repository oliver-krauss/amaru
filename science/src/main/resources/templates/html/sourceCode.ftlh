<#ftl auto_esc=false>
<#macro codifiyChildren node tli><#list node.children as rel><@codify rel.child tli /></#list></#macro>
<#macro codifiyChildrenLB node tli><#list node.children as rel>  <@codify rel.child tli />;
</#list></#macro>
<#macro codifyRelations relations tli><#list relations as rel><@codify rel.child tli /></#list></#macro>
<#macro codify node tli>
<#switch tli.getClass(node.type).shortName>
<#case "block">
{
<@codifiyChildrenLB node tli />
}<#break>
<#case "for">
for (<@codifyRelations node.getChildren("initNode") tli />;<@codifyRelations node.getChildren("loopNode")[0].child.children[0].child.getChildren("conditionNode") tli />; <@codifyRelations node.getChildren("loopNode")[0].child.children[0].child.getChildren("stepNode") tli />) <@codifyRelations node.getChildren("loopNode")[0].child.children[0].child.getChildren("bodyNode") tli /><#break>
<#case "while">
while (<@codifyRelations node.getChildren("loopNode")[0].child.children[0].child.getChildren("conditionNode") tli />) <@codifyRelations node.getChildren("loopNode")[0].child.children[0].child.getChildren("stepNode") tli />) <@codifyRelations node.getChildren("loopNode")[0].child.children[0].child.getChildren("bodyNode") tli /><#break>
<#case "if">
if (<@codifyRelations node.getChildren("condition") tli />) <@codifyRelations node.getChildren("thenPath") tli />
<#if node.getChildren("elsePath")?size != 0> else <@codifyRelations node.getChildren("elsePath") tli /></#if><#break>
<#case "return">
return <@codifiyChildren node tli /><#break>
<#case "write-local-float"><#case "write-local-int">
${node.values["slot:com.oracle.truffle.api.frame.FrameSlot"]} = <@codifiyChildren node tli /><#break>
<#case "copy-array-local-generic">
ARRCPY_${node.values["slot:com.oracle.truffle.api.frame.FrameSlot"]} = <@codifiyChildren node tli /><#break>
<#case "read-arg-float"><#case "read-arg-int"><#case "read-arg-generic">
ARG${node.values["index:int"]}<#break>
<#case "read-local-float"><#case "read-local-int">
${node.values["slot:com.oracle.truffle.api.frame.FrameSlot"]}<#break>
<#case "allocate-array-local-int"><#case "allocate-array-local-float">
int ${node.values["slot:com.oracle.truffle.api.frame.FrameSlot"]}[<#list node.getChildren("size") as rel><@codify rel.child tli /><#if rel_has_next>,</#if></#list>]<#break>
<#case "read-array-global-float"><#case "read-array-local-char"><#case "read-array-local-int"><#case "read-array-local-float">
${node.values["slot:com.oracle.truffle.api.frame.FrameSlot"]}[<#list node.getChildren("arrayPosition") as rel><@codify rel.child tli /><#if rel_has_next>,</#if></#list>]<#break>
<#case "write-array-global-float"><#case "write-array-local-char"><#case "write-array-local-int"><#case "write-array-local-float">
${node.values["slot:com.oracle.truffle.api.frame.FrameSlot"]}[<#list node.getChildren("arrayPosition") as rel><@codify rel.child tli /><#if rel_has_next>,</#if></#list>] = <@codifyRelations node.getChildren("valueNode_") tli /><#break>
<#case "(int) float"><#case "(int) double">
(int) <@codifiyChildren node tli /><#break>
<#case "(float) int"><#case "(float) double">
(float) <@codifiyChildren node tli /><#break>
<#case "(double) int"><#case "(double) float">
(double) <@codifiyChildren node tli /><#break>
<#case "IntLiteral">
${node.values["value:int"]}<#break>
<#case "FloatLiteral">
${node.values["value:float"]}<#break>
<#case "DoubleLiteral">
${node.values["value:double"]}<#break>
<#case "function-literal">
${node.values["name:java.lang.String"]}<#break>
<#case "+"><#case "-"><#case "*"><#case "/"><#case "%"><#case "<"><#case "<="><#case ">"><#case ">="><#case "=="><#case "!="><#case "&&"><#case "||">
(<@codifyRelations node.getChildren("leftNode_") tli /> ${tli.getClass(node.type).shortName} <@codifyRelations node.getChildren("rightNode_") tli />)<#break>
<#case "!">
${tli.getClass(node.type).shortName}(<@codifiyChildren node tli />)<#break>
<#case "invoke-float"><#case "invoke-int"><#case "invoke-void">
<@codifyRelations node.getChildren("functionNode_") tli />(<#list node.getChildren("argumentNodes") as rel><@codify rel.child tli /><#if rel_has_next>,</#if></#list>)<#break>
<#default>
UNKNOWN type ${tli.getClass(node.type).shortName};
    <#list node.values as key, value>
        ${key} = ${value}
    </#list>
    <#list node.children as child>
        ${child.field}
    </#list>
</#switch>
</#macro>

<@codify sourceCode tli />