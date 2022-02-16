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

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.sun.source.tree.Tree;

/**
 * Write for variables on stack
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "write-local", description = "Abstract base class for all nodes writing simple datatypes to the stack")
@NodeField(name = "slot", type = FrameSlot.class)
public abstract class MinicWriteNode extends MinicNode {

    /**
     * Loads the frame slot that is being written to.
     * @return slot for writing on stack
     */
    protected abstract FrameSlot getSlot();

    @NodeInfo(shortName = "write-local-char", description = "Writes char to stack")
    @NodeChild(value = "valueNode", type = MinicCharNode.class)
    public abstract static class MinicCharWriteNode extends MinicWriteNode {
        @Specialization
        protected void writeChar(VirtualFrame frame, char value) {
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Byte);
            frame.setObject(getSlot(), value);
        }
    }

    @NodeInfo(shortName = "write-local-int", description = "Writes int to stack")
    @NodeChild(value = "valueNode", type = MinicIntNode.class)
    public abstract static class MinicIntWriteNode extends MinicWriteNode {
        @Specialization
        protected void writeInt(VirtualFrame frame, int value) {
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Int);
            frame.setInt(getSlot(), value);
        }
    }

    @NodeInfo(shortName = "write-local-float", description = "Writes float to stack")
    @NodeChild(value = "valueNode", type = MinicFloatNode.class)
    public abstract static class MinicFloatWriteNode extends MinicWriteNode {
        @Specialization
        protected void writeFloat(VirtualFrame frame, float value) {
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Float);
            frame.setFloat(getSlot(), value);
        }
    }

    @NodeInfo(shortName = "write-local-double", description = "Writes double to stack")
    @NodeChild(value = "valueNode", type = MinicDoubleNode.class)
    public abstract static class MinicDoubleWriteNode extends MinicWriteNode {
        @Specialization
        protected void writeDouble(VirtualFrame frame, double value) {
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Double);
            frame.setDouble(getSlot(), value);
        }
    }

    @NodeInfo(shortName = "write-local-string", description = "Writes string to stack")
    @NodeChild(value = "valueNode", type = MinicStringNode.class)
    public abstract static class MinicStringWriteNode extends MinicWriteNode {
        @Specialization
        protected void writeString(VirtualFrame frame, String value) {
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            frame.setObject(getSlot(), value);
        }
    }
}
