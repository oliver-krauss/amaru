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
import at.fh.hagenberg.aist.gce.minic.types.complex.*;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.NodeFields;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.graalvm.nativeimage.c.function.CEntryPoint;

/**
 * Global Array Allocator
 * Created by Oliver Krauss on 27.07.2016.
 */
@NodeInfo(shortName = "allocate-array-global", description = "Abstract base class for all nodes creating arrays on the heap")
@NodeFields({
    @NodeField(name = "slot", type = FrameSlot.class),
    @NodeField(name = "globalFrame", type = MaterializedFrame.class)
})
public abstract class AllocateGlobalArrayNode extends MinicNode {

    /**
     * Loads the frame slot that is being written to.
     *
     * @return slot for writing
     */
    protected abstract FrameSlot getSlot();

    /**
     * Loads the heap
     *
     * @return heap
     */
    protected abstract MaterializedFrame getGlobalFrame();

    @NodeInfo(shortName = "allocate-array-global-char", description = "Allocates char array on the heap")
    public abstract static class MinicAllocateGlobalCharArrayNode extends AllocateGlobalArrayNode {

        /**
         * Size of array to be generated (multi-size possible)
         */
        @Children
        private final MinicIntNode[] size;

        public MinicAllocateGlobalCharArrayNode(MinicIntNode[] sizeNodes) {
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
            getGlobalFrame().getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            getGlobalFrame().setObject(getSlot(), new MinicCharArray(evaluatedSize));
        }
    }


    @NodeInfo(shortName = "allocate-array-global-int", description = "Allocates int array on the heap")
    public abstract static class MinicAllocateGlobalIntArrayNode extends AllocateGlobalArrayNode {

        /**
         * Size of array to be generated (multi-size possible)
         */
        @Children
        private final MinicIntNode[] size;

        public MinicAllocateGlobalIntArrayNode(MinicIntNode[] sizeNodes) {
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
            getGlobalFrame().getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            getGlobalFrame().setObject(getSlot(), new MinicIntArray(evaluatedSize));
        }
    }

    @NodeInfo(shortName = "allocate-array-global-float", description = "Allocates float array on the heap")
    public abstract static class MinicAllocateGlobalFloatArrayNode extends AllocateGlobalArrayNode {

        /**
         * Size of array to be generated (multi-size possible)
         */
        @Children
        private final MinicIntNode[] size;

        public MinicAllocateGlobalFloatArrayNode(MinicIntNode[] sizeNodes) {
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
            getGlobalFrame().getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            getGlobalFrame().setObject(getSlot(), new MinicFloatArray(evaluatedSize));
        }
    }

    @NodeInfo(shortName = "allocate-array-global-double", description = "Allocates double array on the heap")
    public abstract static class MinicAllocateGlobalDoubleArrayNode extends AllocateGlobalArrayNode {

        /**
         * Size of array to be generated (multi-size possible)
         */
        @Children
        private final MinicIntNode[] size;

        public MinicAllocateGlobalDoubleArrayNode(MinicIntNode[] sizeNodes) {
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
            getGlobalFrame().getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            getGlobalFrame().setObject(getSlot(), new MinicDoubleArray(evaluatedSize));
        }
    }

    @NodeInfo(shortName = "allocate-array-global-string", description = "Allocates string array on the heap")
    public abstract static class MinicAllocateGlobalStringArrayNode extends AllocateGlobalArrayNode {

        /**
         * Size of array to be generated (multi-size possible)
         */
        @Children
        private final MinicIntNode[] size;

        public MinicAllocateGlobalStringArrayNode(MinicIntNode[] sizeNodes) {
            this.size = sizeNodes;
        }

        @Specialization
        @ExplodeLoop
        protected void allocateString(VirtualFrame frame) {
            int[] evaluatedSize = new int[size.length];
            for (int i = 0; i < size.length; i++) {
                evaluatedSize[i] = (int) size[i].executeGeneric(frame);
            }
            getGlobalFrame().getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
            getGlobalFrame().setObject(getSlot(), new MinicStringArray(evaluatedSize));
        }
    }
}
