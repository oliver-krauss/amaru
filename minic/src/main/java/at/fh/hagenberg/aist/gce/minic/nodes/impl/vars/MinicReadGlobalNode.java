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
import com.oracle.truffle.api.dsl.NodeFields;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 *  Class for handling variable read operations on heap.
 * The read operations actually propagate upwards in scope so both local and global variable access is handled.
 * Created by Oliver Krauss on 19.07.2016.
 */
@NodeInfo(shortName = "read-global", description = "Abstract base class for all nodes reading simple datatypes from the heap")
public abstract class MinicReadGlobalNode extends MinicExpressionNode {

    @NodeInfo(shortName = "read-global-char", description = "Reads char from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicCharReadGlobalNode extends MinicCharNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Specialization
        protected char readChar() {
            return MinicFrameUtil.getChar(getGlobalFrame(), getSlot());
        }
    }

    @NodeInfo(shortName = "read-global-int", description = "Reads int from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicIntReadGlobalNode extends MinicIntNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Specialization
        protected int readInt() {
            return MinicFrameUtil.getInt(getGlobalFrame(), getSlot());
        }
    }

    @NodeInfo(shortName = "read-global-float", description = "Reads float from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicFloatReadGlobalNode extends MinicFloatNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Specialization
        protected float readFloat() {
            return MinicFrameUtil.getFloat(getGlobalFrame(), getSlot());
        }
    }

    @NodeInfo(shortName = "read-global-double", description = "Reads double from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicDoubleReadGlobalNode extends MinicDoubleNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Specialization
        protected double readDouble() {
            return MinicFrameUtil.getDouble(getGlobalFrame(), getSlot());
        }
    }

    @NodeInfo(shortName = "read-global-string", description = "Reads string from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicStringReadGlobalNode extends MinicStringNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Specialization
        protected String readString() {
            return MinicFrameUtil.getString(getGlobalFrame(), getSlot());
        }
    }

}
