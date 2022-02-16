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
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.RandomTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultFrameSlotStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import at.fh.hagenberg.machinelearning.analytics.graph.MinicPatternRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.PatternRepository;
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
import java.util.regex.Pattern;

/**
 * @author Oliver Krauss on 06.12.2019
 */

public class TruffleTreeMutatorTestWithPatterns {

    private TruffleTreeMutator mutator;

    CreationConfiguration creationConfiguration;

    TruffleLanguageSearchSpace tss;

    private MinicWhileNode nodeToOptimize = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(0),
            new MinicBlockNode(PrintNodeFactory.create(new MinicExpressionNode[]{new MinicSimpleLiteralNode.MinicStringLiteralNode("test")}, null)));

    @BeforeClass
    public void setUp() {
        // build mutation
        mutator = new TruffleTreeMutator();

        RandomTruffleTreeSelector randomTruffleTreeSelector = new RandomTruffleTreeSelector();
        mutator.setSelector(randomTruffleTreeSelector);

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
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformationMinimal(MinicLanguage.ID);
        tss = new TruffleLanguageSearchSpace(information, excludes);
        TruffleMasterStrategy masterStrategy = TruffleMasterStrategy.createFromTLI(creationConfiguration, tss, new ArrayList<>(), strategies);
        PatternRepository.register(MinicLanguage.ID, new MinicPatternRepository());
        masterStrategy.autoLoadPatterns();
        mutator.setSubtreeStrategy(masterStrategy);

        TruffleEntryPointStrategy entryPointStrategy = new TruffleEntryPointStrategy(tss, null, nodeToOptimize, masterStrategy, creationConfiguration);
        RandomChooser<Class> chooser = new RandomChooser<>();
        entryPointStrategy.setChooser(chooser);

        mutator.setFullTreeStrategy(entryPointStrategy);
        mutator.setMutationChoice(chooser);
    }

    @Test(invocationCount = 10)
    public void testMutate() {
        // given
        TruffleOptimizationProblem problem = new TruffleOptimizationProblem(MinicLanguage.ID, null, null, null, nodeToOptimize, null, tss, creationConfiguration, null, 1, "ffn");
        TruffleOptimizationSolution solution = new TruffleOptimizationSolution(nodeToOptimize.deepCopy(), problem, this);

        // when
        TruffleOptimizationSolution mutate = mutator.mutate(solution);

        // then
        Assert.assertNotNull(mutate);
    }

}
