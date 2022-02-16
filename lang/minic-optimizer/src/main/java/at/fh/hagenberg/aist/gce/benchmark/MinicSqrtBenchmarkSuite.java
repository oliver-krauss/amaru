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

import java.util.LinkedList;
import java.util.List;

/**
 * Benchmark suite used for optimizing neural network functions
 * @author Oliver Krauss on 17.07.2020
 */
public class MinicSqrtBenchmarkSuite implements BenchmarkSuite {

    public static Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory> getProblem(String name) {
        MinicTestfileOptimizer optimizer = new MinicTestfileOptimizer(name, null);
        TruffleAlgorithmFactory factory = new TruffleAlgorithmFactory(MinicLanguage.ID, optimizer.getMasterStrategy(), optimizer.getEntryPointStrategy());
        // we allow 3 seconds per exec AT MOST.
        factory.setTimeout(3000);
        return new Pair<>(optimizer.getProblem(), factory);
    }

    @Override
    public List<Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory>> benchmarkProblems() {
        LinkedList<Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory>> problems = new LinkedList<>();
        problems.add(getProblem("/sqrt_opts/sqrt_java"));
        problems.add(getProblem("/sqrt_opts/sqrt_java_outline"));
        problems.add(getProblem("/sqrt_opts/sqrt_inline_lookup"));
        problems.add(getProblem("/sqrt_opts/sqrt_inline_lookup_castfree"));
        problems.add(getProblem("/sqrt_opts/sqrt_lookup"));
        problems.add(getProblem("/sqrt_opts/sqrt_lookup_alt"));
        problems.add(getProblem("/sqrt_opts/sqrt_regular"));
        problems.add(getProblem("/sqrt_opts/sqrt_regular_inline"));
        return problems;
    }

    @Override
    public String getName() {
        return "squareRoot";
    }
}
