/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.analytics.graph.nodes;

import at.fh.hagenberg.aist.gce.optimization.util.TruffleFunctionSignature;
import com.oracle.truffle.api.nodes.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Class capturing data flow in an AST consisting of {@link com.oracle.truffle.api.nodes.Node}s
 * TODO #216 Actually turn into a graph
 * @author Oliver Krauss on 29.07.2020
 */
public class DataFlowGraph {

    /**
     * The Tree that this graph is for
     */
    protected Node ast;

    /**
     * Contains information about the available parameters and output of the function
     */
    protected TruffleFunctionSignature signature;

    /**
     * All data item that have been previously WRITTEN to in the creation process (or before)
     * As this field is modified only on already created nodes, it is handed over by reference in copy.
     */
    protected Map<Object, List<DataFlowNode>> availableDataItems;

    /**
     * All data item that have READS that do not have a corresponding available data item
     */
    protected Map<Object, List<DataFlowNode>> requiredDataItems;

    public DataFlowGraph(Node ast, Map<Object, List<DataFlowNode>> availableDataItems, Map<Object, List<DataFlowNode>> requiredDataItems, TruffleFunctionSignature signature) {
        this.ast = ast;
        this.availableDataItems = availableDataItems != null ? availableDataItems : new HashMap<>();
        this.requiredDataItems = requiredDataItems != null ? requiredDataItems : new HashMap<>();
        this.signature = signature;
    }

    public Map<Object, List<DataFlowNode>> getAvailableDataItems() {
        return availableDataItems;
    }

    public Map<Object, List<DataFlowNode>> getRequiredDataItems() {
        return requiredDataItems;
    }

    public TruffleFunctionSignature getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataFlowGraph)) return false;
        DataFlowGraph that = (DataFlowGraph) o;
        return Objects.equals(ast, that.ast) &&
                Objects.equals(signature, that.signature) &&
                Objects.equals(availableDataItems, that.availableDataItems) &&
                Objects.equals(requiredDataItems, that.requiredDataItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ast, signature, availableDataItems, requiredDataItems);
    }
}
