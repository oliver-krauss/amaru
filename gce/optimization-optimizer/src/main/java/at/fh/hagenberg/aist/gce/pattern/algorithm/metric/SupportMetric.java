/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm.metric;

import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.encoding.TracableBitwisePattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Metric to prune by min/max support
 */
public class SupportMetric implements Metric {

    /**
     * The metric must be applied to ANY of the given problems
     */
    private List<TrufflePatternProblem> problems;

    /**
     * Mappin to encoding data of the mining process
     */
    private Map<Integer, Integer> clusters;

    /**
     * If a pattern does not occur in this minimal percentage over all trees the pattern will be excluded
     * Note values are pruned "<" -> 0.25 with 4 trees WILL return all patterns that have 1/4
     * <p>
     * Use this to find often occuring patterns (higher = more occurences)
     */
    private double minSupport;

    /**
     * If a pattern occurs above this maximal percentage over all trees the pattern will be excluded
     * Note values are pruned ">" -> 0.25 with 4 trees WILL return all patterns that have 1/4
     * <p>
     * Use this to find rarely occuring patterns (lower = more rare)
     */
    private double maxSupport;

    public SupportMetric(List<TrufflePatternProblem> problems, double minSupport, double maxSupport) {
        this.problems = problems;
        this.minSupport = minSupport;
        this.maxSupport = maxSupport;
    }

    @Override
    public void init(Map<Integer, TrufflePatternProblem> clusters) {
        if (problems == null) {
            // inject all problems if none were selected
            this.problems = new ArrayList<>(clusters.values());
        }
        this.clusters = new HashMap<>();
        clusters.forEach((k, v) -> {
            if (problems.contains(v)) {
                this.clusters.put(k, v.getSearchSpace().getSearchSpace().size());
            }
        });
    }

    @Override
    public boolean applicable(TracableBitwisePattern pattern) {
        return clusters.entrySet().stream().anyMatch(x -> {
            double support = pattern.getClusterTreeCount(x.getKey()) / (double) x.getValue();
            return minSupport <= support && support <= maxSupport;
        });
    }

    @Override
    public boolean expand(TracableBitwisePattern pattern) {
        return applicable(pattern);
    }

    @Override
    public double rank(TracableBitwisePattern pattern) {
        return clusters.entrySet().stream().mapToDouble(x ->
                pattern.getClusterTreeCount(x.getKey()) / (double) x.getValue()
        ).max().orElse(0);
    }

}
