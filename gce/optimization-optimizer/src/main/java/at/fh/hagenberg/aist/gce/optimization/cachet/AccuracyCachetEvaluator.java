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
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.fitness.Cachet;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestResult;

/**
 * Created by Oliver Krauss on 10.02.2017.
 */
public class AccuracyCachetEvaluator implements CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    public static final String NAME = "Accuracy-0.4";

    @Override
    public double evaluateQuality(Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution) {
        long start = ProfileKeeper.profiler.start();
        double quality = 0.0;

        for (SolutionGene<TruffleOptimizationSolution, TruffleOptimizationProblem> gene : solution.getSolutionGenes()) {
            if (gene.getGene().getTestResults() == null) {
                // in case this problem can't even compile set the error level to max
                quality = Double.MAX_VALUE;
                break;
            }

            for (TruffleOptimizationTestResult result : gene.getGene().getTestResults()) {
                boolean solved = true;
                try {
                    solved = !result.solved();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (solved) {
                    // if solution is not correct increase quality-value
                    if (result.hasFailed()) {
                        // in case of error add 10
                        quality += 10;
                    } else if ((result.getOutput() == null && result.getTest().getOutputValue() != null)
                        || result.getOutput() != null && result.getTest().getOutputValue() == null
                        || !result.correctReturnType()) {
                        // if output is null but shouldn't (or the other way around) we wan't to make sure we return the correct stuff
                        quality += 2;
                    } else {
                        // if the test simply failed add the distance metric
                        quality += result.getOutput().compare(result.getTest().getOutput());
                    }
                }
            }

        }

        solution.getCachets().add(new Cachet(quality, NAME));
        ProfileKeeper.profiler.profile("AccuracyCachetEvaluator.evaluateQuality", start);
        return quality;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
