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
import java.util.List;
import java.util.Map;

/**
 * Metric to identify discriminative patterns in clusters
 */
public class FaultFindingMetric implements Metric {

    /**
     * The failing cluster that this metric covers
     */
    private TrufflePatternProblem fail;

    /**
     * The succeeding cluster that this metric covers
     */
    private TrufflePatternProblem success;

    /**
     * Mapping to encoding data of the mining process
     */
    private Integer failCount;

    /**
     * Mapping to encoding data of the mining process
     */
    private Integer failClusterId;

    /**
     * Mapping to encoding data of the mining process
     */
    private Integer successCount;

    /**
     * the maximum outliers that we allow
     */
    private int maxOutliers;

    /**
     * Mapping to encoding data of the mining process
     */
    private Integer successClusterId;

    public FaultFindingMetric(TrufflePatternProblem success, TrufflePatternProblem fail, int maxOutliers) {
        this.success = success;
        this.fail = fail;
        this.maxOutliers = maxOutliers;
    }

    @Override
    public boolean applicable(TracableBitwisePattern pattern) {
        return faultExclusive(pattern) <= maxOutliers;
    }

    @Override
    public void init(Map<Integer, TrufflePatternProblem> clusters) {
        successClusterId = -1;
        failClusterId = -1;
        clusters.forEach((k, v) -> {
            if (success.equals(v)) {
                // init target
                successClusterId = k;
                successCount = success.getSearchSpace().getSearchSpace().size();
            }
            if (fail.equals(v)) {
                // init target
                failClusterId = k;
                failCount = fail.getSearchSpace().getSearchSpace().size();
            }
        });
    }

    /**
     * Fault exclusive patterns are those that have no counterexample in the succeeding space
     *
     * @param pattern that shall be checked
     * @return if it is exclusive to the fault space
     */
    private long faultExclusive(TracableBitwisePattern pattern) {
        return pattern.getClusterTreeCount(successClusterId);
    }

    /**
     * A fault of omission can only be identified by Co-Located pattern mining
     * This first step attempts to find patterns with a LOW discriminative value,
     * i.e. patterns occuring often in succeeding and failing group which will have additional nodes in the succeeding one
     *
     * @param pattern that shall be checked
     * @return if it is often occuring in the succeeding and failing groups
     */
    private double faultOfOmission(TracableBitwisePattern pattern) {
        double successSupport = pattern.getClusterTreeCount(successClusterId) / (double) successCount;
        double faultSupport = pattern.getClusterTreeCount(failClusterId) / (double) failCount;
        return Math.abs(successSupport - faultSupport);
    }

    @Override
    public boolean expand(TracableBitwisePattern pattern) {
        return  pattern.getOrigin() == null || // always check > 1 size
                (!applicable(pattern) && // if applicable we are already happy, no need to grow
                faultOfOmission(pattern) >= faultOfOmission(pattern.getOrigin())); // the difference must grow to target fault exclusives
    }

    @Override
    public double rank(TracableBitwisePattern pattern) {
        // rank by how many counterexamples we have, AND how strong the pattern itself is
        return faultExclusive(pattern) - pattern.getClusterTreeCount(failClusterId);
    }
}
