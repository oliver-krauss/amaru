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
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestResult;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.fitness.Cachet;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;

/**
 * Created by Oliver Krauss on 10.02.2017.
 */
public class CodeComplexityCachetEvaluator implements CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    public static final String NAME = "Complexity-0.1";

    @Override
    public double evaluateQuality(Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution) {
        long start = ProfileKeeper.profiler.start();
        double quality = 0.0;

        for (SolutionGene<TruffleOptimizationSolution, TruffleOptimizationProblem> gene : solution.getSolutionGenes()) {
            TruffleOptimizationSolution tos = gene.getGene();
            quality += NodeWrapper.cyclomaticComplexity(tos.getTree(), gene.getProblemGenes().get(0).getGene().getLanguage());
        }

        solution.getCachets().add(new Cachet(quality, NAME));
        ProfileKeeper.profiler.profile("CodeComplexityCachetEvaluator.evaluateQuality", start);
        return quality;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
