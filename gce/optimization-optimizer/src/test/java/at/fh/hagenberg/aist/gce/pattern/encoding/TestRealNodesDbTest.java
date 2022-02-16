/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.encoding;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicIfNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteNodeFactory;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.algorithm.HierarchySupportingSubgraphIterator;
import at.fh.hagenberg.aist.gce.pattern.selection.PatternSearchSpaceRepository;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.frame.FrameDescriptor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Oliver Krauss on 18.11.2019
 */
public class TestRealNodesDbTest {

    protected PatternSearchSpaceRepository repository;

    protected NodeWrapper t1, t2, t3, t4;

    @BeforeClass
    public void setUp() {
        ClassPathXmlApplicationContext ctx = ApplicationContextProvider.getCtx();
        repository = ctx.getBean(PatternSearchSpaceRepository.class);

        repository.queryTyped("MATCH (n) detach delete n", null);

        t1 = NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(123));

        FrameDescriptor frameDescriptor = new FrameDescriptor();
        t2 = NodeWrapper.wrap(MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(123), frameDescriptor.addFrameSlot("x")));

        frameDescriptor = new FrameDescriptor();
        t3 = NodeWrapper.wrap(new MinicIfNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(1),
            MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(123), frameDescriptor.addFrameSlot("x")),
            MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(456), frameDescriptor.findFrameSlot("x"))));

        frameDescriptor = new FrameDescriptor();
        t4 = NodeWrapper.wrap(new MinicIfNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(1),
            MinicWriteNodeFactory.MinicDoubleWriteNodeGen.create(new MinicSimpleLiteralNode.MinicDoubleLiteralNode(888), frameDescriptor.addFrameSlot("x")),
            MinicWriteNodeFactory.MinicDoubleWriteNodeGen.create(new MinicSimpleLiteralNode.MinicDoubleLiteralNode(999), frameDescriptor.findFrameSlot("x"))));

        repository.queryTyped("MATCH (n) detach delete n", null);

        repository.save(t1);
        repository.save(t2);
        repository.save(t3);
        repository.save(t4);

        // insert the minimal stuff so this is actually a valid db for mining
        repository.queryTyped("MATCH (n) where not ()-[:CHILD]->(n) CREATE (sg:SolutionGene)-[:RWGENE]->(t:TruffleOptimizationSolution)-[:TREE]->(n)", null);
    }

}
