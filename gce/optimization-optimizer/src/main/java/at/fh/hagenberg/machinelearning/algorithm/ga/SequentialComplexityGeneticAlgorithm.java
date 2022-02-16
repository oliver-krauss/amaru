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
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestComplexity;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SequentialComplexityGeneticAlgorithm extends GeneticAlgorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    /**
     * Amount of sequences that the algorithm will split the given testdata into
     */
    private int sequences = 4;

    /*
     * Amount of elites that will be taken into the next generation
     * NOTE: Should be FAR less than the population size
     */
    private int generationalElites = 10;


    private static Set<TruffleOptimizationTestComplexity> testsToKeep = new HashSet<>();

    private Problem<TruffleOptimizationProblem> createProblem(Problem<TruffleOptimizationProblem> problem, TruffleOptimizationTestComplexity test) {
        testsToKeep.add(test);
        return createProblem(problem, test);
    }

    private Problem<TruffleOptimizationProblem> createProblem(Problem<TruffleOptimizationProblem> problem, Collection<TruffleOptimizationTestComplexity> test) {
        testsToKeep.addAll(test);
        return RepackagingHelper.createProblem(problem, testsToKeep);
    }

    @Override
    public Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solve(Problem<TruffleOptimizationProblem> problem, Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> givenSolution) {
        initializeLog(problem);
        this.analytics.logParam("Algorithm", "SequentialComplexityGeneticAlgorithm");
        this.analytics.logParam("Sequence", 0 + "");


        Set<TruffleOptimizationTestComplexity> tests = problem.getProblemGenes().get(0).getGene().getTests();
        Problem<TruffleOptimizationProblem>[] problems = new Problem[sequences <= tests.size() ? sequences : tests.size()];

        if (sequences < tests.size()) {
            double sequenceSize = tests.size() * 1.0 / sequences;
            double sequenceCnt = sequenceSize;
            Set<TruffleOptimizationTestComplexity>[] complexity = new Set[sequences];
            List<TruffleOptimizationTestComplexity> sortedTests = tests.stream().sorted(Comparator.comparingInt(x -> x.getNodeCount())).collect(Collectors.toList());
            int complexityPos = 0;
            int testNo = 0;
            Iterator<TruffleOptimizationTestComplexity> iterator = sortedTests.iterator();
            while (iterator.hasNext()) {
                if (testNo >= sequenceCnt) {
                    sequenceCnt += sequenceSize;
                    complexityPos++;
                }
                if (complexity[complexityPos] == null) {
                    complexity[complexityPos] = new HashSet<>();
                }
                complexity[complexityPos].add(iterator.next());
                testNo++;
            }
            for (int i = 0; i < complexity.length; i++) {
                problems[i] = createProblem(problem, complexity[i]);
            }


        } else {
            AtomicInteger i = new AtomicInteger();
            tests.stream().sorted(Comparator.comparingInt(x -> x.getNodeCount())).forEach(x -> problems[i.getAndIncrement()] = createProblem(problem, x));
        }

        List<Solution<TruffleOptimizationSolution, TruffleOptimizationProblem>> sequencePopulation = null;

        int sequence = 0;
        for (Problem<TruffleOptimizationProblem> problemSequence : problems) {
            sequence++;
            // push in the best from the previous version
            if (sequencePopulation != null) {
                Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> carrySolution = bestSolution;
                this.reset();
                this.bestSolution = RepackagingHelper.createSolution(problemSequence.getProblemGenes().get(0).getGene(), carrySolution.getSolutionGenes().get(0).getGene().getNode());

                initializeLog(problemSequence);
                this.analytics.logParam("Algorithm", "SequentialComplexityGeneticAlgorithm");
                this.analytics.logParam("Sequence", sequence + "");

                int i = 0;
                sequencePopulation.sort(Comparator.comparingDouble(x -> x.getQuality()));
                Iterator<Solution<TruffleOptimizationSolution, TruffleOptimizationProblem>> iterator = sequencePopulation.iterator();
                while (iterator.hasNext() && i < generationalElites) {
                    Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> next = iterator.next();
                    this.addIndividual(RepackagingHelper.createSolution(problemSequence.getProblemGenes().get(0).getGene(), next.getSolutionGenes().get(0).getGene().getNode()));
                    i++;
                }
            }

            // go through the motions
            while (this.nextGeneration(problemSequence) != null) {
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
