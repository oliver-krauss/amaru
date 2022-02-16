/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.function;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Responsible for reading the arguments of a function
 * Created by Oliver Krauss on 16.06.2016.
 */
@NodeInfo(shortName = "read-arg", description = "Container for reading from function argument")
public abstract class MinicReadFunctionArgumentNode {

    @NodeInfo(shortName = "read-arg-char", description = "Reads char from argument")
    @NodeField(name = "index", type = int.class)
    public abstract static class MinicReadFunctionArgumentCharNode extends MinicCharNode {

        /**
         * Index of argument to be returned
         *
         * @return index
         */
        public abstract int getIndex();

        @Specialization
        public char readChar(VirtualFrame frame) {
            return (char) frame.getArguments()[getIndex()];
        }
    }

    @NodeInfo(shortName = "read-arg-int", description = "Reads int from argument")
    @NodeField(name = "index", type = int.class)
    public abstract static class MinicReadFunctionArgumentIntNode extends MinicIntNode {

        /**
         * Index of argument to be returned
         *
         * @return index
         */
        public abstract int getIndex();

        @Specialization
        public int readInt(VirtualFrame frame) {
            return (int) frame.getArguments()[getIndex()];
        }
    }

    @NodeInfo(shortName = "read-arg-float", description = "Reads float from argument")
    @NodeField(name = "index", type = int.class)
    public abstract static class MinicReadFunctionArgumentFloatNode extends MinicFloatNode {

        /**
         * Index of argument to be returned
         *
         * @return index
         */
        public abstract int getIndex();

        @Specialization
        public float readFloat(VirtualFrame frame) {
            return (float) frame.getArguments()[getIndex()];
        }
    }

    @NodeInfo(shortName = "read-arg-double", description = "Reads double from argument")
    @NodeField(name = "index", type = int.class)
    public abstract static class MinicReadFunctionArgumentDoubleNode extends MinicDoubleNode {

        /**
         * Index of argument to be returned
         *
         * @return index
         */
        public abstract int getIndex();

        @Specialization
        public double readDouble(VirtualFrame frame) {
            return (double) frame.getArguments()[getIndex()];
        }
    }

    @NodeInfo(shortName = "read-arg-string", description = "Reads string from argument")
    @NodeField(name = "index", type = int.class)
    public abstract static class MinicReadFunctionArgumentStringNode extends MinicStringNode {

        /**
         * Index of argument to be returned
         *
         * @return index
         */
        public abstract int getIndex();

        @Specialization
        public String readFloat(VirtualFrame frame) {
            return (String) frame.getArguments()[getIndex()];
        }
    }

    /**
     * Generic implementation for builtins
     */
    @NodeInfo(shortName = "read-arg-generic", description = "Generic read from argument")
    @NodeField(name = "index", type = int.class)
    public abstract static class MinicReadGenericFunctionArgumentNode extends MinicExpressionNode {

        /**
         * Index of argument to be returned
         *
         * @return index
         */
        public abstract int getIndex();

        @Specialization
        public Object readGeneric(VirtualFrame frame) {
            return frame.getArguments()[getIndex()];
        }
    }

}
