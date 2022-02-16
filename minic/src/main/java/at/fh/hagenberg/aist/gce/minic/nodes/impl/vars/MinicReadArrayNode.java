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
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class for handling array read operations on stack.
 * The read operations actually propagate upwards in scope so both local and global variable access is handled.
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "read-array-local", description = "Abstract container class for all nodes reading array datatypes from the stack")
public abstract class MinicReadArrayNode extends MinicExpressionNode {

    @NodeInfo(shortName = "read-array-local-char", description = "Reads char-array from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicCharArrayReadNode extends MinicCharNode {
        protected abstract FrameSlot getSlot();

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicCharArrayReadNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected char readChar(VirtualFrame frame) {
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            return MinicFrameUtil.getChar(frame, getSlot(), evaluatedPosition);
        }
    }

    @NodeInfo(shortName = "read-array-local-int", description = "Reads int-array from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicIntArrayReadNode extends MinicIntNode {
        protected abstract FrameSlot getSlot();

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicIntArrayReadNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected int readInt(VirtualFrame frame) {
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            return MinicFrameUtil.getInt(frame, getSlot(), evaluatedPosition);
        }
    }

    @NodeInfo(shortName = "read-array-local-float", description = "Reads float-array from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicFloatArrayReadNode extends MinicFloatNode {
        protected abstract FrameSlot getSlot();

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicFloatArrayReadNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected float readFloat(VirtualFrame frame) {
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            return MinicFrameUtil.getFloat(frame, getSlot(), evaluatedPosition);
        }
    }

    @NodeInfo(shortName = "read-array-local-double", description = "Reads double-array from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicDoubleArrayReadNode extends MinicDoubleNode {
        protected abstract FrameSlot getSlot();

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicDoubleArrayReadNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected double readDouble(VirtualFrame frame) {
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            return MinicFrameUtil.getDouble(frame, getSlot(), evaluatedPosition);
        }
    }

    @NodeInfo(shortName = "read-array-local-string", description = "Reads string-array from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicStringArrayReadNode extends MinicStringNode {
        protected abstract FrameSlot getSlot();

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicStringArrayReadNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected String readString(VirtualFrame frame) {
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            return MinicFrameUtil.getString(frame, getSlot(), evaluatedPosition);
        }
    }

    @NodeInfo(shortName = "read-array-local-stringaschar", description = "Reads string as char-array from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    @NodeChild(value = "position", type = MinicIntNode.class)
    public abstract static class MinicStringAsCharArrayReadNode extends MinicCharNode {
        protected abstract FrameSlot getSlot();


        @Specialization
        protected char readString(VirtualFrame frame, int position) {
            return MinicFrameUtil.getString(frame, getSlot()).charAt(position);
        }
    }

    @NodeInfo(shortName = "read-array-local-char", description = "Returns entire array from stack")
    @NodeField(name = "slot", type = FrameSlot.class)
    public abstract static class MinicEntireArrayReadNode extends MinicExpressionNode {
        protected abstract FrameSlot getSlot();

        @Specialization
        protected Object readObject(VirtualFrame frame) {
            return MinicFrameUtil.getArray(frame, getSlot());
        }
    }
}
