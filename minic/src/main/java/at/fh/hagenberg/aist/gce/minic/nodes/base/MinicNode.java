/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.base;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Super class of all MiniC Nodes. Nodes that produce results should always inherit from {@link MinicExpressionNode}
 * Created by Oliver Krauss on 13.05.2016.
 */
@NodeInfo(shortName = "C", description = "Abstract base class for all nodes", language = "Mini ANSI C11")
public abstract class MinicNode extends Node {

    /**
     * Base execution method every node has
     * @param frame frame in stack that the node needs to work with
     */
    public abstract void executeVoid(VirtualFrame frame);

}
