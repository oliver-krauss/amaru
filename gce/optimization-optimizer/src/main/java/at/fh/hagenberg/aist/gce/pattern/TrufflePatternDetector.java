/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import at.fh.hagenberg.aist.gce.pattern.algorithm.AprioriClusterPatternDetectorAlgorithm;
import at.fh.hagenberg.aist.gce.pattern.algorithm.PatternGrowthClusterPatternDetectorAlgorithm;
import at.fh.hagenberg.aist.gce.pattern.algorithm.PatternGrowthPatternDetector;
import at.fh.hagenberg.aist.gce.pattern.algorithm.editor.AbstractNodeEditor;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.DifferenceMetric;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.Metric;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.SupportMetric;
import at.fh.hagenberg.aist.gce.pattern.selection.PatternSearchSpaceRepository;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.*;
import at.fh.hagenberg.util.Pair;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Truffle Pattern detector (TPD) whose task it is to find transformation patterns for optimization.
 * It is intended to be used as a service, and essentially only encapsulates the settings and calls for the PatternDetector Algorihtms.
 *
 * @author Oliver Krauss on 28.11.2018
 */
public class TrufflePatternDetector {

    protected PatternSearchSpaceRepository repository;

    protected PatternGrowthPatternDetector algorithm = new PatternGrowthPatternDetector();

    protected PatternGrowthClusterPatternDetectorAlgorithm diffAlgorithm = new PatternGrowthClusterPatternDetectorAlgorithm();

    // TODO #252 Remove all settings below. We left the old interface in but only the new "injectMetric should be used"
    /**
     * Restricts the maximal size of the patterns, -1 is infinite
     */
    private int maxPatternSize = 8;

    /**
     * Determines the deepest level of hierarchy the patterns will look for
     * 0 = Explicit
     * 1 = nodes without values
     * 2+ = parent class of node
     */
    private int hierarchyFloor = 0;

    /**
     * Determines the highest level of hierarchy the patterns will look for
     * 0 = Explicit
     * 1 = nodes without values
     * 2+ = parent class of node
     * <p>
     * Must be >= hierarchyFloor
     */
    private int hierarchyCeil = Integer.MAX_VALUE;

    /**
     * How the patterns will be grouped
     */
    protected SignificanceType grouping = SignificanceType.MAX;

    /**
     * If a pattern does not occur in this minimal percentage over all trees the pattern will be excluded
     * Note values are pruned "<" -> 0.25 with 4 trees WILL return all patterns that have 1/4
     * <p>
     * Use this to find often occuring patterns (higher = more occurences)
     */
    protected double minSimilarityRating = 0.5;

    /**
     * If a pattern occurs above this maximal percentage over all trees the pattern will be excluded
     * Note values are pruned ">" -> 0.25 with 4 trees WILL return all patterns that have 1/4
     * <p>
     * Use this to find rarely occuring patterns (lower = more rare)
     */
    protected double maxSimilarityRating = 1;

    /**
     * In the differential patterns a group must have an at least this difference to be significant
     * Ex. Group A has the pattern in 1/4 trees and in Group B it occurs in 3/4 trees. -> significant
     * Ex. Group A has the pattern in 3/4 trees and in Group B it occurs in 4/4 trees -> NOT significant
     * <p>
     * Use this to find Differences in groups (higher = more different)
     */
    protected double minDifferential = 0.5;

    /**
     * In the differential patterns a group must have a maximum difference to be significant
     * Ex. Group A has the pattern in 0/4 trees and in Group B it occurs in 4/4 trees. -> significant
     * Ex. Setting is 0.5 Group A has the pattern in 0/4 trees and in Group B it occurs in 4/4 trees -> NOT significant
     * <p>
     * Use this to find Similarities in groups (lower = more similar)
     */
    protected double maxDifferential = 1;

    public TrufflePatternDetector() {
        // load the analytics
        ClassPathXmlApplicationContext ctx = ApplicationContextProvider.getCtx();
        repository = ctx.getBean(PatternSearchSpaceRepository.class);

        // equalize the algorithm
        algorithm.setHierarchyFloor(this.hierarchyFloor);
        algorithm.setHierarchyCeil(this.hierarchyCeil);
        algorithm.setMaxPatternSize(this.maxPatternSize);
        diffAlgorithm.setHierarchyFloor(this.hierarchyFloor);
        diffAlgorithm.setHierarchyCeil(this.hierarchyCeil);
        diffAlgorithm.setMaxPatternSize(this.maxPatternSize);
    }


    /**
     * Finds only the significant patterns in the given problem space
     * This is done with the current settings
     */
    public Solution<TrufflePattern, TrufflePatternProblem> findSignificantPatterns(TrufflePatternProblem tpp) {
        return findSignificantPatterns(findPatterns(tpp));
    }

    /**
     * Finds only the significant patterns in the given problem space from an already mined solution
     * This is done with the current settings (Default is MAX and >=50%)
     *
     * @param solution patterns to be mined for significat solutions
     * @return only the patterns deemed siginficant
     */
    public Solution<TrufflePattern, TrufflePatternProblem> findSignificantPatterns(Solution<TrufflePattern, TrufflePatternProblem> solution) {
        return findSignificantPatterns(solution, this.grouping);
    }

    /**
     * Finds only the significant patterns in the given problem space
     *
     * @param tpp                 The problem to be analyized
     * @param grouping            Grouping type (default is MAX)
     * @return only the patterns deemed siginficant found in tpp
     */
    public Solution<TrufflePattern, TrufflePatternProblem> findSignificantPatterns(TrufflePatternProblem tpp, SignificanceType grouping) {
        return findSignificantPatterns(findPatterns(tpp), grouping);
    }

    /**
     * Finds only the significant patterns in the given problem space from an already mined solution
     *
     * @param solution            Patterns to be analyzed
     * @param grouping            Grouping type (default is MAX)
     * @return only the patterns deemed significant in the solution
     */
    public Solution<TrufflePattern, TrufflePatternProblem> findSignificantPatterns(Solution<TrufflePattern, TrufflePatternProblem> solution, SignificanceType grouping) {
        Solution<TrufflePattern, TrufflePatternProblem> significantSolution = new Solution<>(solution);
        List<SolutionGene<TrufflePattern, TrufflePatternProblem>> genes = new ArrayList<>();

        switch (grouping) {
            case MIN:
                significantSolution.getSolutionGenes().stream().sorted(Comparator.comparingInt(x -> x.getGene().getSize())).forEach(gene -> {
                    TrufflePattern x = gene.getGene();
                    if (genes.stream().map(Gene::getGene).noneMatch(sig -> sig.getTreeCount() == x.getTreeCount() &&
                            sig.getTreeIds().containsAll(x.getTreeIds()) && x.getNodeIds().containsAll(sig.getNodeIds()))) {
                        // add minima that are not superset of already found minima
                        genes.add(gene);
                    }
                });
                break;
            case MAX:
                significantSolution.getSolutionGenes().stream().sorted(Comparator.comparingInt(x -> ((SolutionGene<TrufflePattern, TrufflePatternProblem>) x).getGene().getSize()).reversed()).forEach(gene -> {
                    TrufflePattern x = gene.getGene();
                    if (genes.stream().map(Gene::getGene).noneMatch(sig -> sig.getTreeCount() == x.getTreeCount() &&
                            sig.getTreeIds().containsAll(x.getTreeIds()) && sig.getNodeIds().containsAll(x.getNodeIds()))) {
                        // add maxima that are not subset of already found maxima
                        genes.add(gene);
                    }
                });
                break;
            default:
                return significantSolution;
        }

        significantSolution.setSolutionGenes(genes);
        return significantSolution;
    }

    /**
     * Compares only the significant patterns in the given problem space
     * This is done with the current settings
     */
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> compareSignificantPatterns(List<TrufflePatternProblem> tpp) {
        return compareSignificantPatterns(comparePatterns(tpp));
    }

    /**
     * Compares only the significant patterns in the given problem space from an already mined solution
     * This is done with the current settings (Default is MAX and >=50%)
     *
     * @param solution patterns to be mined for significat solutions
     * @return only the patterns deemed siginficant
     */
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> compareSignificantPatterns(Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution) {
        return compareSignificantPatterns(solution, this.grouping, this.minSimilarityRating, this.maxSimilarityRating, this.minDifferential, this.maxDifferential);
    }

    /**
     * Finds only the significant patterns in the given problem space
     *
     * @param tpp                 The problem to be analyized
     * @param grouping            Grouping type (default is MAX)
     * @param minSimilarityRating Minimally required similarity (default is >= 50%)
     * @param maxSimilarityRating Maximally required similarity (default is 100%)
     * @param minDifferential     Minimal difference in a group to be significant (default is >= 50%)
     * @param maxDifferential     Maximal difference in a group to be significant (default is 100%)
     * @return only the patterns deemed siginficant found in tpp
     */
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> compareSignificantPatterns(List<TrufflePatternProblem> tpp, SignificanceType grouping, double minSimilarityRating, double maxSimilarityRating, double minDifferential, double maxDifferential) {
        return compareSignificantPatterns(comparePatterns(tpp), grouping, minSimilarityRating, maxSimilarityRating, minDifferential, maxDifferential);
    }

    /**
     * Finds only the significant patterns in the given problem space from an already mined solution
     *
     * @param solution            Patterns to be analyzed
     * @param grouping            Grouping type (default is MAX)
     * @param minSimilarityRating Minimally required similarity (default is >= 50%)
     * @param maxSimilarityRating Maximally required similarity (default is 100%)
     * @param minDifferential     Minimal difference in a group to be significant (default is >= 50%)
     * @param maxDifferential     Maximal difference in a group to be significant (default is 100%)
     * @return only the patterns deemed siginficant of solution
     */
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> compareSignificantPatterns(Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution, SignificanceType grouping, double minSimilarityRating, double maxSimilarityRating, double minDifferential, double maxDifferential) {
        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> significantSolution = new Solution<>(solution);
        List<SolutionGene<TruffleDifferentialPatternSolution, TrufflePatternProblem>> genes = new ArrayList<>();

        Map<TrufflePatternProblem, List<TrufflePattern>> patternsPerProblem;
        Map<TrufflePattern, Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> differential;

        Map<TrufflePatternProblem, List<TrufflePattern>> sourcePatternsPerProblem = solution.getSolutionGenes().get(0).getGene().getPatternsPerProblem();
        Map<TrufflePattern, Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> sourceDifferential = solution.getSolutionGenes().get(0).getGene().getDifferential();

        // remove patterns that are not significantly different
        if (minDifferential > 0 || maxDifferential < 1 || minSimilarityRating > 0 || maxSimilarityRating < 1) {
            patternsPerProblem = new HashMap<>();
            differential = new HashMap<>();
            long trees = sourceDifferential.keySet().stream().flatMap(x -> x.getTreeIds().stream()).distinct().count();
            Map<TrufflePatternProblem, Pair<Long, Long>> treesForProblems = new HashMap<>();
            Map<TrufflePatternProblem, Long> treesPerProblem = new HashMap<>();
            sourcePatternsPerProblem.forEach((key2, value2) -> {
                long treeCount = value2.stream().flatMap(y -> y.getTreeIds().stream()).distinct().count();
                treesPerProblem.put(key2, treeCount);
                treesForProblems.put(key2, new Pair<>(Math.round(Math.floor(treeCount * minSimilarityRating)), Math.round(Math.ceil(treeCount * maxSimilarityRating))));
            });

            // initialize ppP
            sourcePatternsPerProblem.forEach((key1, value1) -> patternsPerProblem.put(key1, new LinkedList<>()));

            sourceDifferential.forEach((key, value) -> {
                List<Map.Entry<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> collect =
                        value.entrySet().stream().filter(y -> {
                                    double percentageA = sourcePatternsPerProblem.get(y.getKey().getKey()).stream().filter(z -> z.getPatternNode().getHash().equals(key.getPatternNode().getHash())).findAny().map(TrufflePattern::getTreeCount).orElse(0L) / (double) treesPerProblem.get(y.getKey().getKey());
                                    double percentageB = sourcePatternsPerProblem.get(y.getKey().getValue()).stream().filter(z -> z.getPatternNode().getHash().equals(key.getPatternNode().getHash())).findAny().map(TrufflePattern::getTreeCount).orElse(0L) / (double) treesPerProblem.get(y.getKey().getValue());
                                    double diff = Math.abs(percentageA - percentageB);
                                    return diff >= minDifferential && diff <= maxDifferential;
                                }
                        ).collect(Collectors.toList());


                if (!collect.isEmpty()) {
                    // find individual problems
                    Set<TrufflePatternProblem> problems = collect.stream().map(x -> x.getKey().getKey()).collect(Collectors.toSet());
                    problems.addAll(collect.stream().map(x -> x.getKey().getValue()).collect(Collectors.toList()));

                    // add to problems per pattern
                    AtomicBoolean addable = new AtomicBoolean(false);
                    problems.stream().forEach(x -> {
                        TrufflePattern problemPattern = sourcePatternsPerProblem.get(x).stream().filter(y -> y.getPatternNode().getHash().equals(key.getPatternNode().getHash())).findFirst().orElse(null);
                        if (problemPattern != null && problemPattern.getTreeCount() >= treesForProblems.get(x).getKey() && problemPattern.getTreeCount() <= treesForProblems.get(x).getValue()) {
                            patternsPerProblem.get(x).add(problemPattern);
                            addable.set(true);
                        }
                    });

                    // add to differential IF any problem remained
                    if (addable.get()) {
                        HashMap<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long> entries = new HashMap<>();
                        collect.forEach(x -> entries.put(x.getKey(), x.getValue()));
                        differential.put(key, entries);
                    }

                }
            });
        } else {
            patternsPerProblem = new HashMap<>(sourcePatternsPerProblem);
            differential = new HashMap<>(sourceDifferential);
        }

        Map<TrufflePatternProblem, List<TrufflePattern>> finalPatternsPerProblem = new HashMap<>(patternsPerProblem);
        Map<TrufflePattern, Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> finalDifferential = new HashMap<>();


        TruffleDifferentialPatternSolution differentialPatternSolution = null;
        switch (grouping) {
            case MIN:
                differential.keySet().stream().sorted(Comparator.comparingInt(TrufflePattern::getSize)).forEach(pattern -> {
                    if (finalDifferential.keySet().stream().noneMatch(sig -> sig.getTreeCount() == pattern.getTreeCount() &&
                            pattern.getTreeIds().containsAll(sig.getTreeIds()) && pattern.getNodeIds().containsAll(sig.getNodeIds())
                            && hierarchyCompare(pattern, sig))) {
                        // add minima that are not superset of already found minima
                        finalDifferential.put(pattern, differential.get(pattern));
                    } else {
                        Predicate<? super TrufflePattern> patternPurge = item -> item.getPatternNode().getHash().equals(pattern.getPatternNode().getHash());
                        finalPatternsPerProblem.values().forEach(x -> x.removeIf(patternPurge));
                    }
                });
                differentialPatternSolution = new TruffleDifferentialPatternSolution(finalPatternsPerProblem, finalDifferential);
                break;
            case MAX:
                differential.keySet().stream().sorted(Comparator.comparingInt(TrufflePattern::getSize).reversed()).forEach(pattern -> {
                    if (finalDifferential.keySet().stream().noneMatch(sig -> sig.getTreeCount() == pattern.getTreeCount() &&
                            sig.getTreeIds().containsAll(pattern.getTreeIds()) && sig.getNodeIds().containsAll(pattern.getNodeIds())
                            && hierarchyCompare(sig, pattern))) {
                        // add maxima that are not superset of already found minima
                        finalDifferential.put(pattern, differential.get(pattern));
                    } else {
                        Predicate<? super TrufflePattern> patternPurge = item -> item.getPatternNode().getHash().equals(pattern.getPatternNode().getHash());
                        finalPatternsPerProblem.values().forEach(x -> x.removeIf(patternPurge));
                    }
                });
                differentialPatternSolution = new TruffleDifferentialPatternSolution(finalPatternsPerProblem, finalDifferential);
                break;
            default:
                differentialPatternSolution = new TruffleDifferentialPatternSolution(patternsPerProblem, differential);
        }


        genes.add(new SolutionGene<>(differentialPatternSolution));
        significantSolution.setSolutionGenes(genes);
        return significantSolution;
    }

    /**
     * Helper function that checks if all overlapping nodes of pattern and sig are also of the same type (hierarchy equality)
     *
     * @param pattern    to be compared to a smaller or equally sized subpattern
     * @param subpattern to be compared to a larger or equally sized pattern
     * @return if hierarchy is similar
     */
    private boolean hierarchyCompare(TrufflePattern pattern, TrufflePattern subpattern) {
        List<NodeWrapper> collect = NodeWrapper.flatten(pattern.getPatternNode()).collect(Collectors.toList());
        return NodeWrapper.flatten(subpattern.getPatternNode()).allMatch(
                // check equality of types
                x -> {
                    NodeWrapper opposite = collect.stream().filter(y -> (y.getId() == null && x.getId() == null) || (y.getId() != null && y.getId().equals(x.getId()))).findFirst().orElse(null);
                    if (opposite == null) {
                        return false;
                    }
                    return x.getType().equals(opposite.getType()) && mapCompare(x.getValues(), opposite.getValues());
                }
        );
    }

    /**
     * Helper function that compares the value maps of two nodes
     *
     * @param left  map to be compared to right
     * @param right map to be compared to left
     * @return if both are equal
     */
    private boolean mapCompare(Map<String, Object> left, Map<String, Object> right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        if (left.size() != right.size()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : left.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!right.containsKey(key)) {
                return false;
            }
            Object o = right.get(key);
            if (value == null || o == null) {
                return value == null && o == null;
            }
            if (!value.equals(o)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Finds ALL patterns in the given problem space
     *
     * @param tpp problem definition for the patterns
     * @return all patterns
     */
    public Solution<TrufflePattern, TrufflePatternProblem> findPatterns(TrufflePatternProblem tpp) {
        // get problem
        Problem<TrufflePatternProblem> problem = new Problem<>();
        problem.setProblemGenes(new ArrayList<>());
        problem.getProblemGenes().add(new ProblemGene<>(tpp));

        algorithm.setHierarchyFloor(this.hierarchyFloor);
        algorithm.setHierarchyCeil(this.hierarchyCeil);

        return algorithm.solve(problem);
    }

    /**
     * Finds ALL patterns in the given problem space with the default parameters
     *
     * @param tps  problem space for the patterns
     * @param name human readable name for easier analysis
     * @return all patterns
     */
    public Solution<TrufflePattern, TrufflePatternProblem> findPatterns(String language, TrufflePatternSearchSpace tps, String name) {
        return findPatterns(language, tps, name, this.maxPatternSize, this.hierarchyFloor, this.hierarchyCeil);
    }

    /**
     * Finds ALL patterns in the given problem space
     *
     * @param tps            problem space for the patterns
     * @param name           human readable name of search space for easier analysis
     * @param maxPatternSize maximum size of patterns to be mined (-1 is infinite)
     * @param hierarchyFloor minimum hierarchy (0 is explicit, 1+ generalizes the nodes)
     * @param hierarchyCeil  maximum hierarchy (>= hierarchyFloor)
     * @return all patterns
     */
    public Solution<TrufflePattern, TrufflePatternProblem> findPatterns(String language, TrufflePatternSearchSpace tps, String name, int maxPatternSize, int hierarchyFloor, int hierarchyCeil) {
        this.algorithm.setHierarchyFloor(hierarchyFloor);
        this.algorithm.setHierarchyCeil(hierarchyCeil);
        this.algorithm.setMaxPatternSize(maxPatternSize);

        Solution<TrufflePattern, TrufflePatternProblem> patterns = findPatterns(new TrufflePatternProblem(language, tps, name));

        this.algorithm.setHierarchyFloor(this.hierarchyFloor);
        this.algorithm.setHierarchyCeil(this.hierarchyCeil);
        this.algorithm.setMaxPatternSize(this.maxPatternSize);
        return patterns;
    }

    /**
     * Finds ALL patterns in the given problem space with the default parameters
     *
     * @param tpsd problem space definition for the patterns
     * @param name human readable name for easier analysis
     * @return all patterns
     */
    public Solution<TrufflePattern, TrufflePatternProblem> findPatterns(String language, TrufflePatternSearchSpaceDefinition tpsd, String name) {
        return findPatterns(language, tpsd, name, this.maxPatternSize, this.hierarchyFloor, this.hierarchyCeil);
    }

    /**
     * Finds ALL patterns in the given problem space
     *
     * @param tpsd           problem space definition for the patterns
     * @param name           human readable name for easier analysis
     * @param maxPatternSize maximum size of patterns to be mined (-1 is infinite)
     * @param hierarchyFloor minimum hierarchy (0 is explicit, 1+ generalizes the nodes)
     * @param hierarchyCeil  maximum hierarchy (>= hierarchyFloor)
     * @return all found patterns
     */
    public Solution<TrufflePattern, TrufflePatternProblem> findPatterns(String language, TrufflePatternSearchSpaceDefinition tpsd, String name, int maxPatternSize, int hierarchyFloor, int hierarchyCeil) {
        TrufflePatternSearchSpace tps = repository.findTrees(tpsd);
        return findPatterns(language, tps, name, maxPatternSize, hierarchyFloor, hierarchyCeil);
    }

    public TrufflePatternSearchSpace findSearchSpace(TrufflePatternSearchSpaceDefinition tpsd) {
        return repository.findTrees(tpsd);
    }

    /**
     * Compares two problems and creates a differential to view
     *
     * @param problems to be compared (>= 2)
     * @return Comparison of the two problem spaces
     */
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> comparePatterns(List<TrufflePatternProblem> problems) {
        if (problems == null || problems.isEmpty() || problems.size() < 2) {
            Logger.log(Logger.LogLevel.WARN, "At least 2 problems are required to create a differential");
            return null;
        }

        // get problem
        Problem<TrufflePatternProblem> problem = new Problem<>();
        problem.setProblemGenes(new ArrayList<>());
        problems.forEach(x -> problem.getProblemGenes().add(new ProblemGene<>(x)));

        diffAlgorithm.setHierarchyFloor(this.hierarchyFloor);
        diffAlgorithm.setHierarchyCeil(this.hierarchyCeil);
        diffAlgorithm.setMaxPatternSize(this.maxPatternSize);

        return diffAlgorithm.solve(problem);
    }

    /**
     * Compares ALL patterns in the given problem space
     *
     * @param searchSpaces   Search spaces to be compared. Pair is <SearchSpace, Human readable name for easier analysis>
     * @param maxPatternSize maximum size of patterns to be mined (-1 is infinite)
     * @param hierarchyFloor minimum hierarchy (0 is explicit, 1+ generalizes the nodes)
     * @param hierarchyCeil  maximum hierarchy (>= hierarchyFloor)
     * @return all patterns
     */
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> comparePatternsBySearchSpace(String language, List<Pair<TrufflePatternSearchSpace, String>> searchSpaces, int maxPatternSize, int hierarchyFloor, int hierarchyCeil) {
        this.algorithm.setHierarchyFloor(hierarchyFloor);
        this.algorithm.setHierarchyCeil(hierarchyCeil);
        this.algorithm.setMaxPatternSize(maxPatternSize);

        List<TrufflePatternProblem> problems = new ArrayList<>();
        searchSpaces.forEach(x -> problems.add(new TrufflePatternProblem(language, x.getKey(), x.getValue())));
        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> patterns = comparePatterns(problems);

        this.algorithm.setHierarchyFloor(this.hierarchyFloor);
        this.algorithm.setHierarchyCeil(this.hierarchyCeil);
        this.algorithm.setMaxPatternSize(this.maxPatternSize);
        return patterns;
    }

    /**
     * Compares ALL patterns in the given problem space with the default parameters
     *
     * @param searchSpaces search spaces to be compared. Pair is <SearchSpace, Human readable name for easier analysis>
     * @return all patterns
     */
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> comparePatternsBySearchSpace(String language, List<Pair<TrufflePatternSearchSpace, String>> searchSpaces) {
        return comparePatternsBySearchSpace(language, searchSpaces, this.maxPatternSize, this.hierarchyFloor, this.hierarchyCeil);
    }

    /**
     * Compares ALL patterns in the given problem space
     *
     * @param searchSpaceDefinitions Search spaces to be compared. Pair is <SearchSpaceDefinition, Human readable name for easier analysis>
     * @param maxPatternSize         maximum size of patterns to be mined (-1 is infinite)
     * @param hierarchyFloor         minimum hierarchy (0 is explicit, 1+ generalizes the nodes)
     * @param hierarchyCeil          maximum hierarchy (>= hierarchyFloor)
     * @return all found patterns
     */
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> comparePatternsBySearchSpaceDefinition(String language, List<Pair<TrufflePatternSearchSpaceDefinition, String>> searchSpaceDefinitions, int maxPatternSize, int hierarchyFloor, int hierarchyCeil) {
        List<Pair<TrufflePatternSearchSpace, String>> searchSpaces = new ArrayList<>();
        searchSpaceDefinitions.forEach(x -> searchSpaces.add(new Pair<>(repository.findTrees(x.getKey()), x.getValue())));
        return comparePatternsBySearchSpace(language, searchSpaces, maxPatternSize, hierarchyFloor, hierarchyCeil);
    }

    /**
     * Compares ALL patterns in the given problem space with the default parameters
     *
     * @param searchSpaceDefinitions Search spaces to be compared. Pair is <SearchSpaceDefinition, Human readable name for easier analysis>
     * @return all patterns
     */
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> comparePatternsBySearchSpaceDefinition(String language, List<Pair<TrufflePatternSearchSpaceDefinition, String>> searchSpaceDefinitions) {
        return comparePatternsBySearchSpaceDefinition(language, searchSpaceDefinitions, this.maxPatternSize, this.hierarchyFloor, this.hierarchyCeil);
    }

    /**
     * Injects a maximum pattern size restriction into the problem
     *
     * @param problem        to restrict by size
     * @param maxPatternSize maximal size of the patterns, -1 is infinite
     * @return size restriction on problem
     */
    public TrufflePatternProblem injectMetric(TrufflePatternProblem problem, int maxPatternSize) {
        // TODO #252 inject pattern size limit
        return problem;
    }


    /**
     * Injects the Hierarchy Metric into the problem
     * 0 = Explicit
     * 1 = nodes without values
     * 2+ = parent class of node
     *
     * @param problem        to restrict by hierarchy
     * @param hierarchyFloor deepest level of hierarchy the patterns will look for
     * @param hierarchyCeil  highest level of hierarchy the patterns will look for, Must be >= hierarchyFloor
     * @return hierarchy restriction on problem
     */
    public TrufflePatternProblem injectMetric(TrufflePatternProblem problem, int hierarchyFloor, int hierarchyCeil) {
        // TODO #252 inject pattern size limit
        return problem;
    }

    /**
     * How the patterns will be grouped
     *
     * @param problem          to restrict by size
     * @param significanceType to restrict by (min, max)
     * @return significance type restriction on problem
     */
    public TrufflePatternProblem injectMetric(TrufflePatternProblem problem, SignificanceType significanceType) {
        // TODO #252 inject pattern size limit
        return problem;
    }

    /**
     * How the patterns will be pruned to be significant
     *
     * @param problem          to restrict by size
     * @param significanceType to restrict by (specialized, generalized)
     * @return significance type restriction on problem
     */
    public TrufflePatternProblem injectMetric(TrufflePatternProblem problem, SpecializationType significanceType) {
        // TODO #252 inject pattern size limit
        return problem;
    }

    /**
     * How patterns will be pruned if they don't occur often enough. WILL be respected PER CLUSTER!!!
     * <p>
     * If a pattern does not occur in the minimal percentage over all trees the pattern will be excluded
     * Note values are pruned "<" -> 0.25 with 4 trees WILL return all patterns that have 1/4
     * <p>
     * Use this to find often occuring patterns (higher = more occurences)
     * If a pattern occurs above this maximal percentage over all trees the pattern will be excluded
     * Note values are pruned ">" -> 0.25 with 4 trees WILL return all patterns that have 1/4
     * <p>
     * Use this to find rarely occuring patterns (lower = more rare)
     *
     * @param problem    to restrict by minimumSupportMetric - Coupling is OR, so the min support will accept any of the groups
     * @param minSupport minimum percentage of trees the pattern must occur in this cluster
     * @param maxSupport maximum percentage of trees the pattern must occur in this cluster
     * @return support metric restriction on problem
     */
    public TrufflePatternProblem injectMetric(List<TrufflePatternProblem> problem, double minSupport, double maxSupport) {
        SupportMetric metric = new SupportMetric(problem, minSupport, maxSupport);
        // TODO #252 inject metric into problem directly -> Also currently there can only be one!
        algorithm.getMetrics().removeIf(x -> x instanceof SupportMetric);
        algorithm.getMetrics().add(metric);
        diffAlgorithm.getMetrics().removeIf(x -> x instanceof SupportMetric);
        diffAlgorithm.getMetrics().add(metric);
        return problem != null && problem.size() > 0 ? problem.get(0) : null;
    }

    /**
     * How patterns will be pruned if they aren't discriminative between the clusters.
     * Will be honored per Cluster
     * <p>
     * In the differential patterns a group must have an at least this difference to be significant
     * Ex. Group A has the pattern in 1/4 trees and in Group B it occurs in 3/4 trees. -> significant
     * Ex. Group A has the pattern in 3/4 trees and in Group B it occurs in 4/4 trees -> NOT significant
     * <p>
     * Use this to find Differences in groups (higher = more different)
     * <p>
     * In the differential patterns a group must have a maximum difference to be significant
     * Ex. Group A has the pattern in 0/4 trees and in Group B it occurs in 4/4 trees. -> significant
     * Ex. Setting is 0.5 Group A has the pattern in 0/4 trees and in Group B it occurs in 4/4 trees -> NOT significant
     * <p>
     * Use this to find Similarities in groups (lower = more similar)
     *
     * @param problem          to restrict by minimumSupportMetric
     * @param minDifferential  minimum percentage difference
     * @param maxDifferential  maximum percentage of trees the pattern must occur in this cluster
     * @param oppositeProblems (Optional) if set only the difference to these clusters will be considered
     * @return support metric restriction on problem
     */
    public TrufflePatternProblem injectMetric(TrufflePatternProblem problem, double minDifferential, double maxDifferential, List<TrufflePatternProblem> oppositeProblems) {
        DifferenceMetric metric = new DifferenceMetric(problem, minDifferential, maxDifferential, oppositeProblems);
        // TODO #252 inject metric into problem directly -> Also currently there can only be one!
        algorithm.getMetrics().removeIf(x -> x instanceof DifferenceMetric);
        algorithm.getMetrics().add(metric);
        diffAlgorithm.getMetrics().removeIf(x -> x instanceof DifferenceMetric);
        diffAlgorithm.getMetrics().add(metric);
        return problem;
    }

    /**
     * Injects any metric into the Problem
     *
     * @param problem        to restrict by size
     * @param metric         to be injected
     * @return problem restricted to metric
     */
    public TrufflePatternProblem injectMetric(TrufflePatternProblem problem, Metric metric) {
        Class<? extends Metric> metricClass = metric.getClass();
        // TODO #252 inject metric into problem directly -> Also currently there can only be one
        algorithm.getMetrics().removeIf(x -> x.getClass().equals(metricClass));
        algorithm.getMetrics().add(metric);
        diffAlgorithm.getMetrics().removeIf(x -> x.getClass().equals(metricClass));
        diffAlgorithm.getMetrics().add(metric);
        return problem;
    }

    public SignificanceType getGrouping() {
        return grouping;
    }

    public void setGrouping(SignificanceType grouping) {
        this.grouping = grouping;
    }

    public double getMinSimilarityRating() {
        return minSimilarityRating;
    }

    public void setMinSimilarityRating(double minSimilarityRating) {
        this.minSimilarityRating = minSimilarityRating;
    }

    public double getMaxSimilarityRating() {
        return maxSimilarityRating;
    }

    public void setMaxSimilarityRating(double maxSimilarityRating) {
        this.maxSimilarityRating = maxSimilarityRating;
    }

    public int getMaxPatternSize() {
        return maxPatternSize;
    }

    public void setMaxPatternSize(int maxPatternSize) {
        this.maxPatternSize = maxPatternSize;
        algorithm.setMaxPatternSize(maxPatternSize);
        diffAlgorithm.setMaxPatternSize(maxPatternSize);
    }

    public int getHierarchyFloor() {
        return hierarchyFloor;
    }

    public void setHierarchyFloor(int hierarchyFloor) {
        this.hierarchyFloor = hierarchyFloor;
        algorithm.setHierarchyFloor(hierarchyFloor);
        diffAlgorithm.setHierarchyFloor(hierarchyFloor);
    }

    public int getHierarchyCeil() {
        return hierarchyCeil;
    }

    public void setHierarchyCeil(int hierarchyCeil) {
        this.hierarchyCeil = hierarchyCeil;
        algorithm.setHierarchyCeil(hierarchyCeil);
        diffAlgorithm.setHierarchyCeil(hierarchyCeil);
    }

    public double getMinDifferential() {
        return minDifferential;
    }

    public void setMinDifferential(double minDifferential) {
        this.minDifferential = minDifferential;
    }

    public double getMaxDifferential() {
        return maxDifferential;
    }

    public void setMaxDifferential(double maxDifferential) {
        this.maxDifferential = maxDifferential;
    }

    public boolean isEmbedded() {
        return algorithm.isEmbedded();
    }

    public void setEmbedded(boolean embedded) {
        algorithm.setEmbedded(embedded);
        diffAlgorithm.setEmbedded(embedded);
    }

    public void clearMetrics() {
        algorithm.getMetrics().clear();
        diffAlgorithm.getMetrics().clear();
    }

    public void setEditor(AbstractNodeEditor<NodeWrapper> editor) {
        algorithm.setEditor(editor);
        diffAlgorithm.setEditor(editor);
    }
}
