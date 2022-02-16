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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Write for variables on heap
 * Created by Oliver Krauss on 19.07.2016.
 */
@NodeInfo(shortName = "write-global", description = "Abstract base class for all nodes writing simple datatypes to the heap")
@NodeFields({
    @NodeField(name = "slot", type = FrameSlot.class),
    @NodeField(name = "globalFrame", type = MaterializedFrame.class)
})
public abstract class MinicWriteGlobalNode extends MinicNode {

    /**
     * Loads the frame slot that is being written to.
     * @return slot for writing
     */
    protected abstract FrameSlot getSlot();

    /**
     * Loads the heap
     * @return heap
     */
    protected abstract MaterializedFrame getGlobalFrame();

    @NodeInfo(shortName = "write-global-char", description = "Writes char to heap")
    @NodeChild(value = "valueNode", type = MinicCharNode.class)
    public abstract static class MinicCharWriteGlobalNode extends MinicWriteGlobalNode {
        @Specialization
        protected void writeChar(char value) {
            getGlobalFrame().getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Byte);
            getGlobalFrame().setObject(getSlot(), value);
        }
    }

    @NodeInfo(shortName = "write-global-int", description = "Writes int to heap")
    @NodeChild(value = "valueNode", type = MinicIntNode.class)
    public abstract static class MinicIntWriteGlobalNode extends MinicWriteGlobalNode {
        @Specialization
        protected void writeInt(int value) {
            getGlobalFrame().getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Int);
            getGlobalFrame().setInt(getSlot(), value);
        }
    }

    @NodeInfo(shortName = "write-global-float", description = "Writes float to heap")
    @NodeChild(value = "valueNode", type = MinicFloatNode.class)
    public abstract static class MinicFloatWriteGlobalNode extends MinicWriteGlobalNode {
        @Specialization
        protected void writeFloat(float value) {
            getGlobalFrame().getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Float);
            getGlobalFrame().setFloat(getSlot(), value);
        }
    }

    @NodeInfo(shortName = "write-global-double", description = "Writes double to heap")
    @NodeChild(value = "valueNode", type = MinicDoubleNode.class)
    public abstract static class MinicDoubleWriteGlobalNode extends MinicWriteGlobalNode {
        @Specialization
        protected void writeDouble(double value) {
            getGlobalFrame().getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Double);
            getGlobalFrame().setDouble(getSlot(), value);
        }
    }

    @NodeInfo(shortName = "write-global-string", description = "Writes string to heap")
    @NodeChild(value = "valueNode", type = MinicStringNode.class)
    public abstract static class MinicStringWriteGlobalNode extends MinicWriteGlobalNode {
        @Specialization
        protected void writeString(String value) {
            getGlobalFrame().getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            getGlobalFrame().setObject(getSlot(), value);
        }
    }
}
