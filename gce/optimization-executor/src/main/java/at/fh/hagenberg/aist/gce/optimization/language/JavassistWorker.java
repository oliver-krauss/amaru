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
import at.fh.hagenberg.aist.gce.optimization.executor.TraceExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.language.util.ExecutionCommand;
import at.fh.hagenberg.aist.gce.optimization.language.util.JavassistInterceptCallback;
import at.fh.hagenberg.aist.gce.optimization.language.util.JavassistInterceptProvider;
import at.fh.hagenberg.aist.gce.optimization.language.util.JavassistInterceptor;
import com.oracle.truffle.api.nodes.Node;

import java.io.PrintStream;

/**
 * Helper class that conducts a run on the console.
 * It is an extension of the {@link ConsoleWorker} and provides additional information that is available only in
 * Languages that were modified by the {@link at.fh.hagenberg.aist.gce.optimization.executor.JavassistExecutor}
 *
 * @author Oliver Krauss on 31.10.2019
 */
public class JavassistWorker extends ConsoleWorker {

    protected static PrintStream out = System.out;

    public static void setOut(PrintStream out) {
        ConsoleWorker.out = out;
    }

    /**
     * Interceptor that will be used
     */
    protected JavassistInterceptor interceptor;

    /**
     * Node that was parsed, for interceptor
     */
    protected Node parsedNode;

    @Override
    protected void before() {
        interceptor = new JavassistInterceptor();
        JavassistInterceptProvider.setInterceptor(interceptor);
    }

    @Override
    protected Node getParsedNode(ExecutionCommand command, AbstractExecutor executor) {
        parsedNode = command.getParsedNode(executor);
        interceptor.reset();
        return parsedNode;
    }

    @Override
    protected ExecutionResult after(ExecutionCommand command, ExecutionResult result) {
        return new TraceExecutionResult(result, interceptor.getExecutedCount(parsedNode), interceptor.getSpecializedCount(parsedNode), interceptor.getNodeHashes(parsedNode));
    }

    public static void main(String[] args) {
        new JavassistWorker().work(args);
    }

}
