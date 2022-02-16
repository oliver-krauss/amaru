/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.cachet;

import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.machinelearning.core.Solution;

/**
 * This cachet adjusts the given performance value by the weights in {@link at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation}
 * The purpose is to run on different PC systems and convert the values as if they were run on one and the same architecture
 * Created by Oliver Krauss on 31.12.2019.
 */
public class SelfAdjustingApproximatingPerformanceCachetEvaluator extends ApproximatingPerformanceCachetEvaluator {

    public static final String NAME = "SelfAdjusting-PerformanceApproximation-0.1";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * WARNING: call only after setLanguage
     * @param information
     */
    public void setSytemInformation(SystemInformation information) {
        this.weightUtil.setInformation(information);
    }
}
