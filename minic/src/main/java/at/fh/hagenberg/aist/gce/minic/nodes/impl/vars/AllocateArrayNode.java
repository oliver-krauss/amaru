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
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Array Allocator
 * Created by Oliver Krauss on 27.07.2016.
 */
@NodeInfo(shortName = "allocate-array-local", description = "Abstract base class for all nodes creating arrays on the stack")
@NodeField(name = "slot", type = FrameSlot.class)
public abstract class AllocateArrayNode extends MinicNode {

    /**
     * Loads the frame slot that is being written to.
     *
     * @return slot for writing
     */
    public abstract FrameSlot getSlot();

    @NodeInfo(shortName = "allocate-array-local-char", description = "Allocates char array on the stack")
    public abstract static class MinicAllocateCharArrayNode extends AllocateArrayNode {

        /**
         * Size of array to be generated (multi-size possible)
         */
        @Children
        private final MinicIntNode[] size;

        public MinicAllocateCharArrayNode(MinicIntNode[] sizeNodes) {
            this.size = sizeNodes;
        }

        @Specialization
        @ExplodeLoop
        protected void allocateChar(VirtualFrame frame) {
            CompilerAsserts.partialEvaluationConstant(size.length);
            int[] evaluatedSize = new int[size.length];
            for (int i = 0; i < size.length; i++) {
                evaluatedSize[i] = (int) size[i].executeGeneric(frame);
            }
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            frame.setObject(getSlot(), new MinicCharArray(evaluatedSize));
        }
    }

    @NodeInfo(shortName = "allocate-array-local-int", description = "Allocates int array on the stack")
    public abstract static class MinicAllocateIntArrayNode extends AllocateArrayNode {

        /**
         * Size of array to be generated (multi-size possible)
         */
        @Children
        private final MinicIntNode[] size;

        public MinicAllocateIntArrayNode(MinicIntNode[] sizeNodes) {
            this.size = sizeNodes;
        }

        @Specialization
        @ExplodeLoop
        protected void allocateInt(VirtualFrame frame) {
            CompilerAsserts.partialEvaluationConstant(size.length);
            int[] evaluatedSize = new int[size.length];
            for (int i = 0; i < size.length; i++) {
                evaluatedSize[i] = (int) size[i].executeGeneric(frame);
            }
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            frame.setObject(getSlot(), new MinicIntArray(evaluatedSize));
        }
    }

    @NodeInfo(shortName = "allocate-array-local-float", description = "Allocates float array on the stack")
    public abstract static class MinicAllocateFloatArrayNode extends AllocateArrayNode {

        /**
         * Size of array to be generated (multi-size possible)
         */
        @Children
        private final MinicIntNode[] size;

        public MinicAllocateFloatArrayNode(MinicIntNode[] sizeNodes) {
            this.size = sizeNodes;
        }

        @Specialization
        @ExplodeLoop
        protected void allocateFloat(VirtualFrame frame) {
            CompilerAsserts.partialEvaluationConstant(size.length);
            int[] evaluatedSize = new int[size.length];
            for (int i = 0; i < size.length; i++) {
                evaluatedSize[i] = (int) size[i].executeGeneric(frame);
            }
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            frame.setObject(getSlot(), new MinicFloatArray(evaluatedSize));
        }
    }

    @NodeInfo(shortName = "allocate-array-local-double", description = "Allocates double array on the stack")
    public abstract static class MinicAllocateDoubleArrayNode extends AllocateArrayNode {

        /**
         * Size of array to be generated (multi-size possible)
         */
        @Children
        private final MinicIntNode[] size;

        public MinicAllocateDoubleArrayNode(MinicIntNode[] sizeNodes) {
            this.size = sizeNodes;
        }

        @Specialization
        @ExplodeLoop
        protected void allocateDouble(VirtualFrame frame) {
            CompilerAsserts.partialEvaluationConstant(size.length);
            int[] evaluatedSize = new int[size.length];
            for (int i = 0; i < size.length; i++) {
                evaluatedSize[i] = (int) size[i].executeGeneric(frame);
            }
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            frame.setObject(getSlot(), new MinicDoubleArray(evaluatedSize));
        }
    }

    @NodeInfo(shortName = "allocate-array-local-string", description = "Allocates string array on the stack")
    public abstract static class MinicAllocateStringArrayNode extends AllocateArrayNode {

        /**
         * Size of array to be generated (multi-size possible)
         */
        @Children
        private final MinicIntNode[] size;

        public MinicAllocateStringArrayNode(MinicIntNode[] sizeNodes) {
            this.size = sizeNodes;
        }

        @Specialization
        @ExplodeLoop
        protected void allocateString(VirtualFrame frame) {
            CompilerAsserts.partialEvaluationConstant(size.length);
            int[] evaluatedSize = new int[size.length];
            for (int i = 0; i < size.length; i++) {
                evaluatedSize[i] = (int) size[i].executeGeneric(frame);
            }
            frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            frame.setObject(getSlot(), new MinicStringArray(evaluatedSize));
        }
    }
}
