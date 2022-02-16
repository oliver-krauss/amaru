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
import at.fh.hagenberg.aist.gce.minic.types.complex.*;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Write for arrays on stack
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "write-array-local", description = "Abstract base class for all nodes writing array datatypes to the stack")
@NodeField(name = "slot", type = FrameSlot.class)
public abstract class MinicWriteArrayNode extends MinicNode {

    /**
     * Loads the frame slot that is being written to.
     * @return slot for writing
     */
    protected abstract FrameSlot getSlot();

    @NodeInfo(shortName = "write-array-local-char", description = "Writes char-array to stack")
    @NodeChild(value = "valueNode", type = MinicCharNode.class)
    public abstract static class MinicCharArrayWriteNode extends MinicWriteArrayNode {

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicCharArrayWriteNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected void writeChar(VirtualFrame frame, char value) {

            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicCharArray array = (MinicCharArray) FrameUtil.getObjectSafe(frame, getSlot());
            array.setAtPos(evaluatedPosition, value);
        }
    }

    @NodeInfo(shortName = "write-array-local-int", description = "Writes int-array to stack")
    @NodeChild(value = "valueNode", type = MinicIntNode.class)
    public abstract static class MinicIntArrayWriteNode extends MinicWriteArrayNode {

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicIntArrayWriteNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected void writeInt(VirtualFrame frame, int value) {

            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = arrayPosition[i].executeInt(frame);
            }
            MinicIntArray array = (MinicIntArray) FrameUtil.getObjectSafe(frame, getSlot());
            array.setAtPos(evaluatedPosition, value);
        }
    }

    @NodeInfo(shortName = "write-array-local-float", description = "Writes float-array to stack")
    @NodeChild(value = "valueNode", type = MinicFloatNode.class)
    public abstract static class MinicFloatArrayWriteNode extends MinicWriteArrayNode {

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicFloatArrayWriteNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected void writeFloat(VirtualFrame frame, float value) {

            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicFloatArray array = (MinicFloatArray) FrameUtil.getObjectSafe(frame, getSlot());
            array.setAtPos(evaluatedPosition, value);
        }
    }

    @NodeInfo(shortName = "write-array-local-double", description = "Writes double-array to stack")
    @NodeChild(value = "valueNode", type = MinicDoubleNode.class)
    public abstract static class MinicDoubleArrayWriteNode extends MinicWriteArrayNode {

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicDoubleArrayWriteNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected void writeDouble(VirtualFrame frame, double value) {

            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicDoubleArray array = (MinicDoubleArray) FrameUtil.getObjectSafe(frame, getSlot());
            array.setAtPos(evaluatedPosition, value);
        }
    }

    @NodeInfo(shortName = "write-array-local-string", description = "Writes string-array to stack")
    @NodeChild(value = "valueNode", type = MinicStringNode.class)
    public abstract static class MinicStringArrayWriteNode extends MinicWriteArrayNode {

        @Children
        private final MinicIntNode[] arrayPosition;

        public MinicStringArrayWriteNode(MinicIntNode[] arrayPosition) {
            this.arrayPosition = arrayPosition;
        }

        @Specialization
        @ExplodeLoop
        protected void writeString(VirtualFrame frame, String value) {

            CompilerAsserts.partialEvaluationConstant(arrayPosition.length);
            int[] evaluatedPosition = new int[arrayPosition.length];
            for (int i = 0; i < arrayPosition.length; i++) {
                evaluatedPosition[i] = (int) arrayPosition[i].executeGeneric(frame);
            }
            MinicStringArray array = (MinicStringArray) FrameUtil.getObjectSafe(frame, getSlot());
            array.setAtPos(evaluatedPosition, value);
        }
    }

    @NodeInfo(shortName = "write-array-local-stringaschar", description = "Writes string as char-array to stack")
    @NodeChildren({
        @NodeChild(value = "valueNode", type = MinicCharNode.class),
        @NodeChild(value = "arrayPositionNode", type = MinicIntNode.class)
    })
    public abstract static class MinicStringAsCharArrayWriteNode extends MinicWriteArrayNode {

        @Specialization
        protected void writeString(VirtualFrame frame, char value, int arrayPosition) {
            String string = (String) FrameUtil.getObjectSafe(frame, getSlot());
            StringBuilder sBuilder = new StringBuilder(string);
            sBuilder.setCharAt(arrayPosition, value);
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            frame.setObject(getSlot(), sBuilder.toString());
        }
    }
}
