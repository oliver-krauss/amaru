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
import at.fh.hagenberg.aist.gce.optimization.language.util.JavassistInterceptProvider;
import at.fh.hagenberg.aist.gce.optimization.language.util.JavassistInterceptor;
import at.fh.hagenberg.aist.gce.optimization.language.util.XESInterceptor;
import science.aist.seshat.XESLogger;
import com.oracle.truffle.api.nodes.Node;

import java.io.PrintStream;

/**
 * Logs the NodeExecutions to a XES File.
 *
 * @author Oliver Krauss on 31.10.2019
 */
public class XESWorker extends JavassistWorker {

    protected static PrintStream out = System.out;

    public static void setOut(PrintStream out) {
        ConsoleWorker.out = out;
    }

    private XESLogger logger = new XESLogger("test");

    @Override
    protected void before() {
        interceptor = new XESInterceptor();
        JavassistInterceptProvider.setInterceptor(interceptor);
    }

    @Override
    protected Node getParsedNode(ExecutionCommand command, AbstractExecutor executor) {
        Node parsedNode = super.getParsedNode(command, executor);
        ((XESInterceptor) interceptor).setLogger(logger);
        return parsedNode;
    }

    @Override
    protected ExecutionResult after(ExecutionCommand command, ExecutionResult result) {
        logger.close();
        return super.after(command, result);
    }

    public static void main(String[] args) {
        new XESWorker().work(args);
    }

}
