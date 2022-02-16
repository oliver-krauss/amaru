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
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleCombinedStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleHierarchicalStrategy;
import at.fh.hagenberg.machinelearning.core.Configurable;
import com.oracle.truffle.api.nodes.Node;

/**
 * Interface to add configurable while Machinelearning doesn't support Configurable mutators
 * @author Oliver Krauss on 27.10.2019
 */
public interface ConfigurableMutator<ST, PT> extends Configurable, Analyzable, at.fh.hagenberg.machinelearning.algorithm.mutation.Mutator<ST, PT> {

    /**
     * Override for Mutator that allows directly optimizing the TruffleOptimizationSolution
     * @param solution to be mutated
     * @return new solution mutant of original
     */
    TruffleOptimizationSolution mutate(TruffleOptimizationSolution solution);

    /**
     * All mutators need a strategy for mutation
     * @param subtreeStrategy strategy for mutations
     */
    void setSubtreeStrategy(TruffleHierarchicalStrategy<Node> subtreeStrategy);

    /**
     * All mutators need a strategy for the "root" that is to be optimized
     * @param fullTreeStrategy strategy for mutations
     */
    void setFullTreeStrategy(TruffleCombinedStrategy<Node> fullTreeStrategy);
}
