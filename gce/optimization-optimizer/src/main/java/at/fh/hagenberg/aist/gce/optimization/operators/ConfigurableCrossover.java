/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.operators;

import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.machinelearning.algorithm.ga.Crossover;
import at.fh.hagenberg.machinelearning.core.Configurable;

/**
 * @author Oliver Krauss on 27.10.2019
 */
public interface ConfigurableCrossover<ST, PT> extends Configurable, Analyzable, Crossover<ST, PT> {

    /**
     * Override for Crossover that allows directly optimizing the TruffleOptimizationSolution
     *
     * @param left  left tree to cross
     * @param right right tree to cross
     * @return a crossover of left and right (completely new object!)
     */
    TruffleOptimizationSolution breed(TruffleOptimizationSolution left, TruffleOptimizationSolution right);

}

