/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.types.integer.signed;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Representation of the C11 INT32 integer type (INT16 won't be needed since we have short for that)
 * Created by Oliver Krauss on 14.06.2016.
 */
@NodeInfo(shortName = "int", description = "Abstract base class for all nodes returning int")
public abstract class MinicIntNode extends MinicExpressionNode {


    /**
     * Returns int
     * @param frame of stack to work with
     * @return      int result of expression
     */
    public abstract int executeInt(VirtualFrame frame);

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeInt(frame);
    }
}
