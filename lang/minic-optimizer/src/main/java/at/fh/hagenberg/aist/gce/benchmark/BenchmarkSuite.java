/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.benchmark;

import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.run.TruffleAlgorithmFactory;
import at.fh.hagenberg.util.Pair;

import java.util.List;

/**
 * Prepared Benchmark suite for running an Experiment
 * @author Oliver Krauss on 17.07.2020
 */
public interface BenchmarkSuite {

    /**
     * Returns all problems that exist in the benchmark
     * Pair contains a problem and the factory that was configured for it
     * @return all problems in benchmark
     */
    List<Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory>> benchmarkProblems();

    /**
     * Human readable name for printing complete statistics on a suite
     * @return name of benchmark suite
     */
    String getName();
}
