/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.types.floating;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Representation of the C11 double float type
 * Created by Oliver Krauss on 14.06.2016.
 */
@NodeInfo(shortName = "double", description = "Abstract base class for all nodes returning double")
public abstract class MinicDoubleNode extends MinicExpressionNode {

    /**
     * Returns double
     * @param frame of stack to work with
     * @return      double result of expression
     */
    public abstract double executeDouble(VirtualFrame frame);

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeDouble(frame);
    }
}
