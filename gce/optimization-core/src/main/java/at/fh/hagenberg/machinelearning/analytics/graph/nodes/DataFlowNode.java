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

import com.oracle.truffle.api.nodes.Node;

/**
 * Class capturing data flow in a {@link com.oracle.truffle.api.nodes.Node}
 * TODO #216 Actually turn into a graph
 * @author Oliver Krauss on 21.10.2020
 */
public class DataFlowNode {

    /**
     * The frame slot that the node accesses
     */
    private Object slot;

    /**
     * The node in the AST that accesses this slot
     */
    private Node node;

    public DataFlowNode(Object slot, Node node) {
        this.slot = slot;
        this.node = node;
    }

    public Object getSlot() {
        return slot;
    }

    public Node getNode() {
        return node;
    }
}
