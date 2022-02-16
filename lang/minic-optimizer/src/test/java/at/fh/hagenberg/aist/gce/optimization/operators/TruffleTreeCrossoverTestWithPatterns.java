/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.operators;

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.LengthNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.PrintNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicWhileNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadArrayNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteArrayNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.RandomTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationConfiguration;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.DefaultStrategyUtil;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleMasterStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultFrameSlotStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import at.fh.hagenberg.machinelearning.analytics.graph.MinicPatternRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.PatternRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Oliver Krauss on 06.12.2019
 */

public class TruffleTreeCrossoverTestWithPatterns {

    private TrufflePatternAdheringTreeCrossover crossover;

    CreationConfiguration creationConfiguration;

    TruffleLanguageSearchSpace tss;

    private MinicBlockNode nodeToOptimize = null;
    private MinicWhileNode otherAST = null;

    @BeforeClass
    public void setUp() {
        // build mutation
        crossover = new TrufflePatternAdheringTreeCrossover();

        // set up the pairings so we can debug the WRITE requirements
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        information.getClass(MinicReadNodeFactory.MinicIntReadNodeGen.class).getWritePairings().add(information.getClass(MinicWriteNodeFactory.MinicIntWriteNodeGen.class));
        information.getClass(MinicReadArrayNodeFactory.MinicEntireArrayReadNodeGen.class).getWritePairings().add(information.getClass(MinicWriteArrayNodeFactory.MinicCharArrayWriteNodeGen.class));

        RandomTruffleTreeSelector randomTruffleTreeSelector = new RandomTruffleTreeSelector();
        crossover.setSelector(randomTruffleTreeSelector);

        Map<String, TruffleVerifyingStrategy> strategies = DefaultStrategyUtil.defaultStrategies();
        FrameDescriptor frameDescriptor = new FrameDescriptor();
        frameDescriptor.findOrAddFrameSlot("something", FrameSlotKind.Object);
        strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(frameDescriptor));
        FrameDescriptor globalFrameDescriptor = new FrameDescriptor();
        MaterializedFrame materializedFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(materializedFrame));
        creationConfiguration = new CreationConfiguration(5, 5, Double.MAX_VALUE);
        List<Class> excludes = new ArrayList<>();
        excludes.add(ReadNodeFactory.ReadNodeGen.class);
        excludes.add(PrintNodeFactory.PrintNodeGen.class);
        excludes.add(LengthNodeFactory.LengthNodeGen.class);
        excludes.add(ReadNode.class);
        excludes.add(MinicFunctionLiteralNode.class);
        tss = new TruffleLanguageSearchSpace(information, excludes);
        TruffleMasterStrategy masterStrategy = TruffleMasterStrategy.createFromTLI(creationConfiguration, tss, new ArrayList<>(), strategies);
        PatternRepository.register(MinicLanguage.ID, new MinicPatternRepository());
        masterStrategy.autoLoadPatterns();

        crossover.setMasterStrategy(masterStrategy);

        // create node to optimize, with a write (that must not be removed) and a read (that depends on the write)
        nodeToOptimize = new MinicBlockNode(
                MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(0), frameDescriptor.findFrameSlot("something")),
                new MinicSimpleLiteralNode.MinicIntLiteralNode(0),
                new MinicSimpleLiteralNode.MinicIntLiteralNode(0),
                new MinicSimpleLiteralNode.MinicIntLiteralNode(0),
                MinicReadNodeFactory.MinicIntReadNodeGen.create(frameDescriptor.findFrameSlot("something"))
        );
        // this one has an anti pattern (while fails left side of the antipattern)
        otherAST = new MinicWhileNode(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(0),
                new MinicBlockNode(
                        PrintNodeFactory.create(new MinicExpressionNode[]{new MinicSimpleLiteralNode.MinicStringLiteralNode("test")}, null),
                        MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(0), frameDescriptor.findFrameSlot("something"))
                )
        );
    }

    @Test(invocationCount = 10)
    public void testCrossover() {
        // given
        TruffleOptimizationProblem problem = new TruffleOptimizationProblem(MinicLanguage.ID, null, null, null, nodeToOptimize, null, tss, creationConfiguration, null, 1, "ffn");
        TruffleOptimizationSolution solution = new TruffleOptimizationSolution(nodeToOptimize.deepCopy(), problem, this);
        TruffleOptimizationSolution solutionB = new TruffleOptimizationSolution(otherAST.deepCopy(), problem, this);

        // when
        TruffleOptimizationSolution mutate = crossover.breed(solution, solutionB);

        // then
        Assert.assertNotNull(mutate);
        List<NodeWrapper> collect = NodeWrapper.flatten(mutate.getTree()).collect(Collectors.toList());
        Assert.assertTrue(collect.stream().anyMatch(x -> x.getType().contains("IntWriteNodeGen")));
        Assert.assertTrue(collect.stream().noneMatch(x -> x.getType().contains("WhileNode")));
    }

}
