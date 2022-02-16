/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.vars;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Node for copying a given array to the to the stack
 * Created by Oliver Krauss on 27.07.2016.
 */
@NodeInfo(shortName = "copy-array-local", description = "Abstract base class for all nodes copying arrays on the stack")
@NodeField(name = "slot", type = FrameSlot.class)
public abstract class CopyArrayNode extends MinicNode {

    /**
     * Frame slot the copy will be added to.
     * @return
     */
    protected abstract FrameSlot getSlot();

    @NodeInfo(shortName = "copy-array-local-generic", description = "Generic array copy to stack")
    @NodeChild(value = "valueNode", type = MinicExpressionNode.class)
    public abstract static class MinicCopyGenericArrayNode extends CopyArrayNode {

        @Specialization
        protected void copyArray(VirtualFrame frame, Object value) {
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            frame.setObject(getSlot(), value);
        }
    }
}
