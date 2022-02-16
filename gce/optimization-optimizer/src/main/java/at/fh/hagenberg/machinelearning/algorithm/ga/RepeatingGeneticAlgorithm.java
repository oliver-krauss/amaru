/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.algorithm.ga;

import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;

import java.util.*;

public class RepeatingGeneticAlgorithm extends GeneticAlgorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    /**
     * Amount of sequences that the algorithm will split the given testdata into
     */
    private int sequences = 4;

    /*
     * Amount of elites that will be taken into the next generation
     * NOTE: Should be FAR less than the population size
     */
    private int generationalElites = 10;

    @Override
    public Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solve(Problem<TruffleOptimizationProblem> problem, Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> givenSolution) {
        initializeLog(problem);
        this.analytics.logParam("Algorithm", "RepeatingGeneticAlgorithm");
        this.analytics.logParam("Sequence", 0 + "");
        TruffleOptimizationProblem gene = problem.getProblemGenes().get(0).getGene();

        List<Solution<TruffleOptimizationSolution, TruffleOptimizationProblem>> sequencePopulation = null;

        for (int i = 0; i < sequences; i++) {
            // push in the best from the previous version
            if (sequencePopulation != null) {
                Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> carrySolution = bestSolution;
                this.reset();
                this.bestSolution = RepackagingHelper.createSolution(gene, carrySolution.getSolutionGenes().get(0).getGene().getNode());
                initializeLog(problem);
                this.analytics.logParam("Algorithm", "RepeatingGeneticAlgorithm");
                this.analytics.logParam("Sequence", i + "");

                sequencePopulation.sort(Comparator.comparingDouble(x -> x.getQuality()));
                Iterator<Solution<TruffleOptimizationSolution, TruffleOptimizationProblem>> iterator = sequencePopulation.iterator();
                int elites = 0;
                while (iterator.hasNext() && elites < generationalElites) {
                    Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> next = iterator.next();
                    this.addIndividual(RepackagingHelper.createSolution(gene, next.getSolutionGenes().get(0).getGene().getNode()));
                    elites++;
                }
            }

            try {
                // go through the motions
                while (this.nextGeneration(problem) != null) {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // collect the population, and prepare the algorithm for the next sequence
            sequencePopulation = getPopulation();
        }

        return bestSolution;
    }


    public void setSequences(int sequences) {
        this.sequences = sequences;
    }

    public int getSequences() {
        return sequences;
    }

    public int getGenerationalElites() {
        return generationalElites;
    }

    public void setGenerationalElites(int generationalElites) {
        this.generationalElites = generationalElites;
    }

    @Override
    protected Map<String, Descriptor> getSpecificOptions() {
        Map<String, Descriptor> options = super.getSpecificOptions();
        options.put("sequences", new Descriptor(this.sequences));
        options.put("generationalElites", new Descriptor(this.generationalElites));
        return options;
    }

    @Override
    protected boolean setSpecificOption(String name, Descriptor descriptor) {
        if (name.equals("sequences")) {
            this.setSequences((Integer) descriptor.getValue());
        } else if (name.equals("generationalElites")) {
            this.setGenerationalElites((Integer) descriptor.getValue());
        } else {
            return super.setSpecificOption(name, descriptor);
        }
        return true;
    }

}
