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
import at.fh.hagenberg.util.Pair;

import java.util.*;

public class ParallelComplexityGeneticAlgorithm extends GeneticAlgorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    /**
     * Amount of sequences that the algorithm will split the given testdata into
     */
    private int startingGroups = 3;

    /**
     * Divider how many groups will be combined per iteration
     * <p>
     * Ex: 5 Starting Groups
     * Iteration 1 -> 5
     * Iteration 2 -> 5 / 2 -> 2 (always floored)
     * Iteration 3 -> 2 / 2 -> 1
     * Iteration 4 -> 1 / 2 -> 0 ---- no fourth iteration
     */
    private int combinationRate = 2;

    /*
     * Amount of elites that will be taken into the next generation (PER GROUP!)
     * NOTE: Should be FAR less than the population size
     */
    private int generationalElites = 10;

    /**
     * Decides if the grouping should be done by most similar, or by most different;
     */
    private boolean groupSimilar = true;

    private Comparator<Pair<TruffleOptimizationTestComplexity, Double>> similarityComparator = new Comparator<Pair<TruffleOptimizationTestComplexity, Double>>() {
        @Override
        public int compare(Pair<TruffleOptimizationTestComplexity, Double> o1, Pair<TruffleOptimizationTestComplexity, Double> o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    };

    private Comparator<Pair<TruffleOptimizationTestComplexity, Double>> differenceComparator = new Comparator<Pair<TruffleOptimizationTestComplexity, Double>>() {
        @Override
        public int compare(Pair<TruffleOptimizationTestComplexity, Double> o1, Pair<TruffleOptimizationTestComplexity, Double> o2) {
            return o2.getValue().compareTo(o1.getValue());
        }
    };

    private Comparator<Pair<TruffleOptimizationTestComplexity, Double>> groupComparator = similarityComparator;


    private static Set<TruffleOptimizationTestComplexity> testsToKeep = new HashSet<>();


    private Set<TruffleOptimizationTestComplexity>[] equalDistribution(Set<TruffleOptimizationTestComplexity> originalTests, int numberOfGroups) {
        // copy test-set (as we modify it, and set up everything we need
        Set<TruffleOptimizationTestComplexity> tests = new HashSet<>(originalTests);
        Set<TruffleOptimizationTestComplexity>[] distributedTests = new Set[numberOfGroups];
        int testsInGroup = (int) Math.ceil(tests.size() * 1.0 / numberOfGroups);

        // fill every group
        for (int i = 0; i < distributedTests.length; i++) {
            // initialize group and give it ONE test
            distributedTests[i] = new HashSet<>();
            TruffleOptimizationTestComplexity groupLead = tests.iterator().next();
            distributedTests[i].add(groupLead);
            tests.remove(groupLead);

            // fill the group with other tests
            int testsInThisGroup = 1;
            while (testsInThisGroup < testsInGroup && tests.size() > 0) {
                TruffleOptimizationTestComplexity groupMember =
                    tests.stream().map(x -> new Pair<TruffleOptimizationTestComplexity, Double>(x, x.overlap(groupLead))).sorted(groupComparator).findFirst().get().getKey();
                distributedTests[i].add(groupMember);
                tests.remove(groupMember);
                testsInThisGroup++;
            }
        }

        return distributedTests;
    }

    @Override
    public Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solve(Problem<TruffleOptimizationProblem> problem, Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> bestSolution) {
        initializeLog(problem);
        this.analytics.logParam("Algorithm", "ParallelComplexityGeneticAlgorithm");
        this.analytics.logParam("Sequence", 0 + "");
        this.analytics.logParam("Depth", 0 + "");

        // prepare data
        Set<TruffleOptimizationTestComplexity> testComplexities = problem.getProblemGenes().get(0).getGene().getTests();
        int groups = testComplexities.size() > startingGroups ? startingGroups : testComplexities.size();
        Set<TruffleOptimizationTestComplexity>[] tests = equalDistribution(testComplexities, groups);
        List<Solution<TruffleOptimizationSolution, TruffleOptimizationProblem>>[] groupPopulations = new List[groups];
        int depth = -1;

        while (groups >= 1) {
            depth++;
            for (int i = 0; i < tests.length; i++) {

                // create problem
                Problem<TruffleOptimizationProblem> currentProblem = RepackagingHelper.createProblem(problem, tests[i]);

                // do a proper reset if needed
                if (i > 0 || groupPopulations[i] != null) {
                    Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> carrySolution = bestSolution;
                    this.reset();
                    this.bestSolution = RepackagingHelper.createSolution(currentProblem.getProblemGenes().get(0).getGene(), carrySolution.getSolutionGenes().get(0).getGene().getNode());
                    initializeLog(problem);
                    this.analytics.logParam("Algorithm", "ParallelComplexityGeneticAlgorithm");
                    this.analytics.logParam("Sequence", i + "");
                    this.analytics.logParam("Depth", depth + "");
                }

                // load in individuals if necessary
                if (groupPopulations[i] != null) {
                    groupPopulations[i].forEach(x -> this.addIndividual(RepackagingHelper.createSolution(currentProblem.getProblemGenes().get(0).getGene(), x.getSolutionGenes().get(0).getGene().getNode())));
                }

                // store created populations, and reset
                while (this.nextGeneration(currentProblem) != null) {
                }
                groupPopulations[i] = this.getPopulation();
                groupPopulations[i].sort(Comparator.comparingDouble(x -> x.getQuality()));
            }

            groups = groups / combinationRate;
            if (groups > 0) {
                Set<TruffleOptimizationTestComplexity>[] newTests = equalDistribution(testComplexities, groups);
                List<Solution<TruffleOptimizationSolution, TruffleOptimizationProblem>>[] newPops = new List[groups];

                // transfer successful pops to new posp
                for (int i = 0; i < tests.length; i++) {
                    for (int j = 0; j < newTests.length; j++) {
                        final int jj = j;
                        if (tests[i].stream().anyMatch(x -> newTests[jj].contains(x))) {
                            Iterator<Solution<TruffleOptimizationSolution, TruffleOptimizationProblem>> iterator = groupPopulations[i].iterator();
                            int transferRate = 0;
                            while (iterator.hasNext() && transferRate < generationalElites) {
                                transferRate++;
                                if (newPops[j] == null) {
                                    newPops[j] = new ArrayList<>();
                                }
                                newPops[j].add(iterator.next());
                            }
                        }
                    }
                }

                tests = newTests;
                groupPopulations = newPops;
            }
        }

        return bestSolution;
    }

    public int getStartingGroups() {
        return startingGroups;
    }

    public void setStartingGroups(int startingGroups) {
        this.startingGroups = startingGroups;
    }

    public int getCombinationRate() {
        return combinationRate;
    }

    public void setCombinationRate(int combinationRate) {
        this.combinationRate = combinationRate;
    }

    public int getGenerationalElites() {
        return generationalElites;
    }

    public void setGenerationalElites(int generationalElites) {
        this.generationalElites = generationalElites;
    }

    public boolean isGroupSimilar() {
        return groupSimilar;
    }

    public void setGroupSimilar(boolean groupSimilar) {
        this.groupSimilar = groupSimilar;
        if (groupSimilar) {
            groupComparator = similarityComparator;
        } else {
            groupComparator = differenceComparator;
        }
    }

    @Override
    protected Map<String, Descriptor> getSpecificOptions() {
        Map<String, Descriptor> options = super.getSpecificOptions();
        options.put("startingGroups", new Descriptor(this.startingGroups));
        options.put("combinationRate", new Descriptor(this.combinationRate));
        options.put("generationalElites", new Descriptor(this.generationalElites));
        options.put("groupSimilar", new Descriptor(this.groupSimilar));
        return options;
    }

    @Override
    protected boolean setSpecificOption(String name, Descriptor descriptor) {
        if (name.equals("startingGroups")) {
            this.setStartingGroups((Integer) descriptor.getValue());
        } else if (name.equals("combinationRate")) {
            this.setCombinationRate((Integer) descriptor.getValue());
        } else if (name.equals("generationalElites")) {
            this.setGenerationalElites((Integer) descriptor.getValue());
        } else if (name.equals("groupSimilar")) {
            this.setGroupSimilar((Boolean) descriptor.getValue());
        } else {
            return super.setSpecificOption(name, descriptor);
        }
        return true;
    }
}
