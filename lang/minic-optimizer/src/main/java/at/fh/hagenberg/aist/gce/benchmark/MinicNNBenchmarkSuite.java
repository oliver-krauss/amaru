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
 * @author Oliver Krauss on 17.07.2020
 */
public class MinicNNBenchmarkSuite implements BenchmarkSuite {

    public static Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory> getProblem(String name) {
        MinicTestfileOptimizer optimizer = new MinicNNTestfileOptimizer(name, null);
        // TODO rewire the function to optimize something different
        TruffleAlgorithmFactory factory = new TruffleAlgorithmFactory(MinicLanguage.ID, optimizer.getMasterStrategy(), optimizer.getEntryPointStrategy());
        // we allow 10 seconds per exec AT MOST
        factory.setTimeout(30000); // NOTE WAS 10000 in BASELINE
        return new Pair<>(optimizer.getProblem(), factory);
    }

    @Override
    public List<Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory>> benchmarkProblems() {
        LinkedList<Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory>> problems = new LinkedList<>();
        problems.add(getProblem("/nn/nn_options"));
        problems.add(getProblem("/nn/nn_fullinline"));
        problems.add(getProblem("/nn/nn_sigmoid"));
        problems.add(getProblem("/nn/nn_tanh"));
        problems.add(getProblem("/nn/nn_swish"));
        problems.add(getProblem("/nn/nn_relu"));
        problems.add(getProblem("/nn/nn_lrelu"));
        return problems;
    }

    @Override
    public String getName() {
        return "neuralNetwork";
    }

    private static class MinicNNTestfileOptimizer extends MinicTestfileOptimizer {
        public MinicNNTestfileOptimizer(String name, Node bestKnownSolution) {
            super(name, bestKnownSolution);
        }

        @Override
        protected String getEntryPoint() {
            return "nn_entry";
        }
    }
}
