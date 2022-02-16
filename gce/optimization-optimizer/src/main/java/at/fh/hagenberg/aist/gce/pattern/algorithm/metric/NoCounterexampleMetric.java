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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Specialization of the {@link DifferenceMetric}, finding only patterns that are UNIQE to one specific cluster
 */
public class NoCounterexampleMetric implements Metric {

    /**
     * The metric must be applied to ANY of the given problems
     */
    private List<TrufflePatternProblem> problems;

    /**
     * Mapping to encoding data of the mining process
     */
    private Map<Integer, Integer> opposites;

    /**
     * The metric must be applied to this target (if null it applies to ALL problems)
     */
    private TrufflePatternProblem target;

    /**
     * Mapping to encoding data of the mining process
     */
    private Integer targetCount;

    /**
     * Mapping to encoding data of the mining process
     */
    private Integer targetClusterId;

    public NoCounterexampleMetric(TrufflePatternProblem problem, List<TrufflePatternProblem> oppositeProblems) {
        if (oppositeProblems != null && oppositeProblems.contains(problem)) {
            throw new RuntimeException("Difference Metric will always fail with a difference from a cluster to itself");
        }
        this.problems = oppositeProblems;
        this.target = problem;
    }

    @Override
    public void init(Map<Integer, TrufflePatternProblem> clusters) {
        this.opposites = new HashMap<>();
        targetClusterId = -1;
        targetCount = -1;
        clusters.forEach((k, v) -> {
            if (target != null && target.equals(v)) {
                // init target
                targetClusterId = k;
                targetCount = target.getSearchSpace().getSearchSpace().size();
            } else {
                // init opposites
                if (problems == null || problems.contains(v)) {
                    this.opposites.put(k, v.getSearchSpace().getSearchSpace().size());
                }
            }
        });
    }

    @Override
    public boolean applicable(TracableBitwisePattern pattern) {
        if (target != null) {
            // either must not be in target, or not in any other cluster
            if (pattern.getClusterTreeCount(targetClusterId) == 0) {
                return true;
            }
            return this.opposites.entrySet().stream().allMatch(x ->
                    pattern.getClusterTreeCount(x.getKey()) == 0
            );
        } else {
            // if no target we accept this not being in any given cluster
            return this.opposites.entrySet().stream().anyMatch(x ->
                    pattern.getClusterTreeCount(x.getKey()) == 0
            );
        }
    }

    @Override
    public boolean expand(TracableBitwisePattern pattern) {
        // Set this to False if applicable -> since the first interesting difference doesn't really need to be grown anymore
        return !applicable(pattern);
    }

    @Override
    public double rank(TracableBitwisePattern pattern) {
        if (target != null) {
            double targetSupport = pattern.getClusterTreeCount(targetClusterId) / (double) targetCount;
            return this.opposites.entrySet().stream().mapToDouble(x -> {
                double support = pattern.getClusterTreeCount(x.getKey()) / (double) x.getValue();
                return Math.abs(targetSupport - support);
            }).max().orElse(0);
        } else {
            LinkedList<Map.Entry<Integer, Integer>> entries = new LinkedList<>(this.opposites.entrySet());
            double obsdiff = 1;
            while (!entries.isEmpty()) {
                Map.Entry<Integer, Integer> pop = entries.pop();
                double targetSupport = pattern.getClusterTreeCount(pop.getKey()) / (double) pop.getValue();
                double rankdiff = this.opposites.entrySet().stream().mapToDouble(x -> {
                    double support = pattern.getClusterTreeCount(x.getKey()) / (double) x.getValue();
                    return Math.abs(targetSupport - support);
                }).min().orElse(0);
                if (rankdiff < obsdiff) {
                    obsdiff = rankdiff;
                }
            }
            return obsdiff;
        }
    }

}
