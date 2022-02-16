/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.runtime;

/**
 * The Runtime Evaluator is supposed to return statistically valid runtime averages.
 * The evaluator is specific to Graal and Truffle as it considers both pre- and post optimization values
 * <p>
 * It's usage is as follows:
 * <p>
 * Execution:
 * long start = System.nanoTime();
 * DO X
 * long end = System.nanoTime();
 * runtimeEvaluator.addEvaluation(start, end);
 * <p>
 * Evaluation:
 * runtimeEvaluator.getRuntimeProfile();
 */
public class RuntimeEvaluator {

    /**
     * Size of evaluations that will happen
     */
    private int size;

    /**
     * List of all evaluations
     */
    private long[] evaluations;

    /**
     * Index of current evaluation
     */
    private int evaluation = 0;

    public RuntimeEvaluator(int size) {
        this.size = size;
        evaluations = new long[size];
        evaluation = 0;
    }

    /**
     * Adds an evaluation with a start and stop time that will be considered in the return values
     *
     * @param start
     * @param end
     */
    public void addEvaluation(long start, long end) {
        evaluations[evaluation] = end - start;
        evaluation++;
    }

    /**
     * @return the runtime profile of all evaluations BEFORE Graal optimized the code;
     */
    public RuntimeProfile getUnoptimizedRuntimeProfile() {
        return new RuntimeProfile(evaluations);
    }

    /**
     * @return the runtime profile of all evaluations AFTER Graal optimized the code;
     */
    public RuntimeProfile getOptimizedRuntimeProfile() {
        return getUnoptimizedRuntimeProfile();

    }

    public void reset() {
        evaluation = 0;
        evaluations = new long[size];
    }

}