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
import at.fh.hagenberg.aist.gce.minic.nodes.util.MinicFrameUtil;
import at.fh.hagenberg.aist.gce.minic.types.complex.*;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 *  Write for arrays on heap
 * Created by Oliver Krauss on 19.07.2016.
 */
@NodeInfo(shortName = "write-array-global", description = "Abstract base class for all nodes writing array datatypes to the heap")
@NodeFields({
    @NodeField(name = "slot", type = FrameSlot.class),
    @NodeField(name = "globalFrame", type = MaterializedFrame.class)
})
public abstract class MinicWriteGlobalArrayNode extends MinicNode {

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

    @NodeInfo(shortName = "write-array-global-char", description = "Writes char-array to heap")
    @NodeChild(value = "value", type = MinicCharNode.class)
    public abstract static class MinicCharArrayWriteGlobalNode extends MinicWriteGlobalArrayNode {

        /**
         * Position that will be written to
         */
        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicCharArrayWriteGlobalNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        public void writeChar(VirtualFrame frame, char value) {
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicCharArray array = (MinicCharArray) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            array.setAtPos(evaluatedPosition, value);
        }
    }

    @NodeInfo(shortName = "write-array-global-int", description = "Writes int-array to heap")
    @NodeChild(value = "value", type = MinicIntNode.class)
    public abstract static class MinicIntArrayWriteGlobalNode extends MinicWriteGlobalArrayNode {

        /**
         * Position that will be written to
         */
        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicIntArrayWriteGlobalNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        public void writeInt(VirtualFrame frame, int value) {
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicIntArray array = (MinicIntArray) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            array.setAtPos(evaluatedPosition, value);
        }
    }

    @NodeInfo(shortName = "write-array-global-float", description = "Writes float-array to heap")
    @NodeChild(value = "value", type = MinicFloatNode.class)
    public abstract static class MinicFloatArrayWriteGlobalNode extends MinicWriteGlobalArrayNode {

        /**
         * Position that will be written to
         */
        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicFloatArrayWriteGlobalNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        public void writeFloat(VirtualFrame frame, float value) {
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicFloatArray array = (MinicFloatArray) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            array.setAtPos(evaluatedPosition, value);
        }
    }

    @NodeInfo(shortName = "write-array-global-double", description = "Writes double-array to heap")
    @NodeChild(value = "value", type = MinicDoubleNode.class)
    public abstract static class MinicDoubleArrayWriteGlobalNode extends MinicWriteGlobalArrayNode {

        /**
         * Position that will be written to
         */
        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicDoubleArrayWriteGlobalNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        public void writeDouble(VirtualFrame frame, double value) {
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicDoubleArray array = (MinicDoubleArray) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            array.setAtPos(evaluatedPosition, value);
        }
    }

    @NodeInfo(shortName = "write-array-global-string", description = "Writes string-array to heap")
    @NodeChild(value = "value", type = MinicStringNode.class)
    public abstract static class MinicStringArrayWriteGlobalNode extends MinicWriteGlobalArrayNode {

        /**
         * Position that will be written to
         */
        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicStringArrayWriteGlobalNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        public void writeString(VirtualFrame frame, String value) {
            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicStringArray array = (MinicStringArray) MinicFrameUtil.getArray(getGlobalFrame(), getSlot());
            array.setAtPos(evaluatedPosition, value);
        }
    }

    @NodeInfo(shortName = "write-array-global-stringaschar", description = "Writes char to position in string on heap")
    @NodeChildren({
        @NodeChild(value = "value", type = MinicCharNode.class),
        @NodeChild(value = "evaluatedPositionNode", type = MinicIntNode.class)
    })
    public abstract static class MinicStringAsCharArrayWriteGlobalNode extends MinicWriteGlobalArrayNode {

        @Specialization
        public void writeChar(VirtualFrame frame, char value, int evaluatedPosition) {
            String s = MinicFrameUtil.getString(getGlobalFrame(), getSlot());
            StringBuilder sBuilder = new StringBuilder(s);
            sBuilder.setCharAt(evaluatedPosition, value);
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            getGlobalFrame().setObject(getSlot(), sBuilder.toString());
        }
    }
}
