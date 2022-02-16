/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.algorithm.bruteforce;

import at.fh.hagenberg.machinelearning.core.AbstractAlgorithm;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.mapping.SequentialGeneCreator;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;

import java.util.*;

public class BruteForceAlgorithm<GT, PT> extends AbstractAlgorithm<GT, PT> {

    /**
     * Brute force needs to have a gene creator that actually remembers what it created
     */
    private SequentialGeneCreator<GT, PT> geneCreator;

    @Override
    public Solution<GT, PT> solve(Problem<PT> problem) {
        return this.solve(problem, solutionCreator.createSolution(problem));
    }

    @Override
    public Solution<GT, PT> solve(Problem<PT> problem, Solution<GT, PT> solution) {
        if (!geneCreator.hasNext()) {
            return solution; // if nothing to do -> just return the given solution
        }

        ArrayList qualities = new ArrayList();
        if (this.analytics != null) {
            this.analytics.startAnalytics();
            analytics.logParam("problemSize", problem.getProblemSize());
            analytics.logParam("geneCreator", geneCreator.getClass().getName());
            qualities.add("best quality");
            qualities.add("worst quality");
            qualities.add("average quality");
            this.analytics.logAlgorithmStepHeaders(qualities);
        }

        Solution<GT, PT> bestSolution = solution != null ? solution : solutionCreator.createSolution(problem);
        Solution<GT, PT> currentSolution = null;
        long amoutOfSolutions = 1;
        double averageQuality = bestSolution.getQuality();
        double worstQuality = bestSolution.getQuality();

        while (geneCreator.hasNext()) {
            amoutOfSolutions++;
            currentSolution = solutionCreator.createSolution(problem);
            averageQuality += evaluator.evaluateQuality(currentSolution);
            if (currentSolution.getQuality() < bestSolution.getQuality()) {
                bestSolution = currentSolution;
            }
            if (worstQuality > currentSolution.getQuality()) {
                worstQuality = currentSolution.getQuality();
            }
        }

        if (this.analytics != null) {
            averageQuality = averageQuality / amoutOfSolutions;
            qualities.clear();
            qualities.add(String.valueOf(bestSolution.getQuality()));
            qualities.add(String.valueOf(averageQuality));
            qualities.add(String.valueOf(worstQuality));
            this.analytics.logAlgorithmStep(qualities);
            this.analytics.logProblem(problem);
            this.analytics.logSolution(solution);
            this.analytics.finishAnalytics();
        }

        return bestSolution;
    }

    protected Map<String, Descriptor> getSpecificOptions() {
        Map<String, Descriptor> options = new HashMap();
        options.put("geneCrator", new Descriptor<>(geneCreator));
        return options;
    }

    protected boolean setSpecificOption(String name, Descriptor descriptor) {
        try {
            if (name.equals("geneCrator")) {
                setGeneCreator((SequentialGeneCreator<GT, PT>) descriptor.getValue());
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void setGeneCreator(SequentialGeneCreator<GT, PT> geneCreator) {
        this.geneCreator = geneCreator;
    }
}
