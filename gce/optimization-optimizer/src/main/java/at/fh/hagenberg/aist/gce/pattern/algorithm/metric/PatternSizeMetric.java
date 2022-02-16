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

/**
 * Metrics to prune patterns during the mining process.
 */
public class PatternSizeMetric implements Metric {

    private int patternSize;

    @Override
    public boolean applicable(TracableBitwisePattern pattern) {
        return patternSize < 0 || pattern.getSize() <= patternSize;
    }

    @Override
    public boolean expand(TracableBitwisePattern pattern) {
        return patternSize < 0 || pattern.getSize() < patternSize;
    }

    public PatternSizeMetric(int patternSize) {
        this.patternSize = patternSize;
    }

    public int getPatternSize() {
        return patternSize;
    }

    public void setPatternSize(int patternSize) {
        this.patternSize = patternSize;
    }

    @Override
    public double rank(TracableBitwisePattern pattern) {
        return pattern.getSize();
    }
}
