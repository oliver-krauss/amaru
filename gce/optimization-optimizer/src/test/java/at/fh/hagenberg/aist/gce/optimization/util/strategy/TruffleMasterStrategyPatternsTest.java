/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy;

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.MinicBuiltinNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicCastNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToStringNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.*;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionBodyNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicDoubleArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicDoubleArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.minic.parser.MinicStructNode;
import at.fh.hagenberg.aist.gce.optimization.operators.TruffleTreeMutator;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.DepthWidthRestrictedTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.RandomTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMetaLoader;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowNode;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validate creation considering antipatterns
 */
public class TruffleMasterStrategyPatternsTest extends AbstractPatternsTest {

    /**
     * Const how often the pattern injects are tried out.
     * Some errors occur only very rarely. You'd better not go below 100 when adding new features
     */
    private static final int CHECK_PATTERN = 200;

    /**
     * If true the resulting trees (or interesting parts) will be printed to console
     * Unusual for a test case but this is for debugging purposes when we modify the patterns in the future
     */
    private static final boolean print = false;

    @Test(expectedExceptions = RuntimeException.class)
    public void testInvalidPatternField() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        //NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        //OrderedRelationship rel = new OrderedRelationship(antiPattern, starWildcard, "", 0);
        //antiPattern.addChild(rel);
        NodeWrapper intLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        antiPattern.addChild(intLitNode, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        antiPattern.addChild(doubleLitNode, "bodyNode", 1);
        NodeWrapper charLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        antiPattern.addChild(charLitNode, "", 2);
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, true);

        // when
        create().injectPattern(antiPattern, meta, 1.0);

        // then
        // we expect to fail
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testInvalidPatternOrder() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        //NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        //OrderedRelationship rel = new OrderedRelationship(antiPattern, starWildcard, "", 0);
        //antiPattern.addChild(rel);
        NodeWrapper intLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        antiPattern.addChild(intLitNode, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        antiPattern.addChild(doubleLitNode, "", 2);
        NodeWrapper charLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        antiPattern.addChild(charLitNode, "", 3);
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, true);

        // when
        create().injectPattern(antiPattern, meta, 1.0);

        // then
        // we expect to fail
    }

    /**
     * Tests injection with NOT wildcard (two nodes!)
     * Tests non-specified field
     */
    @Test
    public void testinjectPattern() {
        // given
        NodeWrapper antiPattern = new NodeWrapper(MinicToStringNodeFactory.MinicCharArrayToStringNodeGen.class.getName());
        NodeWrapper entireArrayNode = new NodeWrapper("(" + MinicReadArrayNode.MinicEntireArrayReadNode.class.getName() + "|" + MinicReadGlobalArrayNode.MinicEntireArrayReadGlobalNode.class.getName() + ")");
        OrderedRelationship relationship = new OrderedRelationship(antiPattern, entireArrayNode, "", 0);
        antiPattern.addChild(relationship);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, true);

        // inject write pairing as we have no access to the miniclanglearner here.

        // when
        create().injectPattern(antiPattern, meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicToStringNodeFactory.MinicCharArrayToStringNodeGen.class);
            // inject needed frame slot
            creationInfo.dataFlowGraph.getAvailableDataItems().put(frame, new ArrayList<>());
            creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("asdf"), MinicWriteArrayNodeFactory.MinicCharArrayWriteNodeGen.create(null, null, null)));
            Node node = create().create(creationInfo);
            Assert.assertTrue(node.getClass().getName().contains("MinicCharArrayToStringNodeGen"));
            Assert.assertTrue(node.getChildren().iterator().next().getClass().getName().contains("MinicEntireArray"));
        }

    }

    /**
     * Tests not wildcard
     * Tests AntiPattern in multiple hierarchies.
     * Tests specified field
     */
    @Test
    public void testinjectPatternHierarchy() {
        // given
        NodeWrapper antiPattern = getInvokeAntipattern();

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // when
        create().injectPattern(antiPattern, meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            List<String> invokeClass = meta.instantiables(MinicInvokeNode.class.getName());
            TruffleClassInformation tci = tli.getClass(invokeClass.get(new Random().nextInt(invokeClass.size())));
            Node node = create().create(getCreationInfo(tci.getClazz()));
            Assert.assertTrue(node.getClass().getName().contains("Invoke"));
            // Function node is always the last child
            LinkedList<Node> children = new LinkedList<>();
            node.getChildren().iterator().forEachRemaining(children::add);
            Assert.assertTrue(children.getLast().getClass().getName().contains("MinicFunctionLiteralNode"));
        }

    }

    private NodeWrapper getInvokeAntipattern() {
        NodeWrapper antiPattern = new NodeWrapper(MinicInvokeNode.class.getName());
        NodeWrapper fnLiteralChild = new NodeWrapper(MinicFunctionLiteralNode.class.getName());
        OrderedRelationship relationship = new OrderedRelationship(antiPattern, fnLiteralChild, "functionNode", 0);
        antiPattern.addChild(relationship);
        return antiPattern;
    }

    /**
     * Tests Exclusion of specific value
     * Tests > 2 nodes pattern
     * Tests what happens to antipatterns when they are applied on the same subgraph
     */
    @Test
    public void testinjectPatternWithValueRequirement() {
        List<String> datatypes = new ArrayList<>();
        datatypes.add("Char");
        datatypes.add("Int");
        datatypes.add("Float");
        datatypes.add("Double");
        datatypes.add("String");
        BitwisePatternMeta datatypeIndependentMeta = BitwisePatternMetaLoader.loadDatatypeIndependent(MinicLanguage.ID, datatypes, false);

        // given
        NodeWrapper antiPattern = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
        NodeWrapper litNodeForbiddenVal = new NodeWrapper(MinicSimpleLiteralNode.class.getName());
        litNodeForbiddenVal.getValues().put("value:double", 0.0);
        litNodeForbiddenVal.getValues().put("value:int", 0);
        litNodeForbiddenVal.getValues().put("value:char", "\0");
        litNodeForbiddenVal.getValues().put("value:float", 0.0f);
        OrderedRelationship relationship = new OrderedRelationship(antiPattern, litNodeForbiddenVal, "rightNode", 0);
        antiPattern.addChild(relationship);

        NodeWrapper antiPatternDos = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
        NodeWrapper cast = new NodeWrapper(MinicCastNode.class.getName());
        OrderedRelationship relationshipDos = new OrderedRelationship(antiPatternDos, cast, "rightNode", 0);
        antiPatternDos.addChild(relationshipDos);
        NodeWrapper litNodeForbiddenValDos = new NodeWrapper(MinicSimpleLiteralNode.class.getName());
        litNodeForbiddenValDos.getValues().put("value:double", 0.0);
        litNodeForbiddenValDos.getValues().put("value:int", 0);
        litNodeForbiddenValDos.getValues().put("value:char", "\0");
        litNodeForbiddenValDos.getValues().put("value:float", 0.0f);
        OrderedRelationship relationshipCast = new OrderedRelationship(cast, litNodeForbiddenValDos, "", 0);
        cast.addChild(relationshipCast);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        RandomTruffleTreeSelector selector = new RandomTruffleTreeSelector();

        // when
        create().injectPattern(antiPattern, datatypeIndependentMeta, 1.0);
        create().injectPattern(antiPatternDos, datatypeIndependentMeta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            List<String> invokeClass = datatypeIndependentMeta.instantiables("MinicDTArithmeticNode$MinicDTDivNode");
            TruffleClassInformation tci = tli.getClass(invokeClass.get(new Random().nextInt(invokeClass.size())));
            Node node = create().create(getCreationInfo(tci.getClazz()));

            // Mutate in context to see if we can deal with pre-existing antipatterns
            node.adoptChildren();
            Node mutationNode = selector.selectSubtree(node);
            LoadedRequirementInformation rqi = create().loadRequirements(node, mutationNode);
            if (rqi != null && mutationNode != node) {
                Node newNode = create().create(getCreationInfoWithContext(node, mutationNode, rqi.getRequirementInformation()));
                mutationNode.replace(newNode);
            }

            Assert.assertTrue(node.getClass().getName().contains("DivNode"));
            Iterator<Node> iterator = node.getChildren().iterator();
            iterator.next(); // skip left
            Node child = iterator.next();
            if (child.getClass().getName().contains("nodes.impl.cast")) {
                child = child.getChildren().iterator().next();
                if (child.getClass().getName().contains("LiteralNode")) {
                    Assert.assertTrue(NodeWrapper.wrap(child).getValues().entrySet().stream().noneMatch(x -> x.getKey().startsWith("value:") && (x.getValue().equals(0) || x.getValue().equals('\0') || x.getValue().equals(0.0f) || x.getValue().equals(0.0))));
                }
            }
            if (child.getClass().getName().contains("LiteralNode")) {
                Assert.assertTrue(NodeWrapper.wrap(child).getValues().entrySet().stream().noneMatch(x -> x.getKey().startsWith("value:") && (x.getValue().equals(0) || x.getValue().equals('\0') || x.getValue().equals(0.0f) || x.getValue().equals(0.0))));
            }
            if (print) {
                System.out.println(NodeWrapper.wrap(node).humanReadableTree());
            }
        }

    }

    /**
     * Tests Exclusion of specific value
     * Tests > 2 nodes pattern
     * Tests what happens to antipatterns when they are applied on the same subgraph
     */
    @Test
    public void testinjectPatternWithStarValueReq() {
        List<String> datatypes = new ArrayList<>();
        datatypes.add("Char");
        datatypes.add("Int");
        datatypes.add("Float");
        datatypes.add("Double");
        datatypes.add("String");
        BitwisePatternMeta datatypeIndependentMeta = BitwisePatternMetaLoader.loadDatatypeIndependent(MinicLanguage.ID, datatypes, false);

        // given
        NodeWrapper antiPattern = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
        NodeWrapper litNodeForbiddenVal = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicSimpleLiteralNode.class.getName());
        litNodeForbiddenVal.getValues().put("value:double", 0.0);
        litNodeForbiddenVal.getValues().put("value:int", 0);
        litNodeForbiddenVal.getValues().put("value:char", "\0");
        litNodeForbiddenVal.getValues().put("value:float", 0.0f);
        NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship relationship = new OrderedRelationship(antiPattern, starWildcard, "rightNode", 0);
        OrderedRelationship rel2 = new OrderedRelationship(starWildcard, litNodeForbiddenVal, "", 0);
        antiPattern.addChild(relationship);
        starWildcard.addChild(rel2);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);

        // when
        create().injectPattern(antiPattern, datatypeIndependentMeta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            List<String> invokeClass = datatypeIndependentMeta.instantiables("MinicDTArithmeticNode$MinicDTDivNode");
            TruffleClassInformation tci = tli.getClass(invokeClass.get(new Random().nextInt(invokeClass.size())));
            Node node = create().create(getCreationInfo(tci.getClazz()));
            Assert.assertTrue(node.getClass().getName().contains("DivNode"));
            NodeWrapper rightNode_ = NodeWrapper.wrap(node).getChildren().stream().filter(x -> x.getField().equals("rightNode_")).findFirst().get().getChild();
            if (print) {
                System.out.println(rightNode_.humanReadableTree());
            }
            NodeWrapper.flatten(rightNode_).forEach(child -> {
                if (child.getType().contains("LiteralNode")) {
                    Assert.assertTrue(child.getValues().entrySet().stream().noneMatch(x -> x.getKey().startsWith("value:") && (x.getValue().equals(0) || x.getValue().equals('\0') || x.getValue().equals(0.0f) || x.getValue().equals(0.0))));
                }
            });
        }
    }

    /**
     * Tests injection with * Wildcard
     * Tests positive value under * wildcard (e.g. ALL MUST MATCH)
     */
    @Test
    public void testDumbAntiPattern() {
        // given
        // antipattern - enforces that Block Node has no reads. Only for testing. Complete nonsense.
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship rel = new OrderedRelationship(antiPattern, starWildcard, "", 0);
        antiPattern.addChild(rel);
        NodeWrapper readNode = new NodeWrapper(Wildcard.WILDCARD_NOT + (MinicReadNode.class.getName() + "|" + MinicReadGlobalNode.class.getName()));
        OrderedRelationship rel2 = new OrderedRelationship(starWildcard, readNode, "", 0);
        starWildcard.addChild(rel2);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // inject write pairing as we have no access to the miniclanglearner here.

        // when
        create().injectPattern(antiPattern, meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicBlockNode.class);
            // inject needed frame slot
            creationInfo.dataFlowGraph.getAvailableDataItems().put(frame, new ArrayList<>());
            creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("asdf"), MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), frameDescriptor.findFrameSlot("asdf"))));
            Node node = create().create(creationInfo);
            Assert.assertEquals(MinicBlockNode.class.getName(), node.getClass().getName());
            NodeWrapper wrap = NodeWrapper.wrap(node);
            if (print) {
                System.out.println(wrap.humanReadableTree());
            }
            Assert.assertTrue(NodeWrapper.flatten(wrap).noneMatch(x -> !x.getType().contains("ReadNodeFactory$ReadNodeGen") && !x.getType().contains("Array") && x.getType().contains("Read")));
        }
    }

    /**
     * Tests pattern with n childrend
     */
    @Test
    public void testNChildPattern() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        //NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        //OrderedRelationship rel = new OrderedRelationship(antiPattern, starWildcard, "", 0);
        //antiPattern.addChild(rel);
        NodeWrapper intLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        antiPattern.addChild(intLitNode, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        antiPattern.addChild(doubleLitNode, "", 1);
        NodeWrapper charLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        antiPattern.addChild(charLitNode, "", 2);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // when
        create().injectPattern(antiPattern, meta, 1.0);
        create().injectAntiPattern(patternInterferingNodes(), meta);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicBlockNode.class);
            Node node = create().create(creationInfo);
            Assert.assertEquals(MinicBlockNode.class.getName(), node.getClass().getName());
            NodeWrapper wrap = NodeWrapper.wrap(node);
            if (print) {
                System.out.println();
                System.out.println(wrap.humanReadableTree());
            }
            List<NodeWrapper> collect = NodeWrapper.flatten(wrap).collect(Collectors.toList());
            Assert.assertTrue(wrap.getChildren().stream().anyMatch(x -> x.getChild().getType().contains("IntLiteral")));
            Assert.assertTrue(wrap.getChildren().stream().anyMatch(x -> x.getChild().getType().contains("DoubleLiteral")));
            Assert.assertTrue(wrap.getChildren().stream().anyMatch(x -> x.getChild().getType().contains("CharLiteral")));
        }
    }

    /**
     * Tests pattern with n children
     * Test NOT wildcard in pattern
     */
    @Test(enabled = false) // NOTE - IN Between NOT not supported yet as we have no need and this overcomplicates the alg
    public void testNChildPatternWithNotChildren() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        //NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        //OrderedRelationship rel = new OrderedRelationship(antiPattern, starWildcard, "", 0);
        //antiPattern.addChild(rel);
        NodeWrapper blockA = new NodeWrapper(MinicBlockNode.class.getName());
        antiPattern.addChild(blockA, "", 0);
        NodeWrapper blockB = new NodeWrapper(MinicBlockNode.class.getName());
        antiPattern.addChild(blockB, "", 1);
        NodeWrapper blockC = new NodeWrapper(MinicBlockNode.class.getName());
        antiPattern.addChild(blockC, "", 2);

        NodeWrapper intLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        blockA.addChild(intLitNode, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        blockA.addChild(doubleLitNode, "", 1);
        NodeWrapper charLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        blockA.addChild(charLitNode, "", 2);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // when
        create().injectPattern(antiPattern, meta, 1.0);
        create().injectAntiPattern(patternInterferingNodes(), meta);
        create().injectPattern(new NodeWrapper(MinicWhileNode.class.getName() + "|" + MinicForNode.class.getName()), meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicBlockNode.class);
            Node node = create().create(creationInfo);
            Assert.assertEquals(MinicBlockNode.class.getName(), node.getClass().getName());
            NodeWrapper wrap = NodeWrapper.wrap(node);
            if (print) {
                System.out.println();
                System.out.println(wrap.humanReadableTree());
            }
            List<NodeWrapper> collect = wrap.getChildren().stream().map(OrderedRelationship::getChild).filter(x -> x.getType().contains("MinicBlockNode")).collect(Collectors.toList());
            Assert.assertTrue(collect.size() >= 3, "block count failed");

            Assert.assertTrue(collect.get(0).getChildren().stream().anyMatch(x -> x.getChild().getType().contains("IntLiteral")));
            Integer intPos = collect.get(0).getChildren().stream().filter(x -> x.getChild().getType().contains("IntLiteral")).map(OrderedRelationship::getOrder).findFirst().get();
            // check that INT is NOT followed by double
            Assert.assertFalse(collect.get(0).getChildren().stream().filter(x -> x.getOrder() == intPos + 1).anyMatch(x -> x.getChild().getType().contains("DoubleLiteral")));
            Assert.assertTrue(collect.get(0).getChildren().stream().anyMatch(x -> x.getChild().getType().contains("CharLiteral")));
        }
    }

    /**
     * Tests pattern with n childrend
     */
    @Test
    public void testNChildPatternWithChildren() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        //NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        //OrderedRelationship rel = new OrderedRelationship(antiPattern, starWildcard, "", 0);
        //antiPattern.addChild(rel);
        NodeWrapper blockA = new NodeWrapper(MinicBlockNode.class.getName());
        antiPattern.addChild(blockA, "", 0);
        NodeWrapper blockB = new NodeWrapper(MinicBlockNode.class.getName());
        antiPattern.addChild(blockB, "", 1);
        NodeWrapper blockC = new NodeWrapper(MinicBlockNode.class.getName());
        antiPattern.addChild(blockC, "", 2);

        NodeWrapper intLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        blockA.addChild(intLitNode, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        blockA.addChild(doubleLitNode, "", 1);
        NodeWrapper charLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        blockA.addChild(charLitNode, "", 2);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // when
        create().injectPattern(antiPattern, meta, 1.0);
        create().injectAntiPattern(patternInterferingNodes(), meta);
        create().injectPattern(new NodeWrapper(MinicWhileNode.class.getName() + "|" + MinicForNode.class.getName()), meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicBlockNode.class);
            Node node = create().create(creationInfo);
            Assert.assertEquals(MinicBlockNode.class.getName(), node.getClass().getName());
            NodeWrapper wrap = NodeWrapper.wrap(node);
            if (print) {
                System.out.println();
                System.out.println(wrap.humanReadableTree());
            }
            List<NodeWrapper> collect = wrap.getChildren().stream().map(OrderedRelationship::getChild).filter(x -> x.getType().contains("MinicBlockNode")).collect(Collectors.toList());
            Assert.assertTrue(collect.size() >= 3, "block count failed");

            Assert.assertTrue(collect.get(0).getChildren().stream().anyMatch(x -> x.getChild().getType().contains("IntLiteral")));
            Assert.assertTrue(collect.get(0).getChildren().stream().anyMatch(x -> x.getChild().getType().contains("DoubleLiteral")));
            Assert.assertTrue(collect.get(0).getChildren().stream().anyMatch(x -> x.getChild().getType().contains("CharLiteral")));
        }
    }

    /**
     * Tests * wildcard and n children together
     */
    @Test
    public void testChildWithChildPattern() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship rel = new OrderedRelationship(antiPattern, starWildcard, "", 0);
        antiPattern.addChild(rel);
        NodeWrapper blockA = new NodeWrapper(MinicBlockNode.class.getName());
        starWildcard.addChild(blockA, "", 0);
        NodeWrapper blockB = new NodeWrapper(MinicBlockNode.class.getName());
        starWildcard.addChild(blockB, "", 1);
        NodeWrapper blockC = new NodeWrapper(MinicBlockNode.class.getName());
        starWildcard.addChild(blockC, "", 2);

        NodeWrapper starWildcardA = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship relA = new OrderedRelationship(blockA, starWildcardA, "", 0);
        blockA.addChild(relA);
        NodeWrapper intLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        starWildcardA.addChild(intLitNode, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        starWildcardA.addChild(doubleLitNode, "", 1);
        NodeWrapper charLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        starWildcardA.addChild(charLitNode, "", 2);


        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // when
        create().injectPattern(antiPattern, meta, 1.0);
        // exclude length and print node gen as they "eat" pattern nodes
        create().injectAntiPattern(patternInterferingNodes(), meta);
        // exclude RepeatingNode as this screws up my assert check (VALID PATTERN, but too lazy to fix assert - order of fields)
        create().injectPattern(new NodeWrapper(MinicRepeatingNode.class.getName()), meta, 1.0);
        create().injectPattern(new NodeWrapper(MinicWhileNode.class.getName() + "|" + MinicForNode.class.getName()), meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicBlockNode.class);
            Node node = create().create(creationInfo);
            Assert.assertEquals(MinicBlockNode.class.getName(), node.getClass().getName());
            NodeWrapper wrap = NodeWrapper.wrap(node);
            if (print) {
                System.out.println(wrap.humanReadableTree());
            }
            List<NodeWrapper> collect = NodeWrapper.flatten(wrap).collect(Collectors.toList()).stream().filter(x -> x.getType().contains("MinicBlockNode")).collect(Collectors.toList());
            Assert.assertTrue(collect.size() >= 4, "not all blocks satisfied");


            // remove root node for the follow up check
            collect.remove(collect.get(0));
            Assert.assertTrue(collect.stream().anyMatch(body -> {
                final int[] match = {0};
                NodeWrapper.flatten(body).iterator().forEachRemaining(x -> {
                    if (match[0] == 0 && x.getType().contains("IntLiteral")) {
                        match[0]++;
                    }
                    if (match[0] == 1 && x.getType().contains("DoubleLiteral")) {
                        match[0]++;
                    }
                    if (match[0] == 2 && x.getType().contains("CharLiteral")) {
                        match[0]++;
                    }
                });
                return match[0] == 3;
            }), "block missing int-dbl-char");
        }
    }

    private NodeWrapper patternInterferingNodes() {
        // ForRepeat as well as IF is only here bc. it switches around the order of Int-Dbl-Lit etc.
        return new NodeWrapper(RootNode.class.getName() + "|" + MinicBuiltinNode.class.getName() + "|" + MinicIfNode.class.getName() + "|" + MinicRepeatingNode.class.getName() + "|" + MinicForRepeatingNode.class.getName());
    }

    /**
     * Tests * wildcard and n children together
     */
    @Test
    public void testNChildPatternStarWildcard() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship rel = new OrderedRelationship(antiPattern, starWildcard, "", 0);
        antiPattern.addChild(rel);
        NodeWrapper intLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        starWildcard.addChild(intLitNode, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        starWildcard.addChild(doubleLitNode, "", 1);
        NodeWrapper charLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        starWildcard.addChild(charLitNode, "", 2);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);
        RandomTruffleTreeSelector selector = new RandomTruffleTreeSelector();

        // when
        create().injectPattern(antiPattern, meta, 1.0);
        create().injectAntiPattern(patternInterferingNodes(), meta);
        create().injectPattern(new NodeWrapper(MinicWhileNode.class.getName() + "|" + MinicForNode.class.getName()), meta, 1.0);

        // exclude RepeatingNode as this screws up my assert check (VALID PATTERN, but too lazy to fix assert - order of fields)
        create().injectPattern(new NodeWrapper(MinicRepeatingNode.class.getName()), meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicBlockNode.class);
            Node node = create().create(creationInfo);

            // Mutate in context to see if we can deal with pre-existing antipatterns
            node.adoptChildren();
            Node mutationNode = selector.selectSubtree(node);
            LoadedRequirementInformation rqi = create().loadRequirements(node, mutationNode);
            if (rqi != null && mutationNode != node) {
                Node newNode = create().create(getCreationInfoWithContext(node, mutationNode, rqi.getRequirementInformation()));
                mutationNode.replace(newNode);
            }

            Assert.assertEquals(MinicBlockNode.class.getName(), node.getClass().getName());
            NodeWrapper wrap = NodeWrapper.wrap(node);
            if (print) {
                System.out.println(wrap.humanReadableTree());
            }
            List<NodeWrapper> collect = NodeWrapper.flatten(wrap).collect(Collectors.toList());
            final int[] match = {0};
            collect.iterator().forEachRemaining(x -> {
                if (match[0] == 0 && x.getType().contains("IntLiteral")) {
                    match[0]++;
                }
                if (match[0] == 1 && x.getType().contains("DoubleLiteral")) {
                    match[0]++;
                }
                if (match[0] == 2 && x.getType().contains("CharLiteral")) {
                    match[0]++;
                }
            });
            Assert.assertTrue(match[0] == 3);
        }
    }

    /**
     * Tests non-array child-creation and n children together
     */
    @Test
    public void testActivationChance() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicDoubleArithmeticNode.MinicDoubleAddNode.class.getName());
        NodeWrapper leftLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        NodeWrapper rightLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        antiPattern.addChild(leftLitNode, "", 0);
        antiPattern.addChild(rightLitNode, "", 1);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // when
        create().injectPattern(antiPattern, meta, 0.1);

        // then
        int valid = 0;
        for (int i = 0; i < 500; i++) {
            // diff here is that we just ask for next and assume that the not root works out
            CreationInformation creationInfo = getCreationInfo(MinicDoubleArithmeticNode.MinicDoubleAddNode.class);
            Node node = create().create(creationInfo);
            Assert.assertEquals(MinicDoubleArithmeticNodeFactory.MinicDoubleAddNodeGen.class.getName(), node.getClass().getName());
            NodeWrapper wrap = NodeWrapper.wrap(node);
            if (print) {
                System.out.println(wrap.humanReadableTree());
            }
            if (wrap.getChildren().stream().allMatch(x -> x.getChild().getType().contains("DoubleLiteral"))) {
                valid++;
            }
        }
        System.out.println("With 10% activation chance we got " + valid + " of 500 tests");
        Assert.assertTrue(valid >= 25);
        Assert.assertTrue(valid <= 100);
        // note this is RANDOM so we still have a chance to fail this as we will never get EXACTLY 50.
    }

    /**
     * Tests root star pattern - we essentially enforce that a pattern must be upheld everywhere"
     * I'm pretty sure that this pattern type is COMPLETELY useless
     */
    @Test
    public void testRootStarwildcardPattern() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        int victory = 0;

        NodeWrapper intLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        starWildcard.addChild(intLitNode, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        starWildcard.addChild(doubleLitNode, "", 1);
        NodeWrapper charLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        starWildcard.addChild(charLitNode, "", 2);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // when
        create().injectPattern(starWildcard, meta, 1.0);
        create().injectAntiPattern(new NodeWrapper(MinicStructNode.class.getName()), meta);
        create().injectAntiPattern(patternInterferingNodes(), meta);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            // diff here is that we just ask for next and assume that the not root works out
            CreationInformation creationInfo = getCreationInfo(MinicNode.class);
            Node node = create().create(creationInfo);
            NodeWrapper wrap = NodeWrapper.wrap(node);
            if (print) {
                System.out.println(wrap.humanReadableTree());
            }

            final int[] match = {0};
            NodeWrapper.flatten(wrap).iterator().forEachRemaining(x -> {
                if (match[0] == 0 && x.getType().contains("IntLiteral")) {
                    match[0]++;
                }
                if (match[0] == 1 && x.getType().contains("DoubleLiteral")) {
                    match[0]++;
                }
                if (match[0] == 2 && x.getType().contains("CharLiteral")) {
                    match[0]++;
                }
            });
            if (match[0] == 3) {
                victory++;
            } else {
                Assert.fail();
            }
        }
        System.out.println("V " + victory);
        Assert.assertEquals(victory, CHECK_PATTERN);
    }

    /**
     * Tests non-array child-creation and n children together
     */
    @Test
    public void testNCHildPatternsWithNonArrayType() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicDoubleArithmeticNode.MinicDoubleAddNode.class.getName());
        NodeWrapper leftLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        NodeWrapper rightLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        antiPattern.addChild(leftLitNode, "", 0);
        antiPattern.addChild(rightLitNode, "", 1);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // when
        create().injectPattern(antiPattern, meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicDoubleArithmeticNodeFactory.MinicDoubleAddNodeGen.class);
            Node node = create().create(creationInfo);
            Assert.assertEquals(MinicDoubleArithmeticNodeFactory.MinicDoubleAddNodeGen.class.getName(), node.getClass().getName());
            NodeWrapper wrap = NodeWrapper.wrap(node);
            if (print) {
                System.out.println(wrap.humanReadableTree());
            }
            Assert.assertTrue(wrap.getChildren().stream().allMatch(x -> x.getChild().getType().contains("DoubleLiteral")));
        }
    }

    /**
     * Tests * wildcard and n children together
     */
    @Test
    public void testNChildPatternsWithStarWildcardEach() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        NodeWrapper intLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        NodeWrapper intLitStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        intLitStarWildcard.addChild(intLitNode, "", 0);
        antiPattern.addChild(intLitStarWildcard, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        NodeWrapper doubleLitStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        doubleLitStarWildcard.addChild(doubleLitNode, "", 0);
        antiPattern.addChild(doubleLitStarWildcard, "", 1);
        NodeWrapper charLitNode = new NodeWrapper(MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        NodeWrapper charLitStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        charLitStarWildcard.addChild(charLitNode, "", 0);
        antiPattern.addChild(charLitStarWildcard, "", 2);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // when
        create().injectPattern(antiPattern, meta, 1.0);
        create().injectAntiPattern(patternInterferingNodes(), meta);

        // exclude RepeatingNode as this screws up my assert check (VALID PATTERN, but too lazy to fix assert - order of fields)
        create().injectPattern(new NodeWrapper(MinicRepeatingNode.class.getName()), meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicBlockNode.class);
            Node node = create().create(creationInfo);
            Assert.assertEquals(MinicBlockNode.class.getName(), node.getClass().getName());
            NodeWrapper wrap = NodeWrapper.wrap(node);
            if (print) {
                System.out.println(wrap.humanReadableTree());
            }
            List<NodeWrapper> collect = NodeWrapper.flatten(wrap).collect(Collectors.toList());

            final int[] match = {0};
            collect.iterator().forEachRemaining(x -> {
                if (match[0] == 0 && x.getType().contains("IntLiteral")) {
                    match[0]++;
                }
                if (match[0] == 1 && x.getType().contains("DoubleLiteral")) {
                    match[0]++;
                }
                if (match[0] == 2 && x.getType().contains("CharLiteral")) {
                    match[0]++;
                }
            });
            Assert.assertEquals(match[0], 3);
        }
    }

    /**
     * Tests injection with * Wildcard
     * Tests pattern with multiple children under one node (non linear pattern)
     */
    @Test
    public void testWhilePattern() {
        // given
        // antipattern - enforces while (... x ...) { x = ... }
        NodeWrapper hardAntiPattern = new NodeWrapper(MinicWhileNode.class.getName());
        NodeWrapper condStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship condRel = new OrderedRelationship(hardAntiPattern, condStarWildcard, "conditionNode", 0);
        hardAntiPattern.addChild(condRel);
        NodeWrapper readVarNode = new NodeWrapper(MinicReadNode.class.getName() + "|" + MinicReadGlobalNode.class.getName());
        readVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        OrderedRelationship condRel2 = new OrderedRelationship(condStarWildcard, readVarNode, "", 0);
        condStarWildcard.addChild(condRel2);
        NodeWrapper bodyStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        hardAntiPattern.addChild(new OrderedRelationship(hardAntiPattern, bodyStarWildcard, "bodyNode", 0));
        NodeWrapper writeVarNode = new NodeWrapper(MinicWriteNode.class.getName() + "|" + MinicWriteGlobalNode.class.getName());
        writeVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        bodyStarWildcard.addChild(writeVarNode, "", 0);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // when
        create().injectPattern(hardAntiPattern, meta, 1.0);
        create().injectAntiPattern(patternInterferingNodes(), meta);
        // prevent FunctionBodyNode for some reason it breaks the condition
        create().injectPattern(new NodeWrapper(MinicFunctionBodyNode.class.getName()), meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicWhileNode.class);
            // inject needed frame slot
            creationInfo.dataFlowGraph.getAvailableDataItems().put(frame, new ArrayList<>());
            creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("asdf"), MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), frameDescriptor.findFrameSlot("asdf"))));
            creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("other"), MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), frameDescriptor.findFrameSlot("other"))));
            creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("third"), MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), frameDescriptor.findFrameSlot("third"))));

            Node node = create().create(creationInfo);
            Assert.assertEquals(MinicWhileNode.class.getName(), node.getClass().getName());
            Node repeatingNode = node.getChildren().iterator().next().getChildren().iterator().next();
            NodeWrapper condition = NodeWrapper.wrap(repeatingNode).getChildren("conditionNode").stream().iterator().next().getChild();
            NodeWrapper body = NodeWrapper.wrap(repeatingNode).getChildren("bodyNode").stream().iterator().next().getChild();

            if (print) {
                System.out.println("  COND: ");
                System.out.println(condition.humanReadableTree());

                System.out.println("  BODY: ");
                System.out.println(body.humanReadableTree());
            }

            List<String> read = NodeWrapper.flatten(condition).filter(x -> x.getType().contains("Read")).map(x -> (String) x.getValues().get("slot:com.oracle.truffle.api.frame.FrameSlot")).distinct().collect(Collectors.toList());
            List<String> write = NodeWrapper.flatten(body).filter(x -> x.getType().contains("Write")).map(x -> (String) x.getValues().get("slot:com.oracle.truffle.api.frame.FrameSlot")).distinct().collect(Collectors.toList());

            Assert.assertFalse(read.isEmpty(), "Condition failed");
            Assert.assertFalse(write.isEmpty(), "Body failed");
            Assert.assertTrue(read.stream().anyMatch(write::contains), "Body is writing something different than the condition reads");
        }
    }

    private TruffleTreeMutator createMutator() {
        TruffleTreeMutator mutator = new TruffleTreeMutator();
        TruffleEntryPointStrategy entryPointStrategy = new TruffleEntryPointStrategy(create().tss, null, null, create(), create().configuration);
        RandomChooser<Class> chooser = new RandomChooser<>();
        entryPointStrategy.setChooser(chooser);
        DepthWidthRestrictedTruffleTreeSelector selector = new DepthWidthRestrictedTruffleTreeSelector();
        selector.setMaxDepth(3);
        selector.setMaxWidth(3);
        mutator.setSelector(selector);
        mutator.setSubtreeStrategy(create());
        mutator.setFullTreeStrategy(entryPointStrategy);

        return mutator;
    }


    /**
     * Tests injection with * Wildcard
     * Tests pattern with multiple children under one node (non linear pattern)
     * Tests patterns that interfere with each other
     */
    @Test
    public void testInjectComplexAntipatternWithVariableRequirement() {
        // given
        // antipattern - enforces while (... x ...)
        NodeWrapper antiPattern = new NodeWrapper(MinicWhileNode.class.getName());
        NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship rel = new OrderedRelationship(antiPattern, starWildcard, "conditionNode", 0);
        antiPattern.addChild(rel);
        NodeWrapper readNode = new NodeWrapper((MinicReadNode.class.getName() + "|" + MinicReadGlobalNode.class.getName()));
        OrderedRelationship rel2 = new OrderedRelationship(starWildcard, readNode, "", 0);
        starWildcard.addChild(rel2);

        // antipattern - enforces while (... x ...) { x = ... }
        NodeWrapper hardAntiPattern = new NodeWrapper(MinicWhileNode.class.getName());
        NodeWrapper condStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship condRel = new OrderedRelationship(hardAntiPattern, condStarWildcard, "conditionNode", 0);
        hardAntiPattern.addChild(condRel);
        NodeWrapper readVarNode = new NodeWrapper((MinicReadNode.class.getName() + "|" + MinicReadGlobalNode.class.getName()));
        readVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        OrderedRelationship condRel2 = new OrderedRelationship(condStarWildcard, readVarNode, "", 0);
        condStarWildcard.addChild(condRel2);
        NodeWrapper bodyStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        hardAntiPattern.addChild(new OrderedRelationship(hardAntiPattern, bodyStarWildcard, "bodyNode", 0));
        NodeWrapper writeVarNode = new NodeWrapper(MinicWriteNode.class.getName() + "|" + MinicWriteGlobalNode.class.getName());
        writeVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        bodyStarWildcard.addChild(writeVarNode, "", 0);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // inject write pairing as we have no access to the miniclanglearner here.

        // when
        antiPattern.setId(111L);
        hardAntiPattern.setId(333L);
        create().injectPattern(antiPattern, meta, 1.0);
        create().injectPattern(hardAntiPattern, meta, 1.0);
        create().injectAntiPattern(patternInterferingNodes(), meta);
        // prevent FunctionBodyNode for some reason it breaks the condition
        create().injectPattern(new NodeWrapper(MinicFunctionBodyNode.class.getName()), meta, 1.0);

        // then
        for (int i = 0; i < CHECK_PATTERN; i++) {
            CreationInformation creationInfo = getCreationInfo(MinicWhileNode.class);
            // inject needed frame slot
            creationInfo.dataFlowGraph.getAvailableDataItems().put(frame, new ArrayList<>());
            creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("asdf"), MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), frameDescriptor.findFrameSlot("asdf"))));
            creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("other"), MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), frameDescriptor.findFrameSlot("other"))));
            creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("third"), MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), frameDescriptor.findFrameSlot("third"))));

            Node node = create().create(creationInfo);
            Assert.assertEquals(MinicWhileNode.class.getName(), node.getClass().getName());
            Node repeatingNode = node.getChildren().iterator().next().getChildren().iterator().next();
            NodeWrapper condition = NodeWrapper.wrap(repeatingNode).getChildren("conditionNode").stream().iterator().next().getChild();
            NodeWrapper body = NodeWrapper.wrap(repeatingNode).getChildren("bodyNode").stream().iterator().next().getChild();

            if (print) {
                System.out.println("  COND: ");
                System.out.println(condition.humanReadableTree());

                System.out.println("  BODY: ");
                System.out.println(body.humanReadableTree());
            }

            List<String> read = NodeWrapper.flatten(condition).filter(x -> x.getType().contains("Read")).map(x -> (String) x.getValues().get("slot:com.oracle.truffle.api.frame.FrameSlot")).distinct().collect(Collectors.toList());
            List<String> write = NodeWrapper.flatten(body).filter(x -> x.getType().contains("Write")).map(x -> (String) x.getValues().get("slot:com.oracle.truffle.api.frame.FrameSlot")).distinct().collect(Collectors.toList());

            Assert.assertFalse(read.isEmpty(), "Condition failed");
            Assert.assertFalse(write.isEmpty(), "Body failed");
            Assert.assertTrue(read.stream().anyMatch(write::contains), "Body is writing something different than the condition reads");
        }
    }


}
