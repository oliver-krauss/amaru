/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.run;

import at.fh.hagenberg.aist.gce.optimization.TruffleEvaluatorImpl;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.executor.InternalExecutor;
import at.fh.hagenberg.aist.gce.optimization.operators.ConfigurableGeneCreator;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTest;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleTestComplexityEvaluator;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleFunctionAnalyzer;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleFunctionSignature;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultFrameSlotStrategy;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowGraph;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapperWeightUtil;
import at.fh.hagenberg.machinelearning.core.Algorithm;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import com.oracle.truffle.api.nodes.Node;

import java.util.*;

/**
 * Created by Oliver Krauss on 10.02.2017.
 */
public abstract class TruffleLanguageOptimizer {

    /**
     * Algorithm to be used in this context
     */
    protected Algorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> algorithm;

    protected TruffleEvaluatorImpl evaluator;

    protected InternalExecutor executor;

    protected TruffleMasterStrategy masterStrategy;

    protected TruffleEntryPointStrategy entryPointStrategy;

    protected ConfigurableGeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> geneCreator;

    public TruffleLanguageOptimizer() {
    }

    /**
     * Automatically initializes the algorithm settings
     */
    protected void init() {
        // set executor and evaluator
        executor = new InternalExecutor(getLanguage(), getCode(), getEntryPoint(), getFunction());
        evaluator = TruffleAlgorithmFactory.createTruffleEvaluator();

        // define subtree creation strategies
        Map<String, TruffleVerifyingStrategy> terminalStrategies = getTerminalStrategies();
        if (terminalStrategies == null) {
            terminalStrategies = new HashMap<>();
        }
        terminalStrategies.putAll(DefaultStrategyUtil.defaultStrategies());
        terminalStrategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(createProblem().getNode().getRootNode().getFrameDescriptor()));

        // define the syntax graph
        masterStrategy = TruffleMasterStrategy.createFromTLI(getConfiguration(), getTruffleLanguageSearchSpace(), getStrategies(), terminalStrategies);
        masterStrategy.autoLoadPatterns();
        entryPointStrategy = new TruffleEntryPointStrategy(getTruffleLanguageSearchSpace(), getNodeToOptimize(), getNodeToOptimize(), masterStrategy, getConfiguration());

        // we are setting the data flow graph of the master strategy to empty except the signature, as the master strat has no available items
        TruffleFunctionSignature signature = TruffleFunctionAnalyzer.getSignature(createProblem().getNode().getRootNode(), createProblem().getTests());
        masterStrategy.setDataFlowGraph(new DataFlowGraph(null, null, null, signature));


        // create algorithm factory
        TruffleAlgorithmFactory factory = new TruffleAlgorithmFactory(getLanguage(), masterStrategy, entryPointStrategy);


        // create the algorithm
        this.algorithm = factory.createAlgorithm();
    }

    protected List<TruffleHierarchicalStrategy> getStrategies() {
        return new ArrayList<>();
    }

    protected Node getNodeToOptimize() {
        if (problem != null) {
            return problem.getNode();
        }
        return executor.getOrigin();
    }

    public TruffleOptimizationSolution optimize() {
        // get problem
        Problem<TruffleOptimizationProblem> problem = new Problem<>();
        problem.setProblemGenes(new ArrayList<>());
        problem.getProblemGenes().add(new ProblemGene<>(createProblem()));

        // solve problem
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution = algorithm.solve(problem);

        // show result

        return solution.getSolutionGenes().get(0).getGene();
    }

    protected TruffleOptimizationProblem problem;

    protected TruffleOptimizationProblem createProblem() {
        if (problem != null) {
            return problem;
        }

        // get the node we want to replace
        Node node = getNodeToOptimize();


        // create the problem
        problem = new TruffleOptimizationProblem(getLanguage(), getCode(), getEntryPoint(), getFunction(),
                node, getBestKnownSolution(), getTruffleLanguageSearchSpace(),
                getConfiguration(), getTestCases(), 1, evaluator.evaluationIdentity());

        // Check the tests -> Must be done before evaluating the first solution, as otherwise the complexities aren't stored in the db
        TruffleTestComplexityEvaluator complexityEvaluator = new TruffleTestComplexityEvaluator();
        complexityEvaluator.evaluateComplexity(problem);

        // evaluate best solution
        if (problem.getBestKnownSolution() != null) {
            evaluator.evaluateQuality(problem.getBestKnownSolution());
        }

        // Evaluate original solution without storing to DB
        TruffleGraphAnalytics analyticsService = evaluator.getAnalyticsService();
        evaluator.setAnalyticsService(null);
        evaluator.evaluateQuality(problem.getOriginalSolution());
        evaluator.setAnalyticsService(analyticsService);

        // set the best known weight to our inital node
        if (TruffleLanguageInformation.getLanguageInformation(getLanguage()).learned()) {
            double performanceWeight = new NodeWrapperWeightUtil(this.getLanguage()).weight(problem.getWrappedNode());
            // TODO #159 only set weight when we want to instead of everytime
//            problem.getConfiguration().setMaxWeight(performanceWeight);
        }

        return problem;
    }

    protected abstract Map<String, TruffleVerifyingStrategy> getTerminalStrategies();

    protected abstract TruffleLanguageSearchSpace getTruffleLanguageSearchSpace();

    protected CreationConfiguration getConfiguration() {
        if (problem != null) {
            return problem.getConfiguration();
        }
        return new CreationConfiguration(7, 6, Double.MAX_VALUE);
    }

    protected abstract Set<TruffleOptimizationTest> getTestCases();

    protected Node getBestKnownSolution() {
        // Per default we don't know the best solution. IF we do, add it here
        return null;
    }

    /**
     * @return ID of language to be optimized
     */
    protected abstract String getLanguage();

    /**
     * @return code to be optimized
     */
    protected abstract String getCode();

    /**
     * @return name of function to be optimized (must exist in Code!)
     */
    protected abstract String getFunction();

    /**
     * Entry point if it differs from the function to optimize itself
     *
     * @return name of function to be run (Must also exist in code)!
     */
    protected String getEntryPoint() {
        return getFunction();
    };

    public TruffleMasterStrategy getMasterStrategy() {
        return masterStrategy;
    }

    public TruffleEntryPointStrategy getEntryPointStrategy() {
        return entryPointStrategy;
    }

    public TruffleOptimizationProblem getProblem() {
        return problem;
    }
}

