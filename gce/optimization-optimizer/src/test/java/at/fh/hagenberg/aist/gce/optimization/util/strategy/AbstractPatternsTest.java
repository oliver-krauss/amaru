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
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Root for test classes that deal with patterns
 */
public class AbstractPatternsTest extends AbstractTestNeedingMasterStrategyTest {

    /**
     * Const how often the pattern injects are tried out.
     * Some errors occur only very rarely. You'd better not go below 100 when adding new features
     */
    private static final int CHECK_PATTERN = 200;

    /**
     * If true the resulting trees (or interesting parts) will be printed to console
     * Unusual for a test case but this is for debugging purposes when we modify the patterns in the future
     */
    private static final boolean print = true;

    protected CreationInformation getCreationInfo(Class clazz) {
        RequirementInformation requirements = new RequirementInformation(null);
        TruffleFunctionSignature signature = new TruffleFunctionSignature(null, new String[]{"int"});
        CreationConfiguration config = new CreationConfiguration();
        CreationInformation creationInformation = new CreationInformation(new NodeWrapper("FILLER"), null, requirements, new DataFlowGraph(null, null, null, signature), clazz, 0, config);
        return creationInformation;
    }

    protected CreationInformation getCreationInfoWithContext(Node ast, Node node, RequirementInformation loaded) {
        TruffleFunctionSignature signature = new TruffleFunctionSignature(null, new String[]{"int"});
        CreationConfiguration config = new CreationConfiguration();
        CreationInformation creationInformation = new CreationInformation(NodeWrapper.wrap(ast), NodeWrapper.wrap(node), loaded, new DataFlowGraph(null, null, null, signature), node.getClass(), ExtendedNodeUtil.getDepth(node) - 1, config);
        return creationInformation;
    }


    @BeforeMethod
    public void setUpTest() {
        create().invalidateCache();
    }

    @AfterMethod
    public void tearDownTest() {
        create().removePatterns();
    }

}
