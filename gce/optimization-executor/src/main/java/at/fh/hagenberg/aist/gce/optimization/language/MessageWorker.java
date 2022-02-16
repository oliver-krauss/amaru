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

import at.fh.hagenberg.aist.gce.optimization.executor.ExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.executor.InternalExecutor;
import at.fh.hagenberg.aist.gce.optimization.infrastructure.ParanoidPirateProtocolConstants;
import at.fh.hagenberg.aist.gce.optimization.infrastructure.UuidHelper;
import at.fh.hagenberg.aist.gce.optimization.test.ValueDefinitions;
import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;
import org.nustaq.serialization.FSTConfiguration;
import org.zeromq.*;
import science.aist.seshat.SimpleFileLogger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * The message worker is a worker implementation for running single ASTs.
 * TODO #257 - JAVASSIST should be supported over messaging as well
 */
public class MessageWorker {

    private String endpoint;
    private int intervalMax;
    private int intervalInit;
    private int heartbeatInterval;
    private int heartbeatLiveness;

    static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    /**
     * The thing doing the actual work
     */
    InternalExecutor executor;

    /**
     * The language we are working in currently
     */
    String languageId;

    /**
     * timeout for requests in milliseconds
     */
    private long timeout = 1000;

    /**
     * how often the tests are run in the target workers
     */
    private int repeats = 1;

    /**
     * TODO #257 implement behaviour
     * Specialized setting. If turned on every worker will shut itself down after a single execution.
     * Only use this if you fear bleedover effects like happening in performance measurements because of code caching.
     */
    private boolean safeVM = false;

    /**
     * UUID of this worker
     */
    private UUID workerId;

    /**
     * If this worker has been started by a {@link at.fh.hagenberg.aist.gce.optimization.infrastructure.MessageCommandModule}
     * the modules UUID will be encoded here so the Broker knows where to get the error logs
     */
    private String commandPlane;

    private static SimpleFileLogger logger;

    /**
     * @param args
     */
    public static void main(String[] args) {
        String broker = System.getenv("MSG_BROKER_BACKEND_LOC") != null ? System.getenv("MSG_BROKER_BACKEND_LOC") : "localhost:5558";
        String commandPlane = System.getenv("MSG_COMMAND_PLANE");
        String workerID = System.getenv("MSG_COMMAND_PLANE_WORKER_ID");

        if (args != null) {
            for (String arg : args) {
                if (arg.startsWith("broker")) {
                    broker = arg.substring(7);
                } else if (arg.startsWith("commandPlane")) {
                    commandPlane = arg.substring(13);
                } else if (arg.startsWith("workerID")) {
                    workerID = arg.substring(9);
                } else {
                    System.out.println("Did not understand argument " + arg);
                }
            }
        }

        if (commandPlane != null && workerID == null) {
            throw new IllegalArgumentException("If initialized by command plane, the worker MUST be assigned a workerID");
        }

        MessageWorker worker = new MessageWorker();
        worker.setEndpoint("tcp://" + broker);
        worker.setHeartbeatInterval(1000);
        worker.setHeartbeatLiveness(3);
        worker.setIntervalInit(1000);
        worker.setIntervalMax(32000);
        worker.setCommandPlane(commandPlane);
        if (workerID != null) {
            worker.setWorkerId(UUID.fromString(workerID));
            logger = new SimpleFileLogger(workerID);
        } else {
            logger = new SimpleFileLogger("unnamedWorker");
        }
        // TODO #166 I am just cheating here to pre-init the language before going into the main loop. The loop would init the language, but this just speeds up when the worker goes into ready state.
        boolean success = false;
        try {
            worker.initLanguage("c");
            success = true;
        } catch (Exception e) {
            logger.info("Failed to init the language. I am gone.");
        }
        if (success) {
            worker.work();
        }

        logger.info("Goodbye from " + (workerID != null ? UUID.fromString(workerID) : "unknown"));
        System.exit(0);
    }

    public void work() {
        // assign random id if not assigned by command plane
        if (workerId == null) {
            workerId = UUID.randomUUID();
        }

        try (ZContext ctx = new ZContext()) {
            // connect to broker
            ZMQ.Socket worker = createSocket(ctx);

            ZMQ.Poller poller = ctx.createPoller(1);
            poller.register(worker, ZMQ.Poller.POLLIN);

            //  If liveness hits zero, queue is considered disconnected
            int liveness = heartbeatLiveness;
            int interval = intervalInit;

            //  Send out heartbeats at regular intervals
            long heartbeatAt = System.currentTimeMillis() + heartbeatInterval;

            while (true) {
                int rc = poller.poll(heartbeatInterval);
                if (rc == -1)
                    break; //  Interrupted

                if (poller.pollin(0)) {
                    ZMsg msg = ZMsg.recvMsg(worker);
                    if (msg == null)
                        break; //  Interrupted

                    if (msg.size() == 1) { // frames: signal (HEARTBEAT)
                        //  When we get a heartbeat message from the broker, it means the broker was (recently) alive,
                        //  so reset our liveness indicator:
                        ZFrame frame = msg.getFirst();
                        String frameData = new String(frame.getData(), ZMQ.CHARSET);

                        if (ParanoidPirateProtocolConstants.PPP_HEARTBEAT.equals(frameData)) {
                            liveness = heartbeatLiveness;
                        } else {
                            logger.error("invalid message: " + msg);
                        }

                        msg.destroy();
                    } else if (msg.size() > 1) { // frames: ZMQ ID, Request Type, [algorithm run ID], message
                        ZFrame[] objects = new ZFrame[msg.size()];
                        msg.toArray(objects);

                        String messageType = new String(objects[1].getData(), ZMQ.CHARSET);
                        if (messageType.equals(ParanoidPirateProtocolConstants.PPP_INIT)) {
                            logger.info("Received new context");

                            try {
                                String language = new String(objects[2].getData(), ZMQ.CHARSET);
                                String code = new String(objects[3].getData(), ZMQ.CHARSET);
                                String entryPoint = new String(objects[4].getData(), ZMQ.CHARSET);
                                String function = new String(objects[5].getData(), ZMQ.CHARSET);
                                logger.info("Loaded context from message");

                                // prepare executor & language
                                this.languageId = language;
                                initLanguage(this.languageId);
                                logger.info("Loaded language information");
                                executor = new InternalExecutor(languageId, code, entryPoint, function);
                                logger.info("Applied new context");

                                logger.debug(code);
                            } catch (Exception e) {
                                logger.error(e);
                                throw new RuntimeException("Failed to apply context");
                            }

                            msg.destroy();
                            liveness = heartbeatLiveness;

                            // inform broker that the worker is initialized successfully
                            ZMsg readyMsg = new ZMsg();
                            readyMsg.add(ParanoidPirateProtocolConstants.PPP_INIT_ACCEPTED);
                            if (commandPlane != null) {
                                readyMsg.add(commandPlane);
                            }
                            readyMsg.send(worker, true);
                            continue;
                        }

                        if (messageType.equals(ParanoidPirateProtocolConstants.PPP_CONF)) {
                            logger.info("Received new configuration");

                            String config = new String(objects[2].getData(), ZMQ.CHARSET);

                            String[] values = config.split(";");
                            for (String value : values) {
                                if (value.startsWith("timeout")) {
                                    timeout = Long.parseLong(value.substring(8));
                                } else if (value.startsWith("safeVM")) {
                                    safeVM = Boolean.parseBoolean(value.substring(7));
                                } else if (value.startsWith("repeats")) {
                                    repeats = Integer.parseInt(value.substring(8));
                                }
                            }

                            executor.setTimeout(timeout);
                            executor.setRepeats(repeats);

                            logger.info("Applied new context");
                            msg.destroy();
                            liveness = heartbeatLiveness;
                            continue;
                        }

                        if (messageType.equals(ParanoidPirateProtocolConstants.PPP_RUN)) {
                            logger.info("Received new execution request");

                            // parse node
                            Node node = NodeWrapper.unwrap((NodeWrapper) conf.asObject(objects[2].getData()), executor.getRoot().getFrameDescriptor(), executor.getGlobalScope(), this.languageId);
                            // parse input parametesr
                            Object[] input;
                            try {
                                input = (objects[3] == null || objects[3].getData().length == 0) ? null : Arrays.stream(objects[3].toString().split(";")).map(ValueDefinitions::stringToValue).toArray();
                            } catch (Exception ex) {
                                msg.add(ParanoidPirateProtocolConstants.PPP_RUN_FAILURE);
                                msg.add("Input args could not be parsed");
                                msg.send(worker);
                                msg.destroy();
                                liveness = heartbeatLiveness;
                                continue;
                            }
                            msg.add(ParanoidPirateProtocolConstants.PPP_RUN_SUCCESS);
                            // run code
                            logger.debug("Running Test with timeout " + this.timeout);
                            ExecutionResult test = executor.test(node, input);
                            logger.debug("Finished test " + test.isSuccess());
                            try {
                                msg.add(test.serialize());
                            } catch (Exception e) {
                                if (test.getReturnValue() instanceof Error) {
                                    StringWriter sw = new StringWriter();
                                    PrintWriter pw = new PrintWriter(sw);
                                    ((Error) test.getReturnValue()).printStackTrace(pw);
                                    test.setReturnValue(sw.toString());
                                    msg.add(test.serialize());
                                } else if (test.getReturnValue() instanceof RuntimeException) {
                                    StringWriter sw = new StringWriter();
                                    PrintWriter pw = new PrintWriter(sw);
                                    ((Exception) test.getReturnValue()).printStackTrace(pw);
                                    test.setReturnValue(sw.toString());
                                    msg.add(test.serialize());
                                } else {
                                    msg.add(new ExecutionResult("FAILED TO SERIALIZE MESSAGE", "", new long[0], false).serialize());
                                    logger.error("Failed to serialize message", e);
                                    try {
                                        logger.error("Original message", test.getReturnValue());
                                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        final String utf8 = StandardCharsets.UTF_8.name();
                                        try (PrintStream ps = new PrintStream(baos, true, utf8)) {
                                            ((Exception) test.getReturnValue()).printStackTrace(ps);
                                        }
                                        String data = baos.toString(utf8);
                                        logger.error("Original stack trace", data);
                                    } catch (Exception el) {
                                        logger.error("Failed to log original message", el);
                                    }
                                }
                            }
                            msg.send(worker);

                            if (safeVM) {
                                logger.info("Shutting down as safeVM is active");
                                Object AWAIT = "";
                                synchronized (AWAIT) {
                                    // Safety wait for 1 second to ensure that the message is fully transmitted
                                    AWAIT.wait(1000);
                                }
                                throw new RuntimeException("I don't wanna work anymore");
                            }

                            liveness = heartbeatLiveness;
                            continue;
                        }

                        // just echo whatever we don't know
                        logger.info("normal reply");
                        msg.send(worker);
                        liveness = heartbeatLiveness;
                    } else {
                        logger.error("invalid message: " + msg);
                    }


                    interval = intervalInit;

                } else if (--liveness == 0) {
                    //  If the broker hasn't sent us heartbeats in a while,
                    //  destroy the socket and reconnect. This is the simplest
                    //  most brutal way of discarding any messages we might have
                    //  sent in the meantime.
                    logger.warn("heartbeat failure, can't reach broker");
                    logger.warn(String.format("reconnecting in %d msec", interval));

                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (interval < intervalMax) {
                        interval *= 2;
                    }

                    ctx.destroySocket(worker);
                    poller.unregister(worker);
                    worker = createSocket(ctx);
                    poller.register(worker, ZMQ.Poller.POLLIN);
                    liveness = heartbeatLiveness;
                }

                //  Send heartbeat to queue if it's time
                if (System.currentTimeMillis() > heartbeatAt) {
                    long now = System.currentTimeMillis();
                    heartbeatAt = now + heartbeatInterval;
                    //logger.trace("worker heartbeat");
                    ZFrame frame = new ZFrame(ParanoidPirateProtocolConstants.PPP_HEARTBEAT);
                    frame.send(worker, 0);
                }
            }
        } catch (Exception ex) {
            logger.error("Worker crashed", ex);
            ex.printStackTrace();
        }

    }


    /**
     * Executor that actually inits the language
     */
    protected ExecutorService service = Executors.newSingleThreadExecutor();

    private void initLanguage(String languageId) {
        logger.info("Initializing Language " + languageId);
        try {
            service.submit(() -> TruffleLanguageInformation.getLanguageInformationMinimal(languageId)).get(15000, TimeUnit.MILLISECONDS);
            logger.info("Successfully initialized language");
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            // kill the thread and restart the service
            service.shutdownNow();
            service = Executors.newSingleThreadExecutor();
            logger.info("FAILED to initialize language " + languageId);
            logger.error(ex);
            // I have NO CLUE how this happens but randomly the .getLanguageInformation just STOPS doing anything.
            // The thread freezes at the TCI for
            // at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.complex.MinicStringRelationalNodeFactory$MinicStringEqualsNodeGen
            // and NOTHING can recover it. Retrying creation just fails with a timeout where NOTHING happens at all.
            // just kill the process at this point
            throw new RuntimeException("Failed to load language information. SHAME!");
        }
    }

    private ZMQ.Socket createSocket(ZContext ctx) {
        UUID uuid = workerId;
        ZMQ.Socket worker = ctx.createSocket(SocketType.DEALER);
        worker.setIdentity(UuidHelper.getBytesFromUUID(uuid));
        worker.connect(endpoint);
        logger.info("Worker registered with ID: " + uuid);

        //  Tell queue we're ready for work
        logger.info("worker ready");
        ZMsg readyMsg = new ZMsg();
        readyMsg.add(ParanoidPirateProtocolConstants.PPP_READY);
        if (commandPlane != null) {
            readyMsg.add(commandPlane);
        }
        readyMsg.send(worker, true);
        return worker;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getIntervalMax() {
        return intervalMax;
    }

    public void setIntervalMax(int intervalMax) {
        this.intervalMax = intervalMax;
    }

    public int getIntervalInit() {
        return intervalInit;
    }

    public void setIntervalInit(int intervalInit) {
        this.intervalInit = intervalInit;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public int getHeartbeatLiveness() {
        return heartbeatLiveness;
    }

    public void setHeartbeatLiveness(int heartbeatLiveness) {
        this.heartbeatLiveness = heartbeatLiveness;
    }

    public UUID getWorkerId() {
        return workerId;
    }

    public void setWorkerId(UUID workerId) {
        this.workerId = workerId;
    }

    public String getCommandPlane() {
        return commandPlane;
    }

    public void setCommandPlane(String commandPlane) {
        this.commandPlane = commandPlane;
    }
}
