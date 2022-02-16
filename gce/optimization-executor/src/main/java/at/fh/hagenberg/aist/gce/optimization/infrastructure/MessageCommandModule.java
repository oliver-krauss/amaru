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

import at.fh.hagenberg.aist.gce.optimization.language.JavassistWorker;
import at.fh.hagenberg.aist.gce.optimization.language.MessageWorker;
import at.fh.hagenberg.aist.gce.optimization.language.util.CommandProcessor;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import at.fh.hagenberg.util.Pair;
import org.nustaq.serialization.FSTConfiguration;
import org.zeromq.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The command module is a failsafe for the {@link at.fh.hagenberg.aist.gce.optimization.language.MessageWorker}. It raises new workers and directs them to the broker.
 * It runs on the same virtual/physical PC as the workers and also collects error logs of initiated processes to get feedback on unrecoverable errors (GC overhead, Out of Heap, Sigkill ...)
 */
public class MessageCommandModule {

    private String endpoint;
    private int intervalMax;
    private int intervalInit;
    private int heartbeatInterval;
    private int heartbeatLiveness;

    static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    /**
     * The backend all spawned workers will be dealing with
     */
    private String backend;

    /**
     * The UUID of this command module. Will be injected into all spawned workers
     */
    UUID uuid = UUID.randomUUID();

    /**
     * List of processes
     */
    private Map<String, Process> processMap = new HashMap<>();

    /**
     * Cache of logs of processes that have died before the investigation request has come in.
     */
    private Map<String, Pair<String, String>> investigationCache = new HashMap<>();

    /**
     * We spawn at most as many workers as we have processors -1 (to have one processor available for thread switching, and the command module at full load)
     * Exception is single core machines on which we run one worker.
     */
    private static int WORKER_LIMIT = 1;

    public static void purgeProcess(ProcessHandle process) {
        if (process.info().commandLine().map(Object::toString).orElse("").contains("at.fh.hagenberg.aist.gce.optimization.language.MessageWorker")) {
            System.out.println("Destroying remnant process " + process.pid());
            process.destroyForcibly();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // kill remnants of old processes
        ProcessHandle.allProcesses()
                .forEach(process -> purgeProcess(process));

        String broker = System.getenv("MSG_BROKER_COMMAND_LOC") != null ? System.getenv("MSG_BROKER_COMMAND_LOC") : "localhost:5559";
        String backend = System.getenv("MSG_BROKER_BACKEND_LOC") != null ? System.getenv("MSG_BROKER_BACKEND_LOC") : "localhost:5558";
        Integer workerLimit = System.getenv("MSG_COMMANDER_WORKER_LIMIT") != null ? Integer.parseInt(System.getenv("MSG_COMMANDER_WORKER_LIMIT")) : null;

        if (args != null) {
            for (String arg : args) {
                if (arg.startsWith("broker")) {
                    broker = arg.substring(7);
                } else if (arg.startsWith("backend")) {
                    backend = arg.substring(8);
                } else if (arg.startsWith("workerLimit")) {
                    workerLimit = Integer.parseInt(arg.substring(12));
                } else {
                    System.out.println("Did not understand argument " + arg);
                }
            }
        }

        if (workerLimit == null) {
            workerLimit = Runtime.getRuntime().availableProcessors() > 1 ? Runtime.getRuntime().availableProcessors() - 5 : 1;
        }
        WORKER_LIMIT = workerLimit;

        MessageCommandModule module = new MessageCommandModule();
        module.setEndpoint("tcp://" + broker);
        module.setBackend(backend);
        module.setHeartbeatInterval(1000);
        module.setHeartbeatLiveness(3);
        module.setIntervalInit(1000);
        module.setIntervalMax(32000);
        module.work();
    }

    public void work() {
        try (ZContext ctx = new ZContext()) {
            // connect to broker
            ZMQ.Socket worker = createSocket(ctx);

            ZMQ.Poller poller = ctx.createPoller(1);
            poller.register(worker, ZMQ.Poller.POLLIN);

            // spawn workers here
            spawnWorkers();

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
                            Logger.log(Logger.LogLevel.ERROR, "invalid message: " + msg);
                        }

                        msg.destroy();
                    } else if (msg.size() > 1) { // frames: ZMQ ID, Request Type, [algorithm run ID], message
                        ZFrame[] objects = new ZFrame[msg.size()];
                        msg.toArray(objects);

                        String messageType = new String(objects[0].getData(), ZMQ.CHARSET);
                        if (messageType.equals(ParanoidPirateProtocolConstants.PPP_INVESTIGATE)) {
                            String id = objects[1].toString();
                            Logger.log(Logger.LogLevel.INFO, "Investigating crash of worker " + id);
                            if (processMap.containsKey(id)) {
                                Process process = processMap.get(id);

                                String console = streamToString(process.getInputStream());
                                String errors = streamToString(process.getErrorStream());

                                if (process.isAlive()) {
                                    // something is blocking here. Destroy the process
                                    Logger.log(Logger.LogLevel.INFO, "Worker is stuck. Removing it");
                                    process.destroyForcibly();
                                }

                                // send back the entire log and let the frontend decide what to do (easier for debugging)
                                ZMsg makri = new ZMsg();
                                makri.add(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_SUCCESS);
                                makri.add(id);
                                makri.add(errors);
                                makri.add(console);
                                makri.send(worker, true);

                                // since investigation has concluded and the process is dead remove it
                                processMap.remove(id);
                            } else if (investigationCache.containsKey(id)) {
                                ZMsg makri = new ZMsg();
                                makri.add(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_SUCCESS);
                                makri.add(id);
                                makri.add(investigationCache.get(id).getKey());
                                makri.add(investigationCache.get(id).getValue());
                                makri.send(worker, true);

                                // since investigation has concluded and the process is dead remove it
                                investigationCache.remove(id);
                            } else {
                                ZMsg fail = new ZMsg();
                                fail.add(ParanoidPirateProtocolConstants.PPP_INVESTIGATE_ERROR);
                                fail.add(id);
                                fail.add("No worker with ID " + id + " known");
                                fail.send(worker, true);
                            }
                        }

                        // just echo whatever we don't know
                        Logger.log(Logger.LogLevel.INFO, "normal reply");
                        msg.send(worker);
                        liveness = heartbeatLiveness;
                    } else {
                        Logger.log(Logger.LogLevel.ERROR, "invalid message: " + msg);
                    }


                    interval = intervalInit;

                } else if (--liveness == 0) {
                    //  If the broker hasn't sent us heartbeats in a while,
                    //  destroy the socket and reconnect. This is the simplest
                    //  most brutal way of discarding any messages we might have
                    //  sent in the meantime.
                    Logger.log(Logger.LogLevel.WARN, "heartbeat failure, can't reach broker");
                    Logger.log(Logger.LogLevel.WARN, String.format("reconnecting in %d msec", interval));

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
                    Logger.log(Logger.LogLevel.INFO, "Command Module heartbeat");
                    ZFrame frame = new ZFrame(ParanoidPirateProtocolConstants.PPP_HEARTBEAT);
                    frame.send(worker, 0);
                }

                // do a regular health check on all our methods.
                healthCheck();

            }
        } catch (Exception ex) {
            Logger.log(Logger.LogLevel.ERROR, "Command Module crashed", ex);
            ex.printStackTrace();
        }
    }

    private void healthCheck() {
        processMap.entrySet().removeIf(process -> {
            if (!process.getValue().isAlive()) {
                // store failure for future investigations
                Logger.log(Logger.LogLevel.WARN, "Process is dead, cleaning it " + process.getKey());
                String console = streamToString(process.getValue().getInputStream());
                String errors = streamToString(process.getValue().getErrorStream());
                investigationCache.put(process.getKey(), new Pair(errors, console));
                return true;
            }
            return false;
        });

        if (processMap.size() < WORKER_LIMIT) {
            spawnWorkers();
        }
    }

    public static String streamToString(InputStream stream) {
        try (BufferedReader br =
                     new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while (br.ready()) {
                line = br.readLine();
                if (line != null) {
                    sb.append(line);
                    sb.append('\n');
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "Process stream check failed";
        }
    }

    private String languageLocation = new File(EngineConfig.DIST_LOCATION).getAbsolutePath() + "/" + "access-c.jar";

    private String worker = MessageWorker.class.getName();

    private void spawnWorkers() {
        Runtime rt = Runtime.getRuntime();
        // TODO #257 Currently we can only run MiniC here

        try {
            String command = EngineConfig.JAVA_LOCATION + " -Xmx128m" + " " + EngineConfig.JAVA_CALL_PARAMS + " -cp " + languageLocation + " " + worker;
            while (processMap.size() < WORKER_LIMIT) {
                String workerId = UUID.randomUUID().toString();
                String[] env_variables = new String[3];
                env_variables[0] = "MSG_BROKER_BACKEND_LOC=" + backend;
                env_variables[1] = "MSG_COMMAND_PLANE=" + uuid.toString();
                env_variables[2] = "MSG_COMMAND_PLANE_WORKER_ID=" + workerId;
                Process pr = rt.exec(command, env_variables);
                processMap.put(workerId, pr);
                Logger.log(Logger.LogLevel.INFO, "Spawned process " + pr.pid() + " with UID " + workerId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ZMQ.Socket createSocket(ZContext ctx) {
        ZMQ.Socket worker = ctx.createSocket(SocketType.DEALER);
        worker.setIdentity(UuidHelper.getBytesFromUUID(uuid));
        worker.connect(endpoint);
        Logger.log(Logger.LogLevel.INFO, "Command Module registered with ID: " + uuid);

        //  Tell queue we're ready for work
        Logger.log(Logger.LogLevel.INFO, "command module ready");
        ZFrame frame = new ZFrame(ParanoidPirateProtocolConstants.PPP_READY);
        frame.send(worker, 0);
        frame.destroy();
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

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

}
