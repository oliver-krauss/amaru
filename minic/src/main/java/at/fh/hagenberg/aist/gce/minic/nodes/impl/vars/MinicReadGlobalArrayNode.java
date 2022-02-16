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
import at.fh.hagenberg.aist.gce.minic.types.complex.*;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 *  Class for handling array read operations on heap.
 * The read operations actually propagate upwards in scope so both local and global variable access is handled.
 * Created by Oliver Krauss on 27.07.2016.
 */
@NodeInfo(shortName = "read-array-global", description = "Abstract container class for all nodes reading array datatypes from the heap")
public abstract class MinicReadGlobalArrayNode extends MinicExpressionNode {

    @NodeInfo(shortName = "read-array-global-char", description = "Reads char-array from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicCharArrayReadGlobalNode extends MinicCharNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicCharArrayReadGlobalNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected char readChar(VirtualFrame frame) {
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicCharArray array = (MinicCharArray) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            return array.getAtPos(evaluatedPosition);
        }
    }

    @NodeInfo(shortName = "read-array-global-int", description = "Reads int-array from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicIntArrayReadGlobalNode extends MinicIntNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicIntArrayReadGlobalNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected int readInt(VirtualFrame frame) {
            int[] evaluatedPosition = new int[arrayPosition.length];
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            CompilerAsserts.partialEvaluationConstant(evaluatedPosition.length);
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicIntArray array = (MinicIntArray) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            return array.getAtPos(evaluatedPosition);
        }
    }

    @NodeInfo(shortName = "read-array-global-float", description = "Reads float-array from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicFloatArrayReadGlobalNode extends MinicFloatNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicFloatArrayReadGlobalNode(MinicIntNode[] arrayPosition) {
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
            MinicFloatArray array = (MinicFloatArray) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            return array.getAtPos(evaluatedPosition);
        }
    }

    @NodeInfo(shortName = "read-array-global-double", description = "Reads double-array from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicDoubleArrayReadGlobalNode extends MinicDoubleNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicDoubleArrayReadGlobalNode(MinicIntNode[] arrayPosition) {
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
            MinicDoubleArray array = (MinicDoubleArray) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            return array.getAtPos(evaluatedPosition);
        }
    }

    @NodeInfo(shortName = "read-array-global-string", description = "Reads string-array from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicStringArrayReadGlobalNode extends MinicStringNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicStringArrayReadGlobalNode(MinicIntNode[] arrayPosition) {
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
            MinicStringArray array = (MinicStringArray) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            return array.getAtPos(evaluatedPosition);
        }
    }

    @NodeInfo(shortName = "read-array-global-string", description = "Reads string-array from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    @NodeChild(value = "position", type = MinicIntNode.class)
    public abstract static class MinicStringAsCharArrayReadGlobalNode extends MinicCharNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Specialization
        protected char readChar(VirtualFrame frame, int position) {
            String array = (String) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            return array.charAt(position);
        }
    }

    @NodeInfo(shortName = "read-array-global", description = "Reads and returns entire array from heap")
    @NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "globalFrame", type = MaterializedFrame.class)
    })
    public abstract static class MinicEntireArrayReadGlobalNode extends MinicExpressionNode {

        protected abstract FrameSlot getSlot();

        protected abstract MaterializedFrame getGlobalFrame();

        @Specialization
        protected Object readObject(VirtualFrame frame) {
            return MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
        }
    }
}
