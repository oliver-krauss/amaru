/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.test;


import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.executor.JavassistExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.TraceExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Oliver Krauss
 */
public class TruffleTestComplexityEvaluator {

    /**
     * Evaluates the test complexity of all tests given in problem.getTests()
     *
     * @param problem
     */
    public void evaluateComplexity(TruffleOptimizationProblem problem) {
        // List of all methods to intercept
        List<String> methods = new ArrayList<>();
        methods.add("execute");
        methods.add("AndSpecialize");

        System.out.println("Number of test cases: " + problem.getTests().size());

        try {
            JavassistExecutor executor = new JavassistExecutor(problem.getLanguage(), problem.getCode(), problem.getEntryPoint(), problem.getFunction(), null);
            problem.getTests().forEach(complexity -> {
                TraceExecutionResult result = executor.traceTest(executor.getOrigin(), complexity.getTest().getInputArguments());
                complexity.setNodeCount(result.getNumberOfExecutedNodes());
                complexity.setSpezialisations(result.getNumberofSpecializedNodes());
                complexity.setNodes(new ArrayList<>(result.getNodeExecutions().keySet()));
            });
        } catch (Exception e) {
            // note -> if we exclude classes from our TLI, we can't proxy them. But not being able to proxy should never crash the run
            Logger.log(Logger.LogLevel.WARN, "Warning proxying was not possible", e);
        }
    }
}
