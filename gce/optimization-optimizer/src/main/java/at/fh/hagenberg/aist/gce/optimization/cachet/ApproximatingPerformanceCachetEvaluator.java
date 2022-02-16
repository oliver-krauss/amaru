/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.cachet;

import at.fh.hagenberg.aist.gce.optimization.ProfileKeeper;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.executor.TraceExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestResult;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapperWeightUtil;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.fitness.Cachet;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;

import java.util.stream.Collectors;

/**
 * Instead of actually measuring the performance this cachet uses the weights in {@link at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation}
 * to estimate the performance.
 * Created by Oliver Krauss on 10.02.2017.
 */
public class ApproximatingPerformanceCachetEvaluator implements CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    public static final String NAME = "PerformanceApproximation-0.1";

    /**
     * Util class that calculates the weight
     */
    protected NodeWrapperWeightUtil weightUtil;

    /**
     * Language the approximation will happen in
     */
    private String language;

    public double evaluateQuality(TruffleOptimizationSolution solution) {
        return weightUtil.weight(solution.getTree(), solution.getTestResults().stream().map(TruffleOptimizationTestResult::getTraceResult).map(TraceExecutionResult::getNodeExecutions).collect(Collectors.toList()));
    }

    @Override
    public double evaluateQuality(Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution) {
        long start = ProfileKeeper.profiler.start();
        double quality = evaluateQuality(solution.getSolutionGenes().get(0).getGene());
        ProfileKeeper.profiler.profile("ApproximatingPerformanceCachetEvaluator.evaluateQuality", start);
        return quality;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public void setLanguage(String language) {
        this.language = language;
        this.weightUtil = new NodeWrapperWeightUtil(language);
    }
}
