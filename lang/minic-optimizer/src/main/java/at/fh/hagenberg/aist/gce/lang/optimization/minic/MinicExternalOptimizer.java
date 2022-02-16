/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.lang.optimization.minic;

import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.operators.TruffleTreeCrossover;
import at.fh.hagenberg.aist.gce.optimization.operators.TruffleTreeMutator;
import at.fh.hagenberg.aist.gce.optimization.run.TruffleLanguageOptimizer;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTest;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleTestValue;
import at.fh.hagenberg.aist.gce.optimization.test.ValueDefinitions;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationConfiguration;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleHierarchicalStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleSimpleStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicFunctionLiteralStrategy;
import at.fh.hagenberg.machinelearning.analytics.graph.MinicPatternRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.PatternRepository;
import at.fh.hagenberg.machinelearning.core.fitness.Evaluator;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.frame.MaterializedFrame;

import java.util.*;

/**
 * @author Oliver Krauss on 06.05.2019
 */

public class MinicExternalOptimizer extends TruffleLanguageOptimizer {

    private final String file;
    private final String input;
    private final String output;
    private TruffleOptimizationProblem problem;
    private TruffleLanguageSearchSpace tls;
    private String function;

    public MinicExternalOptimizer(TruffleLanguageSearchSpace tls, String file, String function, String input, String output) {
        super();

        // register the minic patterns
        if (PatternRepository.loadForLanguage(MinicLanguage.ID) == null) {
            PatternRepository.register(MinicLanguage.ID, new MinicPatternRepository());
        }

        this.tls = tls;
        this.file = file;
        this.function = function;
        this.input = input;
        this.output = output;
        init();

        if (tls == null) {
            List<Class> excludes = new ArrayList<>();
            excludes.add(ReadNodeFactory.ReadNodeGen.class);
            excludes.add(ReadNode.class);
            TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
            this.tls = new TruffleLanguageSearchSpace(information, excludes);
        }
    }

    @Override
    protected List<TruffleHierarchicalStrategy> getStrategies() {
        List<TruffleHierarchicalStrategy> strategies = super.getStrategies();
        strategies.add(new MinicFunctionLiteralStrategy(MinicLanguage.INSTANCE.getContextReference().get()));
        return strategies;
    }

    @Override
    protected Map<String, TruffleVerifyingStrategy> getTerminalStrategies() {
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        if (MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage() != null) {
            strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage()));
        }
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.INSTANCE.getContextReference().get()));
        return strategies;
    }

    @Override
    protected TruffleLanguageSearchSpace getTruffleLanguageSearchSpace() {
        return tls;
    }

    @Override
    protected Set<TruffleOptimizationTest> getTestCases() {
        return getTestCases(input, output);
    }

    public static Set<TruffleOptimizationTest> getTestCases(String input, String output) {
        Set<TruffleOptimizationTest> cases = new HashSet<>();

        List<String> in = Arrays.asList(input.split(System.lineSeparator()));
        List<String> out = Arrays.asList(output.split(System.lineSeparator()));

        Iterator<String> inIt = in.iterator();
        Iterator<String> outIt = out.iterator();

        while (inIt.hasNext() && outIt.hasNext()) {
            List<TruffleTestValue> inList = new ArrayList<TruffleTestValue>();
            inList.add(cast(inIt.next()));
            cases.add(new TruffleOptimizationTest(inList, cast(outIt.next())));
        }

        return cases;
    }

    @Override
    protected String getLanguage() {
        return "c";
    }

    @Override
    protected String getCode() {
        return file;
    }

    @Override
    protected String getFunction() {
        return function;
    }

    private static TruffleTestValue cast(String s) {
        Pair<String, Object> val = ValueDefinitions.stringToValueTyped(s);
        return new TruffleTestValue(val.getValue(), val.getKey());
    }

    public TruffleOptimizationProblem getProblem() {
        if (this.problem != null) {
            return this.problem;
        }
        TruffleOptimizationProblem problem = super.createProblem();
        problem.setDescription(this.function);
        this.problem = problem;
        return problem;

    }

    @Override
    protected CreationConfiguration getConfiguration() {
        if (problem != null) {
            return problem.getConfiguration();
        }
        return new CreationConfiguration(4, 3, Double.MAX_VALUE);
    }
}
