/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization;

import at.fh.hagenberg.aist.gce.optimization.cachet.ApproximatingPerformanceCachetEvaluator;
import at.fh.hagenberg.aist.gce.optimization.cachet.PerformanceCachetEvaluator;
import at.fh.hagenberg.aist.gce.optimization.cachet.SelfAdjustingApproximatingPerformanceCachetEvaluator;
import at.fh.hagenberg.aist.gce.optimization.executor.*;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.runtime.RuntimeProfile;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTest;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestComplexity;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestResult;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleTestValue;
import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.fitness.Cachet;
import at.fh.hagenberg.machinelearning.core.fitness.GenericEvaluatorImpl;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import org.neo4j.driver.internal.messaging.Message;
import org.springframework.beans.factory.annotation.Required;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TruffleEvaluatorImpl extends GenericEvaluatorImpl<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    private TruffleGraphAnalytics analyticsService;

    /**
     * Executor for regular cachets
     */
    private Executor executor;

    /**
     * Executor for cachets that require the runtime traces
     */
    private JavassistExecutor tracingExecutor;

    /**
     * Timeout (ms) set to the executors of this evaluator
     */
    private long timeout = 15000;

    /**
     * safeVM is needed when we benchmark
     */
    private boolean safeVM = false;

    /**
     * if true the runtimes will be stored
     */
    private boolean benchmark = false;

    @Override
    public double evaluateQuality(Solution solution) {
        double quality = Double.MAX_VALUE;
        long start = ProfileKeeper.profiler.start();

        TruffleOptimizationSolution solutionGene = (TruffleOptimizationSolution) ((SolutionGene) solution.getSolutionGenes().get(0)).getGene();

        // Log our quality to the graph
        if (analyticsService != null) {
            Solution s = analyticsService.findSolution(solutionGene.getNode(), (ProblemGene<TruffleOptimizationProblem>) ((SolutionGene) solution.getSolutionGenes().get(0)).getProblemGenes().get(0));
            if (s == null) {
                run(solutionGene);
                quality = super.evaluateQuality(solution);
                start = ProfileKeeper.profiler.profile("TruffleEvaluatorImpl.evaluateQuality", start);
                analyticsService.logEvaluation(solution);
                // note that solutions only need be logged if they are new as findSolution already logs the new evaluation (for performance!)
            } else {
                quality = s.getQuality();
                solution.adopt(s);
            }
        } else {
            run(solutionGene);
            quality = super.evaluateQuality(solution);
        }
        ProfileKeeper.profiler.profile("TruffleEvaluatorImpl.evaluateQualityLOG", start);
        return quality;
    }

    public TruffleGraphAnalytics getAnalyticsService() {
        return analyticsService;
    }

    @Required
    public void setAnalyticsService(TruffleGraphAnalytics analyticsService) {
        this.analyticsService = analyticsService;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * The problem currently being run. If it changes the executor must be reinitialized!
     */
    private static TruffleOptimizationProblem current_problem;

    private void run(TruffleOptimizationSolution solution) {
        TruffleOptimizationProblem problem = solution.getProblem();
        verifyExecutor(problem, false);

        try {
            // conduct tests
            if (problem == null || problem.getTests() == null) {
                System.out.println("Problem for evaluation is null this should not be happening");
                return;
            }
            Stream<TruffleOptimizationTestComplexity> stream = null;
            if (problem.getRepeats() <= 10) {
                benchmark = false;
                // < 10 we assume that Performance is NOT an issue an thus we can parallelize the execs
                Logger.log(Logger.LogLevel.DEBUG, "Parallel execution as performance is not being measured");
                stream = problem.getTests().parallelStream();
            } else {
                // >= 10 we assume that the user wants to create a valid Runtime Profile -> sequential ONLY
                benchmark = true;
                stream = problem.getTests().stream();
            }
            stream.forEach(complexity -> {
                TruffleOptimizationTest test = complexity.getTest();
                RuntimeProfile runtime = RuntimeProfile.FAILED_PROFILE;
                RuntimeProfile unoptimizedRuntime = RuntimeProfile.FAILED_PROFILE;
                TruffleTestValue value = new TruffleTestValue(null, null);

                // get return value
                ExecutionResult result = executor.test(solution.getNode(), test.getInputArguments());
                int retries = 20;
                while (retries > 0 && !result.isSuccess() && result.getReturnValue() instanceof String && (((String) result.getReturnValue()).startsWith("WARNING: An illegal reflective access") || ((String) result.getReturnValue()).startsWith("The worker crashed"))) {
                    // redo until we get an actual error
                    retries--;
                    try {
                        System.out.println("AWAITING REDO FOR " + solution.getId());
                        // the illegal reflective access exception is a concurrency problem in the threads
                        Thread.sleep(new Random().nextInt(1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    result = executor.test(solution.getNode(), test.getInputArguments());
                }
                if (result.isSuccess()) {
                    // get return value
                    value = new TruffleTestValue(result.getReturnValue(), result.getReturnValue() != null ? decideType(test.getOutput().getType(), result.getReturnValue()) : null);

                    // profile runtime
                    // currently accepted theory: We want to ignore the first 100.000 runs, or the first half if we have <200000
                    int size = result.getPerformance().length >= 200000 ? 100000 : result.getPerformance().length / 2;
                    runtime = new RuntimeProfile(Arrays.copyOfRange(result.getPerformance(), size, result.getPerformance().length));
                    if (size > 0) {
                        unoptimizedRuntime = new RuntimeProfile(Arrays.copyOfRange(result.getPerformance(), 0, size));
                    }
                    if (benchmark) {
                        try {
                            System.out.println("Logging runtime info");
                            String root = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/PerformanceProfiles/runtimeProfiles/";
                            if (solution.getTree().getId() < 0) {
                                // Switch for performance pattern verification
                                root = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/PerformanceProfiles/runtimePATTERNVERIFY/";
                            }
                            File runtimeReportDir = new File(root + solution.getTree().getId() + "_" + test.getId() + ".rtp");
                            BufferedWriter writer = new BufferedWriter(new FileWriter(runtimeReportDir));
                            writer.write(Arrays.stream(result.getPerformance()).mapToObj(String::valueOf).collect(Collectors.joining(",")));
                            writer.close();
                        } catch (Exception e) {
                            System.out.println("Failed to log runtime info");
                        }
                    }
                } else if (benchmark) {
                    System.out.println("FAILED TO BENCH" + test.getId());
                }

                TraceExecutionResult traceResult = null;
                if (tracingExecutor != null) {
                    traceResult = tracingExecutor.traceTest(solution.getNode(), test.getInputArguments());
                }

                synchronized (solution) {
                    if (result.isSuccess()) {
                        Throwable exception = null;
                        // add result
                        solution.testResults.add(new TruffleOptimizationTestResult(
                                test,
                                runtime,
                                unoptimizedRuntime,
                                exception,
                                value,
                                traceResult));
                    } else {
                        if (result.getReturnValue() instanceof Throwable) {
                            solution.testResults.add(new TruffleOptimizationTestResult(
                                    test,
                                    runtime,
                                    unoptimizedRuntime,
                                    (Throwable) result.getReturnValue(),
                                    value,
                                    traceResult));
                        } else {
                            String exception = (result.getReturnValue() != null ? result.getReturnValue().toString() : null);
                            solution.testResults.add(new TruffleOptimizationTestResult(
                                    test,
                                    runtime,
                                    unoptimizedRuntime,
                                    exception,
                                    value,
                                    traceResult));
                        }
                    }
                }


            });
        } catch (Exception e) {
            // in this case we had a HARD FAIL and the entire test wasn't able to execute
            e.printStackTrace();
            solution.testResults = null;
        }
    }

    private String decideType(String supposedType, Object result) {
        if (result == null) {
            return supposedType;
        }
        String actualType = result.getClass().getName();
        String decidedType = null;
        switch (actualType) {
            case "java.lang.Integer":
                decidedType = "int";
                break;
            case "java.lang.Character":
                decidedType = "char";
                break;
            case "java.lang.Double":
                decidedType = "double";
                break;
            case "java.lang.Float":
                decidedType = "float";
                break;
            case "int":
            case "char":
            case "double":
            case "float":
            case "java.lang.String":
                decidedType = actualType;
                break;
            default:
                decidedType = actualType;
        }
        return decidedType;
    }

    @Override
    public Map<String, Descriptor> getOptions() {
        Map<String, Descriptor> options = super.getOptions();
        options.put("timeout", new Descriptor(this.timeout));
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        try {
            if (name.equals("timeout")) {
                this.setTimeout((Long) descriptor.getValue());
            }
        } catch (Exception e) {
            System.out.println("setting timeout failed");
        }
        return super.setOption(name, descriptor);
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
        if (this.tracingExecutor != null) {
            this.tracingExecutor.setTimeout(timeout);
        }
        if (executor instanceof AbstractExecutor) {
            ((AbstractExecutor) executor).setTimeout(timeout);
        }
    }

    public void setSafeVM(boolean safeVM) {
        this.safeVM = safeVM;
        if (executor instanceof MessageExecutor) {
            ((MessageExecutor) executor).setSafeVM(safeVM);
        }
    }

    public Executor getExecutor() {
        return executor;
    }

    public void verifyExecutor(TruffleOptimizationProblem problem, boolean force) {
        if (executor == null) {
            current_problem = problem;
            executor = MessageExecutor.getSingleton(problem.getLanguage(), problem.getCode(), problem.getEntryPoint(), problem.getFunction());
            ((MessageExecutor) executor).setSettings(problem.getRepeats(), timeout, safeVM);
            if (this.evaluationIdentity().contains(ApproximatingPerformanceCachetEvaluator.NAME)
                    || this.evaluationIdentity().contains(SelfAdjustingApproximatingPerformanceCachetEvaluator.NAME)) {
                tracingExecutor = new JavassistExecutor(problem.getLanguage(), problem.getCode(), problem.getEntryPoint(), problem.getFunction(), null);
                tracingExecutor.setTimeout(timeout);
            }
        } else if (force || problem != current_problem) {
            Logger.log(Logger.LogLevel.INFO, "Recreating executor for different context");
            current_problem = problem;
            executor = MessageExecutor.getSingleton(problem.getLanguage(), problem.getCode(), problem.getEntryPoint(), problem.getFunction());
            ((MessageExecutor) executor).setSettings(problem.getRepeats(), timeout, safeVM);
        }
    }
}
