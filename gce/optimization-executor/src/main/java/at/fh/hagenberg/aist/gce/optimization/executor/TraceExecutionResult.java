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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

/**
 * Result of an execution
 *
 * @author Oliver Krauss on 28.10.2019
 */
public class TraceExecutionResult extends ExecutionResult {

    private static final long serialVersionUID = 7258776472858473669L;

    /**
     * Amount of nodes that were executed in this execution
     */
    private int numberOfExecutedNodes;

    /**
     * Amount of nodes that were specialized in this execution
     */
    private int numberofSpecializedNodes;

    /**
     * Nodes that were executed, with the count of how often they were executed
     */
    private Map<String, Integer> nodeExecutions;

    public TraceExecutionResult(ExecutionResult result, int numberOfExecutedNodes, int numberofSpecializedNodes, Map<String, Integer> nodeExecutions)  {
        super(result.returnValue, result.outStreamValue, result.performance, result.success);
        this.numberOfExecutedNodes = numberOfExecutedNodes;
        this.numberofSpecializedNodes = numberofSpecializedNodes;
        this.nodeExecutions = nodeExecutions;
    }

    public TraceExecutionResult(Object returnValue, String outStreamValue, long[] performance, boolean success, int numberOfExecutedNodes, int numberofSpecializedNodes, Map<String, Integer> nodeExecutions) {
        super(returnValue, outStreamValue, performance, success);
        this.numberOfExecutedNodes = numberOfExecutedNodes;
        this.numberofSpecializedNodes = numberofSpecializedNodes;
        this.nodeExecutions = nodeExecutions;
    }

    public int getNumberOfExecutedNodes() {
        return numberOfExecutedNodes;
    }

    public int getNumberofSpecializedNodes() {
        return numberofSpecializedNodes;
    }

    public Map<String, Integer> getNodeExecutions() {
        return nodeExecutions;
    }

    @Override
    public String toString() {
        return "TraceExecutionResult{" +
            "numberOfExecutedNodes=" + numberOfExecutedNodes +
            ", numberofSpecializedNodes=" + numberofSpecializedNodes +
            ", nodeExecutions=" + nodeExecutions +
            ", returnValue=" + returnValue +
            ", outStreamValue='" + outStreamValue + '\'' +
            ", performance=" + Arrays.toString(performance) +
            ", success=" + success +
            '}';
    }
}
