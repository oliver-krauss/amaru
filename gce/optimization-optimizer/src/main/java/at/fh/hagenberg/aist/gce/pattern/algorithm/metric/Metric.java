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
 * Metrics to prune patterns during the mining process.
 */
public interface Metric {

    /**
     * OPTIONAL initializer for metrics, telling them meta information about the encoding
     * @param clusters
     */
    default void init(Map<Integer, TrufflePatternProblem> clusters) {
        // do nothing
    }

    /**
     * Only when a metric is applicable it will be added to the results.
     * @param pattern to be validated
     * @return if the pattern is valid in the final results context
     */
    boolean applicable(TracableBitwisePattern pattern);

    /**
     * Optional Feature of a metric. If the expand op does not succeed it won't be further evaluated
     * If it is NOT valid it will also NOT be extended anymore!
     * @param pattern to be validated
     * @return if the pattern is valid in the final results context
     */
    boolean expand(TracableBitwisePattern pattern);

    /**
     * Distinct value that may be used for ranking (display, filtering, ...)
     * @return ranking value of this metric
     */
    double rank(TracableBitwisePattern pattern);
}
