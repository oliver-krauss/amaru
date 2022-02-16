/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.language;

import at.fh.hagenberg.aist.gce.optimization.executor.AbstractExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.ExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.executor.InternalExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.TraceExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.language.util.CommandProcessor;
import at.fh.hagenberg.aist.gce.optimization.language.util.ExecutionCommand;
import com.oracle.truffle.api.nodes.Node;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * Helper class that conducts a run on the console.
 * It is an extension of the {@link ConsoleWorker} with the sole purpose of running with an inejcted
 * {@link at.fh.hagenberg.aist.gce.optimization.executor.WeightWatcherExecutor} that does nothing, but forces
 * Graal to run our nodes.
 *
 * @author Oliver Krauss on 31.10.2019
 */
public class WeightWatcherWorker extends ConsoleWorker {

    /**
     * Our assumtion of "unoptimized" iterations in the compiler.
     * According to the Graal team 100.000 is a good assumption.
     */
    public static final int REPEATS_UNOTPIMIZED = 100000;

    protected static PrintStream out = System.out;

    public static void setOut(PrintStream out) {
        ConsoleWorker.out = out;
    }

    @Override
    protected ExecutionResult after(ExecutionCommand command, ExecutionResult result) {
        long unopt = Arrays.stream(Arrays.copyOfRange(result.getPerformance(), 0, REPEATS_UNOTPIMIZED)).min().orElse(-1);
        long opt = Arrays.stream(result.getPerformance()).min().orElse(-1);
        return new ExecutionResult(result.getReturnValue(), result.getOutStreamValue(), new long[]{unopt, opt}, result.isSuccess());
    }

    public static void main(String[] args) {
        new WeightWatcherWorker().work(args);
    }

}
