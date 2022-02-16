/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.infrastructure;

import at.fh.hagenberg.aist.gce.optimization.language.MessageWorker;
import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import org.zeromq.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The message broker is the connector between the {@link at.fh.hagenberg.aist.gce.optimization.executor.MessageExecutor} (runs experiments)
 * and the {@link MessageWorker} (runs ASTs). There is also a {@link MessageCommandModule} that solely serves the purpose of checking if
 * a Message Worker has crashed, and taking over sending the exception messages back and starting new workers.
 * <p>
 * The broker solely serves to hold the message queue and distribute messages.
 * <p>
 * # NOTES:
 * Deleted the getConfig from the broker system. currently we assume that ALL workers will be initialized with the same message
 */
public class MessageBroker {

    private int heartbeatInterval;
    private int heartbeatLiveness;
    private String frontend;
    private String backend;
    private String command;

    ZMQ.Socket frontendSocket;
    ZMQ.Socket backendSocket;
    ZMQ.Socket commandSocket;

    /**
     * The message used to initialize all workers that are / will be registered
     */
    private ZMsg initMsg = null;

    /**
     * The message used to configure all workers that are / will be registered
     */
    private ZMsg confMsg = null;

    /**
     * Attempts to resolve exceptions when a crash happened
     */
    private Map<ZFrame, RecoveryData> crashResolver = new HashMap<>();

    /**
     * Cache for when we have more requests than workers at hand
     */
    private Queue<ZMsg> requestCache = new ArrayDeque<>();

    /**
     * Queue of workers currently available
     */
    WorkerQueue queue;

    /**
     * timeout for requests in milliseconds
     */
    private long timeout = 3000;

    /**
     * Addition for round trips
     */
    public final static int TIMEOUT_ADDITION = 500;

    /**
     * Addition for graal being weird sometimes and having huge runtime spikes (especially in the early worker lifecycle)
     */
    public final static int GRAAL_ADDITON = 5000;

    /**
     * all config values between executor and worker that the broker doesn't really need to know about
     */
    private Map<String, String> configs = new HashMap<>();

    /**
     * Specialized setting. If turned on every worker will shut itself down after a single execution.
     * Only use this if you fear bleedover effects like happening in performance measurements because of code caching.
     */
    private boolean safeVM = false;

    /**
     * Timeout on how long we cache without ANY changes in the cache
     */
    private long cacheTimeout;

    public static void main(String[] args) {
        String frontend = System.getenv("MSG_BROKER_FRONTEND") != null ? System.getenv("MSG_BROKER_FRONTEND") : "5557";
        String backend = System.getenv("MSG_BROKER_BACKEND") != null ? System.getenv("MSG_BROKER_BACKEND") : "5558";
        String command = System.getenv("MSG_BROKER_COMMAND") != null ? System.getenv("MSG_BROKER_COMMAND") : "5559";

        if (args != null) {
            for (String arg : args) {
                if (arg.startsWith("frontend")) {
                    frontend = arg.substring(9);
                } else if (arg.startsWith("backend")) {
                    backend = arg.substring(8);
                } else if (arg.startsWith("command")) {
                    command = arg.substring(8);
                } else {
                    System.out.println("Did not understand argument " + arg);
                }
            }
        }

        MessageBroker broker = new MessageBroker();
        broker.setFrontend("tcp://*:" + frontend);
        broker.setBackend("tcp://*:" + backend);
        broker.setCommand("tcp://*:" + command);
        broker.setHeartbeatLiveness(10);
        broker.setHeartbeatInterval(1000);
        try {
            broker.broker();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The main task is an LRU queue with heartbeating on workers so we can detect crashed or blocked worker tasks
     */
    public void broker() {
        try (ZContext ctx = new ZContext()) {
            frontendSocket = ctx.createSocket(SocketType.ROUTER);
            backendSocket = ctx.createSocket(SocketType.ROUTER);
            commandSocket = ctx.createSocket(SocketType.ROUTER);
            frontendSocket.bind(frontend); //  For clients
            backendSocket.bind(backend); //  For workers
            commandSocket.bind(command); // For command modules

            //  Queue of available workers
            queue = new WorkerQueue();

            //  All workers registered to the broker
            Map<byte[], HeartbeatEntity> registeredWorkers = new HashMap<>();
            Map<byte[], HeartbeatEntity> registeredCommanders = new HashMap<>();
            //  Run ID to language
            Map<String, Long> requiredLanguages = new HashMap<>();

            //  Send out heartbeats at regular intervals
            long heartbeatAt = System.currentTimeMillis() + heartbeatInterval;

            ZMQ.Poller poller = ctx.createPoller(3);
            poller.register(backendSocket, ZMQ.Poller.POLLIN);
            poller.register(frontendSocket, ZMQ.Poller.POLLIN);
            poller.register(commandSocket, ZMQ.Poller.POLLIN);
            Logger.log(Logger.LogLevel.INFO, "Broker started");

            while (true) {
                int rc = poller.poll(heartbeatInterval);
                if (rc == -1) {
                    Logger.log(Logger.LogLevel.ERROR, "poller not ready");
                    break; //  Interrupted
                }

                // Handle Worker Activity on Backend
                if (poller.pollin(0)) {
                    //  Use worker address for LRU routing
                    ZMsg msg = ZMsg.recvMsg(backendSocket);
                    if (msg == null) {
                        Logger.log(Logger.LogLevel.ERROR, "message was null");
                        break; //  Interrupted
                    }

                    //  Any sign of life from worker means it's ready
                    ZFrame address = msg.unwrap();
                    byte[] id = address.getData();

                    ZFrame[] objects = new ZFrame[msg.size()];
                    msg.toArray(objects);
                    String data = new String(objects[0].getData(), ZMQ.CHARSET);

                    //  Validate control message, or return reply to client
                    if (data.equals(ParanoidPirateProtocolConstants.PPP_READY)) {
                        // Adds new worker and gives it the current experiment context
                        String commandPlane = null;
                        if (objects.length == 2) {
                            commandPlane = objects[1].toString();
                        }
                        HeartbeatEntity worker = new HeartbeatEntity(address, heartbeatInterval, heartbeatLiveness, commandPlane);
                        registeredWorkers.put(id, worker);
                        pushInitMessage(worker);
                        pushConfMessage(worker);
                        if (initMsg != null) {
                            // do not allow into ready queue while not initialized
                            id = null;
                        }
                        Logger.log(Logger.LogLevel.INFO, "registered a new worker: " + worker + (commandPlane != null ? " at command " + commandPlane : ""));
                    } else if (data.equals(ParanoidPirateProtocolConstants.PPP_INIT_ACCEPTED)) {
                        Logger.log(Logger.LogLevel.INFO, "worker is initialized " + address);


                        // NOTHING. we will push in queue below.
                    } else if (data.equals(ParanoidPirateProtocolConstants.PPP_HEARTBEAT)) {
                        // Handles heartbeat
                        if (registeredWorkers.containsKey(id)) {
                            registeredWorkers.get(id).resetExpiry();
                        } else {
                            Logger.log(Logger.LogLevel.ERROR, "received heartbeat from unregistered worker");
                            id = null;
                        }
                    } else {
                        Logger.log(Logger.LogLevel.TRACE, "forward message to experiment controller");
                        RecoveryData remove = crashResolver.remove(address);
                        if (remove != null) {
                            remove.getMessage().destroy();
                            msg.send(frontendSocket);
                            if (safeVM) {
                                // on safeVM we know that the worker has shut down prevent worker to be added to queue again
                                Logger.log(Logger.LogLevel.INFO, "Preemptively purging safeVM worker");
                                registeredWorkers.remove(id);
                                id = null;
                            } else {
                                // reset the expiration of the worker
                                registeredWorkers.get(id).resetExpiry();
                            }
                        } else {
                            Logger.log(Logger.LogLevel.INFO, "Message has already been abandoned and investigated");
                        }
                    }

                    if (id != null) {
                        // register worker as available to do work
                        queue.push(registeredWorkers.get(id));
                        if (!requestCache.isEmpty()) {
                            // immediately order cache exec
                            Logger.log(Logger.LogLevel.TRACE, "forward cached message to worker");
                            forwardRunRequest(requestCache.poll());
                            cacheTimeout = getTimeout();
                        }
                    }
                    msg.destroy();
                }

                // Handle requests from Frontend (Amaru)
                if (poller.pollin(1)) {
                    // consume the msg even if we don't have a worker to handle it to avoid an endless loop
                    ZMsg msg = ZMsg.recvMsg(frontendSocket);
                    if (msg == null) {
                        Logger.log(Logger.LogLevel.ERROR, "message was null");
                        break; //  Interrupted
                    }

                    ZFrame[] objects = new ZFrame[msg.size()];
                    msg.toArray(objects);

                    String request = new String(objects[1].getData(), ZMQ.CHARSET);
                    if (request.equals(ParanoidPirateProtocolConstants.PPP_INIT)) {
                        // echo request to executor
                        msg.duplicate().send(frontendSocket);

                        // tell all workers to initialize with that message and clear from ready queue until they are
                        initMsg = msg;

                        // full reset of all experiment states
                        queue.clear();
                        requestCache.clear();
                        crashResolver.clear();

                        for (HeartbeatEntity worker : registeredWorkers.values()) {
                            pushInitMessage(worker);
                        }
                        Logger.log(Logger.LogLevel.INFO, "All workers initialized with new context");
                    }

                    if (request.equals(ParanoidPirateProtocolConstants.PPP_CONF)) {
                        // echo request to executor
                        String config = objects[2].toString();
                        msg.send(frontendSocket, true);

                        // set config
                        String[] values = config.split(";");
                        for (String value : values) {
                            if (value.startsWith("timeout")) {
                                timeout = Long.parseLong(value.substring(8));
                                Logger.log(Logger.LogLevel.INFO, "TIMEOUT IS NOW: " + timeout);
                                if (timeout > heartbeatInterval * heartbeatLiveness) {
                                    heartbeatLiveness = (int) (Math.ceil(timeout / heartbeatInterval) + 3);
                                }
                            } else if (value.startsWith("safeVM")) {
                                safeVM = Boolean.parseBoolean(value.substring(7));
                            } else {
                                String key = value.substring(0, value.indexOf("="));
                                configs.put(key, value);
                            }
                        }

                        // Create the config message
                        confMsg = new ZMsg();
                        // easier to just combine all config values into one
                        confMsg.push(""); // add empty string as source of config was removed
                        confMsg.add(ParanoidPirateProtocolConstants.PPP_CONF);
                        confMsg.add("timeout=" + timeout + ";safeVM=" + safeVM + ";" + configs.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.joining(";")));

                        // Update workers
                        for (HeartbeatEntity worker : registeredWorkers.values()) {
                            pushConfMessage(worker);
                        }
                        Logger.log(Logger.LogLevel.INFO, "All workers initialized with new context");
                    }

                    if (request.equals(ParanoidPirateProtocolConstants.PPP_RUN)) {
                        if (queue.size() <= 0) {
                            // cache if we have workers still registered
                            if (!registeredWorkers.isEmpty()) {
                                Logger.log(Logger.LogLevel.TRACE, "all workers busy. Caching");
                                requestCache.add(msg);
                                cacheTimeout = getTimeout();
                                continue;
                            }

                            // Failure because no worker available. Echo failure
                            Logger.log(Logger.LogLevel.ERROR, "no workers available to handle request");
                            msg.add(ParanoidPirateProtocolConstants.PPP_RUN_FATAL);
                            msg.add("No workers available to handle request");
                            msg.send(frontendSocket);
                            msg.destroy();
                            continue;
                        }
                        // Forward request
                        forwardRunRequest(msg);
                    }

                }

                // Handle requests from Command Module
                if (poller.pollin(2)) {
                    //  Use worker address for LRU routing
                    ZMsg msg = ZMsg.recvMsg(commandSocket);
                    if (msg == null) {
                        Logger.log(Logger.LogLevel.ERROR, "message was null");
                        break; //  Interrupted
                    }

                    //  Any sign of life from worker means it's ready
                    ZFrame address = msg.unwrap();
                    byte[] id = address.getData();

                    if (msg.size() == 1) { // signal
                        ZFrame frame = msg.getFirst();
                        String data = new String(frame.getData(), ZMQ.CHARSET);

                        if (data.equals(ParanoidPirateProtocolConstants.PPP_READY)) {
                            // register command module
                            HeartbeatEntity commandModule = new HeartbeatEntity(address, heartbeatInterval, heartbeatLiveness, null);
                            registeredCommanders.put(id, commandModule);
                            Logger.log(Logger.LogLevel.INFO, "registered a new command module: " + commandModule);
                        } else if (data.equals(ParanoidPirateProtocolConstants.PPP_HEARTBEAT)) {
                            // Handles heartbeat
                            if (registeredCommanders.containsKey(id)) {
                                registeredCommanders.get(id).resetExpiry();
                            } else {
                                Logger.log(Logger.LogLevel.ERROR, "received heartbeat from unregistered command module");
                                id = null;
                            }
                        } else {
                            Logger.log(Logger.LogLevel.ERROR, "invalid message from command module: " + msg);
                            id = null;
                        }

                        msg.destroy();
                    } else {
                        ZFrame[] objects = new ZFrame[msg.size()];
                        msg.toArray(objects);

                        String request = new String(objects[0].getData(), ZMQ.CHARSET);
                        if (request.equals(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_SUCCESS) || request.equals(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_ERROR)) {
                            String workerId = objects[1].toString();
                            Map.Entry<ZFrame, RecoveryData> zFrameRecoveryDataEntry = crashResolver.entrySet().stream().filter(x -> x.getValue().getWorkerId() != null && x.getValue().getWorkerId().equals(workerId)).findFirst().orElse(null);
                            // if still in crash resolver answer. Otherwise ignore the investigation report
                            if (zFrameRecoveryDataEntry != null) {
                                crashResolver.remove(zFrameRecoveryDataEntry.getKey());
                                ZMsg message = zFrameRecoveryDataEntry.getValue().getMessage();
                                Logger.log(Logger.LogLevel.TRACE, "Forwarding investigation results to frontend");
                                if (request.equals(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_ERROR)) {
                                    message.add(ParanoidPirateProtocolConstants.PPP_RUN_FAILURE);
                                    message.add(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_ERROR);
                                    message.add("The worker crashed, and the command module can't figure out why.");
                                    message.send(frontendSocket, true);
                                } else if (request.equals(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_SUCCESS)) {
                                    // pipe the investigation report to the frontend
                                    message.add(ParanoidPirateProtocolConstants.PPP_RUN_FAILURE);
                                    message.add(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_SUCCESS);
                                    message.add(objects[2]);
                                    message.add(objects[3]);
                                    message.send(frontendSocket, true);
                                }
                            } else {
                                Logger.log(Logger.LogLevel.WARN, "Warning - crash investigation took so long it is ignored");
                            }
                        }
                    }
                }

                // deal with worker crashes
                if (!crashResolver.isEmpty()) {
                    crashResolver.entrySet().removeIf(x -> {
                        // when time is out tell the frontend that the execution failed
                        if (System.currentTimeMillis() > x.getValue().getExpectedResponse()) {
                            Logger.log(Logger.LogLevel.ERROR, "crash detected " + x.getValue().getWorkerId());
                            if (!x.getValue().isInvestigated() && registeredWorkers.containsKey(x.getKey().getData())) {
                                // if the worker has a command plane investigate the crash
                                HeartbeatEntity worker = registeredWorkers.get(x.getKey().getData());
                                HeartbeatEntity commander;
                                if (worker.getCommandPlane() != null && (commander = registeredCommanders.values().stream().filter(y -> worker.getCommandPlane().equals(y.getIdentity())).findFirst().orElse(null)) != null) {
                                    ZMsg thraxas = new ZMsg();
                                    thraxas.add(commander.getAddress());
                                    thraxas.add(ParanoidPirateProtocolConstants.PPP_INVESTIGATE);
                                    thraxas.add(worker.getIdentity());
                                    thraxas.send(commandSocket);
                                    thraxas.destroy();
                                    Logger.log(Logger.LogLevel.INFO, "Asked command plane at " + commander + " to investigate crash");
                                    // save that we are investigating. In case of no answer we will report failure in next cycle to frontend
                                    x.getValue().setInvestigated(true);
                                    x.getValue().setWorkerId(worker.getIdentity());
                                    x.getValue().expectedResponse = getTimeout();
                                    return false;
                                }
                            }

                            // no command plane - tell frontend this is a failure
                            x.getValue().message.add(ParanoidPirateProtocolConstants.PPP_RUN_FAILURE);
                            x.getValue().message.add(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_ERROR);
                            x.getValue().message.add("The worker crashed. We don't know why without the command module");
                            x.getValue().message.send(frontendSocket, true);
                            return true;
                        }
                        return false;
                    });
                }

                // deal with no workers existing
                if (queue.size() <= 0 && !requestCache.isEmpty() && System.currentTimeMillis() > cacheTimeout) {
                    Logger.log(Logger.LogLevel.ERROR, "no more workers exist");
                    requestCache.forEach(x -> {
                        //  tell frontend we don't have any workers anymore
                        x.add(ParanoidPirateProtocolConstants.PPP_RUN_FAILURE);
                        x.add(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_ERROR);
                        x.add("The worker crashed. We don't know why without the command module");
                        x.send(frontendSocket, true);
                    });
                    requestCache.clear();
                }

                //  We handle heartbeating after any socket activity. First we
                //  send heartbeats to any idle workers or commanders if it's time. Then we
                //  purge any dead workers
                if (System.currentTimeMillis() >= heartbeatAt) {
                    for (HeartbeatEntity worker : queue) {
                        worker.getAddress().send(backendSocket, ZFrame.REUSE + ZFrame.MORE);
                        ZFrame frame = new ZFrame(ParanoidPirateProtocolConstants.PPP_HEARTBEAT);
                        frame.send(backendSocket, 0);
                    }
                    registeredCommanders.entrySet().removeIf(entry -> {
                        if (System.currentTimeMillis() > entry.getValue().getExpiry()) {
                            // purge dead commanders, don't even deal with heartbeats to them
                            Logger.log(Logger.LogLevel.INFO, String.format("purged commander: " + entry.getValue().toString()));
                            return true;
                        }
                        // send heartbeat to live commanders
                        entry.getValue().getAddress().send(commandSocket, ZFrame.REUSE + ZFrame.MORE);
                        ZFrame frame = new ZFrame(ParanoidPirateProtocolConstants.PPP_HEARTBEAT);
                        frame.send(commandSocket, 0);
                        return false;
                    });
                    heartbeatAt += heartbeatInterval;
                }

                //Logger.log(Logger.LogLevel.INFO, "purge inactive workers");
                List<HeartbeatEntity> purgedWorkers = queue.purge();
                if (!purgedWorkers.isEmpty()) {
                    Logger.log(Logger.LogLevel.INFO, String.format("purged worker(s): " +
                            purgedWorkers.stream().map(HeartbeatEntity::toString).collect(Collectors.joining(", "))));
                }
            }

            //  When we're done, clean up properly
            queue.clear();
        } catch (
                Exception ex) {
            Logger.log(Logger.LogLevel.ERROR, "Broker crashed: ", ex);
            ex.printStackTrace();
        }

    }

    private long getTimeout() {
        return System.currentTimeMillis() + timeout + TIMEOUT_ADDITION + GRAAL_ADDITON;
    }

    private void forwardRunRequest(ZMsg msg) {
        // do not echo. We want the client to wait for the answer.
        ZFrame address = queue.pop();
        // add a crash timeout of the regular timeout + 100ms for the round trip
        while (crashResolver.containsKey(address)) {
            //throw new RuntimeException("The worker is already working???? " + trace);
            Logger.log(Logger.LogLevel.INFO, "THe worker is already working " + address);
            try {
                address = queue.pop();
            } catch (Exception e) {
                Logger.log(Logger.LogLevel.INFO, "No More Workers. Please hard reset the CommandModule." + address);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }
        crashResolver.put(address.duplicate(), new RecoveryData(getTimeout(), msg.duplicate(), UuidHelper.getUUIDFromBytes(address.getData()).toString()));
        msg.push(address);
        msg.send(backendSocket);
        Logger.log(Logger.LogLevel.TRACE, "forwarded request to worker");
    }

    private void pushInitMessage(HeartbeatEntity worker) {
        if (initMsg != null) {
            Logger.log(Logger.LogLevel.INFO, "Sending init request to " + worker.getAddress());
            ZMsg broadcastInit = initMsg.duplicate();
            broadcastInit.push(worker.getAddress());
            broadcastInit.send(backendSocket);
            broadcastInit.destroy();
        }
    }

    private void pushConfMessage(HeartbeatEntity worker) {
        if (confMsg != null) {
            ZMsg broadcastConf = confMsg.duplicate();
            broadcastConf.push(worker.getAddress());
            broadcastConf.send(backendSocket);
            broadcastConf.destroy();
        }
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public String getFrontend() {
        return frontend;
    }

    public void setFrontend(String frontend) {
        this.frontend = frontend;
    }

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getHeartbeatLiveness() {
        return heartbeatLiveness;
    }

    public void setHeartbeatLiveness(int heartbeatLiveness) {
        this.heartbeatLiveness = heartbeatLiveness;
    }


}
