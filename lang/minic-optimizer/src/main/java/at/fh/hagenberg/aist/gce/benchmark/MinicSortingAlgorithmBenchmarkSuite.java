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
 * Benchmark suite used for internal testing
 * This is just to test new features etc
 *
 * @author Oliver Krauss on 17.07.2020
 */
public class MinicSortingAlgorithmBenchmarkSuite implements BenchmarkSuite {

    public static Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory> getProblem(String name) {
        MinicTestfileOptimizer optimizer = new MinicTestfileOptimizer(name, null);
        TruffleAlgorithmFactory factory = new TruffleAlgorithmFactory(MinicLanguage.ID, optimizer.getMasterStrategy(), optimizer.getEntryPointStrategy());
        // we allow 3 seconds per exec AT MOST.
        factory.setTimeout(10000); // NOTE: WAS 3000 in BASELINE
        return new Pair<>(optimizer.getProblem(), factory);
    }

    @Override
    public List<Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory>> benchmarkProblems() {
        LinkedList<Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory>> problems = new LinkedList<>();
        problems.add(getProblem("sort/bubbleSort"));
        problems.add(getProblem("sort/heapSort"));
        problems.add(getProblem("sort/insertionSort"));
        problems.add(getProblem("sort/mergeSort"));
        problems.add(getProblem("sort/mergeSortInlined"));
        problems.add(getProblem("sort/quickSort"));
        problems.add(getProblem("sort/quickSortInlined"));
        problems.add(getProblem("sort/selectionSort"));
        problems.add(getProblem("sort/shakerSort"));
        problems.add(getProblem("sort/shellSort"));
        return problems;
    }

    @Override
    public String getName() {
        return "sort";
    }
}
