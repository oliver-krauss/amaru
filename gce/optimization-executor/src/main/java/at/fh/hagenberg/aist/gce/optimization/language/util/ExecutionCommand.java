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

import at.fh.hagenberg.aist.gce.optimization.executor.AbstractExecutor;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * Helper class for all Executor-Worker pairs that must communicate with each other over console.
 * It contains the information to be transferred between the nodes
 *
 * @author Oliver Krauss on 12.12.2019
 */
public class ExecutionCommand {

    /**
     * ID of language to be executed (the truffle ID!)
     */
    private String languageId;

    /**
     * The code to be executed
     */
    private String code;


    /**
     * The function to be executed
     */
    String entryPoint;

    /**
     * The function to be optimized
     */
    String function;

    /**
     * How often the execution shall be repeated
     */
    int repeats;

    /**
     * The node that will be used to replace the function body
     * If null the original function will be executed.
     * Must be a serialized node {@link at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper#serialize(NodeWrapper)}
     */
    String node;

    /**
     * Test input for the execution
     */
    Object[] input;

    Node parsedNode;

    public ExecutionCommand() {
    }

    public ExecutionCommand(String languageId, String code, String entryPoint, String function, int repeats, String node, Object[] input) {
        this.languageId = languageId;
        this.code = code;
        this.entryPoint = entryPoint;
        this.function = function;
        this.repeats = repeats;
        this.node = node;
        this.input = input;
    }

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public int getRepeats() {
        return repeats;
    }

    public void setRepeats(int repeats) {
        this.repeats = repeats;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public Object[] getInput() {
        return input;
    }

    public void setInput(Object[] input) {
        this.input = input;
    }

    public Node getParsedNode(AbstractExecutor executor) {
        if (parsedNode == null) {
            if (this.node.equals("null")) {
                parsedNode = executor.getOrigin();
            } else {
                // initialize language
                TruffleLanguageInformation.getLanguageInformationMinimal(this.languageId);
                // deserialize
                NodeWrapper wrapper = null;
                if (this.node.equals("serial")) {
                    try {
                        Socket client = new Socket("localhost", CommandProcessor.SOCKET_NODE_WRAPPER);
                        try {
                            ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                            wrapper = (NodeWrapper) ois.readObject();
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        } finally {
                            client.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    wrapper = NodeWrapper.deserialize(this.node);
                }
                parsedNode = NodeWrapper.unwrap(wrapper, executor.getRoot().getFrameDescriptor(), executor.getGlobalScope(), this.languageId);
            }
        }

        return parsedNode;
    }

    @Override
    public String toString() {
        return "ExecutionCommand{" +
            "languageId='" + languageId + '\'' +
            ", code='" + code + '\'' +
            ", entryPoint='" + entryPoint + '\'' +
            ", function='" + function + '\'' +
            ", repeats=" + repeats +
            ", node='" + node + '\'' +
            ", input=" + Arrays.toString(input) +
            ", parsedNode=" + parsedNode +
            '}';
    }

    public String getEntryPoint() {
        return entryPoint;
    }
}
