/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.literals;

import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Classes for literals of any "simple" kind, meaning not an array or struct
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "literals", description = "Literal values in the language")
public class MinicSimpleLiteralNode {

    @NodeInfo(shortName = "CharLiteral", description = "char")
    public static class MinicCharLiteralNode extends MinicCharNode {

        private final char value;

        public MinicCharLiteralNode(char value) {
            this.value = value;
        }

        @Override
        public char executeChar(VirtualFrame frame) {
            return value;
        }
    }

    @NodeInfo(shortName = "IntLiteral", description = "int")
    public static class MinicIntLiteralNode extends MinicIntNode {

        private final int value;

        public MinicIntLiteralNode(int value) {
            this.value = value;
        }

        @Override
        public int executeInt(VirtualFrame frame) {
            return value;
        }
    }

    @NodeInfo(shortName = "FloatLiteral", description = "float")
    public static class MinicFloatLiteralNode extends MinicFloatNode {

        private final float value;

        public MinicFloatLiteralNode(float value) {
            this.value = value;
        }

        @Override
        public float executeFloat(VirtualFrame frame) {
            return value;
        }
    }

    @NodeInfo(shortName = "DoubleLiteral", description = "double")
    public static class MinicDoubleLiteralNode extends MinicDoubleNode {

        private final double value;

        public MinicDoubleLiteralNode(double value) {
            this.value = value;
        }

        @Override
        public double executeDouble(VirtualFrame frame) {
            return value;
        }
    }

    @NodeInfo(shortName = "StringLiteral", description = "string")
    public static class MinicStringLiteralNode extends MinicStringNode {

        private final String value;

        public MinicStringLiteralNode(String value) {
            this.value = value;
        }

        @Override
        public String executeString(VirtualFrame frame) {
            return value;
        }
    }

}
