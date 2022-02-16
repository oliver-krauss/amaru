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
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
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
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed.MinicIntRelationalNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed.MinicIntRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.minic.parser.MinicStructNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleFunctionSignature;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMetaLoader;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowGraph;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowNode;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tests for finding context
 */
public class TruffleMasterStrategyFindAntiPatternsTest extends AbstractPatternsTest {

    /**
     * Tests injection with * Wildcard
     * Tests pattern with multiple children under one node (non linear pattern)
     */
    @Test
    public void testFindWhilePattern() {
        // given
        // antipattern to be found - enforces while (... x ...) { x = ... }
        NodeWrapper hardAntiPattern = new NodeWrapper(MinicWhileNode.class.getName());
        NodeWrapper condStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship condRel = new OrderedRelationship(hardAntiPattern, condStarWildcard, "conditionNode", 0);
        hardAntiPattern.addChild(condRel);
        NodeWrapper readVarNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicReadNode.class.getName() + "|" + MinicReadGlobalNode.class.getName());
        readVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        OrderedRelationship condRel2 = new OrderedRelationship(condStarWildcard, readVarNode, "", 0);
        condStarWildcard.addChild(condRel2);
        NodeWrapper bodyStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        hardAntiPattern.addChild(new OrderedRelationship(hardAntiPattern, bodyStarWildcard, "bodyNode", 0));
        NodeWrapper writeVarNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicWriteNode.class.getName() + "|" + MinicWriteGlobalNode.class.getName());
        writeVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        bodyStarWildcard.addChild(writeVarNode, "", 0);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);
        create().injectAntiPattern(hardAntiPattern, meta);

        // also select a structure that contains an ANTI-pattern
        CreationInformation creationInfo = getCreationInfo(MinicWhileNode.class);
        creationInfo.dataFlowGraph.getAvailableDataItems().put(frame, new ArrayList<>());
        creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("asdf"), MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), frameDescriptor.findFrameSlot("asdf"))));
        MinicIntNode readNode = MinicReadNodeFactory.MinicIntReadNodeGen.create(frameDescriptor.findFrameSlot("asdf"));
        MinicExpressionNode condition = MinicIntRelationalNodeFactory.MinicIntLtENodeGen.create(readNode, new MinicSimpleLiteralNode.MinicIntLiteralNode(12));

        MinicWriteNode.MinicIntWriteNode writeNode = MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(30), frameDescriptor.findFrameSlot("asdf"));
        MinicNode body = new MinicBlockNode(new MinicSimpleLiteralNode.MinicDoubleLiteralNode(12.0), writeNode, new MinicSimpleLiteralNode.MinicFloatLiteralNode(44.0f));
        MinicNode whileNode = new MinicWhileNode(condition, body);
        MinicBlockNode block = new MinicBlockNode(whileNode);
        block.adoptChildren();

        // when
        LoadedRequirementInformation reqsUnderWhile = create().loadRequirements(block, whileNode.getChildren().iterator().next());
        LoadedRequirementInformation reqsCondition = create().loadRequirements(block, condition);
        LoadedRequirementInformation reqsWrite = create().loadRequirements(block, body);

        // then
        Assert.assertNotNull(reqsUnderWhile);
        Assert.assertNotNull(reqsCondition);
        Assert.assertNotNull(reqsWrite);

        // Under while we just want the while root antipattern
        Assert.assertFalse(reqsUnderWhile.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsUnderWhile.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicRepeatingNode");

        // under write we just want the req with the variable introduced by read INCLUDING the variable that must be selected
        Assert.assertFalse(reqsCondition.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), Wildcard.WILDCARD_ANYWHERE);
        Assert.assertEquals(reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getChildren().iterator().next().getChild().getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNode|at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalNode");
        Assert.assertTrue(reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().containsProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER));
        Map<String, Pair<FrameSlot, TruffleClassInformation>> property = reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);
        Assert.assertEquals(property.get("0").getKey().getIdentifier(), "asdf");

        // under condition we just want the condition path + the variable from the write (right context)
        Assert.assertFalse(reqsWrite.getRequirementInformation().getRequirements().isEmpty());
        Requirement requirement = reqsWrite.getRequirementInformation().getRequirements().keySet().stream().filter(x -> x.getName().equals(Requirement.REQ_ANTIPATTERN)).findFirst().orElse(null);
        Assert.assertEquals(requirement.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), Wildcard.WILDCARD_ANYWHERE);
        Assert.assertEquals(requirement.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getChildren().iterator().next().getChild().getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteNode|at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteGlobalNode");
        Assert.assertTrue(requirement.containsProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER));
        Map<String, Pair<FrameSlot, TruffleClassInformation>> propertyWr = requirement.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);
        Assert.assertEquals(propertyWr.get("0").getKey().getIdentifier(), "asdf");
    }

    /**
     * Tests injection with * Wildcard
     * Tests pattern with multiple children under one node (non linear pattern)
     */
    @Test
    public void testFindMultiWhilePattern() {
        // given
        // antipattern to be found - enforces while (... x ...) { x = ... }
        NodeWrapper hardAntiPattern = new NodeWrapper(MinicWhileNode.class.getName());
        NodeWrapper condStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship condRel = new OrderedRelationship(hardAntiPattern, condStarWildcard, "conditionNode", 0);
        hardAntiPattern.addChild(condRel);
        NodeWrapper readVarNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicReadNode.class.getName() + "|" + MinicReadGlobalNode.class.getName());
        readVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        OrderedRelationship condRel2 = new OrderedRelationship(condStarWildcard, readVarNode, "", 0);
        condStarWildcard.addChild(condRel2);
        NodeWrapper bodyStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        hardAntiPattern.addChild(new OrderedRelationship(hardAntiPattern, bodyStarWildcard, "bodyNode", 0));
        NodeWrapper writeVarNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicWriteNode.class.getName() + "|" + MinicWriteGlobalNode.class.getName());
        writeVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        bodyStarWildcard.addChild(writeVarNode, "", 0);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);
        create().injectAntiPattern(hardAntiPattern, meta);

        // also select a structure that contains an ANTI-pattern
        CreationInformation creationInfo = getCreationInfo(MinicWhileNode.class);
        creationInfo.dataFlowGraph.getAvailableDataItems().put(frame, new ArrayList<>());
        creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("asdf"), MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), frameDescriptor.findFrameSlot("asdf"))));
        MinicIntNode readNode = MinicReadNodeFactory.MinicIntReadNodeGen.create(frameDescriptor.findFrameSlot("asdf"));
        MinicExpressionNode condition = MinicIntRelationalNodeFactory.MinicIntLtENodeGen.create(readNode, new MinicSimpleLiteralNode.MinicIntLiteralNode(12));

        MinicWriteNode.MinicIntWriteNode writeNode = MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(30), frameDescriptor.findFrameSlot("other"));
        MinicWriteNode.MinicIntWriteNode otherWriteNode = MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(30), frameDescriptor.findFrameSlot("asdf"));
        MinicNode body = new MinicBlockNode(new MinicSimpleLiteralNode.MinicDoubleLiteralNode(12.0), writeNode, new MinicSimpleLiteralNode.MinicFloatLiteralNode(44.0f), otherWriteNode);
        MinicNode whileNode = new MinicWhileNode(condition, body);
        MinicBlockNode block = new MinicBlockNode(whileNode);
        block.adoptChildren();

        // when
        LoadedRequirementInformation reqsUnderWhile = create().loadRequirements(block, whileNode.getChildren().iterator().next());
        LoadedRequirementInformation reqsCondition = create().loadRequirements(block, condition);
        LoadedRequirementInformation reqsWrite = create().loadRequirements(block, body);

        // then
        Assert.assertNotNull(reqsUnderWhile);
        Assert.assertNotNull(reqsCondition);
        Assert.assertNotNull(reqsWrite);

        // Under while we just want the while root antipattern
        Assert.assertFalse(reqsUnderWhile.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertFalse(reqsUnderWhile.isFailed());
        Assert.assertEquals(reqsUnderWhile.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicRepeatingNode");

        // under write we just want the req with the variable introduced by read INCLUDING the variable that must be selected
        Assert.assertFalse(reqsCondition.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), Wildcard.WILDCARD_ANYWHERE);
        Assert.assertEquals(reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getChildren().iterator().next().getChild().getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNode|at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalNode");
        Assert.assertTrue(reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().containsProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER));
        Map<String, Pair<FrameSlot, TruffleClassInformation>> property = reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);
        Assert.assertEquals(property.get("0").getKey().getIdentifier(), "asdf");
        Assert.assertFalse(reqsCondition.isFailed());


        // under condition we just want the condition path + the variable from the write (right context)
        Assert.assertFalse(reqsWrite.getRequirementInformation().getRequirements().isEmpty());
        Requirement requirement = reqsWrite.getRequirementInformation().getRequirements().keySet().stream().filter(x -> x.getName().equals(Requirement.REQ_ANTIPATTERN)).findFirst().orElse(null);
        Assert.assertEquals(requirement.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), Wildcard.WILDCARD_ANYWHERE);
        Assert.assertEquals(requirement.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getChildren().iterator().next().getChild().getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteNode|at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteGlobalNode");
        Assert.assertTrue(requirement.containsProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER));
        Map<String, Pair<FrameSlot, TruffleClassInformation>> propertyWr = requirement.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);
        Assert.assertEquals(propertyWr.get("0").getKey().getIdentifier(), "asdf");
        Assert.assertTrue(reqsWrite.isFailed()); // we failed as the write will be missing under this
    }

    /**
     * Tests injection with * Wildcard
     * Tests pattern with multiple children under one node (non linear pattern)
     */
    @Test
    public void testFindPartialWhilePattern() {
        // given
        // antipattern to be found - enforces while (... x ...) { x = ... }
        NodeWrapper hardAntiPattern = new NodeWrapper(MinicWhileNode.class.getName());
        NodeWrapper condStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship condRel = new OrderedRelationship(hardAntiPattern, condStarWildcard, "conditionNode", 0);
        hardAntiPattern.addChild(condRel);
        NodeWrapper readVarNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicReadNode.class.getName() + "|" + MinicReadGlobalNode.class.getName());
        readVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        OrderedRelationship condRel2 = new OrderedRelationship(condStarWildcard, readVarNode, "", 0);
        condStarWildcard.addChild(condRel2);
        NodeWrapper bodyStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        hardAntiPattern.addChild(new OrderedRelationship(hardAntiPattern, bodyStarWildcard, "bodyNode", 0));
        NodeWrapper writeVarNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicWriteNode.class.getName() + "|" + MinicWriteGlobalNode.class.getName());
        writeVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        bodyStarWildcard.addChild(writeVarNode, "", 0);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);
        create().injectAntiPattern(hardAntiPattern, meta);

        // also select a structure that contains an ANTI-pattern
        CreationInformation creationInfo = getCreationInfo(MinicWhileNode.class);
        creationInfo.dataFlowGraph.getAvailableDataItems().put(frame, new ArrayList<>());
        creationInfo.dataFlowGraph.getAvailableDataItems().get(frame).add(new DataFlowNode(frameDescriptor.findFrameSlot("asdf"), MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), frameDescriptor.findFrameSlot("asdf"))));
        MinicExpressionNode condition = MinicIntRelationalNodeFactory.MinicIntLtENodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(65), new MinicSimpleLiteralNode.MinicIntLiteralNode(12));

        MinicWriteNode.MinicIntWriteNode writeNode = MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(30), frameDescriptor.findFrameSlot("asdf"));
        MinicWriteNode.MinicIntWriteNode otherWriteNode = MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(30), frameDescriptor.findFrameSlot("other"));
        MinicNode body = new MinicBlockNode(new MinicSimpleLiteralNode.MinicDoubleLiteralNode(12.0), writeNode, new MinicSimpleLiteralNode.MinicFloatLiteralNode(44.0f), otherWriteNode);
        MinicNode whileNode = new MinicWhileNode(condition, body);
        MinicBlockNode block = new MinicBlockNode(whileNode);
        block.adoptChildren();

        // when
        LoadedRequirementInformation rootNode = create().loadRequirements(block, whileNode);
        LoadedRequirementInformation reqsUnderWhile = create().loadRequirements(block, whileNode.getChildren().iterator().next());
        LoadedRequirementInformation reqsCondition = create().loadRequirements(block, condition);
        LoadedRequirementInformation reqsWrite = create().loadRequirements(block, body);

        // then
        Assert.assertNotNull(reqsUnderWhile);
        Assert.assertNotNull(reqsCondition);
        Assert.assertNotNull(reqsWrite);

        // Under while we just want the while root antipattern
        Assert.assertFalse(rootNode.isFailed());
        Assert.assertFalse(reqsUnderWhile.isFailed());
        Assert.assertFalse(reqsUnderWhile.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsUnderWhile.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicRepeatingNode");

        // under write we just want the req with the variable introduced by read INCLUDING the variable that must be selected
        Assert.assertFalse(reqsCondition.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), Wildcard.WILDCARD_ANYWHERE);
        Assert.assertEquals(reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getChildren().iterator().next().getChild().getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNode|at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalNode");
        Assert.assertTrue(reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().containsProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER));
        Map<String, Pair<FrameSlot, TruffleClassInformation>> property = reqsCondition.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);
        Assert.assertEquals(property.get("0").getKey().getIdentifier(), "asdf");
        Assert.assertTrue(reqsCondition.isFailed());

        // under condition we just want the condition path + the variable from the write (right context)
        Assert.assertFalse(reqsWrite.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsWrite.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), Wildcard.WILDCARD_ANYWHERE);
        Assert.assertEquals(reqsWrite.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getChildren().iterator().next().getChild().getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteNode|at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteGlobalNode");
        // note - as this is a failed pattern we have no variable information if the write gets removed
        Assert.assertFalse(reqsWrite.getRequirementInformation().getRequirements().keySet().iterator().next().containsProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER));
        Assert.assertFalse(reqsWrite.isFailed());
    }


    /**
     * Tests pattern with n childrend
     */
    @Test
    public void testFindNChildPatternWithChildren() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        NodeWrapper blockA = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicBlockNode.class.getName());
        antiPattern.addChild(blockA, "", 0);
        NodeWrapper blockB = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicBlockNode.class.getName());
        antiPattern.addChild(blockB, "", 1);
        NodeWrapper blockC = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicBlockNode.class.getName());
        antiPattern.addChild(blockC, "", 2);

        NodeWrapper intLitNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        blockA.addChild(intLitNode, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        blockA.addChild(doubleLitNode, "", 1);
        NodeWrapper charLitNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        blockA.addChild(charLitNode, "", 2);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // construct valid example fulfilling antipattern
        MinicSimpleLiteralNode.MinicIntLiteralNode child1A = new MinicSimpleLiteralNode.MinicIntLiteralNode(1);
        MinicSimpleLiteralNode.MinicDoubleLiteralNode child1B = new MinicSimpleLiteralNode.MinicDoubleLiteralNode(2.0);
        MinicSimpleLiteralNode.MinicCharLiteralNode child1C = new MinicSimpleLiteralNode.MinicCharLiteralNode('c');
        MinicBlockNode block1 = new MinicBlockNode(child1A, child1B, child1C);
        MinicSimpleLiteralNode.MinicIntLiteralNode child2A = new MinicSimpleLiteralNode.MinicIntLiteralNode(1);
        MinicSimpleLiteralNode.MinicDoubleLiteralNode child2B = new MinicSimpleLiteralNode.MinicDoubleLiteralNode(2.0);
        MinicSimpleLiteralNode.MinicCharLiteralNode child2C = new MinicSimpleLiteralNode.MinicCharLiteralNode('c');
        MinicBlockNode block2 = new MinicBlockNode(child2A, child2B, child2C);
        MinicBlockNode child3 = new MinicBlockNode();
        MinicBlockNode block3 = new MinicBlockNode(child3);
        MinicExpressionNode right = new MinicSimpleLiteralNode.MinicFloatLiteralNode(1.0f);
        MinicExpressionNode left = new MinicSimpleLiteralNode.MinicIntLiteralNode(1);
        MinicBlockNode block = new MinicBlockNode(left, block1, block2, block3, right);

        // when
        create().injectAntiPattern(antiPattern, meta);


        // then
        LoadedRequirementInformation reqsUnderBlock = create().loadRequirements(block, left);
        LoadedRequirementInformation reqsinIntDblChr_INT = create().loadRequirements(block, child1A);
        LoadedRequirementInformation reqsinIntDblChr_DBL = create().loadRequirements(block, child1B);
        LoadedRequirementInformation reqsinIntDblChr_CHAR = create().loadRequirements(block, child1C);
        LoadedRequirementInformation reqsSub2 = create().loadRequirements(block, child2B);
        LoadedRequirementInformation reqsSub3 = create().loadRequirements(block, child3);

        // under block we want to be forced to create another block
        Assert.assertFalse(reqsUnderBlock.isFailed());
        Assert.assertFalse(reqsUnderBlock.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsUnderBlock.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode");

        // in dbl int char we want to be forced to create INT, DOUBLE and CHAR respectively
        Assert.assertFalse(reqsinIntDblChr_INT.isFailed());
        Assert.assertFalse(reqsinIntDblChr_DBL.isFailed());
        Assert.assertFalse(reqsinIntDblChr_CHAR.isFailed());
        Assert.assertFalse(reqsinIntDblChr_INT.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsinIntDblChr_INT.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode$MinicIntLiteralNode");
        Assert.assertFalse(reqsinIntDblChr_DBL.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsinIntDblChr_DBL.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode$MinicDoubleLiteralNode");
        Assert.assertFalse(reqsinIntDblChr_CHAR.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsinIntDblChr_CHAR.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode$MinicCharLiteralNode");

        // under the other blocks we want NOTHING
        Assert.assertFalse(reqsSub2.isFailed());
        Assert.assertFalse(reqsSub3.isFailed());
        Assert.assertTrue(reqsSub2.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertTrue(reqsSub3.getRequirementInformation().getRequirements().isEmpty());
    }

    /**
     * Tests pattern with n childrend
     */
    @Test
    public void testFindPartialNCHildPatternWithChildren() {
        // given
        // antipattern - enforces that Block Node consists of IntLit, DoubleLit, CharLit
        NodeWrapper antiPattern = new NodeWrapper(MinicBlockNode.class.getName());
        NodeWrapper blockA = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicBlockNode.class.getName());
        antiPattern.addChild(blockA, "", 0);
        NodeWrapper blockB = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicBlockNode.class.getName());
        antiPattern.addChild(blockB, "", 1);
        NodeWrapper blockC = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicBlockNode.class.getName());
        antiPattern.addChild(blockC, "", 2);

        NodeWrapper intLitNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        blockA.addChild(intLitNode, "", 0);
        NodeWrapper doubleLitNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicSimpleLiteralNode.MinicDoubleLiteralNode.class.getName());
        blockA.addChild(doubleLitNode, "", 1);
        NodeWrapper charLitNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        blockA.addChild(charLitNode, "", 2);

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);

        // construct valid example fulfilling antipattern
        MinicSimpleLiteralNode.MinicIntLiteralNode child1A = new MinicSimpleLiteralNode.MinicIntLiteralNode(1);
        MinicBlockNode block1 = new MinicBlockNode(child1A);
        MinicSimpleLiteralNode.MinicIntLiteralNode child2A = new MinicSimpleLiteralNode.MinicIntLiteralNode(1);
        MinicSimpleLiteralNode.MinicDoubleLiteralNode child2B = new MinicSimpleLiteralNode.MinicDoubleLiteralNode(2.0);
        MinicSimpleLiteralNode.MinicCharLiteralNode child2C = new MinicSimpleLiteralNode.MinicCharLiteralNode('c');
        MinicBlockNode block2 = new MinicBlockNode(child2A, child2B, child2C);
        MinicBlockNode child3 = new MinicBlockNode();
        MinicBlockNode block3 = new MinicBlockNode(child3);
        MinicExpressionNode right = new MinicSimpleLiteralNode.MinicFloatLiteralNode(1.0f);
        MinicExpressionNode left = new MinicSimpleLiteralNode.MinicIntLiteralNode(1);
        MinicBlockNode block = new MinicBlockNode(left, block1, block2, block3, right);

        // when
        create().injectAntiPattern(antiPattern, meta);


        // then
        LoadedRequirementInformation rootReqs = create().loadRequirements(block, block);
        LoadedRequirementInformation reqsUnderBlock = create().loadRequirements(block, left);
        LoadedRequirementInformation reqsinIntDblChr_INT = create().loadRequirements(block, child1A);
        LoadedRequirementInformation reqsAtBlock2 = create().loadRequirements(block, block2);
        LoadedRequirementInformation reqsSub2 = create().loadRequirements(block, child2B);
        LoadedRequirementInformation reqsSub3 = create().loadRequirements(block, child3);
        LoadedRequirementInformation reqsAtEnd = create().loadRequirements(block, right);

        // we wanna validate that we find the failed pattern
        Assert.assertFalse(rootReqs.isFailed());

        // under block we want to be forced to create another block
        Assert.assertFalse(reqsUnderBlock.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsUnderBlock.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode");
        Assert.assertFalse(reqsUnderBlock.isFailed());

        // in dbl int char we want to be forced to create INT, DOUBLE and CHAR respectively
        Assert.assertFalse(reqsinIntDblChr_INT.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertFalse(reqsinIntDblChr_INT.isFailed());
        Assert.assertEquals(reqsinIntDblChr_INT.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode$MinicIntLiteralNode");

        // under child 2b we now want the req that the first block didn't fulfill
        Assert.assertFalse(reqsAtBlock2.isFailed());
        Assert.assertFalse(reqsSub2.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsSub2.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode$MinicDoubleLiteralNode");

        // under the follow up block we want no requirement again
        Assert.assertFalse(reqsSub3.isFailed());
        Assert.assertTrue(reqsSub3.getRequirementInformation().getRequirements().isEmpty());

        // under the final req we want the next block requirement
        Assert.assertFalse(reqsAtEnd.getRequirementInformation().getRequirements().isEmpty());
        Assert.assertEquals(reqsAtEnd.getRequirementInformation().getRequirements().keySet().iterator().next().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType(), "¬at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode");
    }

    /**
     * Tests Exclusion of specific value
     */
    @Test
    public void testFindViolatedValueReq() {
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
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        create().injectAntiPattern(antiPattern, datatypeIndependentMeta);


        MinicSimpleLiteralNode.MinicIntLiteralNode right = new MinicSimpleLiteralNode.MinicIntLiteralNode(0);
        MinicIntArithmeticNode.MinicIntDivNode div = MinicIntArithmeticNodeFactory.MinicIntDivNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(23), right);

        // when
        LoadedRequirementInformation rightReq = create().loadRequirements(div, right);

        // then
        Assert.assertTrue(rightReq.isFailed());
    }

    /**
     * Tests Exclusion of specific value
     */
    @Test
    public void testFindValidValReq() {
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
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        create().injectAntiPattern(antiPattern, datatypeIndependentMeta);


        MinicSimpleLiteralNode.MinicIntLiteralNode right = new MinicSimpleLiteralNode.MinicIntLiteralNode(55);
        MinicIntArithmeticNode.MinicIntDivNode div = MinicIntArithmeticNodeFactory.MinicIntDivNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(23), right);

        // when
        LoadedRequirementInformation rightReq = create().loadRequirements(div, right);

        // then
        Assert.assertFalse(rightReq.isFailed());
    }

    /**
     * Tests Exclusion of specific value
     */
    @Test
    public void testFindDataReqs() {

        // TODO #216 -> I really want to move away from the data flow graph and instead go for representing this as patterns only
//        NodeWrapper antiPattern = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
//        NodeWrapper writeVarNode = new NodeWrapper(MinicWriteNode.class.getName() + "|" + MinicWriteGlobalNode.class.getName());
//        writeVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
//        NodeWrapper readVarNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicReadNode.class.getName() + "|" + MinicReadGlobalNode.class.getName());
//        readVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
//        antiPattern.addChild(writeVarNode, "", 0);
//        antiPattern.addChild(readVarNode, "", 1);
//
//        // given
//        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
//        BitwisePatternMeta meta = new BitwisePatternMeta(tli, false);
//        create().injectAntiPattern(antiPattern, meta);


        MinicWriteNode.MinicIntWriteNode writeA = MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(30), frameDescriptor.findFrameSlot("asdf"));
        MinicWriteNode.MinicIntWriteNode writeC = MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(30), frameDescriptor.findFrameSlot("third"));
        MinicBlockNode writes = new MinicBlockNode(writeA, writeC);
        MinicSimpleLiteralNode.MinicIntLiteralNode middle = new MinicSimpleLiteralNode.MinicIntLiteralNode(213);
        MinicIntNode readA = MinicReadNodeFactory.MinicIntReadNodeGen.create(frameDescriptor.findFrameSlot("asdf"));
        MinicIntNode readB = MinicReadNodeFactory.MinicIntReadNodeGen.create(frameDescriptor.findFrameSlot("other"));
        MinicIntNode readC = MinicReadNodeFactory.MinicIntReadNodeGen.create(frameDescriptor.findFrameSlot("third"));
        MinicBlockNode reads = new MinicBlockNode(readA, readB, readC);
        MinicBlockNode block = new MinicBlockNode(writes, middle, reads);
        block.adoptChildren();

        // when
        LoadedRequirementInformation middleReq = create().loadRequirements(block).get(middle);
        LoadedRequirementInformation rightReq = create().loadRequirements(block, reads);
        LoadedRequirementInformation bREq = create().loadRequirements(block, readB);
        LoadedRequirementInformation leftReq = create().loadRequirements(block).get(writes);

        // then
        Assert.assertTrue(middleReq.isFailed());
        Assert.assertEquals(middleReq.getRequirementInformation().getRequirements().keySet().stream().filter(x -> x.getName().equals(Requirement.REQ_DATA_WRITE)).count(), 1);
        Assert.assertFalse(rightReq.isFailed());
        Assert.assertEquals(rightReq.getRequirementInformation().getRequirements().keySet().stream().filter(x -> x.getName().equals(Requirement.REQ_DATA_WRITE)).count(), 0);
        Assert.assertFalse(bREq.isFailed());
        Assert.assertEquals(bREq.getRequirementInformation().getRequirements().keySet().stream().filter(x -> x.getName().equals(Requirement.REQ_DATA_WRITE)).count(), 0);
        Assert.assertTrue(leftReq.isFailed());
        Assert.assertEquals(leftReq.getRequirementInformation().getRequirements().keySet().stream().filter(x -> x.getName().equals(Requirement.REQ_DATA_WRITE)).count(), 3);

    }
}
