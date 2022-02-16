/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.executor;

import com.oracle.truffle.api.nodes.Node;

/**
 * Executor to be used by the TruffleOptimizationSolution
 * It runs a single AST and returns the run information
 * @author Oliver Krauss on 28.10.2019
 */
public interface Executor {

    /**
     * Runs the program (node replaces the origin!)
     * @param node  to be run in context
     * @param input for the main function!
     * @return execution results
     */
    ExecutionResult test(Node node, Object[] input);

    /**
     * Method for when the executor must be re-initialized during runtime (ex. multiple experiments being run etc.)
     * @param language  that the code will be run in
     * @param code      that is being tested
     * @param entryPoint function that will be called
     * @param function  being optimized
     * @return can be a completely different executor, or the same one with a re-initialized context
     */
    Executor replace(String language, String code, String entryPoint, String function);
}
