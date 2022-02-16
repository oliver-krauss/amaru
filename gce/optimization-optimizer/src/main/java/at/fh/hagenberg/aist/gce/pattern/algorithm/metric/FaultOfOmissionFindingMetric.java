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

import java.util.Map;

/**
 * Metric to identify discriminative patterns in clusters
 */
public class FaultOfOmissionFindingMetric implements Metric {

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
     * The target discriminativity we are aiming for
     */
    private double minSupport;

    /**
     * Mapping to encoding data of the mining process
     */
    private Integer successClusterId;

    public FaultOfOmissionFindingMetric(TrufflePatternProblem success, TrufflePatternProblem fail, double minSupport) {
        this.success = success;
        this.fail = fail;
        this.minSupport = minSupport;
    }

    @Override
    public boolean applicable(TracableBitwisePattern pattern) {
        // this is a pure rank based metric. Use this together with TopN!
        return faultOfOmission(pattern) >= minSupport;
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
        return successSupport + (faultSupport * 3); // overvalue faults as they are the more important metric
    }

    @Override
    public boolean expand(TracableBitwisePattern pattern) {
        return  pattern.getOrigin() == null || // always check > 1 size
                (!applicable(pattern) && // if applicable we are already happy, no need to grow
                faultOfOmission(pattern) < faultOfOmission(pattern.getOrigin())); // the overlap must have grown
    }

    @Override
    public double rank(TracableBitwisePattern pattern) {
        return faultOfOmission(pattern);
    }
}
