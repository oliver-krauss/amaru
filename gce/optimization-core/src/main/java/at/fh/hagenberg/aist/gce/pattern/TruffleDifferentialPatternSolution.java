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


import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.util.Pair;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Truffle Differential Pattern Solution, grouping patterns against each other
 * It is the solution corresponding to {@link TrufflePatternProblem}
 *
 * @author Oliver Krauss on 28.11.2018
 */
public class TruffleDifferentialPatternSolution {

    /**
     * All original problems with how often the pattern occurs for them
     */
    private Map<TrufflePatternProblem, List<TrufflePattern>> patternsPerProblem;

    /**
     * All patterns with A - B differentials. The differential is the number of occurences (total not trees!):
     * positive: Occurs more often in A than B
     * 0: Occurs exactly the same amount of times in A and B
     * negative: Occurs more often in B than A
     */
    private Map<TrufflePattern, Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> differential;

    public TruffleDifferentialPatternSolution(Map<TrufflePatternProblem, List<TrufflePattern>> patternsPerProblem, Map<TrufflePattern, Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> differential) {
        this.patternsPerProblem = patternsPerProblem;
        this.differential = differential;
    }

    public Map<TrufflePatternProblem, List<TrufflePattern>> getPatternsPerProblem() {
        return patternsPerProblem;
    }

    public Map<TrufflePattern, Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> getDifferential() {
        return differential;
    }

    /**
     * Returns the pattern specific to the context of only one problem
     * @param problem problem that provides the context
     * @param pattern pattern (from differential) that shall be returned
     * @return pattern if contained
     */
    public TrufflePattern getPatternForProblem(TrufflePatternProblem problem, TrufflePattern pattern) {
        if (patternsPerProblem.containsKey(problem)) {
            return patternsPerProblem.get(problem).stream().filter(x -> x.getPatternNode().getHash().equals(pattern.getPatternNode().getHash())).findFirst().orElse(null);
        }
        return null;
    }

    /**
     * this is a helper function that returns the maximum pattern occurence over individual problems
     * Purpose is purely for display
     * @return patternsPerProblem.values.max(pattern.count)
     */
    public int maxPatternCountPerProblem() {
        return patternsPerProblem.values().stream().flatMap(Collection::stream).mapToInt(x -> (int) x.getCount()).max().orElse(0);
    }

    /**
     * this is a helper function that returns the maximum pattern occurence over individual problems
     * Purpose is purely for display
     * @param pattern Pattern that the count will be returned for
     * @return patternsPerProblem.values.max(pattern.count)
     */
    public int maxPatternCountPerProblem(TrufflePattern pattern) {
        return patternsPerProblem.values().stream().flatMap(Collection::stream).filter(x -> x.getPatternNode().getHash().equals(pattern.getPatternNode().getHash())).mapToInt(x -> (int) x.getCount()).max().orElse(0);
    }

    /**
     * Checks if the pattern is contained in ALL problems
     * @param pattern to be checked
     * @return true if all problems contain the pattern
     */
    public boolean contained(TrufflePattern pattern) {
        return patternsPerProblem.values().stream().allMatch(x -> x.stream().anyMatch(y -> y.getPatternNode().getHash().equals(pattern.getPatternNode().getHash())));
    }

    /**
     * Returns the count of patterns that occur in all problems
     * @return count of patterns that occur in all problems
     */
    public long overlapCount() {
        return differential.keySet().stream().filter(pattern -> patternsPerProblem.values().stream().allMatch(x -> x.stream().anyMatch(y -> y.getPatternNode().getHash().equals(pattern.getPatternNode().getHash())))).count();
    }

    /**
     * Returns the truffle patterns sorted after the amount of overlaps between problems from least to most
     * Secondary sort is the amount of occurences of the pattern itself
     * @return pattenrns sorted by overlap, overlap in same area (alphabetical), occurences
     */
    public List<TrufflePattern> patternSorted() {
        return differential.keySet().stream().sorted(new Comparator<TrufflePattern>() {
            @Override
            public int compare(TrufflePattern o1, TrufflePattern o2) {
                // check occurence amount
                int compare = Long.compare(patternsPerProblem.values().stream().filter(x -> x.stream().anyMatch(y -> y.getPatternNode().getHash().equals(o1.getPatternNode().getHash()))).count(),
                    patternsPerProblem.values().stream().filter(x -> x.stream().anyMatch(y -> y.getPatternNode().getHash().equals(o2.getPatternNode().getHash()))).count());

                // check names of occurences
                if (compare == 0) {
                    compare = patternsPerProblem.entrySet().stream().filter(x -> x.getValue().stream().anyMatch(y -> y.getPatternNode().getHash().equals(o1.getPatternNode().getHash()))).map(x -> x.getKey().getName()).sorted().collect(Collectors.joining("-"))
                        .compareTo(patternsPerProblem.entrySet().stream().filter(x -> x.getValue().stream().anyMatch(y -> y.getPatternNode().getHash().equals(o2.getPatternNode().getHash()))).map(x -> x.getKey().getName()).sorted().collect(Collectors.joining("-")));
                }

                // check amount of occurences
                if (compare == 0) {
                    compare = Long.compare(o1.getCount(), o2.getCount());
                }

                return compare;
            }
        }).collect(Collectors.toList());
    }
}
