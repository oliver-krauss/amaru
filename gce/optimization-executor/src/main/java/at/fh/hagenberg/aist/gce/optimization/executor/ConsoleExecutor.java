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

import at.fh.hagenberg.aist.gce.optimization.language.util.CommandProcessor;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import com.oracle.truffle.api.nodes.Node;

import java.io.File;
import java.io.IOException;

/**
 * Executor that uses the {@link at.fh.hagenberg.aist.gce.optimization.language.ConsoleWorker} of a class
 * to conduct the tests. It still needs the language data as it also parses the nodes to provide an origin.
 *
 * @author Oliver Krauss on 28.10.2019
 */
public class ConsoleExecutor extends AbstractExecutor {

    /**
     * Where the JAR with {@link at.fh.hagenberg.aist.gce.optimization.language.ConsoleWorker} is located.
     */
    private String languageLocation;


    public ConsoleExecutor(String languageId, String code, String entryPoint, String function) {
        super(languageId, code, entryPoint, function);
        languageLocation = new File(EngineConfig.DIST_LOCATION).getAbsolutePath() + "/" + ACCESSOR_PREFIX + this.languageId + ".jar";
    }

    /**
     * Process started on console
     */
    private Process pr;

    @Override
    public ExecutionResult conductTest(Node node, Object[] input) {
        // start up jar file
        Runtime rt = Runtime.getRuntime();
        long[] performance = new long[repeats];
        Throwable e = null;

        try {
            // create process and send node
            pr = rt.exec(EngineConfig.JAVA_LOCATION + " -Xmx128m" + " " + EngineConfig.JAVA_CALL_PARAMS + " -jar " + languageLocation, CommandProcessor.prepareCommand(this.languageId, this.code, this.function, this.repeats, input));
            CommandProcessor.sendNode(node);

            // parse results
            return CommandProcessor.receiveExecutionResult();

        } catch (IOException ex) {
            pr.destroyForcibly();
            CommandProcessor.forceReleaseSockets();
            ex.printStackTrace();
            e = ex;
        }

        return new ExecutionResult(e, null, performance, false);
    }

    @Override
    protected void cleanup() {
        pr.destroyForcibly();
        CommandProcessor.forceReleaseSockets();
        super.cleanup();
    }

    public void setLanguageLocation(String languageLocation) {
        this.languageLocation = languageLocation;
    }
}
