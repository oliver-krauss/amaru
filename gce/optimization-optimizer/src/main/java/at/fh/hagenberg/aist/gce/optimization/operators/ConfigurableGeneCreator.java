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

import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.core.Configurable;
import at.fh.hagenberg.machinelearning.core.mapping.GeneCreator;

/**
 * @author Oliver Krauss on 27.10.2019
 * @param <ST> Solution
 * @param <PT> Problem
 */
public interface ConfigurableGeneCreator<ST, PT> extends Configurable, Analyzable, GeneCreator<ST, PT> {

    /**
     * Override for Truffle gene creators directly accessing the correct classes
     * @param gene of problem to turn into solution
     * @return solution in search space of problem
     */
    TruffleOptimizationSolution createGene(TruffleOptimizationProblem gene);

}
