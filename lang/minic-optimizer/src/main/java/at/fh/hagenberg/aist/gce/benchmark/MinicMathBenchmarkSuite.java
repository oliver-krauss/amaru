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

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.run.TruffleAlgorithmFactory;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.nodes.Node;

import java.util.LinkedList;
import java.util.List;

/**
 * Benchmark suite used for optimizing neural network functions
 *
 * @author Oliver Krauss on 17.07.2020
 */
public class MinicMathBenchmarkSuite implements BenchmarkSuite {

    public static Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory> getProblem(String name) {
        MinicTestfileOptimizer optimizer = new MinicTestfileOptimizer(name, null);
        TruffleAlgorithmFactory factory = new TruffleAlgorithmFactory(MinicLanguage.ID, optimizer.getMasterStrategy(), optimizer.getEntryPointStrategy());
        // we allow 3 seconds per exec AT MOST.
        factory.setTimeout(10000); // NOTE: WAS 3000 in BASELINE
        return new Pair<>(optimizer.getProblem(), factory);
    }

    public static Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory> getLookupProblem(String name) {
        LookupEntryOptimizer optimizer = new LookupEntryOptimizer(name, null);
        TruffleAlgorithmFactory factory = new TruffleAlgorithmFactory(MinicLanguage.ID, optimizer.getMasterStrategy(), optimizer.getEntryPointStrategy());
        // we allow 3 seconds per exec AT MOST.
        factory.setTimeout(3000);
        return new Pair<>(optimizer.getProblem(), factory);
    }

    @Override
    public List<Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory>> benchmarkProblems() {
        LinkedList<Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory>> problems = new LinkedList<>();
        problems.add(getProblem("/math/sqrt_java"));
        problems.add(getLookupProblem("/math/sqrt_lookup"));
        problems.add(getProblem("/math/sqrt_nolookup"));
        problems.add(getProblem("/math/cbrt"));
        problems.add(getProblem("/math/surt"));
        problems.add(getProblem("/math/log"));
        problems.add(getProblem("/math/ln"));
        problems.add(getProblem("/math/invSqrt"));
        return problems;
    }

    private static class LookupEntryOptimizer extends MinicTestfileOptimizer {
        public LookupEntryOptimizer(String name, Node bestKnownSolution) {
            super(name, bestKnownSolution);
        }

        @Override
        protected String getEntryPoint() {
            return "lookup_entry";
        }
    }

    @Override
    public String getName() {
        return "math";
    }
}
