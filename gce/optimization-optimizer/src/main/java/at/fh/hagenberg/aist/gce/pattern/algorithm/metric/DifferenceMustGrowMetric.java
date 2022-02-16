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
 * Metric to identify discriminative patterns in clusters
 */
public class DifferenceMustGrowMetric extends DifferenceMetric {


    public DifferenceMustGrowMetric(TrufflePatternProblem problem, double minDifferential, double maxDifferential, List<TrufflePatternProblem> oppositeProblems) {
        super(problem, minDifferential, maxDifferential, oppositeProblems);
    }

    @Override
    public boolean applicable(TracableBitwisePattern pattern) {
        return super.applicable(pattern);
    }

    @Override
    public boolean expand(TracableBitwisePattern pattern) {
        return pattern.getOrigin() == null || super.rank(pattern) - super.rank(pattern.getOrigin()) > 0;
    }

    @Override
    public double rank(TracableBitwisePattern pattern) {
        return pattern.getOrigin() == null ? super.rank(pattern) : super.rank(pattern) - super.rank(pattern.getOrigin());
    }
}
