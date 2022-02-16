/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.language.util;

import at.fh.hagenberg.aist.gce.optimization.executor.ExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.test.ValueDefinitions;
import science.aist.seshat.LogConfiguration;
import science.aist.seshat.SimpleFileLogger;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Helper class for all Executor-Worker pairs that must communicate with each other over console.
 * It writes and reads the commands.
 *
 * @author Oliver Krauss on 12.12.2019
 */
public class CommandProcessor {

    public static int SOCKET_EXECUTION_RESULT = 61000;

    public static int SOCKET_NODE_WRAPPER = 61001;

    private static SimpleFileLogger logger = new SimpleFileLogger(CommandProcessor.class);

    /**
     * Prepares a command to be sent to another jar
     *
     * @param languageId language to be executed
     * @param code       code to be exeucted
     * @param function   function to be executed
     * @param repeats    times the execution is repeated
     * @param input      input to be used
     * @return
     */
    public static String[] prepareCommand(String languageId, String code, String function, int repeats, Object[] input) {
        String inputStr = "null";
        if (input != null) {
            inputStr = Arrays.stream(input).map(ValueDefinitions::valueToString).collect(Collectors.joining(";"));
        }
        return new String[]{
            "languageId=" + languageId,
            "LOG_LOC=" + LogConfiguration.LOG_LOCATION,
            "code=" + code,
            "function=" + function,
            "repeats=" + String.valueOf(repeats),
            "node=" + "serial",
            "input=" + inputStr
        };
    }

    /**
     * Loads the command either by the given function arguments, or the system environment (args supercede system!)
     *
     * @param args to be received
     */
    public static ExecutionCommand receiveCommands(String[] args) {
        if (args.length != 7 && System.getenv().size() < 7) {
            System.out.println("Usage: languageId, code, entryPoint, function, repeats, node, input");
            return null;
        }

        // parse input

        String languageId;
        String code;
        String function;
        String entryPoint;
        int repeats;
        String node;
        Object[] input;
        if (args.length == 6) {
            languageId = args[0];
            code = args[1];
            entryPoint = args[2];
            function = args[3];
            repeats = Integer.valueOf(args[4]);
            node = args[5];
            input = args[6].equals("null") ? null : Arrays.stream(args[6].split(";")).map(ValueDefinitions::stringToValue).toArray();
        } else {
            languageId = System.getenv("languageId");
            code = System.getenv("code");
            entryPoint = System.getenv("entryPoint");
            function = System.getenv("function");
            repeats = Integer.valueOf(System.getenv("repeats"));
            node = System.getenv("node");
            input = System.getenv("input").equals("null") ? null : Arrays.stream(System.getenv("input").split(";")).map(ValueDefinitions::stringToValue).toArray();
        }

        return new ExecutionCommand(languageId, code, entryPoint, function, repeats, node, input);
    }

    protected static ServerSocket nodeServe;
    protected static Socket nodeClient;

    /**
     * Sends a node on a given stream. this is an additive measure to "prepareCommand"
     *
     * @param node to be sent
     * @throws IOException if something is wrong with the stream
     */
    public static void sendNode(Node node) throws IOException {
        nodeServe = new ServerSocket();
        nodeServe.setReuseAddress(true);
        nodeServe.bind(new InetSocketAddress(SOCKET_NODE_WRAPPER));
        nodeClient = nodeServe.accept();
        ObjectOutputStream oos = new ObjectOutputStream(nodeClient.getOutputStream());
        oos.writeObject(NodeWrapper.wrap(node));
        oos.close();
        nodeClient.close();
        nodeServe.close();
        nodeServe = null;
        nodeClient = null;
    }


    /**
     * Sends the execution result over the given stream
     *
     * @param stream to be sent on
     * @param result to be received
     */
    public static void sendExecutionResult(PrintStream stream, ExecutionResult result, boolean serial) throws IOException {
        if (serial) {
            logger.trace("Sending serial execution result");
            if (result.getReturnValue() instanceof Exception) {
                // must rewrite as truffle exceptions aren't serializable
                Exception ret = (Exception) result.getReturnValue();
                String retStr = ret.getClass().getName() + (ret.getMessage() != null ? " " + ret.getMessage() : "");
                for (int i = 0; i < ret.getStackTrace().length; i++) {
                    retStr += System.lineSeparator() + ret.getStackTrace()[i].toString();
                }

                result.setReturnValue(retStr);
            }

            // send over socket
            logger.debug("Execution Result: " + result.toString());
            Socket client = new Socket("localhost", SOCKET_EXECUTION_RESULT);
            ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
            oos.writeObject(result);
            oos.close();
            client.close();
            logger.trace("Finished sending serial execution result");
        } else {
            logger.trace("Sending console execution result");
            if (result != null && result.getReturnValue() != null && Exception.class.isAssignableFrom(result.getReturnValue().getClass())) {
                stream.println("ERROR");
                ((Exception) result.getReturnValue()).printStackTrace(stream);
            }

            // return results
            stream.println("returnValue:" + ValueDefinitions.valueToString(result.getReturnValue()));
            stream.println("performance:" + Arrays.stream(result.getPerformance()).mapToObj(String::valueOf).collect(Collectors.joining(",")));
            stream.println("out:" + result.getOutStreamValue());
        }
    }

    protected static ServerSocket execServer;
    protected static Socket execClient;

    public static ExecutionResult receiveExecutionResult() throws IOException {
        execServer = new ServerSocket();
        execServer.setReuseAddress(true);
        execServer.bind(new InetSocketAddress(SOCKET_EXECUTION_RESULT));
        execClient = execServer.accept();
        ObjectInputStream ois = new ObjectInputStream(execClient.getInputStream());
        Object o = null;
        try {
            o = ois.readObject();
            ois.close();
            return (ExecutionResult) o;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            execClient.close();
            execServer.close();
            execServer = null;
            execClient = null;
        }
        return null;
    }

    /**
     * if threading issues occur this allows force removing sockets that were allocated.
     */
    public static void forceReleaseSockets() {
        if (execClient != null) {
            try {
                execClient.close();
            } catch (IOException e) {
                System.out.println("WARNING Unable to close execClient");
            }
        }
        if (nodeClient != null) {
            try {
                nodeClient.close();
            } catch (IOException e) {
                System.out.println("WARNING Unable to close nodeClient");
            }
        }
        if (execServer != null) {
            try {
                execServer.close();
            } catch (IOException e) {
                System.out.println("WARNING Unable to close execServer");
            }
        }
        if (nodeServe != null) {
            try {
                nodeServe.close();
            } catch (IOException e) {
                System.out.println("WARNING Unable to close nodeServe");
            }
        }

    }
}
