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
import at.fh.hagenberg.aist.gce.optimization.language.util.JavassistInterceptor;
import com.oracle.truffle.api.nodes.Node;
import science.aist.seshat.SimpleFileLogger;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Helper class that conducts a run on the console.
 * This class must be your mainClass in a packaged jar
 *
 * @author Oliver Krauss on 31.10.2019
 */
public class ConsoleWorker {

    protected static PrintStream out = System.out;

    public static void setOut(PrintStream out) {
        ConsoleWorker.out = out;
    }

    /**
     * Logger for console worker
     */
    private SimpleFileLogger logger = new SimpleFileLogger(this.getClass());

    /**
     * Allows initializing values before the execution begins
     */
    protected void before() {
        // do nothing in regular worker
    }

    /**
     * Allows modification of the result before it is sent back over the socket
     * @param command original command
     * @param result  result of execution
     * @return possibly modified result of execution
     */
    protected ExecutionResult after(ExecutionCommand command, ExecutionResult result) {
        return result;
    }

    /**
     * Allows modification of the parsed node if necessary
     * @param command original command
     * @param executor executor of worker
     * @return possibly modified node
     */
    protected Node getParsedNode(ExecutionCommand command, AbstractExecutor executor) {
        return command.getParsedNode(executor);
    }

    public void work(String[] args) {
        before();
        logger.trace("Starting Worker");
        ExecutionCommand command = CommandProcessor.receiveCommands(args);
        if (command == null) {
            return;
        }
        logger.debug("Received commands" + command.toString());


        try {
            // prepare executor
            InternalExecutor executor = new InternalExecutor(command.getLanguageId(), command.getCode(), command.getEntryPoint(), command.getFunction());
            executor.setRepeats(command.getRepeats());

            // run test
            ExecutionResult test = executor.test(getParsedNode(command, executor), command.getInput());
            CommandProcessor.sendExecutionResult(out, after(command, test), command.getNode().equals("serial"));
        } catch (Exception | Error e) {
            try {
                logger.warn("Worker execution failed", e);
                CommandProcessor.sendExecutionResult(out, new ExecutionResult(e, null, new long[0], false), command.getNode().equals("serial"));
            } catch (IOException e1) {
                logger.error("Worker failed to send execution result", e1);
            }
        }
        logger.trace("Finished Worker");
    }

    public static void main(String[] args) {
        new ConsoleWorker().work(args);
    }

}
