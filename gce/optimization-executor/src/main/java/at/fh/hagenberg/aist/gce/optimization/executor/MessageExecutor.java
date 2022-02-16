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

import at.fh.hagenberg.aist.gce.optimization.infrastructure.MessageBroker;
import at.fh.hagenberg.aist.gce.optimization.infrastructure.ParanoidPirateProtocolConstants;
import at.fh.hagenberg.aist.gce.optimization.infrastructure.UuidHelper;
import at.fh.hagenberg.aist.gce.optimization.test.ValueDefinitions;
import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import at.fh.hagenberg.aist.gce.optimization.util.NanoProfiler;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;
import org.nustaq.serialization.FSTConfiguration;
import org.zeromq.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MessageExecutor extends AbstractExecutor {

    static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    private String endpoint;

    /**
     * Context for talking to broker
     */
    private ZContext ctx;

    private Queue<ZMQ.Socket> executors = new ArrayBlockingQueue<>(Runtime.getRuntime().availableProcessors());

    private int executorCount = 0;

    private final static int maxExecutorCount = Runtime.getRuntime().availableProcessors();

    /**
     * Specialized setting. If turned on every worker will shut itself down after a single execution.
     * Only use this if you fear bleedover effects like happening in performance measurements because of code caching.
     */
    protected boolean safeVM = false;


    private static MessageExecutor executor;

    /**
     * The Message executor MUST exist as singleton in the system (for now) because we don't support multiple run contexts (yet)
     * @param languageId
     * @param code
     * @return
     */
    public static Executor getSingleton(String languageId, String code, String entryPoint, String function) {
        // TODO #257 IF localhost && no broker / control plane we should auto-spawn them.
        if (executor != null) {
            System.out.println("Initializing Executor singleton with new context");
            return executor.replace(languageId, code, entryPoint, function);
        }
        executor = new MessageExecutor(languageId, code, entryPoint, function);
        return executor;
    }

    private ZMQ.Socket createSocket() {
        ZMQ.Socket worker = ctx.createSocket(SocketType.DEALER);
        worker.connect(endpoint);
        Logger.log(Logger.LogLevel.INFO, "Executor registered");

        return worker;
    }

    /**
     * The Executor will initialize an accessor for the language we want to use
     *
     * @param languageId Language you want to optimize with
     * @param code       that will be parsed
     * @param function   that will be selected in root and origin
     */
    public MessageExecutor(String languageId, String code, String entryPoint, String function) {
        this(languageId, code, entryPoint, function, (System.getenv("MSG_BROKER_FRONTEND_LOC") != null ? System.getenv("MSG_BROKER_FRONTEND_LOC") : "localhost:5557"));
    }

    public MessageExecutor(String languageId, String code, String entryPoint, String function, String endpoint) {
        super(languageId, code, entryPoint, function);
        this.endpoint = "tcp://" + endpoint;

        // Tell the broker to initialize a new experiment context for all workers
        this.ctx = new ZContext();
        sendInit();
    }

    protected void sendInit() {
        ZMQ.Socket executor = getExecutor();
        // Ask Broker to initialize the workers
        ZMsg message = new ZMsg();
        message.add(ParanoidPirateProtocolConstants.PPP_INIT);
        message.add(languageId);
        message.add(code);
        message.add(entryPoint);
        message.add(function);
        message.send(executor);
        message.destroy();
        // ZeroMQ REQUIRES you to get an answer to the message.
        ZMsg zFrames = ZMsg.recvMsg(executor);
        zFrames.destroy();
        executors.add(executor);
    }


    protected void sendConfig(String conf) {
        ZMQ.Socket executor = getExecutor();
        // Ask Broker to set configuration and forward to workers if needed
        ZMsg message = new ZMsg();
        message.add(ParanoidPirateProtocolConstants.PPP_CONF);
        message.add(conf);
        message.send(executor);
        message.destroy();
        // ZeroMQ REQUIRES you to get an answer to the message.
        ZMsg zFrames = ZMsg.recvMsg(executor);
        zFrames.destroy();
        executors.add(executor);
    }

    @Override
    public ExecutionResult test(Node node, Object[] input) {
        // Message Executor doesn't deal with timeouts, it trusts in the Broker.
        // Due to caching we may actually get the message LONG after the actual timeout would finish.
        return conductTest(node, input);
    }

    @Override
    public ExecutionResult conductTest(Node node, Object[] input) {
        // get next available executor or wait for one to become available
        ZMQ.Socket executor = getExecutor();

        // send request
        ZMsg message = new ZMsg();
        message.add(ParanoidPirateProtocolConstants.PPP_RUN);
        message.add(conf.asByteArray(NodeWrapper.wrap(node)));
        String inputStr = null;
        if (input != null) {
            inputStr = Arrays.stream(input).map(ValueDefinitions::valueToString).collect(Collectors.joining(";"));
        }
        message.add(inputStr);
        message.send(executor);
        message.destroy();
        Logger.log(Logger.LogLevel.TRACE, "Sent evaluation request");

        // Wait for answer
        ZMsg msg = ZMsg.recvMsg(executor);

        // put the executor back
        executors.add(executor);

        ZFrame[] frames = new ZFrame[msg.size()];
        msg.toArray(frames);

        if (frames.length < 3) {
            int i = 1;
        }

        String response = new String(frames[3].getData(), ZMQ.CHARSET);
        if (response.equals(ParanoidPirateProtocolConstants.PPP_RUN_SUCCESS)) {
            return ExecutionResult.deserialize(frames[4].getData());
        } else if (response.equals(ParanoidPirateProtocolConstants.PPP_RUN_FAILURE)) {
            String failResponse = new String(frames[4].getData(), ZMQ.CHARSET);
            if (failResponse.equals(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_SUCCESS)) {
                return new ExecutionResult(new String(frames[5].getData(), ZMQ.CHARSET), new String(frames[6].getData(), ZMQ.CHARSET), null, false);
            } else {
                return new ExecutionResult(new String(frames[5].getData(), ZMQ.CHARSET), null, null, false);
            }
        } else if (response.equals(ParanoidPirateProtocolConstants.PPP_RUN_FATAL)) {
            System.out.println("Experiment can't continue. We have a fatal error: " + frames[4].toString());
        }

        return new ExecutionResult("MQ Failure. Received a response that we can't deal with", null, null, false);
    }

    private ZMQ.Socket getExecutor() {
        // open more sockects if we need them
        if (executors.isEmpty() && executorCount < maxExecutorCount) {
            executors.add(createSocket());
            executorCount++;
        }

        ZMQ.Socket executor = executors.poll();
        while (executor == null) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("Wait for socket got interrupted");
            }
            executor = executors.poll();
        }
        return executor;
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        System.out.println("WARNING: we have a chance a socket is lost forever here");
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void setRepeats(int repeats) {
        if (this.repeats == repeats) {
            return;
        }
        super.setRepeats(repeats);
        sendConfig("repeats=" + repeats);
    }

    @Override
    public void setTimeout(long timeout) {
        if (this.timeout == timeout) {
            return;
        }
        super.setTimeout(timeout + MessageBroker.TIMEOUT_ADDITION + MessageBroker.GRAAL_ADDITON);
        sendConfig("timeout=" + timeout);
    }

    public boolean isSafeVM() {
        return safeVM;
    }

    public void setSafeVM(boolean safeVM) {
        if (this.safeVM == safeVM) {
            return;
        }
        this.safeVM = safeVM;
        sendConfig("safeVM=" + safeVM);
    }

    public void setSettings(int repeats, long timeout, boolean safeVM) {
        boolean updated = false;
        if (this.repeats != repeats) {
            updated = true;
        }
        if (this.timeout != timeout) {
            updated = true;
        }
        if (this.safeVM != safeVM) {
            updated = true;;
        }
        if (updated) {
            super.setRepeats(repeats);
            super.setTimeout(timeout);
            this.safeVM = safeVM;
            sendConfig("timeout=" + timeout + ";repeats=" + repeats + ";safeVM=" + safeVM);
        }
    }

    @Override
    protected void init(String languageId, String code, String entryPoint, String function) {
        super.init(languageId, code, entryPoint, function);
        if (ctx != null) {
            sendInit();
        }
    }
}
