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
import at.fh.hagenberg.aist.gce.minic.nodes.util.MinicFrameUtil;
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class for handling variable read operations on stack.
 * The read operations propagate upwards in scope so both local and global variable access is handled.
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "read-local", description = "Container class for all nodes reading simple datatypes from the stack")
public abstract class MinicReadNode extends MinicExpressionNode {

    @NodeInfo(shortName = "read-local-char", description = "Reads char from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicCharReadNode extends MinicCharNode {
        protected abstract FrameSlot getSlot();

        @Specialization
        protected char readChar(VirtualFrame frame) {
            return MinicFrameUtil.getChar(frame, getSlot());
        }
    }

    @NodeInfo(shortName = "read-local-int", description = "Reads int from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicIntReadNode extends MinicIntNode {
        protected abstract FrameSlot getSlot();

        @Specialization
        protected int readInt(VirtualFrame frame) {
            return MinicFrameUtil.getInt(frame, getSlot());
        }
    }

    @NodeInfo(shortName = "read-local-float", description = "Reads float from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicFloatReadNode extends MinicFloatNode {
        protected abstract FrameSlot getSlot();

        @Specialization
        protected float readFloat(VirtualFrame frame) {
            return MinicFrameUtil.getFloat(frame, getSlot());
        }
    }

    @NodeInfo(shortName = "read-local-double", description = "Reads double from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicDoubleReadNode extends MinicDoubleNode {
        protected abstract FrameSlot getSlot();

        @Specialization
        protected double readDouble(VirtualFrame frame) {
            return MinicFrameUtil.getDouble(frame, getSlot());
        }
    }

    @NodeInfo(shortName = "read-local-string", description = "Reads string from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicStringReadNode extends MinicStringNode {
        protected abstract FrameSlot getSlot();

        @Specialization
        protected String readString(VirtualFrame frame) {
            return MinicFrameUtil.getString(frame, getSlot());
        }
    }
}
