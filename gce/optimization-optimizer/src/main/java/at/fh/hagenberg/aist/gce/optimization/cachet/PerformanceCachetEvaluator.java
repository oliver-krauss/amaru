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
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.fitness.Cachet;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;

/**
 * Created by Oliver Krauss on 10.02.2017.
 */
public class PerformanceCachetEvaluator implements CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    public static final String NAME = "Performance-0.3";

    private double originalPerformance = -1;

    private double getOriginalPerformance(Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution) {
        if (originalPerformance < 0) {
            solution = solution.getSolutionGenes().get(0).getProblemGenes().get(0).getGene().getOriginalSolution();
            originalPerformance = solution.getSolutionGenes().stream().mapToDouble(
                x -> x.getGene().getTestResults() != null ? x.getGene().getTestResults().stream().mapToDouble(tr -> tr.getRuntime()).sum()
                    : Double.MAX_VALUE
            ).sum();
        }
        return originalPerformance;
    }

    @Override
    public double evaluateQuality(Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution) {
        long start = ProfileKeeper.profiler.start();
        double quality = solution.getSolutionGenes().stream().mapToDouble(
            x -> x.getGene().getTestResults() != null ? x.getGene().getTestResults().stream().mapToDouble(tr -> tr.getRuntime()).sum()
                : Double.MAX_VALUE
        ).sum();

        quality = quality / getOriginalPerformance(solution);
        solution.getCachets().add(new Cachet(quality, NAME));
        ProfileKeeper.profiler.profile("PerformanceCachetEvaluator.evaluateQuality", start);
        return quality;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
