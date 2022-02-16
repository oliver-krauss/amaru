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

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;
import org.nustaq.serialization.FSTConfiguration;
import org.zeromq.ZFrame;

import java.io.*;
import java.util.Arrays;

/**
 * Result of an execution
 *
 * @author Oliver Krauss on 28.10.2019
 */
public class ExecutionResult implements Serializable {

    static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    private static final long serialVersionUID = 6688355818317356367L;

    /**
     * Value returned by the execution OR the error produced
     */
    Object returnValue;

    /**
     * Data returned on the programs output stream
     */
    String outStreamValue;

    /**
     * Runtimes measured during execution in nanoseconds
     */
    long[] performance;

    /**
     * if the execution failed
     */
    boolean success;

    public ExecutionResult(Object returnValue, String outStreamValue, long[] performance, boolean success) {
        this.returnValue = returnValue;
        this.outStreamValue = outStreamValue;
        this.performance = performance;
        this.success = success;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    /**
     * This Override is ONLY for rewriting values that aren't serializable. DO NOT use otherwise.
     * @param returnValue
     */
    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    public String getOutStreamValue() {
        return outStreamValue;
    }

    public long[] getPerformance() {
        return performance;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "ExecutionResult{" +
            "returnValue=" + returnValue +
            ", outStreamValue='" + outStreamValue + '\'' +
            ", performance=" + Arrays.toString(performance) +
            ", success=" + success +
            '}';
    }

    public byte[] serialize() {
        return conf.asByteArray(this);
    }

    public static ExecutionResult deserialize(byte[] data) {
        return (ExecutionResult) conf.asObject(data);
    }
}
