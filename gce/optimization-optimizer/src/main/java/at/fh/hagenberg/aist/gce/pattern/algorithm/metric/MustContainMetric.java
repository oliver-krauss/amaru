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

import at.fh.hagenberg.aist.gce.pattern.encoding.TracableBitwisePattern;

import java.util.Collection;

/**
 * Metric that enforces containment of given other pattern(s).
 * It currently only helps in applicability not in ranking or expanding.
 */
public class MustContainMetric implements Metric {

    /**
     * ANY of these patterns must be contained in a given pattern to be applicable
     */
    private Collection<TracableBitwisePattern> patterns;

    public MustContainMetric(Collection<TracableBitwisePattern> patterns) {
        this.patterns = patterns;
    }

    @Override
    public boolean applicable(TracableBitwisePattern pattern) {
        return this.patterns.stream().anyMatch(pattern::contains);
    }

    @Override
    public boolean expand(TracableBitwisePattern pattern) {
        return true;
    }

    @Override
    public double rank(TracableBitwisePattern pattern) {
        return 0;
    }
}
