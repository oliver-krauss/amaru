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
import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestResult;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapperWeightUtil;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.fitness.Cachet;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This cachet predicts the node performance according to one specific system information
 * Created by Oliver Krauss on 31.12.2019.
 */
public class SelfAdjustingPerformanceCachetEvaluator extends PerformanceCachetEvaluator {

    public static final String NAME = "SelfAdjusting-Performance-0.1";

    /**
     * Util class that calculates the weight
     */
    protected NodeWrapperWeightUtil primaryWeightUtil;

    /**
     * Util class that calculates the weight on THIS system
     */
    protected NodeWrapperWeightUtil weightUtil;

    /**
     * Language the adjustment will happen in
     */
    private String language;

    @Override
    public double evaluateQuality(Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution) {
        long start = ProfileKeeper.profiler.start();
        double quality = super.evaluateQuality(solution);

        if (SystemInformation.getCurrentSystem() != primaryWeightUtil.getInformation()) {
            TruffleOptimizationSolution gene = solution.getSolutionGenes().get(0).getGene();
            List<Map<String, Integer>> collect = gene.getTestResults().stream().map(TruffleOptimizationTestResult::getTraceResult).map(TraceExecutionResult::getNodeExecutions).collect(Collectors.toList());
            double assertedWeight = weightUtil.weight(gene.getTree(), collect);
            double primaryWeight = primaryWeightUtil.weight(gene.getTree(), collect);

            quality = quality * (assertedWeight / primaryWeight);
        }

        ProfileKeeper.profiler.profile("SelfAdjustingPerformanceCachetEvaluator.evaluateQuality", start);
        return quality;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Warning call only AFTER set language
     * @param information
     */
    public void setPrimarySystemInformation(SystemInformation information) {
        this.primaryWeightUtil.setInformation(information);
    }

    public void setLanguage(String language) {
        this.language = language;
        this.weightUtil = new NodeWrapperWeightUtil(language);
        this.primaryWeightUtil = new NodeWrapperWeightUtil(language);
    }
}
