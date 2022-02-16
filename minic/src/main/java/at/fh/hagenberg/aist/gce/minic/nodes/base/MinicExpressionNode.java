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
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Abstract base class for all nodes that return a value (read of a variable, arithmetric operation, ...)
 * Created by Oliver Krauss on 13.05.2016.
 */
@NodeInfo(shortName = "expr", description = "Abstract base class for all nodes that return a value")
public abstract class MinicExpressionNode extends MinicNode {

    @Override
    public void executeVoid(VirtualFrame frame) {
        executeGeneric(frame);
    }

    /**
     * Execution returning any value (determined by subclasses)
     * @param frame of stack the node needs to work with
     * @return result of expression
     */
    public abstract Object executeGeneric(VirtualFrame frame);
}
